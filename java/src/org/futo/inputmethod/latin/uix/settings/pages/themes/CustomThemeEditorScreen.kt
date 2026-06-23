package org.futo.inputmethod.latin.uix.settings.pages.themes

import android.content.Context
import android.graphics.Bitmap
import android.view.ContextThemeWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.futo.inputmethod.latin.R
import org.futo.inputmethod.latin.RichInputMethodManager
import org.futo.inputmethod.latin.uix.BasicThemeProvider
import org.futo.inputmethod.latin.uix.DynamicThemeProvider
import org.futo.inputmethod.latin.uix.DynamicThemeProviderOwner
import org.futo.inputmethod.latin.uix.KeyboardBackground
import org.futo.inputmethod.latin.uix.KeyboardLayoutPreview
import org.futo.inputmethod.latin.uix.LocalKeyboardScheme
import org.futo.inputmethod.latin.uix.LocalThemeProvider
import org.futo.inputmethod.latin.uix.THEME_KEY
import org.futo.inputmethod.latin.uix.getSetting
import org.futo.inputmethod.latin.uix.setSetting
import org.futo.inputmethod.latin.uix.theme.CustomThemeBuilderConfiguration
import org.futo.inputmethod.latin.uix.theme.SerializableCustomTheme
import org.futo.inputmethod.latin.uix.theme.ThemeDecodingContext
import org.futo.inputmethod.latin.uix.theme.ZipThemes
import org.futo.inputmethod.latin.uix.theme.toSColor
import java.io.ByteArrayOutputStream
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.platform.LocalDensity
import androidx.core.net.toUri
import android.graphics.BitmapFactory
import android.net.Uri
import org.futo.inputmethod.latin.uix.urlDecode
import org.futo.inputmethod.latin.uix.urlEncode
import org.futo.inputmethod.latin.uix.settings.Route

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomThemeEditorScreen(
    navController: NavHostController, 
    name: String, 
    startingBitmap: Bitmap?,
    initialTheme: SerializableCustomTheme? = null
) {
    val context = LocalContext.current

    val isInitiallyGradient = initialTheme?.gradientColors != null
    val isInitiallyImage = startingBitmap != null && (startingBitmap.width > 1 || startingBitmap.height > 1)
    val initialBgMode = if (isInitiallyGradient) 2 else if (isInitiallyImage) 0 else 1

    var bgMode by remember { mutableIntStateOf(initialBgMode) }

    var selectedColor by remember {
        mutableStateOf(
            if (startingBitmap != null && startingBitmap.width == 1 && startingBitmap.height == 1) {
                Color(startingBitmap.getPixel(0, 0))
            } else Color.Transparent
        )
    }

    var bitmap by remember {
        mutableStateOf(
            if (startingBitmap != null && (startingBitmap.width > 1 || startingBitmap.height > 1)) startingBitmap else null
        )
    }

    var showColorPicker by remember { mutableStateOf(false) }
    var pickerHct by remember { mutableStateOf(Hct2(0f, 0f, 0f)) }

    var gradientColorsList by remember {
        mutableStateOf(initialTheme?.gradientColors ?: listOf("#8E2DE2", "#4A00E0"))
    }
    var gradientDirectionState by remember {
        mutableIntStateOf(initialTheme?.gradientDirection ?: 0)
    }
    var editingColorIndex by remember { mutableIntStateOf(-1) }

    val pickLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                try {
                    val resolver = context.contentResolver
                    val newBitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                    if (newBitmap != null) {
                        bitmap = newBitmap
                        selectedColor = Color.Transparent
                        bgMode = 0
                    }
                } catch(e: Exception) { }
            }
        }
    )

    // --- Các state cho tab Phím ---
    var keyBgColor by remember { mutableStateOf(initialTheme?.keyboardContainer?.toColor()) }
    var keyTextColor by remember { mutableStateOf(initialTheme?.onKeyboardContainer?.toColor()) }
    var keyHeightScaleState by remember { mutableFloatStateOf(initialTheme?.keyboardHeightScale ?: 1.0f) }

    var toolbarBgColor by remember { mutableStateOf(initialTheme?.toolbarColor?.toColor()) }
    var toolbarTextColor by remember { mutableStateOf(initialTheme?.onToolbarColor?.toColor()) }

    var popupBgColor by remember { mutableStateOf(initialTheme?.popupColor?.toColor()) }
    var popupTextColor by remember { mutableStateOf(initialTheme?.onPopupColor?.toColor()) }

    var keyRoundnessState by remember { mutableFloatStateOf(initialTheme?.keyRoundness ?: 1.0f) }
    var keyShadowSizeState by remember { mutableFloatStateOf(initialTheme?.keyShadowSize ?: 0.0f) }
    var keyBorderSizeState by remember { mutableFloatStateOf(initialTheme?.keyBorderSize ?: 0.0f) }
    var keyBorderColorState by remember { mutableStateOf(initialTheme?.keyBorderColor?.toColor()) }

    var functionalCustomEnabled by remember { mutableStateOf(initialTheme?.functionalKeyColor != null) }
    var functionalBgColor by remember { mutableStateOf(initialTheme?.functionalKeyColor?.toColor()) }
    var functionalTextColor by remember { mutableStateOf(initialTheme?.onFunctionalKeyColor?.toColor()) }
    var functionalBorderColor by remember { mutableStateOf(initialTheme?.functionalKeyBorderColor?.toColor()) }

    var spacebarCustomEnabled by remember { mutableStateOf(initialTheme?.spacebarColor != null) }
    var spacebarBgColor by remember { mutableStateOf(initialTheme?.spacebarColor?.toColor()) }
    var spacebarTextColor by remember { mutableStateOf(initialTheme?.onSpacebarColor?.toColor()) }
    var spacebarBorderColor by remember { mutableStateOf(initialTheme?.spacebarBorderColor?.toColor()) }

    var activeColorPickerField by remember { mutableStateOf<String?>(null) }

    var currentTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Nền", "Phím", "Kiểu chữ", "Âm thanh", "Hiệu ứng")

    val backgroundBitmap = remember(bitmap, selectedColor, bgMode) {
        if (bgMode == 0 && bitmap != null) bitmap
        else if (bgMode == 1 && selectedColor != Color.Transparent) {
            val bmp = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            bmp.setPixel(0, 0, selectedColor.toArgb())
            bmp
        } else null
    }

    val isDarkBackground = remember(bitmap, selectedColor, bgMode, gradientColorsList) {
        if (bgMode == 0 && bitmap != null) true
        else if (bgMode == 2) {
            if (gradientColorsList.isNotEmpty()) {
                try {
                    Color(android.graphics.Color.parseColor(gradientColorsList[0])).luminance() < 0.5f
                } catch(e: Exception) { true }
            } else true
        }
        else selectedColor.luminance() < 0.5f
    }

    val themeCfg = remember(bitmap, selectedColor, bgMode, gradientColorsList, gradientDirectionState, backgroundBitmap) {
        val rect = if (backgroundBitmap != null) {
            androidx.compose.ui.geometry.Rect(0f, 0f, backgroundBitmap.width.toFloat(), backgroundBitmap.height.toFloat())
        } else {
            androidx.compose.ui.geometry.Rect.Zero
        }
        // Hue and chroma are 0 so keys stay default color. Only background changes.
        CustomThemeBuilderConfiguration(
            hue = 0.0,
            chroma = 0.0,
            contrast = 0.5f,
            darkMode = isDarkBackground,
            borders = true,
            backgroundImagePath = if ((bgMode == 0 && bitmap != null) || (bgMode == 1 && selectedColor != Color.Transparent)) "background.jpg" else null,
            backgroundImageOpacity = if (bgMode == 0) 0.7f else 1.0f,
            backgroundImageRect = rect,
            gradientColors = if (bgMode == 2) gradientColorsList else null,
            gradientDirection = if (bgMode == 2) gradientDirectionState else null
        )
    }

    val themeCtx = remember(backgroundBitmap) {
        object : ThemeDecodingContext {
            override val context: Context get() = context
            override fun getFileBytes(path: String): ByteArray? {
                if (path == "background.jpg" && backgroundBitmap != null) {
                    val stream = ByteArrayOutputStream()
                    backgroundBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                    return stream.toByteArray()
                }
                return null
            }
            override fun getFileHash(path: String): String? {
                if (path == "background.jpg" && backgroundBitmap != null) {
                    return "bg_" + backgroundBitmap.hashCode().toString()
                }
                return null
            }
            override fun close() {}
        }
    }

    val theme = remember(
        themeCfg, backgroundBitmap,
        keyBgColor, keyTextColor, keyHeightScaleState,
        toolbarBgColor, toolbarTextColor,
        popupBgColor, popupTextColor,
        keyRoundnessState, keyShadowSizeState, keyBorderSizeState, keyBorderColorState,
        functionalCustomEnabled, functionalBgColor, functionalTextColor, functionalBorderColor,
        spacebarCustomEnabled, spacebarBgColor, spacebarTextColor, spacebarBorderColor
    ) {
        val baseScheme = themeCfg.build().toKeyboardScheme(themeCtx)
        val updatedExtended = baseScheme.extended.copy(
            keyboardContainer = keyBgColor ?: baseScheme.extended.keyboardContainer,
            onKeyboardContainer = keyTextColor ?: baseScheme.extended.onKeyboardContainer,
            advancedThemeOptions = baseScheme.extended.advancedThemeOptions.copy(
                backgroundImage = backgroundBitmap?.asImageBitmap(),
                keyRoundness = keyRoundnessState,
                keyBorderSize = keyBorderSizeState,
                keyShadowSize = keyShadowSizeState,
                keyBorderColor = keyBorderColorState,
                spacebarColor = if (spacebarCustomEnabled) spacebarBgColor else null,
                onSpacebarColor = if (spacebarCustomEnabled) spacebarTextColor else null,
                spacebarBorderColor = if (spacebarCustomEnabled) spacebarBorderColor else null,
                functionalKeyColor = if (functionalCustomEnabled) functionalBgColor else null,
                onFunctionalKeyColor = if (functionalCustomEnabled) functionalTextColor else null,
                functionalKeyBorderColor = if (functionalCustomEnabled) functionalBorderColor else null,
                toolbarColor = toolbarBgColor,
                onToolbarColor = toolbarTextColor,
                popupColor = popupBgColor,
                onPopupColor = popupTextColor,
                keyboardHeightScale = keyHeightScaleState
            )
        )
        baseScheme.copy(extended = updatedExtended)
    }

    val themeProvider = remember(theme) { BasicThemeProvider(context, theme) }

    val customThemeCtx = remember(themeProvider) {
        object : ContextThemeWrapper(context, R.style.KeyboardTheme_LXX_Light),
            DynamicThemeProviderOwner {
            override fun getDrawableProvider(): DynamicThemeProvider = themeProvider
        }
    }

    val lifecycle = LocalLifecycleOwner.current
    var saving by remember { mutableStateOf(false) }

    val buildFinalSerializableCustomTheme = {
        val baseTheme = themeCfg.build()
        baseTheme.copy(
            keyboardContainer = keyBgColor?.toSColor() ?: baseTheme.keyboardContainer,
            onKeyboardContainer = keyTextColor?.toSColor() ?: baseTheme.onKeyboardContainer,
            keyRoundness = keyRoundnessState,
            keyBorderSize = keyBorderSizeState,
            keyShadowSize = keyShadowSizeState,
            keyBorderColor = keyBorderColorState?.toSColor(),
            spacebarColor = if (spacebarCustomEnabled) spacebarBgColor?.toSColor() else null,
            onSpacebarColor = if (spacebarCustomEnabled) spacebarTextColor?.toSColor() else null,
            spacebarBorderColor = if (spacebarCustomEnabled) spacebarBorderColor?.toSColor() else null,
            functionalKeyColor = if (functionalCustomEnabled) functionalBgColor?.toSColor() else null,
            onFunctionalKeyColor = if (functionalCustomEnabled) functionalTextColor?.toSColor() else null,
            functionalKeyBorderColor = if (functionalCustomEnabled) functionalBorderColor?.toSColor() else null,
            toolbarColor = toolbarBgColor?.toSColor(),
            onToolbarColor = toolbarTextColor?.toSColor(),
            popupColor = popupBgColor?.toSColor(),
            onPopupColor = popupTextColor?.toSColor(),
            keyboardHeightScale = keyHeightScaleState
        )
    }

    val finish = {
        saving = true
        lifecycle.lifecycleScope.launch {
            withContext(Dispatchers.Default) {
                ZipThemes.save(
                    themeCtx,
                    buildFinalSerializableCustomTheme(),
                    ZipThemes.custom(name)
                )
            }
            withContext(Dispatchers.Main) {
                if (context.getSetting(THEME_KEY).endsWith("_")) {
                    context.setSetting(THEME_KEY.key, "custom$name")
                } else {
                    context.setSetting(THEME_KEY.key, "custom${name}_")
                }
                navController.navigateUp()
            }
        }
        Unit
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tạo chủ đề", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    IconButton(onClick = finish) {
                        if (saving) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        else Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).background(MaterialTheme.colorScheme.surface)) {
            
            Box(
                modifier = Modifier.fillMaxWidth().weight(0.4f).clipToBounds(),
                contentAlignment = Alignment.BottomCenter
            ) {
                KeyboardBackground(theme)
                CompositionLocalProvider(
                    LocalThemeProvider provides themeProvider,
                    LocalKeyboardScheme provides theme
                ) {
                    KeyboardLayoutPreview(
                        RichInputMethodManager.getInstance().currentSubtype.keyboardLayoutSetName,
                        width = with(LocalDensity.current) { context.resources.displayMetrics.widthPixels.toDp() },
                        customThemeCtx = customThemeCtx
                    )
                }
            }

            // Tabs
            ScrollableTabRow(
                selectedTabIndex = currentTab,
                edgePadding = 8.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = currentTab == index,
                        onClick = { currentTab = index },
                        text = { Text(title, fontSize = 14.sp) }
                    )
                }
            }

            // Tab Content
            Box(modifier = Modifier.fillMaxWidth().weight(0.6f)) {
                if (currentTab == 0) { // Nền
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            "Chế độ nền",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Hàng chọn chế độ nền bằng các nút icon trực quan
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                Pair(R.drawable.image, "Ảnh nền"),
                                Pair(R.drawable.themes, "Đơn sắc"),
                                Pair(null, "Tuyến tính")
                            ).forEachIndexed { index, (drawableId, label) ->
                                val isSelected = bgMode == index
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(22.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { bgMode = index }
                                        .border(
                                            1.dp,
                                            if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                            RoundedCornerShape(22.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (drawableId != null) {
                                            Icon(
                                                painterResource(drawableId),
                                                contentDescription = label,
                                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        } else {
                                            // Vẽ vòng tròn gradient mẫu
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        Brush.sweepGradient(
                                                            listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
                                                        )
                                                    )
                                            )
                                        }
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        if (bgMode == 0) { // Ảnh nền
                            Text(
                                "Ảnh của bạn",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Button(
                                onClick = { pickLauncher.launch("image/*") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066FF))
                            ) {
                                Text("Chọn ảnh từ thư viện", color = Color.White, fontSize = 16.sp)
                            }
                        } else if (bgMode == 1) { // Màu đơn sắc
                            Text(
                                "Màu đơn sắc",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            val colors = listOf(
                                Color.Transparent,
                                Color(0xFF0066FF), Color.White, Color.Gray, Color.DarkGray,
                                Color.Black, Color(0xFF00BCD4), Color(0xFF009688), Color(0xFF4CAF50),
                                Color(0xFF8BC34A), Color(0xFFCDDC39), Color(0xFFFFEB3B), Color(0xFFFFC107),
                                Color(0xFFFF9800), Color(0xFFF44336), Color(0xFFE91E63), Color(0xFF9C27B0),
                                Color(0xFF3F51B5), Color(0xFF795548), Color(0xFF607D8B), Color.Cyan
                            )
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(4),
                                modifier = Modifier.height(260.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(colors.size) { index ->
                                    val color = colors[index]
                                    if (index == 0) {
                                        Box(
                                            modifier = Modifier
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    Brush.sweepGradient(
                                                        listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
                                                    )
                                                )
                                                .clickable {
                                                    editingColorIndex = -1
                                                    showColorPicker = true
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.Add,
                                                    contentDescription = "Add Color",
                                                    tint = Color.Black,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(color)
                                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                .clickable {
                                                    selectedColor = color
                                                    bitmap = null
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (selectedColor == color && bitmap == null) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = if (color == Color.White) Color.Black else Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else { // Tuyến tính (Gradient)
                            // 1. Mẫu có sẵn
                            Text(
                                "Mẫu gradient",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            val presets = listOf(
                                listOf("#8E2DE2", "#4A00E0"),
                                listOf("#00C6FF", "#0072FF"),
                                listOf("#F12711", "#F5AF19"),
                                listOf("#11998E", "#38EF7D"),
                                listOf("#FF416C", "#FF4B2B"),
                                listOf("#EE9CA7", "#FFDDE1"),
                                listOf("#3A1C71", "#D76D77", "#FFAF7B"),
                                listOf("#0F2027", "#203A43", "#2C5364")
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                            ) {
                                items(presets.size) { pIndex ->
                                    val preset = presets[pIndex]
                                    val brush = Brush.linearGradient(preset.map { Color(android.graphics.Color.parseColor(it)) })
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(brush)
                                            .clickable {
                                                gradientColorsList = preset
                                            }
                                            .border(
                                                2.dp,
                                                if (gradientColorsList == preset) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                CircleShape
                                            )
                                    )
                                }
                            }

                            // 2. Chỉnh sửa màu sắc
                            Text(
                                "Tùy chỉnh màu sắc",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                            ) {
                                gradientColorsList.forEachIndexed { colorIdx, hexColor ->
                                    val color = Color(android.graphics.Color.parseColor(hexColor))
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .clickable {
                                                editingColorIndex = colorIdx
                                                try {
                                                    val hct = com.google.android.material.color.utilities.Hct.fromInt(color.toArgb())
                                                    pickerHct = Hct2(hct.hue.toFloat(), hct.chroma.toFloat(), hct.tone.toFloat())
                                                } catch (e: Exception) {}
                                                showColorPicker = true
                                            }
                                            .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                    )
                                }

                                if (gradientColorsList.size < 5) {
                                    IconButton(
                                        onClick = {
                                            gradientColorsList = gradientColorsList + "#FFFFFF"
                                        },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Thêm màu", modifier = Modifier.size(20.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                if (gradientColorsList.size > 2) {
                                    IconButton(
                                        onClick = {
                                            gradientColorsList = gradientColorsList.dropLast(1)
                                        },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .border(1.dp, MaterialTheme.colorScheme.error, CircleShape)
                                    ) {
                                        Icon(
                                            painterResource(R.drawable.delete),
                                            contentDescription = "Xóa màu",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            // 3. Chọn hướng
                            Text(
                                "Hướng màu",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            ) {
                                val directions = listOf(
                                    Pair(R.drawable.arrow_down, "Trên xuống"),
                                    Pair(R.drawable.arrow_up, "Dưới lên"),
                                    Pair(R.drawable.arrow_right, "Trái qua"),
                                    Pair(R.drawable.arrow_left, "Phải qua")
                                )
                                directions.forEachIndexed { dIndex, (dirDrawable, dirLabel) ->
                                    val isDirSelected = gradientDirectionState == dIndex
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isDirSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                            .border(
                                                1.dp,
                                                if (isDirSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { gradientDirectionState = dIndex },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painterResource(dirDrawable),
                                            contentDescription = dirLabel,
                                            tint = if (isDirSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (currentTab == 1) { // Tab Phím
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // 1. BÀN PHÍM
                        Text("Bàn phím", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                        
                        CustomColorSelector(
                            label = "Màu nền",
                            selectedColor = keyBgColor,
                            onColorSelected = { keyBgColor = it },
                            onCustomColorRequested = {
                                activeColorPickerField = "keyBg"
                                pickerHct = try {
                                    val hct = com.google.android.material.color.utilities.Hct.fromInt((keyBgColor ?: Color.Gray).toArgb())
                                    Hct2(hct.hue.toFloat(), hct.chroma.toFloat(), hct.tone.toFloat())
                                } catch(e: Exception) { Hct2(0f, 0f, 0f) }
                                showColorPicker = true
                            }
                        )

                        CustomColorSelector(
                            label = "Màu chữ",
                            selectedColor = keyTextColor,
                            onColorSelected = { keyTextColor = it },
                            onCustomColorRequested = {
                                activeColorPickerField = "keyText"
                                pickerHct = try {
                                    val hct = com.google.android.material.color.utilities.Hct.fromInt((keyTextColor ?: Color.White).toArgb())
                                    Hct2(hct.hue.toFloat(), hct.chroma.toFloat(), hct.tone.toFloat())
                                } catch(e: Exception) { Hct2(0f, 0f, 0f) }
                                showColorPicker = true
                            }
                        )

                        // Slider Kích cỡ
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Kích cỡ", fontSize = 14.sp, modifier = Modifier.width(90.dp))
                            Slider(
                                value = keyHeightScaleState,
                                onValueChange = { keyHeightScaleState = it },
                                valueRange = 0.7f..1.3f,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        // 2. THANH CÔNG CỤ
                        Text("Thanh công cụ", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))

                        CustomColorSelector(
                            label = "Màu nền",
                            selectedColor = toolbarBgColor,
                            onColorSelected = { toolbarBgColor = it },
                            onCustomColorRequested = {
                                activeColorPickerField = "toolbarBg"
                                pickerHct = try {
                                    val hct = com.google.android.material.color.utilities.Hct.fromInt((toolbarBgColor ?: Color.Gray).toArgb())
                                    Hct2(hct.hue.toFloat(), hct.chroma.toFloat(), hct.tone.toFloat())
                                } catch(e: Exception) { Hct2(0f, 0f, 0f) }
                                showColorPicker = true
                            }
                        )

                        CustomColorSelector(
                            label = "Màu chữ",
                            selectedColor = toolbarTextColor,
                            onColorSelected = { toolbarTextColor = it },
                            onCustomColorRequested = {
                                activeColorPickerField = "toolbarText"
                                pickerHct = try {
                                    val hct = com.google.android.material.color.utilities.Hct.fromInt((toolbarTextColor ?: Color.White).toArgb())
                                    Hct2(hct.hue.toFloat(), hct.chroma.toFloat(), hct.tone.toFloat())
                                } catch(e: Exception) { Hct2(0f, 0f, 0f) }
                                showColorPicker = true
                            }
                        )

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        // 3. POPUP
                        Text("Popup", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))

                        CustomColorSelector(
                            label = "Màu nền",
                            selectedColor = popupBgColor,
                            onColorSelected = { popupBgColor = it },
                            onCustomColorRequested = {
                                activeColorPickerField = "popupBg"
                                pickerHct = try {
                                    val hct = com.google.android.material.color.utilities.Hct.fromInt((popupBgColor ?: Color.Gray).toArgb())
                                    Hct2(hct.hue.toFloat(), hct.chroma.toFloat(), hct.tone.toFloat())
                                } catch(e: Exception) { Hct2(0f, 0f, 0f) }
                                showColorPicker = true
                            }
                        )

                        CustomColorSelector(
                            label = "Màu chữ",
                            selectedColor = popupTextColor,
                            onColorSelected = { popupTextColor = it },
                            onCustomColorRequested = {
                                activeColorPickerField = "popupText"
                                pickerHct = try {
                                    val hct = com.google.android.material.color.utilities.Hct.fromInt((popupTextColor ?: Color.White).toArgb())
                                    Hct2(hct.hue.toFloat(), hct.chroma.toFloat(), hct.tone.toFloat())
                                } catch(e: Exception) { Hct2(0f, 0f, 0f) }
                                showColorPicker = true
                            }
                        )

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        // 4. VIỀN
                        Text("Viền", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))

                        // Slider Viền cong
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Viền cong", fontSize = 14.sp, modifier = Modifier.width(90.dp))
                            Slider(
                                value = keyRoundnessState,
                                onValueChange = { keyRoundnessState = it },
                                valueRange = 0f..2f,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Slider Đổ bóng
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Đổ bóng", fontSize = 14.sp, modifier = Modifier.width(90.dp))
                            Slider(
                                value = keyShadowSizeState,
                                onValueChange = { keyShadowSizeState = it },
                                valueRange = 0f..8f,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Slider Cỡ viền
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Cỡ viền", fontSize = 14.sp, modifier = Modifier.width(90.dp))
                            Slider(
                                value = keyBorderSizeState,
                                onValueChange = { keyBorderSizeState = it },
                                valueRange = 0f..5f,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        CustomColorSelector(
                            label = "Màu viền",
                            selectedColor = keyBorderColorState,
                            onColorSelected = { keyBorderColorState = it },
                            onCustomColorRequested = {
                                activeColorPickerField = "keyBorder"
                                pickerHct = try {
                                    val hct = com.google.android.material.color.utilities.Hct.fromInt((keyBorderColorState ?: Color.Gray).toArgb())
                                    Hct2(hct.hue.toFloat(), hct.chroma.toFloat(), hct.tone.toFloat())
                                } catch(e: Exception) { Hct2(0f, 0f, 0f) }
                                showColorPicker = true
                            }
                        )

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        // 5. PHÍM CHỨC NĂNG
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Chỉnh màu cho phím chức năng", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                            Switch(
                                checked = functionalCustomEnabled,
                                onCheckedChange = { functionalCustomEnabled = it }
                            )
                        }

                        if (functionalCustomEnabled) {
                            CustomColorSelector(
                                label = "Màu nền",
                                selectedColor = functionalBgColor,
                                onColorSelected = { functionalBgColor = it },
                                onCustomColorRequested = {
                                    activeColorPickerField = "functionalBg"
                                    pickerHct = try {
                                        val hct = com.google.android.material.color.utilities.Hct.fromInt((functionalBgColor ?: Color.Gray).toArgb())
                                        Hct2(hct.hue.toFloat(), hct.chroma.toFloat(), hct.tone.toFloat())
                                    } catch(e: Exception) { Hct2(0f, 0f, 0f) }
                                    showColorPicker = true
                                }
                            )

                            CustomColorSelector(
                                label = "Màu chữ",
                                selectedColor = functionalTextColor,
                                onColorSelected = { functionalTextColor = it },
                                onCustomColorRequested = {
                                    activeColorPickerField = "functionalText"
                                    pickerHct = try {
                                        val hct = com.google.android.material.color.utilities.Hct.fromInt((functionalTextColor ?: Color.White).toArgb())
                                        Hct2(hct.hue.toFloat(), hct.chroma.toFloat(), hct.tone.toFloat())
                                    } catch(e: Exception) { Hct2(0f, 0f, 0f) }
                                    showColorPicker = true
                                }
                            )

                            CustomColorSelector(
                                label = "Màu viền",
                                selectedColor = functionalBorderColor,
                                onColorSelected = { functionalBorderColor = it },
                                onCustomColorRequested = {
                                    activeColorPickerField = "functionalBorder"
                                    pickerHct = try {
                                        val hct = com.google.android.material.color.utilities.Hct.fromInt((functionalBorderColor ?: Color.Gray).toArgb())
                                        Hct2(hct.hue.toFloat(), hct.chroma.toFloat(), hct.tone.toFloat())
                                    } catch(e: Exception) { Hct2(0f, 0f, 0f) }
                                    showColorPicker = true
                                }
                            )
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        // 6. PHÍM CÁCH
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Chỉnh màu cho phím cách", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                            Switch(
                                checked = spacebarCustomEnabled,
                                onCheckedChange = { spacebarCustomEnabled = it }
                            )
                        }

                        if (spacebarCustomEnabled) {
                            CustomColorSelector(
                                label = "Màu nền",
                                selectedColor = spacebarBgColor,
                                onColorSelected = { spacebarBgColor = it },
                                onCustomColorRequested = {
                                    activeColorPickerField = "spacebarBg"
                                    pickerHct = try {
                                        val hct = com.google.android.material.color.utilities.Hct.fromInt((spacebarBgColor ?: Color.Gray).toArgb())
                                        Hct2(hct.hue.toFloat(), hct.chroma.toFloat(), hct.tone.toFloat())
                                    } catch(e: Exception) { Hct2(0f, 0f, 0f) }
                                    showColorPicker = true
                                }
                            )

                            CustomColorSelector(
                                label = "Màu chữ",
                                selectedColor = spacebarTextColor,
                                onColorSelected = { spacebarTextColor = it },
                                onCustomColorRequested = {
                                    activeColorPickerField = "spacebarText"
                                    pickerHct = try {
                                        val hct = com.google.android.material.color.utilities.Hct.fromInt((spacebarTextColor ?: Color.White).toArgb())
                                        Hct2(hct.hue.toFloat(), hct.chroma.toFloat(), hct.tone.toFloat())
                                    } catch(e: Exception) { Hct2(0f, 0f, 0f) }
                                    showColorPicker = true
                                }
                            )

                            CustomColorSelector(
                                label = "Màu viền",
                                selectedColor = spacebarBorderColor,
                                onColorSelected = { spacebarBorderColor = it },
                                onCustomColorRequested = {
                                    activeColorPickerField = "spacebarBorder"
                                    pickerHct = try {
                                        val hct = com.google.android.material.color.utilities.Hct.fromInt((spacebarBorderColor ?: Color.Gray).toArgb())
                                        Hct2(hct.hue.toFloat(), hct.chroma.toFloat(), hct.tone.toFloat())
                                    } catch(e: Exception) { Hct2(0f, 0f, 0f) }
                                    showColorPicker = true
                                }
                            )
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Chức năng đang phát triển", color = Color.Gray)
                    }
                }
            }
        }

        if (showColorPicker) {
            AlertDialog(
                onDismissRequest = { 
                    editingColorIndex = -1
                    activeColorPickerField = null
                    showColorPicker = false 
                },
                title = { Text(if (activeColorPickerField != null) "Tùy chỉnh màu" else "Chọn màu nền") },
                text = {
                    HctSliderPicker(hct = pickerHct, onColorChange = { pickerHct = it })
                },
                confirmButton = {
                    TextButton(onClick = {
                        val c = com.google.android.material.color.utilities.Hct.from(pickerHct.hue.toDouble(), pickerHct.chroma.toDouble(), pickerHct.tone.toDouble())
                        val selectedCol = Color(c.toInt())
                        if (activeColorPickerField != null) {
                            when (activeColorPickerField) {
                                "keyBg" -> keyBgColor = selectedCol
                                "keyText" -> keyTextColor = selectedCol
                                "toolbarBg" -> toolbarBgColor = selectedCol
                                "toolbarText" -> toolbarTextColor = selectedCol
                                "popupBg" -> popupBgColor = selectedCol
                                "popupText" -> popupTextColor = selectedCol
                                "keyBorder" -> keyBorderColorState = selectedCol
                                "functionalBg" -> functionalBgColor = selectedCol
                                "functionalText" -> functionalTextColor = selectedCol
                                "functionalBorder" -> functionalBorderColor = selectedCol
                                "spacebarBg" -> spacebarBgColor = selectedCol
                                "spacebarText" -> spacebarTextColor = selectedCol
                                "spacebarBorder" -> spacebarBorderColor = selectedCol
                            }
                            activeColorPickerField = null
                        } else if (bgMode == 2 && editingColorIndex != -1) {
                            val hexColor = "#%06X".format(0xFFFFFF and c.toInt())
                            val newList = gradientColorsList.toMutableList()
                            newList[editingColorIndex] = hexColor
                            gradientColorsList = newList
                            editingColorIndex = -1
                        } else {
                            selectedColor = selectedCol
                            bitmap = null
                            bgMode = 1
                        }
                        showColorPicker = false
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        editingColorIndex = -1
                        activeColorPickerField = null
                        showColorPicker = false
                    }) {
                        Text("Hủy")
                    }
                }
            )
        }
    }
}

val presetThemeColors = listOf(
    null,
    Color(0xFF0066FF), Color.White, Color.Gray, Color.DarkGray,
    Color.Black, Color(0xFF00BCD4), Color(0xFF009688), Color(0xFF4CAF50),
    Color(0xFF8BC34A), Color(0xFFFFEB3B), Color(0xFFFFC107),
    Color(0xFFFF9800), Color(0xFFF44336), Color(0xFFE91E63), Color(0xFF9C27B0)
)

@Composable
fun CustomColorSelector(
    label: String,
    selectedColor: Color?,
    onColorSelected: (Color?) -> Unit,
    onCustomColorRequested: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            modifier = Modifier.width(90.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.sweepGradient(
                                listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
                            )
                        )
                        .clickable { onCustomColorRequested() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("...", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            items(presetThemeColors.size) { index ->
                val color = presetThemeColors[index]
                val isSelected = selectedColor == color
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(color ?: Color.Transparent)
                        .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .clickable { onColorSelected(color) },
                    contentAlignment = Alignment.Center
                ) {
                    if (color == null) {
                        Icon(
                            painterResource(R.drawable.themes),
                            contentDescription = "Mặc định",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = if (color == Color.White || color == null) Color.Black else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
