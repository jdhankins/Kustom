package com.hank.kustom.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.hank.kustom.data.model.FieldCategory
import com.hank.kustom.data.model.GlobalTypographyConfig
import com.hank.kustom.data.model.KustomFieldCatalog
import com.hank.kustom.data.model.KustomFieldConfig
import com.hank.kustom.data.model.KustomFieldDefinition
import com.hank.kustom.data.model.KustomFont
import com.hank.kustom.data.model.KustomSettings
import com.hank.kustom.data.model.SmoothingOption
import com.hank.kustom.data.model.TypographyConfig
import com.hank.kustom.data.model.ZoneFormat
import com.hank.kustom.domain.KustomColorGradeMapper
import com.hank.kustom.domain.KustomFieldRenderer
import com.hank.kustom.util.KustomLogger
import io.hammerhead.karooext.models.ViewConfig
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Dynamic adaptive color scheme model for Light and Dark modes.
 *
 * @property bg Base screen background color.
 * @property cardBg Card surface background color.
 * @property trackBg Input track / pill container background color.
 * @property textPrimary Primary text color.
 * @property textSecondary Secondary text color.
 * @property accent Primary accent highlight color (Electric Blue).
 * @property border Card border stroke color.
 * @property sliderTrack Slider track background color.
 * @property headerText Section header text color.
 * @property isDark Whether dark theme is active.
 */
data class KustomColorScheme(
    /** Base screen background color. */
    val bg: Color,

    /** Card surface background color. */
    val cardBg: Color,

    /** Input track / pill container background color. */
    val trackBg: Color,

    /** Primary text color. */
    val textPrimary: Color,

    /** Secondary text color. */
    val textSecondary: Color,

    /** Primary accent highlight color. */
    val accent: Color,

    /** Card border stroke color. */
    val border: Color,

    /** Slider track background color. */
    val sliderTrack: Color,

    /** Section header text color. */
    val headerText: Color,

    /** Whether dark theme is active. */
    val isDark: Boolean,
)

/**
 * Resolves the active [KustomColorScheme] based on system Light/Dark mode settings.
 *
 * @param isDark Whether dark mode is enabled on the device.
 * @return The corresponding [KustomColorScheme].
 */
@Composable
fun getKustomColors(isDark: Boolean = isSystemInDarkTheme()): KustomColorScheme {
    return if (isDark) {
        KustomColorScheme(
            bg = Color(0xFF0F1A2A),
            cardBg = Color(0xFF17283C),
            trackBg = Color(0xFF1F354E),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFF94A3B8),
            accent = Color(0xFF00A3FF),
            border = Color(0xFF2A4363),
            sliderTrack = Color(0xFF2A4363),
            headerText = Color(0xFFE2E8F0),
            isDark = true,
        )
    } else {
        KustomColorScheme(
            bg = Color(0xFFDCE1E3),
            cardBg = Color(0xFFF4F6F7),
            trackBg = Color(0xFFEEF1F2),
            textPrimary = Color(0xFF1E2225),
            textSecondary = Color(0xFF4A5258),
            accent = Color(0xFF00A3FF),
            border = Color(0xFFCFD8DC),
            sliderTrack = Color(0xFFDCDFE2),
            headerText = Color(0xFF334155),
            isDark = false,
        )
    }
}

/**
 * Main application entry activity for user settings and typography configuration.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: KustomSettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        KustomLogger.installCrashHandler(this)
        super.onCreate(savedInstanceState)
        KustomLogger.d("MainActivity created")

        WindowCompat.setDecorFitsSystemWindows(window, true)

        setContent {
            val colors = getKustomColors()
            val context = LocalContext.current

            val window = (context as? ComponentActivity)?.window
            LaunchedEffect(colors.isDark) {
                window?.let { w ->
                    w.statusBarColor = colors.bg.toArgb()
                    val controller = WindowCompat.getInsetsController(w, w.decorView)
                    controller.isAppearanceLightStatusBars = !colors.isDark
                }
            }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = colors.bg,
            ) {
                KustomKarooNativeScreen(viewModel = viewModel, colors = colors)
            }
        }
    }
}

/**
 * Primary Jetpack Compose screen layout for Kustom settings activity.
 *
 * @param viewModel Settings view model instance ([KustomSettingsViewModel]).
 * @param colors Active adaptive color scheme ([KustomColorScheme]).
 */
