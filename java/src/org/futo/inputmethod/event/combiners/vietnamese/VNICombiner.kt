package org.futo.inputmethod.event.combiners.vietnamese

import android.text.TextUtils
import org.futo.inputmethod.event.Combiner
import org.futo.inputmethod.event.Event
import org.futo.inputmethod.latin.common.Constants
import java.util.ArrayList

class VNICombiner: Combiner {
    private val buffer = StringBuilder() // holds a single Vietnamese word/syllable

    override fun processEvent(
        previousEvents: ArrayList<Event?>?,
        event: Event?
    ): Event {
        if (event == null) return Event.createNotHandledEvent()
        if (event.eventType != Event.EVENT_TYPE_INPUT_KEYPRESS && event.eventType != Event.EVENT_TYPE_INPUT_KEYPRESS_RESUMED) return event

        val keypress = event.mCodePoint.toChar()

        if (keypress.code in 0xFF10..0xFF19) {
            buffer.append((keypress.code - 0xFEE0).toChar())
            return Event.createConsumedEvent(event)
        }

        val decomposed = Common.decomposeVni(keypress)

        if (event.mKeyCode == Constants.CODE_DELETE) {
            if (!TextUtils.isEmpty(buffer)) {
                val smartDelete = org.futo.inputmethod.latin.settings.Settings.getInstance().current?.mSmartBackspaceDeleteTone ?: true
                val currentWord = getCombiningStateFeedback()?.toString() ?: ""
                val hasDiacritics = currentWord.any { it.code > 127 }
                if (smartDelete || !hasDiacritics) {
                    buffer.setLength(buffer.length - 1)
                } else {
                    if (currentWord.isNotEmpty()) {
                        val newWord = currentWord.substring(0, currentWord.length - 1)
                        buffer.setLength(0)
                        buffer.append(Common.decomposeStringToVni(newWord))
                    } else {
                        buffer.setLength(buffer.length - 1)
                    }
                }
                return Event.createConsumedEvent(event)
            }
            return event
        }

        if (event.isFunctionalKeyEvent) {
            return event
        }

        if (!keypress.isLetterOrDigit()) {
            return event
        }

        val lastChar = buffer.lastOrNull()
        if (lastChar != null && lastChar in VNI.TONES.keys && buffer.length >= 2 && buffer[buffer.length - 2] == lastChar) {
            buffer.setLength(buffer.length - 1)
        }

        for (ch in decomposed) {
            buffer.append(ch)
        }
        return Event.createConsumedEvent(event)
    }

    override fun getCombiningStateFeedback(): CharSequence? =
        try {
            val result = VNI.VNIToVietnamese(buffer.toString())
            val original = buffer.toString()
            val avoidAccentError = org.futo.inputmethod.latin.settings.Settings.getInstance().current?.mAvoidAccentError ?: true
            
            val lastChar = original.lastOrNull()
            val isToneRepeated = lastChar != null && lastChar in VNI.TONES.keys && original.length >= 2 && original[original.length - 2] == lastChar
            
            val preventAccent = avoidAccentError && Common.containsInvalidDigits(original, true) && !isToneRepeated
            if (preventAccent || (!isToneRepeated && avoidAccentError && result != original && !Common.isValidVietnameseSyllable(result))) {
                original
            } else {
                result
            }
        } catch (e: Exception) {
            buffer
        }

    override fun reset() {
        buffer.clear()
    }
}