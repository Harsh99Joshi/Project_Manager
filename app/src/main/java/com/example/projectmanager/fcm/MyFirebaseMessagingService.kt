package com.example.projectmanager.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.projectmanager.R
import com.example.projectmanager.activities.MainActivity
import com.example.projectmanager.activities.SignInActivity
import com.example.projectmanager.firebase.FirestoreClass
import com.example.projectmanager.utils.Constants
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService: FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {


        Log.d(TAG,"From: ${remoteMessage.from}")

        remoteMessage.data.isNotEmpty().let {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")

            val title = remoteMessage.data[Constants.FCM_KEY_TITLE]!!
            val message = remoteMessage.data[Constants.FCM_KEY_MESSAGE]!!

            sendNotification(title, message)
        }

        remoteMessage.notification?.let {
            Log.d(TAG, "Message notification body: ${it.body}")
        }

    }

    override fun onNewToken(token: String) {
        Log.e(TAG, "refresh token: $token")
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String?){
        val sharedPreferences =
            this.getSharedPreferences(Constants.PROJECT_MANAGER_PREFERENCES, Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.putString(Constants.FCM_TOKEN, token)
        editor.apply()
    }


    private fun sendNotification(title:String, message: String){

        val intent = if(FirestoreClass().getCurrentUserID().isNotEmpty()){
            Intent(this, MainActivity::class.java)
        }else{
            Intent(this, SignInActivity::class.java)
        }
        intent.flags = ( Intent.FLAG_ACTIVITY_NEW_TASK or
                              Intent.FLAG_ACTIVITY_CLEAR_TASK or
                              Intent.FLAG_ACTIVITY_CLEAR_TOP)


        val pendingIntent = PendingIntent.getActivity(this,0,intent,PendingIntent.FLAG_ONE_SHOT)

        val channelID = this.resources.getString(R.string.default_notification_channel_id)
        val defaultSoundURI = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(
            this, channelID)
            .setSmallIcon(R.drawable.ic_stat_ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setSound(defaultSoundURI)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val channel = NotificationChannel( channelID,
            "ProjectManager Channel Title",
            NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0, notificationBuilder.build())
    }


    companion object{
        private const val TAG = "MyFirebaseMsgService"
    }
}