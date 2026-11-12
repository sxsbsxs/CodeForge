package dev.codeforge

import android.app.IntentService
import android.content.Intent

class TermuxResultService : IntentService("CodeForgeTermuxResultService") {
    override fun onHandleIntent(intent: Intent?) {
        if (intent == null) return
        val requestId = intent.getIntExtra(EXTRA_EXECUTION_ID, -1)
        if (requestId < 0) return

        val bundle = intent.getBundleExtra(EXTRA_RESULT_BUNDLE)
        if (bundle == null) {
            TermuxResultRegistry.complete(
                requestId,
                TermuxCommandResult("", "", -1, "Termux returned no result bundle.")
            )
            return
        }

        val internalErrorCode = bundle.getInt("err", -1)
        val internalError = if (internalErrorCode == -1) {
            null
        } else {
            bundle.getString("errmsg") ?: "Termux error code $internalErrorCode"
        }

        TermuxResultRegistry.complete(
            requestId,
            TermuxCommandResult(
                stdout = bundle.getString("stdout").orEmpty(),
                stderr = bundle.getString("stderr").orEmpty(),
                exitCode = bundle.getInt("exitCode", -1),
                internalError = internalError
            )
        )
    }

    companion object {
        const val EXTRA_EXECUTION_ID = "codeforge_execution_id"
        private const val EXTRA_RESULT_BUNDLE = "result"
    }
}
