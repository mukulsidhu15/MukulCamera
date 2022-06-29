package com.example.mukulcamera

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.video.VideoCapture
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.example.mukulcamera.Constants.TAG
import com.example.mukulcamera.databinding.ActivityMainBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var imageCapture: ImageCapture?=null
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutors: ExecutorService
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    var s= "Mukul"
    var m= "ManishaSharma"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        outputDirectory = getoutputDirectory()
        cameraExecutors = Executors.newSingleThreadExecutor()

        if (allPermisssionGranted())
        {
             startCamera()
        }
        else{
            ActivityCompat.requestPermissions(this, Constants.REQUIRED_PERMISSION, Constants.REQUEST_CODE_PERMISSION)
        }

        binding.buttonTakePhoto.setOnClickListener(){
            takePhoto()
        }

        binding.buttonVideoCapture.setOnClickListener(){
            captureVideo()
        }
    }

    private fun getoutputDirectory(): File{
       val mediaDir = externalMediaDirs.firstOrNull()?.let { mFile->
            File(mFile.absolutePath,"/MyPics").apply {
                mkdirs()
            }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }


    private fun takePhoto(){
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        val name = SimpleDateFormat(Constants.FILE_NAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())

        // Create time stamped name and MediaStore entry.
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        // Create output options object which contains file + metadata
        val outputOption = ImageCapture
            .OutputFileOptions
            .Builder(contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
            .build()

        // Set up image capture listener, which is triggered after photo has been taken
        imageCapture.takePicture(outputOption, ContextCompat.getMainExecutor(this),
               object : ImageCapture.OnImageSavedCallback{
                   override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {

                       val msg = "Photo capture succeeded: ${outputFileResults.savedUri}"
                       Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                       Log.d(TAG, msg)

                   }

                   override fun onError(exception: ImageCaptureException) {
                       Log.e(TAG, "onError: ${exception.message}",exception )
                   }
               }
            )
    }

    private fun captureVideo() {
        val videoCapture = this.videoCapture ?: return

        binding.buttonVideoCapture.isEnabled = false

        val curRecording = recording
        if (curRecording != null) {
            // Stop the current recording session.
            curRecording.stop()
            recording = null
            return
        }

        // create and start a new recording session
        val name = SimpleDateFormat(Constants.FILE_NAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
            }
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()
        recording = videoCapture.output
            .prepareRecording(this, mediaStoreOutputOptions)
            .apply {
                if (PermissionChecker.checkSelfPermission(this@MainActivity,
                        Manifest.permission.RECORD_AUDIO) ==
                    PermissionChecker.PERMISSION_GRANTED)
                {
                    withAudioEnabled()
                }
            }
            .start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                when(recordEvent) {
                    is VideoRecordEvent.Start -> {
                        binding.buttonVideoCapture.apply {
                            text = getString(R.string.stop_capture)
                            isEnabled = true
                        }
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            val msg = "Video capture succeeded: " +
                                    "${recordEvent.outputResults.outputUri}"
                            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT)
                                .show()
                            Log.d(TAG, msg)
                        } else {
                            recording?.close()
                            recording = null
                            Log.e(TAG, "Video capture ends with error: " +
                                    "${recordEvent.error}")
                        }
                        binding.buttonVideoCapture.apply {
                            text = getString(R.string.start_capture)
                            isEnabled = true
                        }
                    }
                }
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Constants.REQUEST_CODE_PERMISSION){
            startCamera()
       }else{
           Toast.makeText(this, "Permission not granted by user", Toast.LENGTH_SHORT).show()
           finish()
       }
    }

    // Start camera after granted Permission
    private fun startCamera(){
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also { mPreview->
                    mPreview.setSurfaceProvider(binding.previewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .build()

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST,
                    FallbackStrategy.higherQualityOrLowerThan(Quality.SD)))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                 // Unbind use cases before rebinding
                 cameraProvider.unbindAll()
                 // Bind use cases to camera
                 cameraProvider.bindToLifecycle(this, cameraSelector,preview,imageCapture, videoCapture)
            } catch (e: Exception){
                Log.d(TAG, "startCamera Fail",e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

   // For access all Permission
   private fun allPermisssionGranted() =
       Constants.REQUIRED_PERMISSION.all {
           ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
       }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutors.shutdown()
    }

}