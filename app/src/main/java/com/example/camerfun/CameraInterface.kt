package com.example.camerfun

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

class CameraInterface : AppCompatActivity() {
    val REQUEST_VIDEO_CAPTURE = 1;
    private var videoView:VideoView? = null;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_layout);
        videoView = findViewById<VideoView>(R.id.video_view);
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        Log.d("WORKING@@@@@@1231",requestCode.toString());

            if (requestCode == REQUEST_VIDEO_CAPTURE && resultCode == RESULT_OK) {
                var videoUri: Uri? = intent?.data;
                videoView?.setVideoURI(videoUri);
                videoView?.start()
            }
    }



    fun dispatchTakeVideoIntent(view: View) {
        Intent(MediaStore.ACTION_VIDEO_CAPTURE).also { takeVideoIntent ->
            takeVideoIntent.resolveActivity(packageManager)?.also {
                startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
            }
        }
    }
}