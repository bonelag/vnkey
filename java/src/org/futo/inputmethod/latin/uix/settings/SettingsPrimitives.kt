package org.futo.inputmethod.latin.uix.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.math.roundToInt

enum class SettingsIconGlyph {
    ArrowBack, ArrowForward, Send, Warning, Search, Add, Clear, Close, Check,
    Info, KeyboardArrowDown, KeyboardArrowUp, KeyboardArrowRight, Menu, Refresh
}

object Icons {
    object Default {
        val ArrowBack = SettingsIconGlyph.ArrowBack
        val ArrowForward = SettingsIconGlyph.ArrowForward
        val Send = SettingsIconGlyph.Send
        val Warning = SettingsIconGlyph.Warning
        val Search = SettingsIconGlyph.Search
        val Add = SettingsIconGlyph.Add
        val Clear = SettingsIconGlyph.Clear
        val Close = SettingsIconGlyph.Close
        val Check = SettingsIconGlyph.Check
        val KeyboardArrowDown = SettingsIconGlyph.KeyboardArrowDown
        val KeyboardArrowUp = SettingsIconGlyph.KeyboardArrowUp
        val KeyboardArrowRight = SettingsIconGlyph.KeyboardArrowRight
        val Menu = SettingsIconGlyph.Menu
        val Refresh = SettingsIconGlyph.Refresh
    }

    object Filled {
        val Info = SettingsIconGlyph.Info
        val Warning = SettingsIconGlyph.Warning
    }
}

@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
    style: TextStyle = TextStyle.Default,
    textAlign: TextAlign? = null,
    fontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    fontWeight: androidx.compose.ui.text.font.FontWeight? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.merge(
            TextStyle(
                color = color,
                textAlign = textAlign ?: TextAlign.Unspecified,
                fontSize = fontSize,
                fontWeight = fontWeight
            )
        ),
        maxLines = maxLines,
        overflow = overflow,
        softWrap = softWrap
    )
}

@Composable
fun Text(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
    style: TextStyle = TextStyle.Default,
    textAlign: TextAlign? = null,
    inlineContent: Map<String, InlineTextContent> = emptyMap(),
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.merge(TextStyle(color = color, textAlign = textAlign ?: TextAlign.Unspecified)),
        inlineContent = inlineContent,
        maxLines = maxLines,
        overflow = overflow
    )
}

@Composable
fun Icon(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current
) {
    Canvas(modifier.size(24.dp)) {
        translate(
            left = size.width / 2.0f - painter.intrinsicSize.width / 2.0f,
            top = size.height / 2.0f - painter.intrinsicSize.height / 2.0f
        ) {
            with(painter) {
                draw(painter.intrinsicSize, colorFilter = ColorFilter.tint(tint))
            }
        }
    }
}

@Composable
fun Icon(
    imageVector: SettingsIconGlyph,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current
) {
    val label = when (imageVector) {
        SettingsIconGlyph.ArrowBack -> "‹"
        SettingsIconGlyph.ArrowForward, SettingsIconGlyph.KeyboardArrowRight -> "›"
        SettingsIconGlyph.Send -> "➤"
        SettingsIconGlyph.Warning -> "!"
        SettingsIconGlyph.Search -> "⌕"
        SettingsIconGlyph.Add -> "+"
        SettingsIconGlyph.Clear, SettingsIconGlyph.Close -> "×"
        SettingsIconGlyph.Check -> "✓"
        SettingsIconGlyph.Info -> "i"
        SettingsIconGlyph.KeyboardArrowDown -> "⌄"
        SettingsIconGlyph.KeyboardArrowUp -> "⌃"
        SettingsIconGlyph.Menu -> "≡"
        SettingsIconGlyph.Refresh -> "↻"
    }
    Box(modifier.size(24.dp), contentAlignment = Alignment.Center) {
        Text(label, color = tint, fontSize = 22.sp, textAlign = TextAlign.Center)
    }
}

data class ButtonColors(
    val containerColor: Color,
    val contentColor: Color,
    val disabledContainerColor: Color = containerColor.copy(alpha = 0.35f),
    val disabledContentColor: Color = contentColor.copy(alpha = 0.45f)
)

object ButtonDefaults {
    @Composable
    fun buttonColors(
        containerColor: Color = SettingsTheme.colors.primary,
        contentColor: Color = SettingsTheme.colors.onPrimary,
        disabledContainerColor: Color = SettingsTheme.colors.surfaceContainerHigh,
        disabledContentColor: Color = SettingsTheme.colors.onSurfaceVariant
    ) = ButtonColors(containerColor, contentColor, disabledContainerColor, disabledContentColor)

