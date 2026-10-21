package com.lawyerup.app.web

import android.annotation.SuppressLint
import android.content.Context
import android.net.http.SslError
import android.webkit.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.lawyerup.app.domain.*
import kotlinx.coroutines.*
import kotlin.coroutines.resume

class PortalController(context: Context, private val repository: WorkspaceRepository) {
    var status by mutableStateOf("ورود به سامانه را خودتان تکمیل کنید؛ رمز در برنامه ذخیره نمی‌شود")
    private set
    var loading by mutableStateOf(false) private set
    var busy by mutableStateOf(false) private set
    var url by mutableStateOf("") private set
    private var failed = false; private var cancelled = false; private var disposed = false
    private var epoch = 0
    private var lastPage: PageRead? = null
    var pendingDownload by mutableStateOf<String?>(null) private set
    private val snapshotScript = context.assets.open("sana/snapshot.js").bufferedReader().use { it.readText() }
    private val nextScript = context.assets.open("sana/next.js").bufferedReader().use { it.readText() }

    @SuppressLint("SetJavaScriptEnabled")
    val view = WebView(context).apply {
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            allowFileAccessFromFileURLs = false
            allowUniversalAccessFromFileURLs = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            safeBrowsingEnabled = true
            mediaPlaybackRequiresUserGesture = true
            setSupportMultipleWindows(false)
            javaScriptCanOpenWindowsAutomatically = false
            cacheMode = WebSettings.LOAD_NO_CACHE
            saveFormData = false
            setGeolocationEnabled(false)
        }
        settings.userAgentString += " Payesh/2.0.0 (Secure)"
        CookieManager.getInstance().apply {
            setAcceptThirdPartyCookies(this@apply, false)
            setAcceptCookie(true)
        }
        webChromeClient = WebChromeClient()
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val urlStr = request.url.toString()
                val allowed = officialUrl(urlStr) && urlStr.startsWith("https:")
                if (!allowed) { status = "نشانی غیرمجاز مسدود شد: ${urlStr.take(80)}"; return true }
                return false
            }
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                val urlStr = request.url.toString()
                val allowed = officialUrl(urlStr) && urlStr.startsWith("https:")
                if (!allowed) { status = "درخواست مسدود: ${urlStr.take(60)}"; return WebResourceResponse("text/html", "UTF-8", null) }
                return null
            }
            override fun onPageStarted(view: WebView, location: String, favicon: android.graphics.Bitmap?) {
                if (disposed) return
                loading = true; failed = false; url = location; epoch++
            }
            override fun onPageFinished(view: WebView, location: String) {
                if (disposed) return
                loading = false; url = location
                if (!failed && !busy) status = "پس از بازکردن فهرست، دریافت و تطبیق را بزنید"
                CookieManager.getInstance().flush()
            }
            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) { failed = true; loading = false; status = "بارگذاری ناموفق؛ کد ${error.errorCode}" }
            }
            override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, error: WebResourceResponse) {
                if (request.isForMainFrame) { failed = true; loading = false; status = "خطای سرویس: ${error.statusCode}" }
            }
            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                handler.cancel(); failed = true; loading = false; status = "گواهی امنیتی معتبر نیست؛ اتصال متوقف شد"
            }
        }
        setDownloadListener { location, userAgent, contentDisposition, mimeType, contentLength ->
            val allowed = officialUrl(location) && location.startsWith("https:")
            if (allowed) pendingDownload = location
            else status = "فقط PDF از نشانی HTTPS رسمی قابل دریافت است"
        }
    }
    fun open(location: String) { if (busy || !officialUrl(location) || !location.startsWith("https:")) return; view.loadUrl(location) }
    fun cancel() { cancelled = true }
    fun dismissDownload() { pendingDownload = null }
    suspend fun download(): ByteArray {
        val location = requireNotNull(pendingDownload)
        return CourtDownload.fetch(location) { destination ->
            withContext(Dispatchers.Main) { CookieManager.getInstance().getCookie(destination).orEmpty() }
        }.also { pendingDownload = null }
    }
    fun close() { disposed = true; cancelled = true; view.stopLoading(); view.destroy() }
    private suspend fun evaluate(script: String): String = withTimeout(15000) {
        suspendCancellableCoroutine { continuation ->
            if (disposed) { continuation.cancel(); return@suspendCancellableCoroutine }
            view.evaluateJavascript(script) { raw -> if (continuation.isActive) continuation.resume(raw) }
        }
    }
}
