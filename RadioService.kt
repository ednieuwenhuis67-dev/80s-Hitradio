package nl.eightieshitradio.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

/** Speelt de livestream af, ook met scherm uit of app op de achtergrond. */
class RadioService : Service() {

    enum class State { STOPPED, LOADING, PLAYING, ERROR }

    companion object {
        const val ACTION_PLAY = "nl.eightieshitradio.PLAY"
        const val ACTION_STOP = "nl.eightieshitradio.STOP"
        private const val CHANNEL_ID = "radio"
        private const val NOTIF_ID = 1

        var state = State.STOPPED
            private set
        var listener: ((State) -> Unit)? = null
    }

    private var player: MediaPlayer? = null
    private var wifiLock: WifiManager.WifiLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) stopRadio(State.STOPPED) else startRadio()
        return START_NOT_STICKY
    }

    private fun startRadio() {
        val type = if (Build.VERSION.SDK_INT >= 29)
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
        ServiceCompat.startForeground(this, NOTIF_ID, buildNotification(), type)
        if (player != null) return

        setState(State.LOADING)
        wifiLock = (applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager)
            .createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "radio").apply { acquire() }

        try {
            // Android's ingebouwde MediaPlayer: MP3/AAC zonder extra codecs
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
                setDataSource(getString(R.string.stream_url))
                setOnPreparedListener { it.start(); setState(State.PLAYING) }
                setOnErrorListener { _, _, _ -> stopRadio(State.ERROR); true }
                prepareAsync()
            }
        } catch (e: Exception) {
            stopRadio(State.ERROR)
        }
    }

    private fun stopRadio(newState: State) {
        player?.release()
        player = null
        wifiLock?.takeIf { it.isHeld }?.release()
        wifiLock = null
        setState(newState)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun setState(s: State) {
        state = s
        listener?.invoke(s)
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL_ID, "Radio", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, RadioService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_radio)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Live – de lekkerste hits uit de jaren 80")
            .setContentIntent(open)
            .addAction(0, "Stop", stop)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        player?.release()
        player = null
        wifiLock?.takeIf { it.isHeld }?.release()
        super.onDestroy()
    }
}
