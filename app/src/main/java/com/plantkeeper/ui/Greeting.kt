package com.plantkeeper.ui

/** Begroeting bovenaan het startscherm, bijvoorbeeld "Goedemorgen, Rocco". [hour] is 0 t/m 23. */
fun greeting(hour: Int, name: String?): String {
    val part = when (hour) {
        in 6..11 -> "Goedemorgen"
        in 12..17 -> "Goedemiddag"
        in 18..23 -> "Goedenavond"
        else -> "Goedenacht"
    }
    return if (name.isNullOrBlank()) part else "$part, ${name.trim()}"
}