@Composable
fun KustomKarooNativeScreen(
    viewModel: KustomSettingsViewModel,
    colors: KustomColorScheme,
) {
    val settings by viewModel.settingsState.collectAsState()
    var selectedTopNavTab by remember { mutableIntStateOf(0) } // 0 = Global, 1 = Fields
    var selectedField by remember { mutableStateOf(KustomFieldCatalog.POWER) }
    var expandedCategory by remember { mutableStateOf<FieldCategory?>(FieldCategory.POWER) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        // Sticky Header Section: Pill Navigation + Live Field Preview (Global Only)
        KustomStickyHeaderSection(
            settings = settings,
            selectedTab = selectedTopNavTab,
            colors = colors,
            onTabSelected = { selectedTopNavTab = it },
        )

        // Scrollable Menu Body
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            if (selectedTopNavTab == 0) {
                GlobalFontsTab(
                    settings = settings,
                    colors = colors,
                    onUpdateGlobalTypography = { viewModel.updateGlobalTypography(it) },
                )
            } else {
                FieldsTab(
                    settings = settings,
                    colors = colors,
                    expandedCategory = expandedCategory,
                    onFieldSelected = { field ->
                        selectedField = field
                        expandedCategory = field.category
                    },
                    onCategoryToggled = { category ->
                        expandedCategory = if (expandedCategory == category) null else category
                    },
                    onUpdateFieldConfig = { typeId, config ->
                        viewModel.updateFieldConfig(typeId, config)
                    },
                )
            }
        }
    }
}

/**
 * Top sticky header section featuring pill navigation and global live field preview.
 *
 * @param settings Current [KustomSettings] state.
 * @param selectedTab Active tab index (0 = Global, 1 = Fields).
 * @param colors Active adaptive color scheme ([KustomColorScheme]).
 * @param onTabSelected Callback invoked when a navigation tab is selected.
 */
