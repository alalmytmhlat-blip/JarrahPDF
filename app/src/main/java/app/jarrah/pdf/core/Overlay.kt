package app.jarrah.pdf.core

import android.graphics.Bitmap

enum class OverlayKind { TEXT, IMAGE, SIGNATURE, HIGHLIGHT, COVER }

/** عنصر يُرسم فوق الصفحة. الإحداثيات كسور (0..1) من الصفحة كما تظهر، والأصل أعلى-يسار. */
data class Overlay(
    val id: Int,
    val page: Int,
    val kind: OverlayKind,
    val bitmap: Bitmap?,
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,
    val color: Int = 0,
)
