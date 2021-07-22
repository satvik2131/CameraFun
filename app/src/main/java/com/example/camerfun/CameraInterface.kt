
package com.example.camerfun

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.Camera
import android.media.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toFile
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*

class CameraInterface : AppCompatActivity() {
    private var mCamera: Camera? = null
    private var mPreview: Preview? = null
    private var mediaRecorder:MediaRecorder? = null
    private var isRecording:Boolean = false
    private var capture:Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_layout);

        // Create an instance of Camera
        mCamera = getCameraInstance();
        capture = findViewById(R.id.button_capture)

   
//        Log.d("NO OF CAMERA ------>",
//
//        )

        mCamera?.setDisplayOrientation(90);
        mPreview = mCamera?.let {
            // Create our Preview view
            Preview(this, it)
        }

        // Set the Preview view as the content of our activity.
        mPreview?.also {
            val preview: FrameLayout = findViewById(R.id.camera_preview)
            preview.addView(it)
        }


    }

    override fun onDestroy() {
        super.onDestroy();
        //After executing the functionality
        mCamera?.release();
    }


    fun getCameraInstance(): Camera? {
        return try {
                Camera.open() // attempt to get a Camera instance
        } catch (e: Exception) {
            Log.d("ERROR_MESSAGE911",e.message.toString());
            // Camera is not available (in use or does not exist)
            null // returns null if camera is unavailable
        }
    }



    //MAIN CODE ----> CAPTURES VIDEO (Capture button function)
    @RequiresApi(Build.VERSION_CODES.O)
    fun Capture(view: View){
        try{
            if (isRecording) {
                // stop recording and release camera
                mediaRecorder?.stop() // stop the recording
                releaseMediaRecorder() // release the MediaRecorder object
                mCamera?.lock() // take camera access back from MediaRecorder


                //Save the media with URI
                var uri:Uri = getOutputMediaFileUri(MEDIA_TYPE_VIDEO);
                //This is the file containing data
                //  audio <----- FILE ---------> Video

                var file:File = uri.toFile()
                var audioIntent = Intent(this,CameraInterface::class.java)
                audioIntent.putExtra("VIDEO",file.readBytes())

                var recognizer = SpeechRecognizer.createSpeechRecognizer(this)

//                var listener:RecognitionListener



                recognizer.setRecognitionListener(null)
                recognizer.startListening(audioIntent)


//                Log.d("RUNNING------><--",extractor.trackCount.toString())

                //                Saving Process
//                var op:FileOutputStream = FileOutputStream(file)
//                op.write(file.toString().toByteArray())
//                op.close()


                // inform the user that recording has stopped
                setCaptureButtonText("Capture")
                isRecording = false
            } else {
                // initialize video camera
                if (prepareVideoRecorder()) {
                    // Camera is available and unlocked, MediaRecorder is prepared,
                    // now you can start recording
                    mediaRecorder?.start()

                    // inform the user that recording has started
                    setCaptureButtonText("Stop")
                    isRecording = true
                } else {
                    // prepare didn't work, release the camera
                    releaseMediaRecorder()
                    Toast.makeText(this,"Did not worked out",Toast.LENGTH_SHORT).show()
                    // inform user
                }
            }
        }catch(e:Error){
            e.message?.let { Log.d("ERROR6363333", it) }
        }
    }


    private fun setCaptureButtonText(s: String) {
        capture?.setText(s)
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun prepareVideoRecorder():Boolean {
        mediaRecorder = MediaRecorder()

        mCamera?.let { camera ->
            // Step 1: Unlock and set camera to MediaRecorder
            camera?.unlock()

            mediaRecorder?.run {
                setCamera(camera)

                // Step 2: Set sources
                setAudioSource(MediaRecorder.AudioSource.CAMCORDER)
                setVideoSource(MediaRecorder.VideoSource.CAMERA)

                // Step 4: Set output file
                setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO).toString())


                // Step 5: Set the preview output
                setPreviewDisplay(mPreview?.holder?.surface)


                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT)
                    setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT)


                // Step 6: Prepare configured MediaRecorder
                return try {
                    prepare()
                    true
                } catch (e: IllegalStateException) {
                    Log.d("TAG", "IllegalStateException preparing MediaRecorder: ${e.message}")
                    releaseMediaRecorder()
                    false
                } catch (e: IOException) {
                    Log.d("TAG", "IOException preparing MediaRecorder: ${e.message}")
                    releaseMediaRecorder()
                    false
                }
            }

        }
        return false
    }

    //Release the camera recorder resources
    private fun releaseMediaRecorder() {
        mediaRecorder?.reset() // clear recorder configuration
        mediaRecorder?.release() // release the recorder object
        mediaRecorder = null
        mCamera?.lock() // lock camera for later use
    }

    /** Create a file Uri for saving an image or video */
    private fun getOutputMediaFileUri(type: Int): Uri {
        return Uri.fromFile(getOutputMediaFile(type))
    }


    //Checks the camera is free or not and proceed
    /** Create a File for saving an image or video */
    private fun getOutputMediaFile(type: Int): File? {

        val mediaStorageDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            "CamerFun"
        )

        // Create the storage directory if it does not exist
        mediaStorageDir.apply {
            if (!exists()) {
                if (!mkdirs()) {
                    Log.d("CamerFun", "failed to create directory")
                    return null
                }
            }
        }

        // Create a media file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        return when (type) {

            MEDIA_TYPE_VIDEO -> {
                File("${mediaStorageDir.path}${File.separator}VID_$timeStamp.mp4")
            }
            else -> null
        }
    }
}