    @Composable
    fun textButtonColors(contentColor: Color = SettingsTheme.colors.primary) =
        ButtonColors(Color.Transparent, contentColor, Color.Transparent, contentColor.copy(alpha = 0.45f))

    @Composable
    fun outlinedButtonColors(contentColor: Color = SettingsTheme.colors.primary) =
        ButtonColors(Color.Transparent, contentColor, Color.Transparent, contentColor.copy(alpha = 0.45f))
}

@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    shape: Shape = RoundedCornerShape(8.dp),
    border: BorderStroke? = null,
    content: @Composable RowScope.() -> Unit
) {
    val container = if (enabled) colors.containerColor else colors.disabledContainerColor
    val contentColor = if (enabled) colors.contentColor else colors.disabledContentColor
    Row(
        modifier = modifier
            .clip(shape)
            .background(container)
            .let {
                border?.let { b -> it.border(b, shape) }
                    ?: it.border(1.dp, if (container == Color.Transparent) SettingsTheme.colors.outline else container, shape)
            }
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}

@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    shape: Shape = RoundedCornerShape(8.dp),
    content: @Composable RowScope.() -> Unit
) = Button(onClick, modifier, enabled, colors, shape, null, content)

@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    shape: Shape = RoundedCornerShape(8.dp),
    border: BorderStroke? = null,
    content: @Composable RowScope.() -> Unit
) = Button(onClick, modifier, enabled, colors, shape, border, content)

data class IconButtonColors(
    val containerColor: Color = Color.Transparent,
    val contentColor: Color = Color.Unspecified,
    val disabledContentColor: Color = Color.Unspecified
)

object IconButtonDefaults {
    fun iconButtonColors(
        containerColor: Color = Color.Transparent,
        contentColor: Color = Color.Unspecified,
        disabledContentColor: Color = Color.Unspecified
    ) = IconButtonColors(containerColor, contentColor, disabledContentColor)
}

@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    content: @Composable BoxScope.() -> Unit
) {
    val contentColor = when {
        !enabled && colors.disabledContentColor != Color.Unspecified -> colors.disabledContentColor
        colors.contentColor != Color.Unspecified -> colors.contentColor
        else -> LocalContentColor.current
    }
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.containerColor)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}

@Composable
fun Surface(
    modifier: Modifier = Modifier,
    color: Color = SettingsTheme.colors.surface,
    contentColor: Color = contentColorFor(color),
    shape: Shape = RoundedCornerShape(0.dp),
    border: BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(color)
            .let { if (border != null) it.border(border, shape) else it }
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}

@Composable
fun Switch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, enabled: Boolean = true) {
    val colors = SettingsTheme.colors
    val target by animateFloatAsState(if (checked) 1f else 0f)
    Canvas(
        Modifier
            .size(width = 46.dp, height = 28.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled && onCheckedChange != null) { onCheckedChange?.invoke(!checked) }
    ) {
        val track = if (checked) colors.primary else colors.surfaceContainerHighest
        drawRoundRect(track, cornerRadius = CornerRadius(size.height / 2f))
        val radius = size.height * 0.36f
        val x = radius + (size.width - radius * 2f - size.height * 0.28f) * target + size.height * 0.14f
        drawCircle(if (checked) colors.onPrimary else colors.onSurfaceVariant, radius, Offset(x, size.height / 2f))
    }
}

