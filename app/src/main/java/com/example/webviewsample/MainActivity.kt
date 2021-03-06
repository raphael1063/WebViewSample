package com.example.webviewsample

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import com.example.webviewsample.AppUtil.createAndSaveFileFromBase64Url
import com.example.webviewsample.AppUtil.path
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber
import timber.log.Timber.d
import java.net.URL
import java.net.URLDecoder
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {

    private val downloadManager: DownloadManager by lazy {
        getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    }
    private lateinit var valueCallback: ValueCallback<Array<Uri>>
    private lateinit var popupWebView: WebView
    private var isPopupVisible = false
    private var isDialogOn = false
    private var isNetworkOk = true
    private var downloadId: Long = 0
    private var downloadUrl = ""
    private var downloadFileName = ""
    private var openFolderCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        start()
        initWebview()
    }

    private fun start() {
        val builder =StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())
        if (0 != applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        registerReceiver(onDownloadComplete, IntentFilter().apply {
            addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            addAction(DownloadManager.ACTION_NOTIFICATION_CLICKED)
        })

        et_viewer_btn.setOnClickListener {
            if(address_bar_edit.isVisible) {
                et_viewer_btn.text = "주소변경"
                main_webview.loadUrl(address_bar_edit.text.toString())
                address_bar_edit.visibility = View.GONE
            } else {
                address_bar_edit.visibility = View.VISIBLE
                et_viewer_btn.text = "이동"
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebview() {
        main_webview.webViewClient = WebViewClient()
        with(main_webview.settings) {
            javaScriptEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            loadWithOverviewMode = true
            textZoom = 100
            cacheMode = WebSettings.LOAD_DEFAULT
            domStorageEnabled = true
        }
        main_webview.loadUrl(MAIN_URL)

        layout_refresh.setOnRefreshListener { main_webview.reload() }

        with(main_webview) {

            /**'HTML' 링크처리*/
            webViewClient = object : WebViewClient() {

                /**현재 웹뷰에 'URL'을 로드할 것인지에 대해 'boolean'값 반환(안드로이드 버전 7 이하)*/
               override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    url?.let {
                        return shouldLoadOnMainWebview(url, view)
                    } ?: return false
//                    return super.shouldOverrideUrlLoading(view, url)

                }

                /**현재 웹뷰에 'URL'을 로드할 것인지에 대해 'boolean'값 반환(안드로이드 버전 7 이상)*/
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    d("request = ${request?.url.toString()}")
                    val url = request?.url.toString()
                    return shouldLoadOnMainWebview(url, view)
//                    return super.shouldOverrideUrlLoading(view, request)
                }


                /**에러 발생 시(ex.네트워크 에러)*/
                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    error?.let {
                        if (it.description == "net::ERR_INTERNET_DISCONNECTED" || it.description == "net::ERR_ADDRESS_UNREACHABLE") {
                            isNetworkOk = false
                            main_webview.visibility = View.INVISIBLE
                            if (!isDialogOn) {
                                showNetworkDialog()
                                isDialogOn = true
                            }
                            return
                        }
                    }
                    main_webview.goBack()
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    layout_refresh.isRefreshing = false
                    currentUrl_text.text = url
                }
            }

            /**웹 브라우저 이벤트 처리*/
            webChromeClient = object : WebChromeClient() {

                /**새 창 생성 시*/
                override fun onCreateWindow(
                    view: WebView?,
                    isDialog: Boolean,
                    isUserGesture: Boolean,
                    resultMsg: Message?
                ): Boolean {
                    d("resultMsg =  $resultMsg")
                    return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg)                }

                /**파일 업로드 시*/
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
            setDownloadListener { url, _, contentDisposition, mimeType, _ ->
                run {
                    d("LOG!! DownLoadListener : %s", url)
                    // bidfunding 내부에 올린 파일인 경우
                    when {
                        url.contains("bidfunding") -> {
                            downloadFileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
                        }
                        // 외부 페이지에 있는 파일인 경우
                        else -> {
                            var decodeStr = URLDecoder.decode(contentDisposition, "UTF-8")
                            val decodeList =
                                ArrayList(
                                    listOf(
                                        *decodeStr.split("=".toRegex()).toTypedArray()
                                    )
                                )
                            decodeStr = decodeList[decodeList.size - 1]
                            decodeStr = decodeStr.replace("\"", "")
                            decodeStr = decodeStr.replace("\u003b", "")
                            if (decodeStr.isEmpty()) {
                                decodeStr = url.substringAfterLast("/")
                            }
                            downloadFileName = decodeStr
                        }
                    }
                    downloadUrl = url
                    setDownload()
                }
            }
        }
    }

    private fun shouldLoadOnMainWebview(url: String, view: WebView?): Boolean {
        return when {
            url.contains(MAIN_URL) -> {
                loadMainWebView(url)
                true
            }
            else -> {
                openExternalBrowser(url)
                true
            }
        }
    }

    /**메인웹뷰에 'url'로드*/
    private fun loadMainWebView(url: String) {
        main_webview.loadUrl(url)
    }

    private fun openExternalBrowser(url: String) {
        Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            startActivity(this)
        }
    }

    /**팝업 제거*/
    private fun clearPopupWebview(view: WebView?) {
        isPopupVisible = false
        main_webview.removeView(view)
        view?.removeAllViews()
        view?.destroy()
    }

    /**권한체크*/
    private fun hasPermission(context: Context, permissions: Array<String>): Boolean {
        for (permission in PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    /**권한요청*/
    private fun getPermission() {
        ActivityCompat.requestPermissions(this, PERMISSIONS, 1000)
    }

    /**다운로드 작업 */
    private fun setDownload() {
        when {
            !hasPermission(this, PERMISSIONS) -> {
                getPermission()
            }
            else -> {
                when(downloadUrl.startsWith("data:")) {
                    true -> {
                        val fileName = createAndSaveFileFromBase64Url(this, downloadUrl)
                        val intent = Intent(Intent.ACTION_VIEW)
                        val dir = Uri.parse("file://${path.toString()}/$fileName")
                        intent.setDataAndType(dir, "text/html")
                        startActivity(intent)
                    }
                    false -> {
                        val request = DownloadManager.Request(Uri.parse(downloadUrl))
                            .setTitle(downloadFileName)
                            .setDescription("파일을 다운로드 중입니다.")
                            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            .setDestinationInExternalPublicDir(
                                Environment.DIRECTORY_DOWNLOADS,
                                downloadFileName
                            )
                            .setAllowedOverMetered(true)
                            .setAllowedOverRoaming(true)
                        downloadId = downloadManager.enqueue(request)
                        Toast.makeText(applicationContext, "파일을 다운로드 중입니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    /**다운로드 이후 작업 */
    private var onDownloadComplete: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            openFolderCount = 0
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == intent.action && downloadId == id) {
                val query = DownloadManager.Query()
                query.setFilterById(id)
                val cursor: Cursor = downloadManager.query(query)
                if (!cursor.moveToFirst()) return

                val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                when (cursor.getInt(columnIndex)) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        when (openFolderCount) {
                            0 -> startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS))
                        }
                        openFolderCount++
                    }
                    DownloadManager.STATUS_FAILED -> {
                        Toast.makeText(context, "다운로드에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    /**네트워크 에러 처리 다이얼로그*/
    private fun showNetworkDialog() {
        AlertDialog.Builder(this@MainActivity).apply {
            setTitle("네트워크에 접속할 수 없습니다.")
            setMessage("네트워크 연결 상태를 확인해주세요.")
            setPositiveButton("새로고침") { _, _ ->
                main_webview.reload()
                isDialogOn = false
            }
            setNegativeButton("앱 종료") { _, _ ->
                finishAffinity()
                exitProcess(0)
            }
            create().apply {
                setCancelable(false)
                setCanceledOnTouchOutside(false)
                show()
            }
        }
    }

    /**백버튼 처리*/
    override fun onBackPressed() {
        when {
            /**홈 화면인 경우*/
            main_webview.originalUrl.equals(MAIN_URL, false) -> {
                super.onBackPressed()
            }
            /**현재 페이지가 팝업인 경우*/
            isPopupVisible -> {
                popupWebView.loadUrl("javascript:window.close();")
                isPopupVisible = false
            }
            /**현재 페이지가 홈이 아닌 경우*/
            main_webview.canGoBack() -> {
                main_webview.goBack()
            }
        }
        main_webview.goBack()
    }

    /**파일 업로드 처리*/
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
//        private const val MAIN_URL = "http://192.168.0.173:8089"//찬호주임님
        private const val MAIN_URL = "http://192.168.0.39:8086"//명철씨
//        private const val MAIN_URL = "http://192.168.0.162:8086"//임원진
        private const val SERVER_HOST = "192.168.0.39"
        private val PERMISSIONS = arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
}