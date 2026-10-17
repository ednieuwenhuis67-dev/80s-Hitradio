package nl.eightieshitradio.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var playButton: Button
    private lateinit var status: TextView
    private lateinit var nowPlaying: WebView
    private lateinit var recentlyPlayed: WebView
    private val handler = Handler(Looper.getMainLooper())

    // Ververs "nu op de radio" en de playlist elke minuut
    private val refresher = object : Runnable {
        override fun run() {
            nowPlaying.reload()
            recentlyPlayed.reload()
            handler.postDelayed(this, 60_000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        playButton = findViewById(R.id.playButton)
        status = findViewById(R.id.status)
        nowPlaying = findViewById(R.id.nowPlaying)
        recentlyPlayed = findViewById(R.id.recentlyPlayed)

        loadLogo()
        setupWebView(nowPlaying, getString(R.string.now_playing_url))
        setupWebView(recentlyPlayed, getString(R.string.recently_played_url))

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        playButton.setOnClickListener {
            val stopped = RadioService.state == RadioService.State.STOPPED ||
                RadioService.state == RadioService.State.ERROR
            val intent = Intent(this, RadioService::class.java)
                .setAction(if (stopped) RadioService.ACTION_PLAY else RadioService.ACTION_STOP)
            if (stopped) ContextCompat.startForegroundService(this, intent) else startService(intent)
        }
    }

    /**
     * Logo tonen. Volgorde:
     * 1. res/drawable/logo.png (als je die zelf in het project zet)
     * 2. eerder gedownloade kopie
     * 3. downloaden van de website en bewaren voor de volgende keer
     */
    private fun loadLogo() {
        val logo = findViewById<ImageView>(R.id.logo)
        val title = findViewById<TextView>(R.id.title)
        fun show() { logo.visibility = android.view.View.VISIBLE; title.visibility = android.view.View.GONE }

        val resId = resources.getIdentifier("logo", "drawable", packageName)
        if (resId != 0) { logo.setImageResource(resId); show(); return }

        val cache = File(filesDir, "logo.png")
        if (cache.exists()) {
            BitmapFactory.decodeFile(cache.path)?.let { logo.setImageBitmap(it); show(); return }
        }

        thread {
            try {
                val conn = URL(getString(R.string.logo_url)).openConnection() as HttpURLConnection
                conn.connectTimeout = 10_000
                conn.readTimeout = 10_000
                val bytes = conn.inputStream.use { it.readBytes() }
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@thread
                cache.writeBytes(bytes)
                runOnUiThread { logo.setImageBitmap(bmp); show() }
            } catch (_: Exception) {
                // Geen internet: de tekst "80s Hitradio" blijft staan
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(view: WebView, url: String) {
        view.settings.javaScriptEnabled = true
        view.settings.domStorageEnabled = true
        view.setBackgroundColor(0)
        view.webViewClient = WebViewClient()
        view.loadUrl(url)
    }

    private fun render(state: RadioService.State) {
        when (state) {
            RadioService.State.STOPPED -> { playButton.setText(R.string.play); status.text = "" }
            RadioService.State.LOADING -> { playButton.setText(R.string.stop); status.setText(R.string.loading) }
            RadioService.State.PLAYING -> { playButton.setText(R.string.stop); status.text = "● LIVE" }
            RadioService.State.ERROR -> { playButton.setText(R.string.play); status.setText(R.string.error) }
        }
    }

    override fun onResume() {
        super.onResume()
        RadioService.listener = { render(it) }
        render(RadioService.state)
        handler.postDelayed(refresher, 60_000)
    }

    override fun onPause() {
        super.onPause()
        RadioService.listener = null
        handler.removeCallbacks(refresher)
    }
}