@Composable
fun Checkbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    colors: Any? = null
) {
    Box(
        modifier
            .size(24.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(if (checked) SettingsTheme.colors.primary else Color.Transparent)
            .border(1.dp, SettingsTheme.colors.outline, RoundedCornerShape(5.dp))
            .clickable(enabled = onCheckedChange != null) { onCheckedChange?.invoke(!checked) },
        contentAlignment = Alignment.Center
    ) {
        if (checked) Text("✓", color = SettingsTheme.colors.onPrimary, fontSize = 16.sp)
    }
}

object CheckboxDefaults {
    fun colors(
        checkedColor: Color = Color.Unspecified,
        uncheckedColor: Color = Color.Unspecified,
        checkmarkColor: Color = Color.Unspecified,
    ) = Unit
}

@Composable
fun RadioButton(selected: Boolean, onClick: (() -> Unit)?) {
    val colors = SettingsTheme.colors
    Canvas(
        Modifier
            .size(24.dp)
            .clip(CircleShape)
            .clickable(enabled = onClick != null) { onClick?.invoke() }
    ) {
        drawCircle(colors.outline, style = Stroke(2.dp.toPx()))
        if (selected) drawCircle(colors.primary, radius = size.minDimension * 0.28f)
    }
}

@Composable
fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    enabled: Boolean = true,
    steps: Int = 0
) {
    val colors = SettingsTheme.colors
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val fraction = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
    Canvas(
        modifier
            .height(42.dp)
            .onSizeChanged { canvasSize = it }
            .pointerInput(enabled, valueRange) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { offset ->
                        val f = (offset.x / canvasSize.width).coerceIn(0f, 1f)
                        onValueChange(valueRange.start + f * (valueRange.endInclusive - valueRange.start))
                    }
                ) { change, _ ->
                    val f = (change.position.x / canvasSize.width).coerceIn(0f, 1f)
                    onValueChange(valueRange.start + f * (valueRange.endInclusive - valueRange.start))
                }
            }
    ) {
        val y = center.y
        drawRoundRect(
            colors.surfaceContainerHighest,
            topLeft = Offset(0f, y - 3.dp.toPx()),
            size = Size(size.width.toFloat(), 6.dp.toPx()),
            cornerRadius = CornerRadius(3.dp.toPx())
        )
        drawRoundRect(
            colors.primary,
            topLeft = Offset(0f, y - 3.dp.toPx()),
            size = Size(size.width.toFloat() * fraction, 6.dp.toPx()),
            cornerRadius = CornerRadius(3.dp.toPx())
        )
        drawCircle(colors.primary, 9.dp.toPx(), Offset(size.width * fraction, y))
    }
}

@Composable
fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: (@Composable () -> Unit)? = null,
    placeholder: (@Composable () -> Unit)? = null,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    trailingIcon: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    colors: Any? = null,
    textStyle: TextStyle = TextStyle.Default,
    shape: Shape = RoundedCornerShape(8.dp),
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    val themeColors = SettingsTheme.colors
    Column(modifier) {
        label?.invoke()
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(themeColors.surfaceContainerHigh)
                .border(1.dp, themeColors.outlineVariant, shape)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingIcon?.invoke()
            if (leadingIcon != null) Spacer(Modifier.width(8.dp))
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) placeholder?.invoke()
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                    readOnly = readOnly,
                    singleLine = singleLine,
                    textStyle = textStyle.merge(TextStyle(color = themeColors.onSurface)),
                    cursorBrush = SolidColor(themeColors.primary),
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    visualTransformation = visualTransformation
                )
            }
            if (trailingIcon != null) Spacer(Modifier.width(8.dp))
            trailingIcon?.invoke()
        }
    }
}

@Composable
fun OutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: (@Composable () -> Unit)? = null,
    singleLine: Boolean = true,
    colors: Any? = null,
    textStyle: TextStyle = TextStyle.Default,
    shape: Shape = RoundedCornerShape(8.dp),
    visualTransformation: VisualTransformation = VisualTransformation.None,
) = TextField(value, onValueChange, modifier, label, singleLine = singleLine, colors = colors, textStyle = textStyle, shape = shape, visualTransformation = visualTransformation)

object OutlinedTextFieldDefaults {
    fun colors(
        focusedTextColor: Color = Color.Unspecified,
        unfocusedTextColor: Color = Color.Unspecified,
        focusedContainerColor: Color = Color.Unspecified,
        unfocusedContainerColor: Color = Color.Unspecified,
        focusedBorderColor: Color = Color.Unspecified,
        unfocusedBorderColor: Color = Color.Unspecified,
    ) = Unit
}

@Composable
fun LinearProgressIndicator(progress: Float, modifier: Modifier = Modifier) {
    val colors = SettingsTheme.colors
    Canvas(modifier.height(4.dp)) {
        drawRoundRect(colors.surfaceContainerHighest, cornerRadius = CornerRadius(2.dp.toPx()))
        drawRoundRect(
            colors.primary,
            size = Size(size.width * progress.coerceIn(0f, 1f), size.height),
            cornerRadius = CornerRadius(2.dp.toPx())
        )
    }
}

@Composable
fun CircularProgressIndicator(modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    val actualColor = if (color == Color.Unspecified) SettingsTheme.colors.primary else color
    Canvas(modifier.size(28.dp)) {
        drawArc(actualColor, startAngle = -90f, sweepAngle = 290f, useCenter = false, style = Stroke(3.dp.toPx()))
    }
}