@Composable
fun KustomStickyHeaderSection(
    settings: KustomSettings,
    selectedTab: Int,
    colors: KustomColorScheme,
    onTabSelected: (Int) -> Unit,
) {
    val context = LocalContext.current

    val currentField = KustomFieldCatalog.POWER

    var mockStepIndex by remember(selectedTab) { mutableIntStateOf(0) }
    LaunchedEffect(selectedTab) {
        while (true) {
            delay(1200L)
            mockStepIndex = (mockStepIndex + 1) % currentField.mockValues.size
        }
    }

    val currentMockVal = currentField.mockValues.getOrElse(mockStepIndex) { currentField.mockValues.first() }
    val fieldConfig = settings.getFieldConfig(currentField.typeId)

    val sampleValueText = currentField.formatNumericValueOnly(currentMockVal)
    val dynamicLabel = currentField.resolveDynamicLabel(fieldConfig.smoothingOption)

    val mappedColor = if (currentField.supportsZones) {
        KustomColorGradeMapper.getPowerZoneColor(currentMockVal, null, fieldConfig.colorMode)
    } else null

    val globalTypo = settings.globalTypography

    val previewViewConfig = remember {
        ViewConfig(
            gridSize = Pair(60, 20),
            viewSize = Pair(360, 240),
            textSize = 38,
            alignment = ViewConfig.Alignment.CENTER,
            boundariesEnabled = true,
            preview = true,
        )
    }

    val bitmap = KustomFieldRenderer.render(
        context = context,
        config = previewViewConfig,
        label = dynamicLabel,
        valueText = sampleValueText,
        labelTypography = TypographyConfig(
            font = globalTypo.fontFamily,
            fontWeight = globalTypo.labelWeight,
            fontSizeSp = globalTypo.labelSizeSp.toFloat(),
        ),
        valueTypography = TypographyConfig(
            font = globalTypo.fontFamily,
            fontWeight = globalTypo.valueWeight,
            fontSizeSp = globalTypo.valueSizeSp.toFloat(),
        ),
        colorMode = fieldConfig.colorMode,
        mappedColor = mappedColor,
        isDarkTheme = colors.isDark,
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.cardBg),
        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // 1. Navigation Pill Bar (Global | Fields)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(50))
                    .background(colors.trackBg)
                    .padding(3.dp),
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .background(if (selectedTab == 0) colors.accent else Color.Transparent)
                            .clickable { onTabSelected(0) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Global",
                            fontSize = 14.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                            color = colors.textPrimary,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .background(if (selectedTab == 1) colors.accent else Color.Transparent)
                            .clickable { onTabSelected(1) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Fields",
                            fontSize = 14.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                            color = colors.textPrimary,
                        )
                    }
                }
            }

            // 2. Sticky Live Field Preview Box Centered (GLOBAL TAB ONLY)
            if (selectedTab == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .width(160.dp)
                            .height(76.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(BorderStroke(1.dp, colors.border), RoundedCornerShape(12.dp)),
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Live Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.FillBounds,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Global typography settings tab containing font family dropdown and typography sliders.
 *
 * @param settings Current [KustomSettings] state.
 * @param colors Active adaptive color scheme ([KustomColorScheme]).
 * @param onUpdateGlobalTypography Callback invoked to update global typography configuration.
 */
@Composable
fun GlobalFontsTab(
    settings: KustomSettings,
    colors: KustomColorScheme,
    onUpdateGlobalTypography: (GlobalTypographyConfig) -> Unit,
) {
    val global = settings.globalTypography

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Spacer(modifier = Modifier.height(2.dp))
        }

        // FONT FAMILY DROPDOWN
        item {
            KarooFontDropdown(
                selectedFont = global.fontFamily,
                colors = colors,
                onFontSelected = { font ->
                    onUpdateGlobalTypography(global.copy(fontFamily = font))
                },
            )
        }

        // VALUE FONT SIZE (36, 42, 48, 56, 64, 72)
        item {
            val valueSizes = listOf(36f, 42f, 48f, 56f, 64f, 72f)
            val valueSizeSteps = listOf("36", "42", "48", "56", "64", "72")
            val currentIdx = findNearestIndex(valueSizes, global.valueSizeSp.toFloat())
            KarooSteppedSlider(
                label = "Value Font Size",
                steps = valueSizeSteps,
                selectedIndex = currentIdx,
                colors = colors,
                onStepSelected = { idx ->
                    val newSize = valueSizes.getOrElse(idx) { 48f }.toInt()
                    onUpdateGlobalTypography(global.copy(valueSizeSp = newSize))
                },
            )
        }

        // VALUE FONT WEIGHT (300, 400, 500, 700, 900)
        item {
            val weights = listOf(300f, 400f, 500f, 700f, 900f)
            val weightSteps = listOf("300", "400", "500", "700", "900")
            val currentIdx = findNearestIndex(weights, global.valueWeight.toFloat())
            KarooSteppedSlider(
                label = "Value Font Weight",
                steps = weightSteps,
                selectedIndex = currentIdx,
                colors = colors,
                onStepSelected = { idx ->
                    val newWeight = weights.getOrElse(idx) { 700f }.toInt()
                    onUpdateGlobalTypography(global.copy(valueWeight = newWeight))
                },
            )
        }

        // LABEL FONT SIZE (10, 12, 14, 16, 18, 20)
        item {
            val labelSizes = listOf(10f, 12f, 14f, 16f, 18f, 20f)
            val labelSizeSteps = listOf("10", "12", "14", "16", "18", "20")
            val currentIdx = findNearestIndex(labelSizes, global.labelSizeSp.toFloat())
            KarooSteppedSlider(
                label = "Label Font Size",
                steps = labelSizeSteps,
                selectedIndex = currentIdx,
                colors = colors,
                onStepSelected = { idx ->
                    val newSize = labelSizes.getOrElse(idx) { 14f }.toInt()
                    onUpdateGlobalTypography(global.copy(labelSizeSp = newSize))
                },
            )
        }

        // LABEL FONT WEIGHT (300, 400, 500, 700, 900)
        item {
            val weights = listOf(300f, 400f, 500f, 700f, 900f)
            val weightSteps = listOf("300", "400", "500", "700", "900")
            val currentIdx = findNearestIndex(weights, global.labelWeight.toFloat())
            KarooSteppedSlider(
                label = "Label Font Weight",
                steps = weightSteps,
                selectedIndex = currentIdx,
                colors = colors,
                onStepSelected = { idx ->
                    val newWeight = weights.getOrElse(idx) { 700f }.toInt()
                    onUpdateGlobalTypography(global.copy(labelWeight = newWeight))
                },
            )
        }

        // Bottom padding to ensure bottom slider is never clipped at screen edge
        item {
            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

/**
 * Per-metric fields configuration accordion tab grouping configurable metrics by category.
 *
 * @param settings Current [KustomSettings] state.
 * @param colors Active adaptive color scheme ([KustomColorScheme]).
 * @param expandedCategory Currently expanded field category ([FieldCategory]).
 * @param onFieldSelected Callback invoked when a field card is selected.
 * @param onCategoryToggled Callback invoked to toggle category expansion.
 * @param onUpdateFieldConfig Callback invoked to update configuration for a specific field.
 */
@Composable
fun FieldsTab(
    settings: KustomSettings,
    colors: KustomColorScheme,
    expandedCategory: FieldCategory?,
    onFieldSelected: (KustomFieldDefinition) -> Unit,
    onCategoryToggled: (FieldCategory) -> Unit,
    onUpdateFieldConfig: (String, KustomFieldConfig) -> Unit,
) {
    val configurableGrouped = remember {
        KustomFieldCatalog.configurableFields.groupBy { it.category }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        configurableGrouped.forEach { (category, fields) ->
            item {
                val isExpanded = expandedCategory == category

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = colors.cardBg),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCategoryToggled(category) }
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "${category.displayName.uppercase()} FIELDS",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                            )
                            Text(
                                text = if (isExpanded) "▲" else "▼",
                                fontSize = 14.sp,
                                color = colors.textSecondary,
                            )
                        }

                        AnimatedVisibility(visible = isExpanded) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                fields.forEach { field ->
                                    val fieldConfig = settings.getFieldConfig(field.typeId)
                                    FieldCardItem(
                                        field = field,
                                        fieldConfig = fieldConfig,
                                        globalTypography = settings.globalTypography,
                                        colors = colors,
                                        onFieldSelected = onFieldSelected,
                                        onUpdateFieldConfig = onUpdateFieldConfig,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual field configuration card displaying metric name, description, live preview, and controls.
 *
 * @param field Field definition metadata ([KustomFieldDefinition]).
 * @param fieldConfig Field specific configuration ([KustomFieldConfig]).
 * @param globalTypography Shared global typography configuration ([GlobalTypographyConfig]).
 * @param colors Active adaptive color scheme ([KustomColorScheme]).
 * @param onFieldSelected Callback invoked when card is selected.
 * @param onUpdateFieldConfig Callback invoked to update field configuration.
 */
@Composable
fun FieldCardItem(
    field: KustomFieldDefinition,
    fieldConfig: KustomFieldConfig,
    globalTypography: GlobalTypographyConfig,
    colors: KustomColorScheme,
    onFieldSelected: (KustomFieldDefinition) -> Unit,
    onUpdateFieldConfig: (String, KustomFieldConfig) -> Unit,
) {
    val context = LocalContext.current

    // Live Mock Ticker every 1200ms for per-metric preview
    var mockStepIndex by remember(field) { mutableIntStateOf(0) }
    LaunchedEffect(field) {
        while (true) {
            delay(1200L)
            mockStepIndex = (mockStepIndex + 1) % field.mockValues.size
        }
    }

    val currentMockVal = field.mockValues.getOrElse(mockStepIndex) { field.mockValues.first() }
    val sampleValueText = field.formatNumericValueOnly(currentMockVal)
    val dynamicLabel = field.resolveDynamicLabel(fieldConfig.smoothingOption)

    val mappedColor = if (field.supportsZones) {
        when (field.typeId) {
            "power", "np", "avg-power", "max-power", "lap-power", "last-lap-power", "percent-ftp", "power-balance", "power-zone", "power-to-weight", "workout-target-power", "workout-primary-target" -> KustomColorGradeMapper.getPowerZoneColor(currentMockVal, null, fieldConfig.colorMode)
            "hr", "avg-hr", "max-hr", "lap-hr", "percent-max-hr", "hr-zone" -> KustomColorGradeMapper.getHeartRateZoneColor(currentMockVal, null, fieldConfig.colorMode)
            "grade" -> KustomColorGradeMapper.getGradeColor(currentMockVal, fieldConfig.colorMode)
            else -> null
        }
    } else null

    val previewViewConfig = remember {
        ViewConfig(
            gridSize = Pair(60, 20),
            viewSize = Pair(320, 180),
            textSize = 38,
            alignment = ViewConfig.Alignment.CENTER,
            boundariesEnabled = true,
            preview = true,
        )
    }

    val bitmap = KustomFieldRenderer.render(
        context = context,
        config = previewViewConfig,
        label = dynamicLabel,
        valueText = sampleValueText,
        labelTypography = TypographyConfig(
            font = globalTypography.fontFamily,
            fontWeight = globalTypography.labelWeight,
            fontSizeSp = globalTypography.labelSizeSp.toFloat(),
        ),
        valueTypography = TypographyConfig(
            font = globalTypography.fontFamily,
            fontWeight = globalTypography.valueWeight,
            fontSizeSp = globalTypography.valueSizeSp.toFloat(),
        ),
        colorMode = fieldConfig.colorMode,
        mappedColor = mappedColor,
        isDarkTheme = colors.isDark,
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onFieldSelected(field) },
        colors = CardDefaults.cardColors(containerColor = colors.cardBg),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, colors.border),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Bold Metric Name
            Text(
                text = field.childTitle,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
            )

            // Description on Next Line
            Text(
                text = field.description,
                fontSize = 13.sp,
                color = colors.textSecondary,
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Per-Metric Live Preview Box (Centered, Narrower Width)
            Box(
                modifier = Modifier
                    .width(150.dp)
                    .height(72.dp)
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(12.dp))
                    .border(BorderStroke(1.dp, colors.border), RoundedCornerShape(12.dp)),
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Metric Preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds,
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Controls Below
            if (field.supportsSmoothing) {
                val smoothingSteps = listOf("0s", "3s", "5s", "10s", "30s")
                val currentSmoothIdx = SmoothingOption.entries.indexOf(fieldConfig.smoothingOption).coerceAtLeast(0)
                KarooSteppedSlider(
                    label = "Smoothing",
                    steps = smoothingSteps,
                    selectedIndex = currentSmoothIdx,
                    colors = colors,
                    onStepSelected = { idx ->
                        val option = SmoothingOption.entries.getOrElse(idx) { SmoothingOption.THREE_SEC }
                        onUpdateFieldConfig(field.typeId, fieldConfig.copy(smoothingOption = option))
                    },
                )
            }

            if (field.supportsZones) {
                KarooSegmentedPill(
                    selectedZoneFormat = fieldConfig.zoneFormat,
                    fieldName = field.childTitle,
                    colors = colors,
                    onZoneFormatSelected = { format ->
                        onUpdateFieldConfig(field.typeId, fieldConfig.copy(zoneFormat = format))
                    },
                )
            }
        }
    }
}

/**
 * Dropdown menu selector for global font family choice.
 *
 * @param selectedFont Currently selected [KustomFont] choice.
 * @param colors Active adaptive color scheme ([KustomColorScheme]).
 * @param onFontSelected Callback invoked when a font is selected.
 * @param modifier Optional Compose layout modifier.
 */
@Composable
fun KarooFontDropdown(
    selectedFont: KustomFont,
    colors: KustomColorScheme,
    onFontSelected: (KustomFont) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "FONT FAMILY",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = colors.headerText,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(colors.trackBg)
                .clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = selectedFont.displayName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                )
                Text(
                    text = if (expanded) "▲" else "▼",
                    fontSize = 14.sp,
                    color = colors.textSecondary,
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .background(colors.cardBg),
            ) {
                KustomFont.entries.forEach { font ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = font.displayName,
                                fontSize = 14.sp,
                                fontWeight = if (font == selectedFont) FontWeight.Bold else FontWeight.Normal,
                                color = colors.textPrimary,
                            )
                        },
                        onClick = {
                            onFontSelected(font)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

/**
 * High-precision interactive stepped slider component with 60fps local gesture tracking.
 *
 * @param label Section title label string.
 * @param steps List of step label strings.
 * @param selectedIndex Currently selected step index.
 * @param colors Active adaptive color scheme ([KustomColorScheme]).
 * @param onStepSelected Callback invoked when a step is selected.
 * @param modifier Optional Compose layout modifier.
 */
@Composable
fun KarooSteppedSlider(
    label: String,
    steps: List<String>,
    selectedIndex: Int,
    colors: KustomColorScheme,
    onStepSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = colors.headerText,
        )

        val safeIndex = selectedIndex.coerceIn(0, (steps.size - 1).coerceAtLeast(0))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            val totalWidthPx = constraints.maxWidth.toFloat()
            val density = LocalDensity.current
            val paddingPx = with(density) { 16.dp.toPx() }
            val thumbRadiusPx = with(density) { 11.dp.toPx() }
            val travelPx = (totalWidthPx - (paddingPx * 2f)).coerceAtLeast(1f)

            val stepCount = steps.size
            val fraction = if (stepCount > 1) safeIndex.toFloat() / (stepCount - 1) else 0f
            val thumbCenterXPx = paddingPx + (fraction * travelPx)

            val activeTrackWidthDp = with(density) { (thumbCenterXPx - paddingPx).toDp() }
            val thumbOffsetDp = with(density) { (thumbCenterXPx - thumbRadiusPx).toDp() }

            fun updateFromPx(xPx: Float) {
                if (stepCount <= 1 || travelPx <= 0f) return
                val clampedX = (xPx - paddingPx).coerceIn(0f, travelPx)
                val targetFraction = clampedX / travelPx
                val nearestIdx = (targetFraction * (stepCount - 1)).roundToInt().coerceIn(0, stepCount - 1)
                onStepSelected(nearestIdx)
            }

            // Touch & Drag Gesture Container (40dp Height Target)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(stepCount) {
                        detectTapGestures { offset ->
                            updateFromPx(offset.x)
                        }
                    }
                    .pointerInput(stepCount) {
                        detectHorizontalDragGestures { change, _ ->
                            change.consume()
                            updateFromPx(change.position.x)
                        }
                    },
                contentAlignment = Alignment.CenterStart,
            ) {
                // Inactive Background Track
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(colors.sliderTrack),
                )

                // Active Progress Track (Accent)
                if (activeTrackWidthDp > 0.dp) {
                    Box(
                        modifier = Modifier
                            .offset(x = 16.dp)
                            .width(activeTrackWidthDp)
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(colors.accent),
                    )
                }

                // Thumb Circle (22dp Accent Target)
                Box(
                    modifier = Modifier
                        .offset(x = thumbOffsetDp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(colors.accent),
                )
            }
        }

        // Aligned Step Labels Row with Individual Click Targets
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            steps.forEachIndexed { i, stepLabel ->
                val isSelected = i == safeIndex
                Box(
                    modifier = Modifier.clickable { onStepSelected(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stepLabel,
                        fontSize = 12.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) colors.accent else colors.textSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}

/**
 * Segmented pill option selector for zone color formatting choices (None, Text, Fill).
 *
 * @param selectedZoneFormat Currently selected [ZoneFormat].
 * @param fieldName Metric child title string.
 * @param colors Active adaptive color scheme ([KustomColorScheme]).
 * @param onZoneFormatSelected Callback invoked when a zone format option is selected.
 * @param modifier Optional Compose layout modifier.
 */
@Composable
fun KarooSegmentedPill(
    selectedZoneFormat: ZoneFormat,
    fieldName: String,
    colors: KustomColorScheme,
    onZoneFormatSelected: (ZoneFormat) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "ZONE COLOR",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = colors.headerText,
        )

        val descriptionText = when (selectedZoneFormat) {
            ZoneFormat.NONE -> "Your ${fieldName.lowercase()} will display in numbers only."
            ZoneFormat.TEXT -> "Your ${fieldName.lowercase()} will display in numbers with the corresponding zone as the text color."
            ZoneFormat.BACKGROUND -> "Your ${fieldName.lowercase()} will display in numbers with the corresponding zone as the background color."
        }

        Text(
            text = descriptionText,
            fontSize = 13.sp,
            color = colors.textSecondary,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(50))
                .background(colors.trackBg)
                .border(BorderStroke(1.dp, colors.border), RoundedCornerShape(50))
                .padding(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val options = listOf(
                    Triple(ZoneFormat.NONE, "None", 1f),
                    Triple(ZoneFormat.TEXT, "Text", 1f),
                    Triple(ZoneFormat.BACKGROUND, "Fill", 1f),
                )

                options.forEach { (format, labelText, weight) ->
                    val isSelected = selectedZoneFormat == format

                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .background(if (isSelected) colors.accent else Color.Transparent)
                            .clickable { onZoneFormatSelected(format) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = labelText,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else colors.textSecondary,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Helper finding the list index closest in value to a target float.
 *
 * @param values Candidate float values list.
 * @param target Target float value.
 * @return Index of nearest value in list, or 0 if empty.
 */
fun findNearestIndex(values: List<Float>, target: Float): Int {
    return values.minByOrNull { abs(it - target) }?.let { values.indexOf(it) } ?: 0
}
