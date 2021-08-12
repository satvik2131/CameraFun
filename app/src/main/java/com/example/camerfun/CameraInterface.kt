package com.example.camerfun

import android.content.Intent
import android.hardware.Camera
import android.media.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslatorOptions
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class CameraInterface : AppCompatActivity() {
    private var mCamera: Camera? = null
    private var mPreview: Preview? = null
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording: Boolean = false
    private var capture: Button? = null
    var recognizer: SpeechRecognizer? = null
    var recognitionListener: RecognitionListener? = null
    var audioIntent: Intent? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_layout);

        //Firebase App Initialization
        FirebaseApp.initializeApp(this)



        //Audio Listener Setup
        //Audio Listener and Video Listener are used separately here
        recognizer = SpeechRecognizer.createSpeechRecognizer(this)

        audioIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        audioIntent?.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );
        audioIntent?.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        //Setting Recognition Listener
        recognitionListener = object : RecognitionListener {
            override fun onReadyForSpeech(p0: Bundle?) {
                Log.d("MESSAGE---->", "Started55555")
            }

            override fun onBeginningOfSpeech() {
                Log.d("MEASAGE----->", "STAAARTED")
            }

            override fun onRmsChanged(p0: Float) {
                Log.d("MEASAGE----->", "onRMSCHANGEdd")
            }

            override fun onBufferReceived(p0: ByteArray?) {
                Log.d("MEASAGE----->", "BUFFER received")
            }

            override fun onEndOfSpeech() {
                Log.d("MEASAGE----->", "end of speeech")
            }

            override fun onError(p0: Int) {
                Log.d("EERRRROOORRR45---->", p0.toString())
            }

            override fun onResults(result: Bundle) {
                try {

                    //Code to Translate text to another language
                    var data: ArrayList<String>? =
                        result.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    var text: String = data?.get(0).toString()

                    var options = FirebaseTranslatorOptions.Builder()
                        .setSourceLanguage(FirebaseTranslateLanguage.EN)
                        .setTargetLanguage(FirebaseTranslateLanguage.HI)
                        .build()

                    val englToHindiTranslator = FirebaseNaturalLanguage.getInstance().getTranslator(options)

                    englToHindiTranslator.downloadModelIfNeeded()
                        .addOnSuccessListener {
                            Log.d("STATUS_OF_TRANSLATION","Success"+it.toString())
                        }.addOnFailureListener {
                            Log.d("STATUS_OF_TRANSLATION","Failure"+it.message)
                        }

                    englToHindiTranslator.translate(text)
                        .addOnSuccessListener {
                            Log.d("Translated_text",it.toString())
                        }.addOnFailureListener {
                            Log.d("Error",it.message.toString())
                        }

//                    Log.d("T-E-X-T-->",text)

                } catch (error: Error) {
                    Log.d("ERROR", error.message.toString())
                }

            }

            override fun onPartialResults(partialResults: Bundle?) {

                var data: ArrayList<String>? =
                    partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                var unstableData: ArrayList<String>? =
                    partialResults?.getStringArrayList("android.speech.extra.UNSTABLE_TEXT")
                var mResult = data
                Log.d("PARTIAL--RESULT-->", mResult.toString())
            }

            override fun onEvent(p0: Int, p1: Bundle?) {
                TODO("Not yet implemented")
            }

        }
        recognizer?.setRecognitionListener(recognitionListener)


        //***************************//

        // Create an instance of Camera
        mCamera = getCameraInstance();
        capture = findViewById(R.id.button_capture)


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
            Log.d("ERROR_MESSAGE911", e.message.toString());
            // Camera is not available (in use or does not exist)
            null // returns null if camera is unavailable
        }
    }


    //MAIN CODE ----> CAPTURES VIDEO (Capture button function)
    @RequiresApi(Build.VERSION_CODES.O)
    fun Capture(view: View) {

        try {
            if (isRecording) {

                //Audio Listener
                recognizer?.stopListening()

                // stop recording and release camera
                //Video Listener

                mediaRecorder?.stop() // stop the recording
                releaseMediaRecorder() // release the MediaRecorder object
                mCamera?.lock() // take camera access back from MediaRecorder


                // inform the user that recording has stopped
                setCaptureButtonText("Capture")
                isRecording = false
            } else {

                // initialize video camera
                if (prepareVideoRecorder()) {

                    //Audio listener
                    recognizer?.startListening(audioIntent)

                    // Camera is available and unlocked, MediaRecorder is prepared,
                    // now you can start recording
                    //Video Listener
                    mediaRecorder?.start()


                    // inform the user that recording has started
                    setCaptureButtonText("Stop")
                    isRecording = true
                } else {
                    // prepare didn't work, release the camera
                    releaseMediaRecorder()
                    Toast.makeText(this, "Did not worked out", Toast.LENGTH_SHORT).show()
                    // inform user
                }
            }
        } catch (e: Error) {
            e.message?.let { Log.d("ERROR6363333", it) }
        }
    }


    private fun setCaptureButtonText(s: String) {
        capture?.setText(s)
    }

    //Video Camera Setup only
    @RequiresApi(Build.VERSION_CODES.O)
    private fun prepareVideoRecorder(): Boolean {
        mediaRecorder = MediaRecorder()

        mCamera?.let { camera ->
            // Step 1: Unlock and set camera to MediaRecorder
            camera?.unlock()

            mediaRecorder?.run {
                setCamera(camera)

                // Step 2: Set sources
                setVideoSource(MediaRecorder.VideoSource.CAMERA)

                // Step 4: Set output file
                setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO).toString())


                // Step 5: Set the preview output
                setPreviewDisplay(mPreview?.holder?.surface)


                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
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


