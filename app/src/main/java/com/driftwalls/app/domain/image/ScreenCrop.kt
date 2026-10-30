package com.driftwalls.app.domain.image

/** A source rectangle in pixels: [left, top) to [right, bottom). */
data class CropRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width get() = right - left
    val height get() = bottom - top
}

object ScreenCrop {
    /**
     * The largest centered region of a [srcWidth] x [srcHeight] image with the target's aspect
     * ratio. Scaling that region to the target fills the screen with no letterboxing.
     */
    fun centerCrop(srcWidth: Int, srcHeight: Int, dstWidth: Int, dstHeight: Int): CropRect {
        require(srcWidth > 0 && srcHeight > 0 && dstWidth > 0 && dstHeight > 0)
        // Compare srcW/srcH with dstW/dstH without floating point.
        return if (srcWidth.toLong() * dstHeight > dstWidth.toLong() * srcHeight) {
            // Source is wider: keep full height, trim the sides.
            val w = (srcHeight.toLong() * dstWidth / dstHeight).toInt().coerceIn(1, srcWidth)
            val left = (srcWidth - w) / 2
            CropRect(left, 0, left + w, srcHeight)
        } else {
            // Source is taller (or equal): keep full width, trim top and bottom.
            val h = (srcWidth.toLong() * dstHeight / dstWidth).toInt().coerceIn(1, srcHeight)
            val top = (srcHeight - h) / 2
            CropRect(0, top, srcWidth, top + h)
        }
    }
}
