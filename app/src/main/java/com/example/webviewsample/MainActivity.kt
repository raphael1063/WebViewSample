package com.example.webviewsample

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.webkit.*
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var valueCallback: ValueCallback<Array<Uri>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initWebview()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebview() {
        main_webview.webViewClient = WebViewClient()
        with(main_webview.settings) {
            javaScriptEnabled = true
            supportMultipleWindows()
            javaScriptCanOpenWindowsAutomatically = true
            loadWithOverviewMode = true
            useWideViewPort = true
            supportZoom()
            builtInZoomControls = false
            cacheMode = WebSettings.LOAD_NO_CACHE
            domStorageEnabled = true
        }
        main_webview.loadUrl("https://github.com/")

        with(main_webview) {
            webChromeClient = object : WebChromeClient() {
                override fun onShowFileChooser(webView: WebView?, filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: FileChooserParams?): Boolean {

                    filePathCallback?.let {
                        valueCallback = it
                    }

                    val intent = Intent(Intent.ACTION_GET_CONTENT)
                    with(intent) {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        setType("*/*")
                    }
                    startActivityForResult(intent, 0)
                    return true
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == 0 && resultCode == Activity.RESULT_OK) {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                valueCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data))
            } else {
                valueCallback.onReceiveValue(data?.data?.let { arrayOf(it) })
            }

        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}