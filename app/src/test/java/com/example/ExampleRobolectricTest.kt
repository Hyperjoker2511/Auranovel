package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("AuraNovel", appName)
  }

  @Test
  fun `normalize github manifest url`() {
    val raw = "https://github.com/my-org/novel-extensions/blob/main/index.json"
    var candidate = raw
    if (candidate.contains("github.com") && candidate.contains("/blob/")) {
      candidate = candidate.replace("github.com", "raw.githubusercontent.com").replace("/blob/", "/")
    }
    assertEquals("https://raw.githubusercontent.com/my-org/novel-extensions/main/index.json", candidate)
  }

  @Test
  fun `validate extension url validity`() {
    val validUrl = "https://wuxiaworld.site"
    val invalidUrl = "not-a-valid-url"
    val isHttpOrHttps = validUrl.startsWith("http://") || validUrl.startsWith("https://")
    val invalidIsHttp = invalidUrl.startsWith("http://") || invalidUrl.startsWith("https://")
    assertEquals(true, isHttpOrHttps)
    assertEquals(false, invalidIsHttp)
  }
}
