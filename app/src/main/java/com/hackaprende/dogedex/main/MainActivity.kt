package com.hackaprende.dogedex.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.hackaprende.dogedex.LABEL_PATH
import com.hackaprende.dogedex.MODEL_PATH
import com.hackaprende.dogedex.R
import com.hackaprende.dogedex.api.ApiResponseStatus
import com.hackaprende.dogedex.api.ApiServiceInterceptor
import com.hackaprende.dogedex.auth.LoginActivity
import com.hackaprende.dogedex.databinding.ActivityMainBinding
import com.hackaprende.dogedex.dogdetail.DogDetailActivity
import com.hackaprende.dogedex.dogdetail.DogDetailActivity.Companion.DOG_KEY
import com.hackaprende.dogedex.dogdetail.DogDetailActivity.Companion.IS_RECOGNITION_KEY
import com.hackaprende.dogedex.doglist.DogListActivity
import com.hackaprende.dogedex.machinelearning.Classifier
import com.hackaprende.dogedex.machinelearning.DogRecognition
import com.hackaprende.dogedex.model.Dog
import com.hackaprende.dogedex.model.User
import com.hackaprende.dogedex.settings.SettingsActivity
import org.tensorflow.lite.support.common.FileUtil
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                setupCamera()
            } else {
                Toast.makeText(this, R.string.camera_permission_rejected_message,
                    Toast.LENGTH_SHORT).show()
            }
        }

    private lateinit var binding: ActivityMainBinding
    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var classifier: Classifier
    private var isCameraReady = false
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val user = User.getLoggedInUser(this)
        if (user == null) {
            openLoginActivity()
            return
        } else {
            ApiServiceInterceptor.setSessionToken(user.authenticationToken)
        }

        binding.settingsFab.setOnClickListener {
            openSettingsActivity()
        }

        binding.dogListFab.setOnClickListener {
            openDogListActivity()
        }

        viewModel.status.observe(this) {
                status ->

            when(status) {
                is ApiResponseStatus.Error -> {
                    binding.loadingWheel.visibility = View.GONE
                    Toast.makeText(this, status.messageId, Toast.LENGTH_SHORT).show()
                }
                is ApiResponseStatus.Loading -> binding.loadingWheel.visibility = View.VISIBLE
                is ApiResponseStatus.Success -> binding.loadingWheel.visibility = View.GONE
            }
        }

        viewModel.dog.observe(this) {
            dog ->
            if (dog != null) {
                openDogDetailActivity(dog)
            }
        }

        viewModel.dogRecognition.observe(this) {
            enabledTakePhotoButton(it)
        }

        requestCameraPermission()
    }

    private fun openDogDetailActivity(dog: Dog) {
        val intent = Intent(this, DogDetailActivity::class.java)
        intent.putExtra(DOG_KEY, dog)
        intent.putExtra(IS_RECOGNITION_KEY, true)
        startActivity(intent)
    }

    override fun onStart() {
        super.onStart()
        viewModel.setupClassifier(
            FileUtil.loadMappedFile(this@MainActivity, MODEL_PATH),
            FileUtil.loadLabels(this@MainActivity, LABEL_PATH)
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::cameraExecutor.isInitialized) {
            cameraExecutor.shutdown()
        }
    }

    private fun setupCamera() {
        binding.cameraPreview.post {
            imageCapture = ImageCapture.Builder()
                .setTargetRotation(binding.cameraPreview.display.rotation)
                .build()
            cameraExecutor = Executors.newSingleThreadExecutor()
            startCamera()
            isCameraReady = true
        }
    }

    private fun requestCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED -> {
                    setupCamera()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                    AlertDialog.Builder(this)
                        .setTitle(R.string.camera_permission_rationale_dialog_title)
                        .setMessage(R.string.camera_permission_rationale_dialog_message)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            requestPermissionLauncher.launch(
                                Manifest.permission.CAMERA
                            )
                        }
                        .setNegativeButton(android.R.string.cancel) { _, _ ->
                        }.show()
                }
                else -> {
                    requestPermissionLauncher.launch(
                        Manifest.permission.CAMERA
                    )
                }
            }
        } else {
            setupCamera()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider = cameraProviderFuture.get()
            // Preview
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(binding.cameraPreview.surfaceProvider)

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                viewModel.recognizeImage(imageProxy)
            }

            // Bind use cases to camera
            cameraProvider.bindToLifecycle(
                this, cameraSelector,
                preview, imageCapture, imageAnalysis
            )
        }, ContextCompat.getMainExecutor(this))
    }

    private fun enabledTakePhotoButton(dogRecognition: DogRecognition) {
        if (dogRecognition.confidence > 70.0) {
            binding.takePhotoFab.alpha = 1f
            binding.takePhotoFab.setOnClickListener {
                viewModel.getDogByMlId(dogRecognition.id)
            }
        } else {
            binding.takePhotoFab.alpha = 0.2f
            binding.takePhotoFab.setOnClickListener(null)
        }
    }

    private fun openDogListActivity() {
        startActivity(Intent(this, DogListActivity::class.java))
    }

    private fun openSettingsActivity() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    private fun openLoginActivity() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}