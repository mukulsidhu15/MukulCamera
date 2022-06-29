package com.example.mukulcamera

import java.util.jar.Manifest

object Constants {

    const val  TAG = "cameraX"
    const val FILE_NAME_FORMAT = "yy-MM-dd-HH-mm-ss-SS"
    const val REQUEST_CODE_PERMISSION = 123
    val REQUIRED_PERMISSION = arrayOf(android.Manifest.permission.CAMERA,android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.RECORD_AUDIO)


}