package com.example.webviewsample

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

object AppUtil {

    val timeStampFormat = SimpleDateFormat("yyyyMMddHHmmss", Locale.KOREA)
    var path: File? = null

    fun createAndSaveFileFromBase64Url(
        context: Context,
        url: String
    ): String {
        Toast.makeText(context, "파일을 다운로드합니다.", Toast.LENGTH_SHORT).show()
//        val path = "storage/emulated/0/Download"
//        val path = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        var fileType = url.substring(url.indexOf("/") + 1, url.indexOf(";"))
        if (fileType == "vnd.ms-excel") {
            fileType = "html"
        }
        var fileName = "EUBID_" + timeStampFormat.format(Date()) + "." + fileType
        val file = File(path, fileName)

        try {
//            if (!path.exists()) path.mkdirs()
            if (!file.exists()) file.createNewFile()
            val base64EncodedString = url.substring(url.indexOf(",") + 1)
            val decodedBytes = Base64.decode(base64EncodedString, Base64.DEFAULT)
            Timber.d("decodedBytes : $decodedBytes")
            val os: OutputStream = FileOutputStream(file)
            os.write(decodedBytes)
            os.flush()
            os.close()

            //Tell the media scanner about the new file so that it is immediately available to the user.
            MediaScannerConnection.scanFile(
                context, arrayOf(file.toString()), null
            ) { path, uri ->
                Log.i("ExternalStorage", "Scanned $path:")
                Log.i("ExternalStorage", "-> uri=$uri")
            }

//            createNotificationChannel(context, NotificationManagerCompat.IMPORTANCE_HIGH,
//                false, context.getString(R.string.app_name), "App notification channel") // 1
//
//            val channelId = "${context.packageName}-${context.getString(R.string.app_name)}" // 2
//            val title = "파일을 다운로드 합니다."
//
////            val intent = Intent(context, NewActivity::class.java)
////            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//
//            val intent = Intent(Intent.ACTION_VIEW)
//            val uri = Uri.parse("storage/emulated/0/Download")
//            with(intent) {
//                addCategory(Intent.CATEGORY_OPENABLE)
//                setDataAndType(uri, "*/*")
//            }
//            val pendingIntent = PendingIntent.getActivity(context, 0,
//                intent, PendingIntent.FLAG_UPDATE_CURRENT)    // 3
//
//            val builder = NotificationCompat.Builder(context, channelId)  // 4
//            with(builder) {
//                setSmallIcon(R.drawable.ic_launcher_foreground)    // 5
//                setContentTitle(title)    // 6
//                setContentText(fileName)    // 7
//                priority = NotificationCompat.PRIORITY_HIGH    // 8
//                setAutoCancel(true)   // 9
//                setContentIntent(pendingIntent)
//                setFullScreenIntent(pendingIntent, true)// 10
//            }
//
//            val notificationManager = NotificationManagerCompat.from(context)
//            notificationManager.notify(NOTIFICATION_ID, builder.build())    // 11

            Toast.makeText(context, "다운로드가 완료되었습니다.", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Timber.w("Error: $e")
            Toast.makeText(context, R.string.error_downloading, Toast.LENGTH_LONG).show()
        }
        fileName = fileName.replace("vnd.ms-excel", "xls")
        return fileName
    }

//    private fun createNotificationChannel(context: Context, importance: Int, showBadge: Boolean,
//                                          name: String, description: String) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val channelId = "${context.packageName}-$name"
//            val channel = NotificationChannel(channelId, name, importance)
//            channel.description = description
//            channel.setShowBadge(showBadge)
//
//            val notificationManager = context.getSystemService(NotificationManager::class.java)
//            notificationManager.createNotificationChannel(channel)
//        }
//    }
//
//    private val NOTIFICATION_ID = 1000
}