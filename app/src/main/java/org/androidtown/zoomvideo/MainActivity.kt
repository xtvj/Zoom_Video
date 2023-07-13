package org.androidtown.zoomvideo

import android.content.res.Resources
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import org.androidtown.zoomvideo.databinding.ActivityMainBinding


@UnstableApi
class MainActivity : AppCompatActivity() {

    private val path: String = "https://www.rmp-streaming.com/media/big-buck-bunny-360p.mp4"
 
    private val exoPlayer by lazy {
        ExoPlayer.Builder(this).build()
    }
    private val binding by lazy { 
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)

        binding.customview.setOnClickListener {
            Toast.makeText(this,"Click TextureView",Toast.LENGTH_SHORT).show()
        }

        binding.customview.surfaceTextureListener = object : TextureView.SurfaceTextureListener {

            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                exoPlayer.setVideoSurface(Surface(surface))
                exoPlayer.setMediaItem(MediaItem.fromUri(path))
                exoPlayer.prepare()
                exoPlayer.play()
                exoPlayer.addAnalyticsListener(object : AnalyticsListener {
                    override fun onVideoSizeChanged(eventTime: AnalyticsListener.EventTime, videoSize: VideoSize) {
                        super.onVideoSizeChanged(eventTime, videoSize)
                        val params = binding.customview.layoutParams
                        if (videoSize.width >= videoSize.height) {
                            params.width = ViewGroup.LayoutParams.MATCH_PARENT
                            params.height = Resources.getSystem().displayMetrics.widthPixels * videoSize.height / videoSize.width
                        } else {
                            params.width = Resources.getSystem().displayMetrics.heightPixels * videoSize.width / videoSize.height
                            params.height = ViewGroup.LayoutParams.MATCH_PARENT
                        }
                        binding.customview.layoutParams = params
                    }
                })
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            }
        }
    }

    override fun onPause() {
        super.onPause()
        exoPlayer.pause()
    }

}
