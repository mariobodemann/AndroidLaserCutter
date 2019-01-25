package net.karmacoder.alc

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.text.Html
import android.util.Log
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

class MainActivity : AppCompatActivity() {
    private var scaledBitmap: Bitmap? = null
    private var laserCutters = arrayOf(
        EpilogZing(),
        EpilogHelix(),
        LaosCutter(),
        Lasersaur(),
        IModelaMill()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        main_search_button.setOnClickListener { browse() }

        setupLasers()

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

        val (dpi: Double, property: LaserProperty) = propertyFromUser()
        val job = createJob(dpi, property)

        val ip = main_ip.text.toString()
        GlobalScope.launch {
            val cutter = EpilogZing(ip)

            try {
                cutter.sendJob(job)

                runOnUiThread {
                    info("Successfully sentToLaserCutter!")
                }
            } catch (e: Exception) {
                val exceptionAsString = e.stackTraceToString()

                runOnUiThread {
                    val message = "Could not print on '${cutter.javaClass.simpleName}'."
                    Log.e(TAG, message, e)
                    error("<i>$message</i><br><br>$exceptionAsString".toHtml())
                }
            }
        }
    }

    private fun propertyFromUser(): Pair<Double, LaserProperty> {
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

    private fun createJob(dpi: Double, property: LaserProperty): LaserJob {
        val vp = VectorPart(property, dpi)
        vp.moveto(0, 0)
        vp.lineto(scaledBitmap!!.width, 0)
        vp.lineto(scaledBitmap!!.width, scaledBitmap!!.height)
        vp.lineto(0, scaledBitmap!!.height)
        vp.lineto(0, 0)

        val job = LaserJob("PhotoPrint", "Android", "Mom")
        job.addPart(
            Raster3dPart(
                scaledBitmap!!.toGreyscaleRaster(),
                property,
                Point(0, 0),
                dpi
            )
        )
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
