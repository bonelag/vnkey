package org.futo.inputmethod.latin.uix.actions

import android.view.KeyEvent
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.futo.inputmethod.latin.R
import org.futo.inputmethod.latin.common.Constants
import org.futo.inputmethod.latin.uix.Action
import org.futo.inputmethod.latin.uix.ActionWindow

@Composable
fun IconWithColor(@DrawableRes iconId: Int, iconColor: Color, modifier: Modifier = Modifier) {
    val icon = painterResource(id = iconId)

    Canvas(modifier = modifier) {
        translate(
            left = this.size.width / 2.0f - icon.intrinsicSize.width / 2.0f,
            top = this.size.height / 2.0f - icon.intrinsicSize.height / 2.0f
        ) {
            with(icon) {
                draw(
                    icon.intrinsicSize,
                    colorFilter = ColorFilter.tint(
                        iconColor
                    )
                )
            }
        }
    }
}

@Composable
fun TogglableKey(
    onToggle: (Boolean) -> Unit,
    toggled: Boolean,
    modifier: Modifier = Modifier,
    contents: @Composable (color: Color) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(isPressed) {
        if(isPressed) {
            onToggle(!toggled)
        }
    }

    Surface(
        modifier = modifier
            .padding(4.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = { }
            ),
        shape = RoundedCornerShape(8.dp),
        color = if(toggled) { MaterialTheme.colorScheme.secondary } else { MaterialTheme.colorScheme.secondaryContainer }
    ) {
        contents(if(toggled) { MaterialTheme.colorScheme.onSecondary } else { MaterialTheme.colorScheme.onSecondaryContainer })
    }

}

@Composable
fun Modifier.repeatablyClickableAction(repeatable: Boolean = true, onTrigger: (Boolean) -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val currentOnTrigger by rememberUpdatedState(onTrigger)

    LaunchedEffect(interactionSource, repeatable) {
        var repeatJob: Job? = null

        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    currentOnTrigger(false)

                    if (repeatable) {
                        repeatJob?.cancel()
                        repeatJob = launch {
                            delay(670L)
                            while (isActive) {
                                currentOnTrigger(true)
                                delay(50L)
                            }
                        }
                    }
                }
                is PressInteraction.Release,
                is PressInteraction.Cancel -> {
                    repeatJob?.cancel()
                    repeatJob = null
                }
            }
        }
    }

    return this.clickable(
        interactionSource = interactionSource,
        indication = LocalIndication.current,
        onClick = { }
    )
}

@Composable
fun ActionKey(
    onTrigger: () -> Unit,
    modifier: Modifier = Modifier,
    repeatable: Boolean = true,
    color: Color = MaterialTheme.colorScheme.primary,
    contents: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .padding(4.dp)
            .repeatablyClickableAction(
                repeatable = repeatable,
                onTrigger = { onTrigger() }
            ),
        shape = RoundedCornerShape(8.dp),
        color = color
    ) {
        contents()
    }
}

@Composable
fun IconActionKey(
    iconId: Int,
    modifier: Modifier = Modifier,
    repeatable: Boolean = true,
    iconColor: Color = MaterialTheme.colorScheme.onSurface,
    onTrigger: () -> Unit
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .repeatablyClickableAction(
                repeatable = repeatable,
                onTrigger = { onTrigger() }
            ),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        IconWithColor(iconId = iconId, iconColor = iconColor)
    }
}

