package com.gali.ui

object FuzzyNameMatcher {
    fun matches(text: String, query: String): Boolean {
        val normalizedQuery = normalize(query)
        if (normalizedQuery.isEmpty()) return true

        val normalizedText = normalize(text.removeSuffix(".xml"))
        if (normalizedText.contains(normalizedQuery)) return true

        val tokens = normalizedText.split(' ').filter { it.isNotBlank() }
        return tokens.any { token -> token.startsWith(normalizedQuery) }
    }

    private fun normalize(value: String): String = value
        .trim()
        .lowercase()
        .replace(Regex("\\.xml$"), "")
        .replace(Regex("[^a-z0-9]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}
