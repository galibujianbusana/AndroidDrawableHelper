package com.gali.drawable

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class DrawableXmlParserTest : BasePlatformTestCase() {
    fun testParsesShapeDrawable() {
        val file = myFixture.addFileToProject(
            "app/src/main/res/drawable/bg.xml",
            """
            <shape xmlns:android="http://schemas.android.com/apk/res/android">
                <solid android:color="#ff0000" />
                <corners android:radius="12dp" />
                <stroke android:width="2dp" android:color="#000000" />
            </shape>
            """.trimIndent(),
        ).virtualFile

        val model = DrawableXmlParser(project).parse(file)

        assertTrue(model is DrawableModel.Shape)
        model as DrawableModel.Shape
        assertEquals(12f, model.cornerRadius)
        assertEquals(2f, model.strokeWidth)
        assertNotNull(model.fillColor)
    }

    fun testParsesSelectorDefaultItem() {
        val shape = myFixture.addFileToProject(
            "app/src/main/res/drawable/button_normal.xml",
            """
            <shape xmlns:android="http://schemas.android.com/apk/res/android">
                <solid android:color="#00ff00" />
            </shape>
            """.trimIndent(),
        ).virtualFile
        val selector = myFixture.addFileToProject(
            "app/src/main/res/drawable/button.xml",
            """
            <selector xmlns:android="http://schemas.android.com/apk/res/android">
                <item android:drawable="@drawable/${shape.nameWithoutExtension}" />
            </selector>
            """.trimIndent(),
        ).virtualFile

        val model = DrawableXmlParser(project).parse(selector)

        assertTrue(model is DrawableModel.Selector)
        assertTrue((model as DrawableModel.Selector).item is DrawableModel.Shape)
    }

    fun testResolvesThemeAttributeColor() {
        myFixture.addFileToProject(
            "app/src/main/res/values/styles.xml",
            """
            <resources>
                <style name="Theme.Sample">
                    <item name="colorPrimary">#112233</item>
                </style>
            </resources>
            """.trimIndent(),
        )
        val file = myFixture.addFileToProject(
            "app/src/main/res/drawable/bg_theme.xml",
            """
            <shape xmlns:android="http://schemas.android.com/apk/res/android">
                <solid android:color="?attr/colorPrimary" />
            </shape>
            """.trimIndent(),
        ).virtualFile

        val model = DrawableXmlParser(project, AppTheme("Theme.Sample")).parse(file)

        assertTrue(model is DrawableModel.Shape)
        assertNotNull((model as DrawableModel.Shape).fillColor)
    }

    fun testResolvesThemeAttrsAttributeColor() {
        myFixture.addFileToProject(
            "app/src/main/res/values/styles.xml",
            """
            <resources>
                <style name="Theme.Sample">
                    <item name="colorPrimary">#112233</item>
                </style>
            </resources>
            """.trimIndent(),
        )
        val file = myFixture.addFileToProject(
            "app/src/main/res/drawable/bg_theme_attrs.xml",
            """
            <shape xmlns:android="http://schemas.android.com/apk/res/android">
                <solid android:color="?attrs/colorPrimary" />
            </shape>
            """.trimIndent(),
        ).virtualFile

        val model = DrawableXmlParser(project, AppTheme("Theme.Sample")).parse(file)

        assertTrue(model is DrawableModel.Shape)
        assertNotNull((model as DrawableModel.Shape).fillColor)
    }

    fun testResolvesThemeAttributeAliasColor() {
        myFixture.addFileToProject(
            "app/src/main/res/values/styles.xml",
            """
            <resources>
                <style name="Theme.Sample" parent="Theme.Parent">
                    <item name="buttonColor">?attrs/colorPrimary</item>
                </style>
                <style name="Theme.Parent">
                    <item name="colorPrimary">@color/primary</item>
                </style>
            </resources>
            """.trimIndent(),
        )
        myFixture.addFileToProject(
            "app/src/main/res/values/colors.xml",
            """
            <resources>
                <color name="primary">#445566</color>
            </resources>
            """.trimIndent(),
        )
        val file = myFixture.addFileToProject(
            "app/src/main/res/drawable/bg_theme_alias.xml",
            """
            <shape xmlns:android="http://schemas.android.com/apk/res/android">
                <solid android:color="?attrs/buttonColor" />
            </shape>
            """.trimIndent(),
        ).virtualFile

        val model = DrawableXmlParser(project, AppTheme("Theme.Sample")).parse(file)

        assertTrue(model is DrawableModel.Shape)
        assertNotNull((model as DrawableModel.Shape).fillColor)
    }

    fun testResolvesDirectQuestionMarkThemeAttributeFromSelectedTheme() {
        myFixture.addFileToProject(
            "app/src/main/res/values/attrs.xml",
            """
            <resources>
                <attr name="first_txt_color" format="color" />
            </resources>
            """.trimIndent(),
        )
        myFixture.addFileToProject(
            "app/src/main/res/values/styles.xml",
            """
            <resources>
                <style name="AppTheme" />
                <style name="DayTheme" parent="AppTheme">
                    <item name="first_txt_color">#111111</item>
                </style>
                <style name="NightTheme" parent="AppTheme">
                    <item name="first_txt_color">#eeeeee</item>
                </style>
            </resources>
            """.trimIndent(),
        )
        val file = myFixture.addFileToProject(
            "app/src/main/res/drawable/bg_direct_attr.xml",
            """
            <shape xmlns:android="http://schemas.android.com/apk/res/android"
                android:strokeColor="?first_txt_color"
                android:strokeWidth="2dp">
                <solid android:color="#ffffff" />
            </shape>
            """.trimIndent(),
        ).virtualFile

        val model = DrawableXmlParser(project, AppTheme("NightTheme")).parse(file)

        assertTrue(model is DrawableModel.Shape)
        assertNotNull((model as DrawableModel.Shape).strokeColor)
    }

    fun testParsesThemeAwareVectorDrawable() {
        myFixture.addFileToProject(
            "app/src/main/res/values/styles.xml",
            """
            <resources>
                <style name="NightTheme">
                    <item name="first_txt_color">#eeeeee</item>
                </style>
            </resources>
            """.trimIndent(),
        )
        val file = myFixture.addFileToProject(
            "app/src/main/res/drawable/ic_more.xml",
            """
            <vector xmlns:android="http://schemas.android.com/apk/res/android"
                android:width="16dp"
                android:height="16dp"
                android:viewportWidth="16"
                android:viewportHeight="16">
              <group>
                <path
                    android:pathData="M8,1.583L8,1.583A6.417,6.417 0,0 1,14.416 8L14.416,8A6.417,6.417 0,0 1,8 14.417L8,14.417A6.417,6.417 0,0 1,1.583 8L1.583,8A6.417,6.417 0,0 1,8 1.583z"
                    android:strokeWidth="1.16667"
                    android:fillColor="#00000000"
                    android:strokeColor="?first_txt_color"/>
                <path
                    android:pathData="M8,8m-0.817,0a0.817,0.817 0,1 1,1.633 0a0.817,0.817 0,1 1,-1.633 0"
                    android:fillColor="?first_txt_color"/>
              </group>
            </vector>
            """.trimIndent(),
        ).virtualFile

        val model = DrawableXmlParser(project, AppTheme("NightTheme")).parse(file)

        assertTrue(model is DrawableModel.Vector)
        model as DrawableModel.Vector
        assertEquals(16f, model.viewportWidth)
        assertTrue(model.paths.isNotEmpty())
        assertNotNull(model.paths.first().strokeColor)
    }

    fun testParsesEmptyVectorDrawable() {
        val file = myFixture.addFileToProject(
            "app/src/main/res/drawable/ic_sample.xml",
            "<vector xmlns:android=\"http://schemas.android.com/apk/res/android\" />",
        ).virtualFile

        val model = DrawableXmlParser(project).parse(file)

        assertTrue(model is DrawableModel.Vector)
    }
}
