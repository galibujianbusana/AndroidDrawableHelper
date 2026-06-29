package com.gali.drawable

data class AppTheme(
    val name: String,
    val displayName: String = name,
) {
    override fun toString(): String = displayName

    companion object {
        val None = AppTheme("", "No theme")
    }
}
