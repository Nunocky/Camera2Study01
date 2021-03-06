package org.nunocky.camera2study01

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.view.Surface
import android.view.TextureView
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import org.nunocky.camera2study01.databinding.ActivityMainBinding
import pub.devrel.easypermissions.EasyPermissions

class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {
    companion object {
        private const val REQUEST_CODE = 0
        private const val CAMERA_ID = "1"
    }

    private val viewModel: MainViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null

    private val cameraManager: CameraManager by lazy {
        getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        if (binding.textureView.isAvailable) {
            openCamera()
        } else {
            binding.textureView.surfaceTextureListener =
                object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(
                        surface: SurfaceTexture,
                        width: Int,
                        height: Int
                    ) {
                        rotateTextureView()
                        openCamera()
                    }

                    override fun onSurfaceTextureSizeChanged(
                        surface: SurfaceTexture,
                        width: Int,
                        height: Int
                    ) {
                        rotateTextureView()
                    }

                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                        return true
                    }

                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}

                }
        }

        viewModel.rotation.observe(this) { rotateTextureView() }
        viewModel.scaleX.observe(this) { rotateTextureView() }
        viewModel.scaleY.observe(this) { rotateTextureView() }

        val permissions = arrayOf(
            Manifest.permission.CAMERA
        )

        if (!EasyPermissions.hasPermissions(this, *permissions)) {
            EasyPermissions.requestPermissions(this, "パーミッションに関する説明", REQUEST_CODE, *permissions)
            return
        }
    }

    override fun onPermissionsGranted(requestCode: Int, list: List<String>) {
        // ユーザーの許可が得られたときに呼び出される
        recreate()
    }

    override fun onPermissionsDenied(requestCode: Int, list: List<String>) {
        // ユーザーの許可が得られなかったときに呼び出される
        finish()
    }

    private fun openCamera() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        cameraManager.openCamera(CAMERA_ID, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                createCameraPreviewSession()
            }

            override fun onDisconnected(camera: CameraDevice) {
                cameraDevice?.close()
                cameraDevice = null
            }

            override fun onError(camera: CameraDevice, p1: Int) {
                cameraDevice?.close()
                cameraDevice = null
            }
        }, null)
    }

    private fun createCameraPreviewSession() {
        if (cameraDevice == null) {
            return
        }
        val texture = binding.textureView.surfaceTexture
        texture?.setDefaultBufferSize(500, 1000)
        val surface = Surface(texture)

        val previewRequestBuilder =
            cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        previewRequestBuilder.addTarget(surface)

        cameraDevice?.createCaptureSession(
            listOf(surface),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    captureSession?.setRepeatingRequest(previewRequestBuilder.build(), null, null)
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {}
            },
            null
        )
    }

    private fun getCameraOrientation(cameraId: String): Int {
        val characteristic = cameraManager.getCameraCharacteristics(cameraId)
        return characteristic.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
    }

    private fun getApplicationOrientation(): Int {
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val rotation = windowManager.defaultDisplay.rotation
        return when (rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> throw IllegalStateException()
        }
    }

    private fun rotateTextureView() {
        val orientation = getApplicationOrientation()
        val viewWidth = binding.textureView.width
        val viewHeight = binding.textureView.height

//        val matrix = Matrix()
//        matrix.postRotate(-orientation.toFloat(), viewWidth * 0.5F, viewHeight * 0.5F)
//        binding.textureView.setTransform(matrix)

        val centerX = viewWidth / 2f
        val centerY = viewHeight / 2f

        val matrix = Matrix()
        val rot = viewModel.rotation.value ?: 0
        val scaleX = (viewModel.scaleX.value ?: 100) / 100f
        val scaleY = (viewModel.scaleY.value ?: 100) / 100f

        matrix.postScale(scaleX, scaleY, centerX, centerY)
        matrix.postRotate(rot.toFloat(), centerX, centerY)
        binding.textureView.setTransform(matrix)
    }

}