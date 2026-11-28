package com.ashmeet.hyperlauncher.utils

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import android.widget.ToggleButton
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import coil.Coil
import coil.request.ImageRequest
import coil.target.Target
import com.ashmeet.hyperbackend.imgcropper.BitmapCropBehaviour
import com.ashmeet.hyperbackend.imgcropper.CropperBehaviour
import com.ashmeet.hyperbackend.imgcropper.CropperView
import com.ashmeet.hyperbackend.imgcropper.RegionDecoderCropBehaviour
import com.ashmeet.hyperlauncher.activity.PojavApplication
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import net.ashmeet.hyperlauncher.R
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object CropperUtils {

    @JvmStatic
    fun registerCropper(activity: AppCompatActivity, cropperReceiver: CropperReceiver): ActivityResultLauncher<*> {
        return registerCropper(ActivityContextProvider(activity), cropperReceiver)
    }

    @JvmStatic
    fun registerCropper(fragment: Fragment, cropperReceiver: CropperReceiver): ActivityResultLauncher<*> {
        return registerCropper(FragmentContextProvider(fragment), cropperReceiver)
    }

    private fun registerCropper(contextProvider: ContextProvider, cropperReceiver: CropperReceiver): ActivityResultLauncher<Array<String>> {
        return contextProvider.resultCaller.registerForActivityResult(ActivityResultContracts.OpenDocument()) { result ->
            val context = contextProvider.context ?: return@registerForActivityResult
            if (result == null) {
                Toast.makeText(context, R.string.cropper_select_cancelled, Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }
            openCropperDialog(context, result, cropperReceiver)
        }
    }

    private fun openCropperDialog(
        context: Context,
        selectedUri: Uri,
        cropperReceiver: CropperReceiver
    ) {
        val contentResolver = context.contentResolver
        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.cropper_title)
            .setView(R.layout.dialog_cropper)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null)
            .show()
        val cropImageView = dialog.findViewById<CropperView>(R.id.crop_dialog_view)
        val finishProgressBar = dialog.findViewById<View>(R.id.crop_dialog_progressbar)
        requireNotNull(cropImageView)
        requireNotNull(finishProgressBar)
        bindViews(dialog, cropImageView)
        cropImageView.setAspectRatio(cropperReceiver.getAspectRatio())
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            dialog.dismiss()
            cropperReceiver.onCropped(cropImageView.crop(cropperReceiver.getTargetMaxSide()))
        }
        PojavApplication.sExecutorService.execute {
            var cropperBehaviour: CropperBehaviour? = null
            try {
                cropperBehaviour = createBehaviour(cropImageView, contentResolver, selectedUri)
            } catch (e: Exception) {
                cropperReceiver.onFailed(e)
            }
            val finalBehaviour = cropperBehaviour
            HyperUtils.runOnUiThread { finishSetup(dialog, finishProgressBar, cropImageView, finalBehaviour) }
        }
    }

    private fun fixDialogHeight(dialog: AlertDialog) {
        val dialogWindow = dialog.window
        dialogWindow?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun finishSetup(
        dialog: AlertDialog,
        progressBar: View,
        cropImageView: CropperView,
        cropperBehaviour: CropperBehaviour?
    ) {
        if (cropperBehaviour == null) {
            dialog.dismiss()
            return
        }
        progressBar.visibility = View.GONE
        cropImageView.setCropperBehaviour(cropperBehaviour)
        cropperBehaviour.applyImage()
        cropImageView.post {
            fixDialogHeight(dialog)
            cropImageView.requestLayout()
        }
    }

    @Throws(Exception::class)
    private fun createBehaviour(
        cropImageView: CropperView,
        contentResolver: ContentResolver,
        selectedUri: Uri
    ): CropperBehaviour? {
        try {
            contentResolver.openInputStream(selectedUri)?.use { inputStream ->
                try {
                    @Suppress("DEPRECATION")
                    val regionDecoder = BitmapRegionDecoder.newInstance(inputStream, false)
                    if (regionDecoder != null) {
                        val cropBehaviour = RegionDecoderCropBehaviour(cropImageView)
                        cropBehaviour.setRegionDecoder(regionDecoder)
                        return cropBehaviour
                    }
                } catch (e: IOException) {
                    Log.w("CropperUtils", "Failed to load image into BitmapRegionDecoder", e)
                }
            }
        } catch (e: Exception) {
            Log.w("CropperUtils", "Error opening stream for BitmapRegionDecoder", e)
        }

        val context = cropImageView.context
        if (context != null) {
            try {
                val latch = CountDownLatch(1)
                val resultBitmap = arrayOfNulls<Bitmap>(1)
                val request = ImageRequest.Builder(context)
                    .data(selectedUri)
                    .size(2048, 2048)
                    .allowHardware(false)
                    .target(object : Target {
                        override fun onSuccess(result: Drawable) {
                            if (result is BitmapDrawable) {
                                resultBitmap[0] = result.bitmap
                            }
                            latch.countDown()
                        }

                        override fun onError(error: Drawable?) {
                            latch.countDown()
                        }
                    })
                    .build()
                Coil.imageLoader(context).enqueue(request)
                try {
                    latch.await(10, TimeUnit.SECONDS)
                } catch (_: InterruptedException) {}

                val bitmap = resultBitmap[0]
                if (bitmap != null) {
                    val cropBehaviour = BitmapCropBehaviour(cropImageView)
                    cropBehaviour.setBitmap(bitmap)
                    return cropBehaviour
                }
            } catch (e: Exception) {
                Log.w("CropperUtils", "Failed to load image via Coil", e)
            }
        }

        contentResolver.openInputStream(selectedUri)?.use { inputStream ->
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeStream(inputStream, null, options)
            options.inSampleSize = calculateInSampleSize(options, 2048, 2048)
            options.inJustDecodeBounds = false

            contentResolver.openInputStream(selectedUri)?.use { innerInput ->
                val originalBitmap = BitmapFactory.decodeStream(innerInput, null, options)
                    ?: throw IOException("Image format not supported")
                val cropBehaviour = BitmapCropBehaviour(cropImageView)
                cropBehaviour.setBitmap(originalBitmap)
                return cropBehaviour
            }
        }
        return null
    }

    @Suppress("SameParameterValue")
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun bindViews(alertDialog: AlertDialog, imageCropperView: CropperView) {
        val horizontalLock = alertDialog.findViewById<ToggleButton>(R.id.crop_dialog_hlock)
        val verticalLock = alertDialog.findViewById<ToggleButton>(R.id.crop_dialog_vlock)
        val reset = alertDialog.findViewById<View>(R.id.crop_dialog_reset)
        requireNotNull(horizontalLock)
        requireNotNull(verticalLock)
        requireNotNull(reset)
        horizontalLock.setOnClickListener {
            imageCropperView.horizontalLock = horizontalLock.isChecked
        }
        verticalLock.setOnClickListener {
            imageCropperView.verticalLock = verticalLock.isChecked
        }
        reset.setOnClickListener {
            imageCropperView.resetTransforms()
        }
    }

    @Suppress("UNCHECKED_CAST")
    @JvmStatic
    fun startCropper(resultLauncher: ActivityResultLauncher<*>) {
        val realResultLauncher = resultLauncher as ActivityResultLauncher<Array<String>>
        realResultLauncher.launch(arrayOf("image/*"))
    }

    interface CropperReceiver {
        fun getAspectRatio(): Float
        fun getTargetMaxSide(): Int
        fun onCropped(contentBitmap: Bitmap?)
        fun onFailed(exception: Exception?)
    }

    private interface ContextProvider {
        val context: Context?
        val resultCaller: ActivityResultCaller
    }

    private class FragmentContextProvider(private val mFragment: Fragment) : ContextProvider {
        override val context: Context?
            get() = mFragment.context

        override val resultCaller: ActivityResultCaller
            get() = mFragment
    }

    private class ActivityContextProvider(private val mActivity: AppCompatActivity) : ContextProvider {
        override val context: Context?
            get() {
                if (mActivity.isDestroyed || mActivity.isFinishing) return null
                return mActivity
            }

        override val resultCaller: ActivityResultCaller
            get() = mActivity
    }
}
