package com.gali.drawable

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AppThemeResolverTest : BasePlatformTestCase() {
    fun testFindsThemeStyles() {
        myFixture.addFileToProject(
            "app/src/main/res/values/styles.xml",
            """
            <resources>
                <style name="Theme.Sample" parent="Theme.MaterialComponents.DayNight">
                    <item name="colorPrimary">#ff0000</item>
                </style>
                <style name="Widget.Sample" />
            </resources>
            """.trimIndent(),
        )

        val themes = AppThemeResolver(project).findThemes()

        assertTrue(themes.any { it.name == "Theme.Sample" })
        assertFalse(themes.any { it.name == "Widget.Sample" })
    }
}
