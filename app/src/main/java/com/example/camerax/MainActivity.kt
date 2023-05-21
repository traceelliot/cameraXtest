package com.example.camerax


import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.camerax.ui.theme.CameraXTheme
import androidx.camera.core.*
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.example.camerax.MainActivity.Companion.CAMERA_AUDIO_PERMISSION_CODE
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class MainActivity : ComponentActivity() {

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_AUDIO_PERMISSION_CODE){
            if(grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "Permissao Ok", Toast.LENGTH_LONG).show()
            }else{
                Toast.makeText(this, "Permissao Nao", Toast.LENGTH_LONG).show()
            }
        }

    }

    companion object{
        const val CAMERA_AUDIO_PERMISSION_CODE = 1000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var readQRcode by remember {
                mutableStateOf(true)
            }
            CameraXTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if(!isCameraPermissionGranted(this)){
                        requestCameraPermission(this)
                    } else {
                        var textQrCode by remember {
                            mutableStateOf("")
                        }
                        Column {
                            if(readQRcode){
                                CameraPreview {
                                    //Toast.makeText(applicationContext, it, Toast.LENGTH_LONG).show()
                                    textQrCode = it
                                    readQRcode = false
                                }
                            }
                            //Button and text
                            Spacer(modifier = Modifier.size(50.dp))
                            Text(text = "text: $textQrCode")
                            Button(onClick = {
                                readQRcode = true
                                textQrCode = ""
                            }) {
                                Text(text = "Start")
                            }
                        }

                    }
                }
            }
        }
    }
}

//Function permissions

private fun isCameraPermissionGranted(context: Context) : Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
}

private fun requestCameraPermission(context: Context){
    ActivityCompat.requestPermissions(
        context as Activity,
        arrayOf(Manifest.permission.CAMERA),
        CAMERA_AUDIO_PERMISSION_CODE
    )
}

//END Function permissions


@SuppressLint("UnsafeOptInUsageError")
@Composable
fun CameraPreview(
    onQRCodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { context ->
            PreviewView(context).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
        },
        update = { view ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener(
                Runnable {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build()
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    val options = BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                        .build()
                    val scanner = BarcodeScanning.getClient(options)

                    val imageAnalyzer = ImageAnalysis.Analyzer { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )
                            scanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    for (barcode in barcodes) {
                                        if (barcode.valueType == Barcode.TYPE_TEXT) {
                                            val qrCode = barcode.rawValue ?: ""
                                            onQRCodeScanned(qrCode)
                                        }
                                    }
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    }

                    imageAnalysis.setAnalyzer(
                        ContextCompat.getMainExecutor(context),
                        imageAnalyzer
                    )

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )

                    preview.setSurfaceProvider(view.surfaceProvider)
                },
                ContextCompat.getMainExecutor(context)
            )
        }
    )
}





