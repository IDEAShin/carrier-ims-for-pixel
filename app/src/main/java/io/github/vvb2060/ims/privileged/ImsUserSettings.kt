package io.github.vvb2060.ims.privileged

import android.annotation.SuppressLint
import android.app.Activity
import android.app.IActivityManager
import android.app.Instrumentation
import android.content.Context
import android.os.Bundle
import android.os.ServiceManager
import android.system.Os
import android.telephony.ims.ImsManager
import android.util.Log
import java.lang.reflect.InvocationTargetException
import rikka.shizuku.ShizukuBinderWrapper

/** Reads and updates the per-subscription IMS user settings behind the system VoLTE UI. */
class ImsUserSettings : Instrumentation() {
    companion object {
        private const val TAG = "ImsUserSettings"

        const val BUNDLE_SELECT_SIM_ID = "select_sim_id"
        const val BUNDLE_SET_ADVANCED_CALLING = "set_advanced_calling"
        const val BUNDLE_RESULT = "result"
        const val BUNDLE_RESULT_MSG = "result_msg"
        const val BUNDLE_ADVANCED_CALLING_ENABLED = "advanced_calling_enabled"
        const val BUNDLE_VOWIFI_ENABLED = "vowifi_enabled"
        const val BUNDLE_VOWIFI_RESULT_MSG = "vowifi_result_msg"
    }

    @SuppressLint("MissingPermission")
    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        if (arguments == null) {
            finish(Activity.RESULT_CANCELED, Bundle())
            return
        }

        val result = Bundle()
        if (!waitForShizukuBinderReady()) {
            result.putBoolean(BUNDLE_RESULT, false)
            result.putString(BUNDLE_RESULT_MSG, "shizuku binder is not ready")
            finish(Activity.RESULT_OK, result)
            return
        }

        val binder = ServiceManager.getService(Context.ACTIVITY_SERVICE)
        val am = IActivityManager.Stub.asInterface(ShizukuBinderWrapper(binder))
        var delegated = false
        try {
            am.startDelegateShellPermissionIdentity(Os.getuid(), null)
            delegated = true

            val subId = arguments.getInt(BUNDLE_SELECT_SIM_ID, -1)
            require(subId >= 0) { "invalid subId" }

            val imsManager = context.getSystemService(ImsManager::class.java)
                ?: error("IMS service is unavailable")
            val mmTelManager = imsManager.getImsMmTelManager(subId)

            if (arguments.containsKey(BUNDLE_SET_ADVANCED_CALLING)) {
                val requested = arguments.getBoolean(BUNDLE_SET_ADVANCED_CALLING)
                invokeBooleanSetter(
                    target = mmTelManager,
                    methodNames = listOf(
                        "setAdvancedCallingSettingEnabled",
                        "setAdvancedCallingSetting",
                    ),
                    value = requested,
                )
            }

            val advancedCallingEnabled = invokeBooleanGetter(
                target = mmTelManager,
                methodNames = listOf(
                    "isAdvancedCallingSettingEnabled",
                    "isAdvancedCallingEnabled",
                ),
            )
            result.putBoolean(BUNDLE_ADVANCED_CALLING_ENABLED, advancedCallingEnabled)

            if (arguments.containsKey(BUNDLE_SET_ADVANCED_CALLING)) {
                val requested = arguments.getBoolean(BUNDLE_SET_ADVANCED_CALLING)
                check(advancedCallingEnabled == requested) {
                    "advanced calling setting remained $advancedCallingEnabled after requesting $requested"
                }
            }

            runCatching {
                invokeBooleanGetter(
                    target = mmTelManager,
                    methodNames = listOf("isVoWiFiSettingEnabled"),
                )
            }.onSuccess {
                result.putBoolean(BUNDLE_VOWIFI_ENABLED, it)
            }.onFailure {
                val root = unwrapInvocationFailure(it)
                Log.w(TAG, "read VoWiFi user setting failed", root)
                result.putString(
                    BUNDLE_VOWIFI_RESULT_MSG,
                    root.message ?: root.javaClass.simpleName,
                )
            }

            Log.i(
                TAG,
                "IMS user settings: subId=$subId advancedCalling=$advancedCallingEnabled " +
                    "voWiFi=${if (result.containsKey(BUNDLE_VOWIFI_ENABLED)) result.getBoolean(BUNDLE_VOWIFI_ENABLED) else "N/A"}",
            )
            result.putBoolean(BUNDLE_RESULT, true)
        } catch (t: Throwable) {
            val root = unwrapInvocationFailure(t)
            Log.e(TAG, "read or update IMS user settings failed", root)
            result.putBoolean(BUNDLE_RESULT, false)
            result.putString(BUNDLE_RESULT_MSG, root.message ?: root.javaClass.simpleName)
        } finally {
            if (delegated) {
                runCatching { am.stopDelegateShellPermissionIdentity() }
                    .onFailure { Log.w(TAG, "stop delegate shell identity failed", it) }
            }
        }

        finish(Activity.RESULT_OK, result)
    }

    private fun invokeBooleanGetter(target: Any, methodNames: List<String>): Boolean {
        val method = methodNames.firstNotNullOfOrNull { name ->
            runCatching { target.javaClass.getMethod(name) }.getOrNull()
        } ?: error("IMS setting getter is unavailable: ${methodNames.joinToString()}")
        return method.invoke(target) as? Boolean
            ?: error("IMS setting getter returned a non-boolean value")
    }

    private fun invokeBooleanSetter(target: Any, methodNames: List<String>, value: Boolean) {
        val method = methodNames.firstNotNullOfOrNull { name ->
            runCatching {
                target.javaClass.getMethod(name, Boolean::class.javaPrimitiveType)
            }.getOrNull()
        } ?: error("IMS setting setter is unavailable: ${methodNames.joinToString()}")
        method.invoke(target, value)
    }

    private fun unwrapInvocationFailure(throwable: Throwable): Throwable {
        return if (throwable is InvocationTargetException && throwable.targetException != null) {
            throwable.targetException
        } else {
            throwable
        }
    }
}
