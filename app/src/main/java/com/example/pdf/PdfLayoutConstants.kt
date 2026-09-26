package com.example.pdf

object PdfLayoutConstants {
    // Standard A4 in Points (72 points = 1 inch)
    const val A4_WIDTH_POINTS = 595
    const val A4_HEIGHT_POINTS = 842

    // Margins (0.5 inch = 36 points)
    const val MARGIN_LEFT = 36f
    const val MARGIN_RIGHT = 36f
    const val MARGIN_TOP = 36f
    const val MARGIN_BOTTOM = 36f

    const val HEADER_HEIGHT = 42f
    const val FOOTER_HEIGHT = 38f
    const val DISCLAIMER_BOX_HEIGHT = 44f

    const val COLOR_INK_BLACK = 0xFF111111.toInt()
    const val COLOR_BORDER_GRAY = 0xFF333333.toInt()
    const val COLOR_LIGHT_BORDER = 0xFF888888.toInt()
    const val COLOR_HEADER_BG = 0xFFF2F2F2.toInt()
    const val COLOR_ZEBRA_BG = 0xFFFAFAFA.toInt()
    const val COLOR_WHITE = 0xFFFFFFFF.toInt()

    val LEGAL_DISCLAIMER_TEXT =
        "Personal family record only. This document is not a government land record, title document, or proof of legal ownership. Verify all information against official records and registered documents."
}
