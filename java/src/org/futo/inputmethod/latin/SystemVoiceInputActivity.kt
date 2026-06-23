package org.futo.inputmethod.latin

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent

object SystemVoiceInputSession {
    private var onResult: ((String) -> Unit)? = null
    private var onFallback: (() -> Unit)? = null

    fun start(
        context: Context,
        languageTag: String?,
        onResult: (String) -> Unit,
        onFallback: () -> Unit
    ): Boolean {
        this.onResult = onResult
        this.onFallback = onFallback

        val intent = Intent(context, SystemVoiceInputActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            languageTag?.let { putExtra(SystemVoiceInputActivity.EXTRA_LANGUAGE_TAG, it) }
        }

        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            clear()
            false
        }
    }

    internal fun commit(text: String?) {
        val callback = onResult
        clear()

        val cleanedText = text?.trim()
        if (!cleanedText.isNullOrEmpty()) {
            callback?.invoke(cleanedText)
        }
    }

    internal fun fallback() {
        val callback = onFallback
        clear()
        callback?.invoke()
    }

    internal fun cancel() {
        clear()
    }

    private fun clear() {
        onResult = null
        onFallback = null
    }
}

class SystemVoiceInputActivity : Activity() {
    private var launched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            launched = savedInstanceState.getBoolean(STATE_LAUNCHED, false)
        }

        if (!launched) {
            launchRecognizer()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_LAUNCHED, launched)
        super.onSaveInstanceState(outState)
    }

    private fun launchRecognizer() {
        launched = true

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            intent.getStringExtra(EXTRA_LANGUAGE_TAG)?.let {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, it)
            }
        }

        try {
            startActivityForResult(intent, REQUEST_RECOGNIZE_SPEECH)
        } catch (_: ActivityNotFoundException) {
            SystemVoiceInputSession.fallback()
            finish()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_RECOGNIZE_SPEECH) {
            if (resultCode == RESULT_OK) {
                val text = data
                    ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    ?.firstOrNull()
                SystemVoiceInputSession.commit(text)
            } else {
                SystemVoiceInputSession.cancel()
            }

            finish()
        }
    }

    companion object {
        const val EXTRA_LANGUAGE_TAG = "languageTag"

        private const val REQUEST_RECOGNIZE_SPEECH = 1
        private const val STATE_LAUNCHED = "launched"
    }
}
