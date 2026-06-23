package org.futo.inputmethod.event.combiners.vietnamese

import android.text.TextUtils
import org.futo.inputmethod.event.Combiner
import org.futo.inputmethod.event.Event
import org.futo.inputmethod.latin.common.Constants
import java.util.ArrayList

class VietTelexCombiner: Combiner {
    private val buffer = StringBuilder() // holds a single Vietnamese word/syllable

    override fun processEvent(
        previousEvents: ArrayList<Event?>?,
        event: Event?
    ): Event {
        if (event == null) return Event.createNotHandledEvent()
        if (event.eventType != Event.EVENT_TYPE_INPUT_KEYPRESS && event.eventType != Event.EVENT_TYPE_INPUT_KEYPRESS_RESUMED) return event

        val keypress = event.mCodePoint.toChar()
        val decomposed = Common.decomposeTelex(keypress)

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
                        buffer.append(Common.decomposeStringToTelex(newWord))
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

        // (Không dedup phím thanh lặp ở đây — telexToVietnamese tự xử lý tone-restoration,
        //  vd "cass"→"cas". Dedup cũ sẽ phá hành vi đó.)
        for (ch in decomposed) {
            buffer.append(ch)
        }
        return Event.createConsumedEvent(event)
    }

    override fun getCombiningStateFeedback(): CharSequence? =
        try {
            val result = Telex.telexToVietnamese(buffer.toString())
            val original = buffer.toString()
            val avoidAccentError = org.futo.inputmethod.latin.settings.Settings.getInstance().current?.mAvoidAccentError ?: true
            
            val lastChar = original.lastOrNull()
            val isToneRepeated = lastChar != null && lastChar in Telex.TONES.keys && original.length >= 2 && original[original.length - 2] == lastChar
            
            var hasTripleDiacritic = false
            val targets = setOf('o', 'a', 'e', 'd', 'w')
            for (idx in 0..original.length - 3) {
                val c = original[idx].lowercaseChar()
                if (c in targets && original[idx + 1].lowercaseChar() == c && original[idx + 2].lowercaseChar() == c) {
                    hasTripleDiacritic = true
                    break
                }
            }

            val preventAccent = avoidAccentError && Common.containsInvalidDigits(original, false) && !isToneRepeated
            if ((preventAccent || (!isToneRepeated && avoidAccentError && result != original && !Common.isValidVietnameseSyllable(result))) && !hasTripleDiacritic) {
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