@Composable
fun ActionTextKey(
    text: String,
    modifier: Modifier = Modifier,
    repeatable: Boolean = false,
    color: Color = MaterialTheme.colorScheme.surfaceVariant,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onTrigger: () -> Unit
) {
    Surface(
        modifier = modifier
            .padding(4.dp)
            .repeatablyClickableAction(
                repeatable = repeatable,
                onTrigger = { onTrigger() }
            ),
        shape = RoundedCornerShape(12.dp),
        color = color
    ) {
        androidx.compose.foundation.layout.Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
            androidx.compose.material3.Text(
                text = text,
                color = textColor,
                style = MaterialTheme.typography.titleMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

enum class Direction {
    Left, Right, Up, Down, Home, End
}

@Composable
fun TextEditScreen(
    onCodePoint: (Int) -> Unit,
    onEvent: (Int, Int) -> Unit,
    moveCursor: (direction: Direction, ctrl: Boolean, shift: Boolean) -> Unit,
    keyboardShown: Boolean
) {
    val selectingState = remember { mutableStateOf(false) }
    val sendMoveCursor = { direction: Direction -> moveCursor(direction, false, selectingState.value) }

    Row(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 8.dp)) {
        // LEFT HALF: D-Pad and Selection
        androidx.compose.foundation.layout.Box(modifier = Modifier
            .fillMaxHeight()
            .weight(1.0f)
        ) {
            // Center D-Pad
            androidx.compose.foundation.layout.Box(modifier = Modifier.align(androidx.compose.ui.Alignment.Center).size(190.dp)) {
                
                TogglableKey(
                    onToggle = { selectingState.value = it },
                    toggled = selectingState.value,
                    modifier = Modifier.align(androidx.compose.ui.Alignment.Center).size(72.dp)
                ) { color ->
                    androidx.compose.foundation.layout.Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                        androidx.compose.material3.Text(
                            text = stringResource(R.string.action_text_editor_select),
                            color = color,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                IconActionKey(
                    iconId = R.drawable.ic_triangle_up,
                    modifier = Modifier.align(androidx.compose.ui.Alignment.TopCenter).size(54.dp),
                    onTrigger = { sendMoveCursor(Direction.Up) },
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconActionKey(
                    iconId = R.drawable.ic_triangle_down,
                    modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter).size(54.dp),
                    onTrigger = { sendMoveCursor(Direction.Down) },
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconActionKey(
                    iconId = R.drawable.ic_triangle_left,
                    modifier = Modifier.align(androidx.compose.ui.Alignment.CenterStart).size(54.dp),
                    onTrigger = { sendMoveCursor(Direction.Left) },
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconActionKey(
                    iconId = R.drawable.ic_triangle_right,
                    modifier = Modifier.align(androidx.compose.ui.Alignment.CenterEnd).size(54.dp),
                    onTrigger = { sendMoveCursor(Direction.Right) },
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Corner icons
            IconActionKey(
                iconId = R.drawable.ic_move_start,
                modifier = Modifier.align(androidx.compose.ui.Alignment.BottomStart).padding(start = 12.dp, bottom = 12.dp).size(48.dp),
                onTrigger = { sendMoveCursor(Direction.Home) },
                iconColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconActionKey(
                iconId = R.drawable.ic_move_end,
                modifier = Modifier.align(androidx.compose.ui.Alignment.BottomEnd).padding(end = 12.dp, bottom = 12.dp).size(48.dp),
                onTrigger = { sendMoveCursor(Direction.End) },
                iconColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // RIGHT HALF: Action buttons grid (2 columns x 3 rows)
        Column(modifier = Modifier
            .fillMaxHeight()
            .weight(1.0f)
            .padding(start = 16.dp, end = 8.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            val rowModifier = Modifier.fillMaxWidth().height(72.dp)
            
            // Row 1: SELECT ALL | DELETE
            Row(modifier = rowModifier) {
                ActionTextKey(
                    text = stringResource(R.string.action_select_all_title),
                    modifier = Modifier.weight(1.0f).fillMaxHeight(),
                    onTrigger = { onEvent(KeyEvent.KEYCODE_A, KeyEvent.META_CTRL_ON) }
                )
                
                Surface(
                    modifier = Modifier
                        .weight(1.0f).fillMaxHeight()
                        .padding(4.dp)
                        .repeatablyClickableAction(
                            repeatable = true,
                            onTrigger = { onCodePoint(Constants.CODE_DELETE) }
                        ),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    androidx.compose.foundation.layout.Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                        IconWithColor(
                            iconId = R.drawable.delete,
                            iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            // Row 2: COPY | CUT
            Row(modifier = rowModifier) {
                ActionTextKey(
                    text = stringResource(R.string.action_copy_title),
                    modifier = Modifier.weight(1.0f).fillMaxHeight(),
                    onTrigger = { onEvent(KeyEvent.KEYCODE_C, KeyEvent.META_CTRL_ON) }
                )
                ActionTextKey(
                    text = stringResource(R.string.action_cut_title),
                    modifier = Modifier.weight(1.0f).fillMaxHeight(),
                    onTrigger = { onEvent(KeyEvent.KEYCODE_X, KeyEvent.META_CTRL_ON) }
                )
            }
            // Row 3: PASTE | ENTER
            Row(modifier = rowModifier) {
                ActionTextKey(
                    text = stringResource(R.string.action_paste_title),
                    modifier = Modifier.weight(1.0f).fillMaxHeight(),
                    onTrigger = { onEvent(KeyEvent.KEYCODE_V, KeyEvent.META_CTRL_ON) }
                )
                
                Surface(
                    modifier = Modifier
                        .weight(1.0f).fillMaxHeight()
                        .padding(4.dp)
                        .repeatablyClickableAction(
                            repeatable = false,
                            onTrigger = { onCodePoint(Constants.CODE_ENTER) }
                        ),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    androidx.compose.foundation.layout.Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                        IconWithColor(
                            iconId = R.drawable.ic_enter,
                            iconColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

val TextEditAction = Action(
    icon = R.drawable.edit_text,
    name = R.string.action_text_editor_title,
    simplePressImpl = null,
    persistentState = null,
    canShowKeyboard = true,
    windowImpl = { manager, persistentState ->
        object : ActionWindow() {
            @Composable
            override fun windowName(): String {
                return stringResource(R.string.action_text_editor_title)
            }

            @Composable
            override fun WindowContents(keyboardShown: Boolean) {
                val view = LocalView.current
                TextEditScreen(
                    onCodePoint = { a ->
                        manager.sendCodePointEvent(a)
                        manager.performHapticAndAudioFeedback(a, view)
                    },
                    onEvent = { a, b ->
                        manager.sendKeyEvent(a, b)
                        manager.performHapticAndAudioFeedback(Constants.CODE_TAB, view)
                    },
                    moveCursor = { direction, ctrl, shift ->
                        val keyEventMetaState = 0 or
                                (if(shift) { KeyEvent.META_SHIFT_ON } else { 0 }) or
                                (if(ctrl) { KeyEvent.META_CTRL_ON } else { 0 })

                         when {
                            keyEventMetaState == 0 && direction == Direction.Left ->
                                manager.activateAction(ArrowLeftAction)
                            keyEventMetaState == 0 && direction == Direction.Right ->
                                manager.activateAction(ArrowRightAction)
                            keyEventMetaState == 0 && direction == Direction.Up ->
                                manager.activateAction(ArrowUpAction)
                            keyEventMetaState == 0 && direction == Direction.Down ->
                                manager.activateAction(ArrowDownAction)

                            direction == Direction.Left -> manager.cursorLeft(1, stepOverWords = ctrl, select = shift)
                            direction == Direction.Right -> manager.cursorRight(1, stepOverWords = ctrl, select = shift)
                            direction == Direction.Up -> manager.sendKeyEvent(KeyEvent.KEYCODE_DPAD_UP, keyEventMetaState)
                            direction == Direction.Down -> manager.sendKeyEvent(KeyEvent.KEYCODE_DPAD_DOWN, keyEventMetaState)
                            direction == Direction.Home -> manager.sendKeyEvent(KeyEvent.KEYCODE_MOVE_HOME, keyEventMetaState)
                            direction == Direction.End -> manager.sendKeyEvent(KeyEvent.KEYCODE_MOVE_END, keyEventMetaState)
                        }

                        manager.performHapticAndAudioFeedback(Constants.CODE_TAB, view)
                    },
                    keyboardShown = keyboardShown
                )
            }
        }
    }
)

@Composable
@Preview(showBackground = true)
fun TextEditScreenPreview() {
    Surface(modifier = Modifier.height(256.dp)) {
        TextEditScreen(onCodePoint = { }, onEvent = { _, _ -> }, moveCursor = { _, _, _ -> }, keyboardShown = false)
    }
}
@Composable
@Preview(showBackground = true)
fun TextEditScreenPreviewWithKb() {
    Surface(modifier = Modifier.height(256.dp)) {
        TextEditScreen(onCodePoint = { }, onEvent = { _, _ -> }, moveCursor = { _, _, _ -> }, keyboardShown = true)
    }
}