package com.lighttool.dengyu.service

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager

class TorchController(context: Context) {

    private val cameraManager = context.getSystemService(CameraManager::class.java)
    private val cameraId: String? by lazy {
        cameraManager.cameraIdList.firstOrNull { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val flashAvailable =
                characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
            flashAvailable && facing == CameraCharacteristics.LENS_FACING_BACK
        }
    }

    fun isFlashAvailable(): Boolean = cameraId != null

    fun setTorch(enabled: Boolean): Result<Unit> {
        val targetCameraId = cameraId ?: return Result.failure(
            IllegalStateException("当前设备没有可用闪光灯")
        )
        return runCatching {
            cameraManager.setTorchMode(targetCameraId, enabled)
        }.recoverCatching { throwable ->
            if (throwable is CameraAccessException) {
                throw IllegalStateException("无法访问闪光灯，请检查相机权限或系统占用")
            } else {
                throw throwable
            }
        }
    }
}
