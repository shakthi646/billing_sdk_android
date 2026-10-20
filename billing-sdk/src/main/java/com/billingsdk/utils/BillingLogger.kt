package com.billingsdk.utils

internal object BillingLogger {
    private const val TAG = "BillingSDK"

    @Volatile
    var enabled: Boolean = false
        private set

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    /** Verbose tracing. Only emitted when the host opted in (typically debug builds). */
    fun d(message: String) {
        if (enabled) android.util.Log.d(TAG, message)
    }

    /**
     * Problems are logged unconditionally, including in release builds.
     *
     * Billing failures are rare, low-volume, and carry no user data, and when a
     * paywall comes up empty in production the response code is the only thing
     * that explains why. Gating this behind a debug flag made exactly the builds
     * that matter the silent ones.
     */
    fun w(message: String, throwable: Throwable? = null) {
        if (throwable == null) android.util.Log.w(TAG, message)
        else android.util.Log.w(TAG, message, throwable)
    }

    fun e(message: String, throwable: Throwable? = null) {
        if (throwable == null) android.util.Log.e(TAG, message)
        else android.util.Log.e(TAG, message, throwable)
    }
}
