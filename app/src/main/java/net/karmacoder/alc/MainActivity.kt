package net.karmacoder.alc

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.green
import androidx.core.graphics.scale
import androidx.core.graphics.set
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.visicut.liblasercut.*
import org.visicut.liblasercut.drivers.*
import org.visicut.liblasercut.platform.Point
import org.visicut.liblasercut.platform.Util
import java.io.PrintWriter
import java.io.StringWriter

internal const val READ_REQUEST_CODE: Int = 12
internal const val TAG: String = "MainActivity"

data class Preset(val speed: Int, val power: Int, val frequency: Int)

class MainActivity : AppCompatActivity() {
    private var scaledBitmap: Bitmap? = null
    private var laserCutters = arrayOf(
        EpilogZing(),
        EpilogHelix(),
        LaosCutter(),
        Lasersaur(),
        IModelaMill()
    )

    private val presets = mapOf(
        "plywood" to Preset(100, 100, 2000),
        "wood" to Preset(50, 50, 2000),
        "acrylic" to Preset(25, 100, 2000),
        "custom" to Preset(10, 10, 2000)
    )

    private val sendJobListener = object : ProgressListener {
        override fun progressChanged(source: Any?, percent: Int) {
            Log.i(TAG, "$source at $percent %")

            main_sent_progress_bar.progress = percent
            main_sent_progress_text.text = "$percent %"
        }

        override fun taskChanged(source: Any?, taskName: String?) {
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        main_search_button.setOnClickListener { browse() }

        setupLasers()

        setupPresets()

        main_sent_button.setOnClickListener { sentToLaserCutter() }
    }

    private fun setupLasers() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            laserCutters.map { it.javaClass.simpleName }
        )

        main_laser_spinner.adapter = adapter
        main_laser_spinner.setSelection(0)
    }

    private fun setupPresets() {
        val keys = presets.keys.toTypedArray()
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            keys
        )

        main_preset_spinner.adapter = adapter
        main_preset_spinner.setSelection(0)

        main_preset_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val preset = presets[keys[position]]!!

                main_speed.setText(preset.speed.toString())
                main_power.setText(preset.power.toString())
                main_frequency.setText(preset.frequency.toString())
            }
        }
    }

    private fun browse() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }

        startActivityForResult(intent, READ_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            resultData?.data?.also { uri ->
                Log.i(TAG, "Uri: $uri")
                val parcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r")!!
                val fileDescriptor = parcelFileDescriptor.fileDescriptor
                val bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor)
                parcelFileDescriptor.close()

                val dpi = main_density.text.toString().toDouble()
                val width = main_width.text.toString().toInt()

                val oWidth = Util.mm2px(width.toDouble(), dpi).toInt()
                val oHeight = bitmap!!.height * oWidth / bitmap.width

                scaledBitmap = bitmap.scale(oWidth, oHeight).convertToGrayScale()

                main_image_preview.setImageBitmap(scaledBitmap)
            }
        }
    }

    private fun sentToLaserCutter() {
        if (scaledBitmap == null) {
            error("Bitmap cannot be null!")
            return
        }

        val (dpi: Double, property: PowerSpeedFocusFrequencyProperty) = propertyFromUser()
        val job = createJob(dpi, property)
        val ip = main_ip.text.toString()

        main_sent_progress_group.visibility = View.VISIBLE
        main_sent_progress_bar.progress = 0
        main_sent_progress_text.text = "0 %"

        main_sent_button.hide()

        GlobalScope.launch {
            val cutter = EpilogZing(ip)

            try {
                val warnings = mutableListOf<String>()
                cutter.sendJob(job, sendJobListener, warnings)

                runOnUiThread {
                    if (warnings.size > 0) {
                        val joinedWarnings = warnings.joinToString(
                            separator = "<br>",
                            prefix = "• "
                        )
                        info("Successfully sent <br><br>Warnings:<br>$joinedWarnings".toHtml())
                    } else {
                        info("Successfully sent")
                    }
                }
            } catch (e: Exception) {
                val exceptionAsString = e.stackTraceToString()

                runOnUiThread {
                    val message = "Could not print on '${cutter.javaClass.simpleName}'."
                    Log.e(TAG, message, e)
                    error("<i>$message</i><br><br>$exceptionAsString".toHtml())
                }
            } finally {
                runOnUiThread {
                    main_sent_button.show()
                    main_sent_progress_group.visibility = View.GONE
                }
            }
        }
    }

    private fun propertyFromUser(): Pair<Double, PowerSpeedFocusFrequencyProperty> {
        val dpi = main_density.text.toString().toDouble()
        val power = main_power.text.toString().toInt()
        val speed = main_speed.text.toString().toInt()
        val frequency = main_frequency.text.toString().toInt()

        val property = PowerSpeedFocusFrequencyProperty()
        property.power = power
        property.speed = speed
        property.frequency = frequency

        return Pair(dpi, property)
    }

    private fun createJob(dpi: Double, property: PowerSpeedFocusFrequencyProperty): LaserJob {
        val job = LaserJob(main_job_name.text.toString(), "ACL", "Mom")
        job.addPart(
            Raster3dPart(
                scaledBitmap!!.toGreyscaleRaster(),
                property,
                Point(0, 0),
                dpi
            )
        )

        val vp = VectorPart(PowerSpeedFocusFrequencyProperty().apply {
            power = 100; speed = 100; frequency = property.frequency
        }, dpi)

        vp.moveto(0, 0)
        vp.lineto(scaledBitmap!!.width, 0)
        vp.lineto(scaledBitmap!!.width, scaledBitmap!!.height)
        vp.lineto(0, scaledBitmap!!.height)
        vp.lineto(0, 0)

        job.addPart(vp)

        return job
    }

    private fun error(message: CharSequence) {
        message("⚠️ An Error occurred ⚠️", message)
    }

    private fun info(message: CharSequence) {
        message("Information", message)
    }

    private fun message(title: CharSequence, text: CharSequence) {
        AlertDialog
            .Builder(this)
            .setTitle(title)
            .setMessage(text)
            .show()
    }
}

fun Bitmap.toGreyscaleRaster() = object : GreyscaleRaster {
    override fun getWidth(): Int = this@toGreyscaleRaster.width

    override fun getHeight(): Int = this@toGreyscaleRaster.height

    override fun getGreyScale(x: Int, y: Int): Int = this@toGreyscaleRaster.getPixel(x, y).green

    override fun setGreyScale(x: Int, y: Int, grey: Int) {
        this@toGreyscaleRaster.set(x, y, Color.rgb(grey, grey, grey))
    }
}

fun Bitmap.convertToGrayScale(): Bitmap {
    val result = this.copy(config, true)

    val pixels = IntArray(width * height)
    result.getPixels(pixels, 0, width, 0, 0, width, height);

    pixels.forEachIndexed { index, color ->
        val l = Color.valueOf(color).luminance()
        pixels[index] = Color.rgb(l, l, l)
    }

    result.setPixels(pixels, 0, width, 0, 0, width, height);

    return result
}

internal fun String.toHtml() = Html.fromHtml(this, 0)

internal fun Throwable.stackTraceToString(): String {
    val sw = StringWriter()
    printStackTrace(PrintWriter(sw))
    return sw.toString()
}
