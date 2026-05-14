package com.billingsdk.utils

internal object BillingLogger {
    private const val TAG = "BillingSDK"

    @Volatile
    var enabled: Boolean = false
        private set

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    fun d(message: String) {
        if (enabled) android.util.Log.d(TAG, message)
    }

    fun w(message: String, throwable: Throwable? = null) {
        if (enabled) {
            if (throwable == null) android.util.Log.w(TAG, message)
            else android.util.Log.w(TAG, message, throwable)
        }
    }

    fun e(message: String, throwable: Throwable? = null) {
        if (enabled) {
            if (throwable == null) android.util.Log.e(TAG, message)
            else android.util.Log.e(TAG, message, throwable)
        }
    }
}
