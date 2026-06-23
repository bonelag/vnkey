package org.futo.inputmethod.event.combiners.vietnamese

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Spec + regression cho Telex.telexToVietnamese (pure JVM, không cần thiết bị).
 * Hành vi chuẩn UniKey: restoration (undo phím lặp) + free tone/modifier placement.
 */
class TelexToVietnameseTest {

    private fun check(cases: List<Pair<String, String>>) {
        val fails = StringBuilder()
        for ((input, expected) in cases) {
            val actual = Telex.telexToVietnamese(input)
            if (actual != expected) fails.append("\n  '$input' -> '$actual' (expected '$expected')")
        }
        if (fails.isNotEmpty()) throw AssertionError("Telex mismatches:$fails")
    }

    @Test fun circumflexBreveHornStroke() = check(listOf(
        "aa" to "â", "caa" to "câ", "aas" to "ấ", "caan" to "cân",
        "ee" to "ê", "oo" to "ô", "coo" to "cô", "oof" to "ồ",
        "aw" to "ă", "caw" to "că", "aws" to "ắ", "ow" to "ơ", "uw" to "ư",
        "w" to "ư", "uow" to "ươ", "dd" to "đ", "ddaa" to "đâ",
        "nguoiwf" to "người",
    ))

    @Test fun restoration() = check(listOf(
        "aaa" to "aa", "eee" to "ee", "ooo" to "oo", "ddd" to "dd",
        "caaa" to "caa", "cooo" to "coo", "deee" to "dee",
        "aww" to "aw", "uww" to "uw",
        "cass" to "cas", "caff" to "caf",
        "aaas" to "aas", "caaas" to "caas",
    ))

    @Test fun freePlacement() = check(listOf(
        "tieesng" to "tiếng", "tienges" to "tiếng", "tiengse" to "tiếng",
        "toans" to "toán", "toansx" to "toãn",
        "hoaf" to "hoà", "hoas" to "hoá",
        "quas" to "quá", "cuar" to "của",
        "gias" to "giá", "giaf" to "già",
        "nguyeenx" to "nguyễn", "vieets" to "viết", "Vieejt" to "Việt",
    ))

    @Test fun tonePlacement() = check(listOf(
        "as" to "á", "af" to "à", "ar" to "ả", "ax" to "ã", "aj" to "ạ",
        "oas" to "oá", "uys" to "uý",
    ))

    @Test fun uppercase() = check(listOf(
        "AA" to "Â", "Aa" to "Â", "aA" to "â", "DD" to "Đ", "Dd" to "Đ",
        "AW" to "Ă", "W" to "Ư", "Tieesng" to "Tiếng", "NGUOIWF" to "NGƯỜI",
    ))

    @Test fun nonVietnamesePassthrough() = check(listOf(
        "s" to "s", "f" to "f", "xyz" to "xyz", "the" to "the", "code" to "code",
        "oa" to "oa", "ao" to "ao", "oao" to "oao",
    ))

    /** Round-trip: chữ có dấu -> telex thô (decompose) -> telexToVietnamese phải về nguyên. */
    @Test fun decomposeRoundTrip() = check(listOf("tiếng", "người", "nguyễn", "của", "đâu", "ươ").map {
        Common.decomposeStringToTelex(it) to it
    })

    @Test fun validSyllableSpotCheck() {
        for (w in listOf("tiếng", "người", "toán", "của", "nguyễn")) {
            assertTrue("$w nên hợp lệ", Common.isValidVietnameseSyllable(w))
        }
    }
}