@Composable
fun HorizontalDivider(modifier: Modifier = Modifier, color: Color = SettingsTheme.colors.outlineVariant) {
    Box(modifier.fillMaxWidth().height(1.dp).background(color))
}

@Composable
fun VerticalDivider(modifier: Modifier = Modifier, color: Color = SettingsTheme.colors.outlineVariant) {
    Box(modifier.width(1.dp).background(color))
}

@Composable
fun AlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = SettingsTheme.colors.surface,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = modifier.fillMaxWidth().padding(24.dp),
            color = containerColor,
            contentColor = contentColorFor(containerColor),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, SettingsTheme.colors.outlineVariant)
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                icon?.invoke()
                title?.invoke()
                text?.invoke()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    dismissButton?.invoke()
                    Spacer(Modifier.width(8.dp))
                    confirmButton()
                }
            }
        }
    }
}

enum class FabPosition { Start, End, Center }

@Composable
fun Scaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    content: @Composable (PaddingValues) -> Unit
) {
    Box(modifier) {
        Column {
            topBar()
            Box(Modifier.weight(1f)) {
                content(PaddingValues())
            }
        }
        Box(Modifier.align(if (floatingActionButtonPosition == FabPosition.Start) Alignment.BottomStart else Alignment.BottomEnd).padding(16.dp)) {
            floatingActionButton()
        }
    }
}

@Composable
fun SmallFloatingActionButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SettingsTheme.colors.primary)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides SettingsTheme.colors.onPrimary) {
            content()
        }
    }
}

@Composable
fun TabRow(selectedTabIndex: Int, content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().background(SettingsTheme.colors.surfaceContainerHigh)) { content() }
}

@Composable
fun ScrollableTabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    edgePadding: Dp = 0.dp,
    containerColor: Color = SettingsTheme.colors.surface,
    content: @Composable () -> Unit
) {
    Row(modifier.fillMaxWidth().background(containerColor).padding(horizontal = edgePadding)) { content() }
}

@Composable
fun Tab(selected: Boolean, onClick: () -> Unit, text: @Composable (() -> Unit)? = null, content: @Composable (() -> Unit)? = null) {
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) SettingsTheme.colors.primaryContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides if (selected) SettingsTheme.colors.onPrimaryContainer else SettingsTheme.colors.onSurfaceVariant) {
            text?.invoke() ?: content?.invoke()
        }
    }
}

@Composable
fun TopAppBar(
    title: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    colors: Any? = null
) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).background(SettingsTheme.colors.surface),
        verticalAlignment = Alignment.CenterVertically
    ) {
        navigationIcon()
        Box(Modifier.weight(1f)) { title() }
        actions()
    }
}

object TopAppBarDefaults {
    fun topAppBarColors(containerColor: Color) = containerColor
}

class ExposedDropdownMenuBoxScope {
    fun Modifier.menuAnchor(): Modifier = this
}

@Composable
fun ExposedDropdownMenuBox(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ExposedDropdownMenuBoxScope.() -> Unit
) {
    Box(modifier.clickable { onExpandedChange(!expanded) }) {
        ExposedDropdownMenuBoxScope().content()
    }
}

@Composable
fun ExposedDropdownMenu(expanded: Boolean, onDismissRequest: () -> Unit, content: @Composable () -> Unit) {
    if (expanded) {
        Surface(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            color = SettingsTheme.colors.surfaceContainerHigh,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, SettingsTheme.colors.outlineVariant)
        ) {
            Column { content() }
        }
    }
}

@Composable
fun DropdownMenuItem(text: @Composable () -> Unit, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(12.dp)) {
        text()
    }
}

object ExposedDropdownMenuDefaults {
    @Composable
    fun TrailingIcon(expanded: Boolean) {
        Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null)
    }

    fun textFieldColors(vararg ignored: Any?) = Unit
    fun textFieldColors(
        focusedLabelColor: Color = Color.Unspecified,
        focusedLeadingIconColor: Color = Color.Unspecified,
        focusedIndicatorColor: Color = Color.Unspecified,
        focusedTrailingIconColor: Color = Color.Unspecified,
    ) = Unit
}

@Composable
fun Divider(modifier: Modifier = Modifier, color: Color = SettingsTheme.colors.outlineVariant) =
    HorizontalDivider(modifier, color)
