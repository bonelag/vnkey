package org.futo.inputmethod.latin.uix.settings.pages.themes

import org.futo.inputmethod.latin.uix.settings.*

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.material.color.utilities.Hct
import kotlin.math.roundToInt

private fun hsvToComposeColor(hue: Float, saturation: Float, value: Float): Color {
    val argb = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))
    return Color(argb)
}

@SuppressLint("RestrictedApi")
@Composable
internal fun HctSliderPicker(
    hct: Hct2,
    onColorChange: (Hct2) -> Unit
) {
    // 1. Chuyển đổi Hct2 sang Color
    val colorInt = remember(hct) {
        Hct.from(hct.hue.toDouble(), hct.chroma.toDouble(), hct.tone.toDouble()).toInt()
    }
    val currentColor = Color(colorInt)

    // 2. Trích xuất giá trị HSV từ Color
    val hsv = remember(colorInt) {
        val arr = FloatArray(3)
        android.graphics.Color.colorToHSV(colorInt, arr)
        arr
    }

    var hue by remember(hsv[0]) { mutableStateOf(hsv[0]) }
    var saturation by remember(hsv[1]) { mutableStateOf(hsv[1]) }
    var value by remember(hsv[2]) { mutableStateOf(hsv[2]) }

    // Hàm cập nhật khi HSV thay đổi
    val updateHSV = { h: Float, s: Float, v: Float ->
        hue = h
        saturation = s
        value = v
        val rgb = android.graphics.Color.HSVToColor(floatArrayOf(h, s, v))
        val hctObj = Hct.fromInt(rgb)
        onColorChange(Hct2(hctObj.hue.toFloat(), hctObj.chroma.toFloat(), hctObj.tone.toFloat()))
    }

    var hexText by remember(currentColor) {
        val argb = currentColor.toArgb()
        val hex = "%06X".format(0xFFFFFF and argb).lowercase()
        mutableStateOf("#$hex")
    }

    val onHexTextChanged = { text: String ->
        hexText = text
        val cleaned = text.removePrefix("#").trim()
        if (cleaned.length == 6) {
            try {
                val colorInt = android.graphics.Color.parseColor("#$cleaned")
                val arr = FloatArray(3)
                android.graphics.Color.colorToHSV(colorInt, arr)
                updateHSV(arr[0], arr[1], arr[2])
            } catch (e: Exception) {}
        } else if (cleaned.length == 8) {
            try {
                val colorInt = android.graphics.Color.parseColor("#$cleaned")
                val arr = FloatArray(3)
                android.graphics.Color.colorToHSV(colorInt, arr)
                updateHSV(arr[0], arr[1], arr[2])
            } catch (e: Exception) {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hàng trên gồm Preview màu và Khối chọn màu 2D (Saturation/Value)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Ô hiển thị màu hiện tại ở bên trái (Preview)
            Box(
                modifier = Modifier
                    .weight(0.35f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(currentColor)
                    .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            )

            // Khối chọn màu 2D ở bên phải
            BoxWithConstraints(
                modifier = Modifier
                    .weight(0.65f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            ) {
                val widthPx = constraints.maxWidth.toFloat()
                val heightPx = constraints.maxHeight.toFloat()

                // Vẽ dải màu 2D
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.White, hsvToComposeColor(hue, 1f, 1f))
                            )
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black)
                                )
                            )
                    )
                }

                // Xử lý chạm và kéo trên khối 2D
                val handleDragOrTap = { x: Float, y: Float ->
                    val s = (x / widthPx).coerceIn(0f, 1f)
                    val v = (1f - (y / heightPx)).coerceIn(0f, 1f)
                    updateHSV(hue, s, v)
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(widthPx, heightPx) {
                            detectTapGestures(
                                onPress = { offset ->
                                    handleDragOrTap(offset.x, offset.y)
                                }
                            )
                        }
                        .pointerInput(widthPx, heightPx) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                handleDragOrTap(change.position.x, change.position.y)
                            }
                        }
                )

                // Vòng tròn định vị điểm màu đang chọn (thumb 2D)
                val thumbX = saturation * widthPx
                val thumbY = (1f - value) * heightPx
                val isLightColor = value > 0.7f && saturation < 0.3f
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (thumbX - 10.dp.toPx()).roundToInt(),
                                (thumbY - 10.dp.toPx()).roundToInt()
                            )
                        }
                        .size(20.dp)
                        .clip(CircleShape)
                        .border(2.dp, if (isLightColor) Color.Black else Color.White, CircleShape)
                )
            }
        }

        // Thanh trượt chọn Hue (dải cầu vồng 1D) ở bên dưới
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
        ) {
            val widthPx = constraints.maxWidth.toFloat()
            val hueColors = remember {
                (0..360 step 10).map { hsvToComposeColor(it.toFloat(), 1f, 1f) }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(9.dp))
                    .background(Brush.horizontalGradient(hueColors))
                    .pointerInput(widthPx) {
                        detectTapGestures(
                            onPress = { offset ->
                                val h = (offset.x / widthPx).coerceIn(0f, 1f) * 360f
                                updateHSV(h, saturation, value)
                            }
                        )
                    }
                    .pointerInput(widthPx) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val h = (change.position.x / widthPx).coerceIn(0f, 1f) * 360f
                            updateHSV(h, saturation, value)
                        }
                    }
            )

            // Vòng tròn định vị Hue đang chọn (thumb Hue)
            val thumbX = (hue / 360f) * widthPx
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (thumbX - 10.dp.toPx()).roundToInt(),
                            0
                        )
                    }
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.5.dp, Color.Gray, CircleShape)
                    .align(Alignment.CenterStart)
            )
        }

        // Hàng nhập mã màu CSS thủ công
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = hexText,
                onValueChange = onHexTextChanged,
                modifier = Modifier
                    .width(130.dp)
                    .height(48.dp),
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = Color.White,
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                ),
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF1E1E24),
                    unfocusedContainerColor = Color(0xFF1E1E24),
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                )
            )
        }
    }
}