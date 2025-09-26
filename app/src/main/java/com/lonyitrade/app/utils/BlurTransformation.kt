package com.lonyitrade.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import java.security.MessageDigest

/**
 * A Glide BitmapTransformation to apply a Gaussian Blur effect to an image.
 * Uses RenderScript for fast and efficient blurring (recommended Android practice).
 *
 * NOTE: For RenderScript to work, ensure the necessary configuration is in your build.gradle:
 * android { defaultConfig { ... renderscriptSupportModeEnabled = true } }
 */
class BlurTransformation(context: Context, private val blurRadius: Float = 20f) : BitmapTransformation() {

    private val rs: RenderScript = RenderScript.create(context.applicationContext)
    private val ID = "com.lonyitrade.app.utils.BlurTransformation" + blurRadius.toInt()
    private val ID_BYTES = ID.toByteArray(CHARSET)

    init {
        // Ensure blur radius is within the allowed RenderScript range [0, 25]
        require(blurRadius > 0 && blurRadius <= 25) { "Radius must be between 0 and 25" }
    }

    override fun transform(
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        // Create a scaled down version for faster blur processing
        val scaleFactor = 4.0f
        val downscaledWidth = (toTransform.width / scaleFactor).toInt()
        val downscaledHeight = (toTransform.height / scaleFactor).toInt()

        // Get a temporary bitmap from the pool for scaling/blurring
        val scaledBitmap = pool.get(downscaledWidth, downscaledHeight, Bitmap.Config.ARGB_8888)

        // Draw the original bitmap onto the scaled bitmap to perform downsampling
        val canvas = Canvas(scaledBitmap)
        canvas.scale(1f / scaleFactor, 1f / scaleFactor)
        val paint = Paint()
        paint.flags = Paint.FILTER_BITMAP_FLAG
        canvas.drawBitmap(toTransform, 0f, 0f, paint)

        // 1. Create Allocation from the scaled Bitmap
        val input = Allocation.createFromBitmap(rs, scaledBitmap, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT)

        // 2. Create the output Allocation
        val output = Allocation.createTyped(rs, input.type)

        // 3. Create the blur effect Script
        val script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
        script.setRadius(blurRadius) // Set the blur radius
        script.setInput(input)

        // 4. Run the blur effect and copy result to output
        script.forEach(output)
        output.copyTo(scaledBitmap)

        // Release temporary Allocations
        input.destroy()
        output.destroy()
        script.destroy()

        // Return the blurred bitmap (it's reused from the pool, so no need to return toPool)
        return scaledBitmap
    }

    override fun equals(other: Any?): Boolean {
        if (other is BlurTransformation) {
            return other.blurRadius == blurRadius
        }
        return false
    }

    override fun hashCode(): Int = ID.hashCode()

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(ID_BYTES)
    }
}