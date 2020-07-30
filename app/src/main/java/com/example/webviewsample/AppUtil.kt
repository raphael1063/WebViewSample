package com.example.webviewsample

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

object AppUtil {
    fun createAndSaveFileFromBase64Url(
        context: Context,
        url: String
    ): String {
        val path =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val fileType = url.substring(url.indexOf("/") + 1, url.indexOf(";"))
        val fileName =
            "EUBID_" + System.currentTimeMillis() + "." + fileType
        val file = File(path, fileName)
        try {
            if (!path.exists()) path.mkdirs()
            if (!file.exists()) file.createNewFile()
            val base64EncodedString = url.substring(url.indexOf(",") + 1)
            val decodedBytes =
                Base64.decode(base64EncodedString, Base64.DEFAULT)
            val os: OutputStream = FileOutputStream(file)
            os.write(decodedBytes)
            os.close()

            //Tell the media scanner about the new file so that it is immediately available to the user.
            MediaScannerConnection.scanFile(
                context, arrayOf(file.toString()), null
            ) { path, uri ->
                Log.i("ExternalStorage", "Scanned $path:")
                Log.i("ExternalStorage", "-> uri=$uri")
            }

            //Set notification after download complete and add "click to view" action to that
            val mimeType = url.substring(url.indexOf(":") + 1, url.indexOf("/"))
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            intent.setDataAndType(Uri.fromFile(file), "$mimeType/*")
            val pIntent = PendingIntent.getActivity(context, 0, intent, 0)
            val notification =
                NotificationCompat.Builder(context, "downloadChannel")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentText(context.getString(R.string.msg_file_downloaded))
                    .setContentTitle(fileName)
                    .setContentIntent(pIntent)
                    .build()
            notification.flags = notification.flags or Notification.FLAG_AUTO_CANCEL
            val notificationId = 85851
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(notificationId, notification)
        } catch (e: IOException) {
            Log.w("ExternalStorage", "Error writing $file", e)
            Toast.makeText(context, R.string.error_downloading, Toast.LENGTH_LONG).show()
        }
        return file.toString()
    }
}