package com.gali.drawable

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class DrawableSelectionResolverTest : BasePlatformTestCase() {
    fun testCollectsXmlFilesFromDrawableDirectory() {
        val xml = myFixture.addFileToProject("app/src/main/res/drawable/bg.xml", "<shape />").virtualFile
        myFixture.addFileToProject("app/src/main/res/drawable/icon.png", "png")

        val files = DrawableSelectionResolver.resolve(project, listOf(xml.parent))

        assertEquals(listOf(xml), files)
    }

    fun testIgnoresXmlOutsideDrawableDirectory() {
        val xml = myFixture.addFileToProject("app/src/main/res/layout/main.xml", "<LinearLayout />").virtualFile

        val files = DrawableSelectionResolver.resolve(project, listOf(xml))

        assertTrue(files.isEmpty())
    }

    fun testDeduplicatesMultipleSelections() {
        val xml = myFixture.addFileToProject("app/src/main/res/drawable-night/bg.xml", "<shape />").virtualFile

        val files = DrawableSelectionResolver.resolve(project, listOf(xml, xml.parent))

        assertEquals(listOf(xml), files)
    }
}
