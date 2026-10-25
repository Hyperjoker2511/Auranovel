package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.network.CloudflareChallengeEvent

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CloudflareSolverDialog(
    challenge: CloudflareChallengeEvent,
    onResolved: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var currentTitle by remember { mutableStateOf("Verifying Browser...") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Cloudflare Shield",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Anti-Bot Verification",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = currentTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_cloudflare_solver")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Divider()

                // WebView Sandbox
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                webViewRef = this
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    userAgentString = challenge.userAgent
                                    cacheMode = WebSettings.LOAD_DEFAULT
                                }
                                CookieManager.getInstance().setAcceptCookie(true)
                                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        super.onPageStarted(view, url, favicon)
                                        currentTitle = "Loading challenge..."
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        currentTitle = view?.title ?: "Challenge ready"
                                        val cookies = CookieManager.getInstance().getCookie(url)
                                        if (cookies != null && (cookies.contains("cf_clearance") || cookies.contains("__cf_bm"))) {
                                            // Automatically resolve when clearance token captured!
                                            onResolved(cookies)
                                        }
                                    }
                                }
                                loadUrl(challenge.url)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Divider()

                // Bottom Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val url = webViewRef?.url ?: challenge.url
                            val cookies = CookieManager.getInstance().getCookie(url)
                            onResolved(cookies)
                        },
                        modifier = Modifier.testTag("apply_tokens_button")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("I Passed the Verification")
                    }
                }
            }
        }
    }
}
