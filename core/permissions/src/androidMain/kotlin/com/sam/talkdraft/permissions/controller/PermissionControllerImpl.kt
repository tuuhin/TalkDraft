package com.sam.talkdraft.permissions.controller

import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Singleton

@Singleton(binds = [IPermissionController::class])
internal class PermissionControllerImpl(
    private val dispatches: IPlatformCoroutineDispatchers,
) : IPermissionController, DefaultLifecycleObserver {

    private var _activity: ComponentActivity? = null
    private var _launcher: ActivityResultLauncher<Array<String>>? = null
    private var _currentCallback: ((Map<String, @JvmSuppressWildcards Boolean>) -> Unit)? = null

    override fun bindToActivity(activity: ComponentActivity) {
        this._activity = activity
        _activity?.lifecycle?.addObserver(this)
    }

    override fun isActivityBounded(): Boolean {
        return _activity != null
    }

    override suspend fun requestPermission(permission: Array<String>): Map<String, @JvmSuppressWildcards Boolean> {
        val activity = _activity ?: return permission.associateWith { false }

        val grantMap = permission.associateWith { permission ->
            ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
        }

        val requestRequired = permission.filter { grantMap[it] == false }
            .toTypedArray()

        if (requestRequired.isEmpty()) return emptyMap()

        val launcher = _launcher
            ?: return permission.associateWith { false }

        return withContext(dispatches.mainImmediate) {
            suspendCancellableCoroutine { cont ->

                _currentCallback = { resultMap ->
                    if (cont.isActive) cont.resume(resultMap)
                }

                cont.invokeOnCancellation {
                    _currentCallback = null
                }
                launcher.launch(requestRequired)
            }
        }
    }

    override suspend fun shouldShowRational(permission: String): Boolean {
        val activity = _activity ?: return false
        return withContext(dispatches.mainImmediate) {
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        val currentActivity = _activity ?: return

        // Register launcher with the activity's registry
        _launcher = currentActivity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { resultMap ->
            _currentCallback?.invoke(resultMap)
            _currentCallback = null
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        // Prevent leaks when activity is destroyed
        _activity?.lifecycle?.removeObserver(this)
        _activity = null
        _launcher = null
        _currentCallback = null
    }
}
