package com.gali.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FuzzyNameMatcherTest {
    @Test
    fun matchesSubstringIgnoringCase() {
        assertTrue(FuzzyNameMatcher.matches("ic_more_menu.xml", "MORE"))
    }

    @Test
    fun matchesTokenPrefix() {
        assertTrue(FuzzyNameMatcher.matches("ic_more_menu.xml", "mor"))
    }

    @Test
    fun rejectsLooseCharacterSequence() {
        assertFalse(FuzzyNameMatcher.matches("bg_item_border.xml", "more"))
    }

    @Test
    fun rejectsCharactersOutOfOrder() {
        assertFalse(FuzzyNameMatcher.matches("ic_more_menu.xml", "zmm"))
    }
}
