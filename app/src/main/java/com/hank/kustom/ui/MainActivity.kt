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
import androidx.compose.runtime.rememberUpdatedState
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
import com.hank.kustom.data.model.ColorMode
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
 */
data class KustomColorScheme(
    val bg: Color,
    val cardBg: Color,
    val trackBg: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val border: Color,
    val sliderTrack: Color,
    val headerText: Color,
    val isDark: Boolean,
)

/**
 * Resolves active [KustomColorScheme] based on system Light/Dark mode settings.
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
                    viewModel = viewModel,
                )
            } else {
                FieldsTab(
                    settings = settings,
                    colors = colors,
                    viewModel = viewModel,
                    expandedCategory = expandedCategory,
                    onFieldSelected = { field ->
                        selectedField = field
                        expandedCategory = field.category
                    },
                    onCategoryToggled = { category ->
                        expandedCategory = if (expandedCategory == category) null else category
                    },
                )
            }
        }
    }
}

/**
 * Top sticky header section featuring pill navigation and global live field preview.
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
                    BoxWithConstraints(
                        modifier = Modifier
                            .width(168.dp)
                            .height(78.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(BorderStroke(1.dp, colors.border), RoundedCornerShape(12.dp)),
                    ) {
                        val widthPx = constraints.maxWidth.coerceAtLeast(1)
                        val heightPx = constraints.maxHeight.coerceAtLeast(1)

                        val previewViewConfig = remember(widthPx, heightPx) {
                            ViewConfig(
                                gridSize = Pair(60, 20),
                                viewSize = Pair(widthPx, heightPx),
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

                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Live Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Global typography settings tab containing font family dropdown and typography sliders.
 */
@Composable
fun GlobalFontsTab(
    settings: KustomSettings,
    colors: KustomColorScheme,
    viewModel: KustomSettingsViewModel,
) {
    val global = settings.globalTypography
    var isTypographyExpanded by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
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
                            .clickable { isTypographyExpanded = !isTypographyExpanded }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "TYPOGRAPHY",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                        )
                        Text(
                            text = if (isTypographyExpanded) "▲" else "▼",
                            fontSize = 14.sp,
                            color = colors.textSecondary,
                        )
                    }

                    AnimatedVisibility(visible = isTypographyExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            // FONT FAMILY DROPDOWN
                            KarooFontDropdown(
                                selectedFont = global.fontFamily,
                                colors = colors,
                                onFontSelected = { font ->
                                    viewModel.updateFontFamily(font)
                                },
                            )

                            // VALUE FONT SIZE (36, 42, 48, 56, 64, 72)
                            val valueSizes = listOf(36f, 42f, 48f, 56f, 64f, 72f)
                            val valueSizeSteps = listOf("36", "42", "48", "56", "64", "72")
                            val currentValSizeIdx = findNearestIndex(valueSizes, global.valueSizeSp.toFloat())
                            KarooSteppedSlider(
                                label = "Value Font Size",
                                steps = valueSizeSteps,
                                selectedIndex = currentValSizeIdx,
                                colors = colors,
                                onStepSelected = { idx ->
                                    val newSize = valueSizes.getOrElse(idx) { 48f }.toInt()
                                    viewModel.updateValueFontSizeSp(newSize)
                                },
                            )

                            // VALUE FONT WEIGHT (300, 400, 500, 700, 900)
                            val weights = listOf(300f, 400f, 500f, 700f, 900f)
                            val weightSteps = listOf("300", "400", "500", "700", "900")
                            val currentValWeightIdx = findNearestIndex(weights, global.valueWeight.toFloat())
                            KarooSteppedSlider(
                                label = "Value Font Weight",
                                steps = weightSteps,
                                selectedIndex = currentValWeightIdx,
                                colors = colors,
                                onStepSelected = { idx ->
                                    val newWeight = weights.getOrElse(idx) { 700f }.toInt()
                                    viewModel.updateValueFontWeight(newWeight)
                                },
                            )

                            // LABEL FONT SIZE (10, 12, 14, 16, 18, 20)
                            val labelSizes = listOf(10f, 12f, 14f, 16f, 18f, 20f)
                            val labelSizeSteps = listOf("10", "12", "14", "16", "18", "20")
                            val currentLabelSizeIdx = findNearestIndex(labelSizes, global.labelSizeSp.toFloat())
                            KarooSteppedSlider(
                                label = "Label Font Size",
                                steps = labelSizeSteps,
                                selectedIndex = currentLabelSizeIdx,
                                colors = colors,
                                onStepSelected = { idx ->
                                    val newSize = labelSizes.getOrElse(idx) { 14f }.toInt()
                                    viewModel.updateLabelFontSizeSp(newSize)
                                },
                            )

                            // LABEL FONT WEIGHT (300, 400, 500, 700, 900)
                            val currentLabelWeightIdx = findNearestIndex(weights, global.labelWeight.toFloat())
                            KarooSteppedSlider(
                                label = "Label Font Weight",
                                steps = weightSteps,
                                selectedIndex = currentLabelWeightIdx,
                                colors = colors,
                                onStepSelected = { idx ->
                                    val newWeight = weights.getOrElse(idx) { 700f }.toInt()
                                    viewModel.updateLabelFontWeight(newWeight)
                                },
                            )
                        }
                    }
                }
            }
        }

        // Bottom padding to ensure bottom slider is never clipped at screen edge
        item {
            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

/**
 * Per-metric fields configuration accordion tab grouping configurable metrics by category.
 */
@Composable
fun FieldsTab(
    settings: KustomSettings,
    colors: KustomColorScheme,
    viewModel: KustomSettingsViewModel,
    expandedCategory: FieldCategory?,
    onFieldSelected: (KustomFieldDefinition) -> Unit,
    onCategoryToggled: (FieldCategory) -> Unit,
) {
    val configurableGrouped = remember {
        KustomFieldCatalog.configurableFields.groupBy { it.category }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
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
                                        viewModel = viewModel,
                                        onFieldSelected = onFieldSelected,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

/**
 * Individual field configuration card displaying metric name, description, live preview, and controls.
 */
@Composable
fun FieldCardItem(
    field: KustomFieldDefinition,
    fieldConfig: KustomFieldConfig,
    globalTypography: GlobalTypographyConfig,
    colors: KustomColorScheme,
    viewModel: KustomSettingsViewModel,
    onFieldSelected: (KustomFieldDefinition) -> Unit,
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
            "power", "np", "avg-power", "max-power", "power-20m", "power-1hr", "lap-power", "lap-np", "last-lap-power", "last-lap-np", "percent-ftp", "power-balance", "power-zone", "power-to-weight", "workout-target-power", "workout-primary-target" -> KustomColorGradeMapper.getPowerZoneColor(currentMockVal, null, fieldConfig.colorMode)
            "hr", "avg-hr", "max-hr", "lap-hr", "last-lap-hr", "percent-max-hr", "hr-zone" -> KustomColorGradeMapper.getHeartRateZoneColor(currentMockVal, null, fieldConfig.colorMode)
            "grade" -> KustomColorGradeMapper.getGradeColor(currentMockVal, fieldConfig.colorMode)
            else -> null
        }
    } else null

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
            // Top Row: Left Column (Title + Desc) & Right Column (Live Preview Box)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = field.childTitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                    )
                    Text(
                        text = field.description,
                        fontSize = 13.sp,
                        color = colors.textSecondary,
                    )
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .size(width = 132.dp, height = 68.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(BorderStroke(1.dp, colors.border), RoundedCornerShape(12.dp)),
                ) {
                    val widthPx = constraints.maxWidth.coerceAtLeast(1)
                    val heightPx = constraints.maxHeight.coerceAtLeast(1)

                    val previewViewConfig = remember(widthPx, heightPx) {
                        ViewConfig(
                            gridSize = Pair(60, 20),
                            viewSize = Pair(widthPx, heightPx),
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

                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Metric Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                }
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
                        viewModel.updateFieldSmoothing(field.typeId, option)
                    },
                )
            }

            if (field.supportsZones) {
                KarooSegmentedPill(
                    selectedZoneFormat = fieldConfig.zoneFormat,
                    fieldName = field.childTitle,
                    colors = colors,
                    onZoneFormatSelected = { format ->
                        viewModel.updateFieldColorMode(field.typeId, format.toColorMode())
                    },
                )
            }
        }
    }
}

/**
 * Dropdown menu selector for global font family choice.
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
 * High-precision interactive stepped slider component with sleek teal track and zero edge clipping.
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
    val currentOnStepSelected by rememberUpdatedState(onStepSelected)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
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
                .height(34.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            val thumbRadius = 10.dp
            val usableWidth = maxWidth - (thumbRadius * 2)
            val stepCount = steps.size
            val fraction = if (stepCount > 1) safeIndex.toFloat() / (stepCount - 1) else 0f

            val tealAccent = Color(0xFF00C4B4)
            val inactiveTrackColor = Color(0xFFCBD5E1)

            // Inactive Background Track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = thumbRadius)
                    .height(3.dp)
                    .background(inactiveTrackColor, RoundedCornerShape(50)),
            )

            // Active Teal Track
            if (fraction > 0f) {
                Box(
                    modifier = Modifier
                        .padding(start = thumbRadius)
                        .width(usableWidth * fraction)
                        .height(3.dp)
                        .background(tealAccent, RoundedCornerShape(50)),
                )
            }

            // Solid Teal Thumb
            Box(
                modifier = Modifier
                    .offset(x = usableWidth * fraction)
                    .size(20.dp)
                    .background(tealAccent, CircleShape),
            )

            // Interactive Touch & Drag Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(steps.size) {
                        detectTapGestures { offset ->
                            if (steps.size > 1 && size.width > 0) {
                                val idx = ((offset.x / size.width.toFloat()) * (steps.size - 1)).roundToInt().coerceIn(0, steps.lastIndex)
                                currentOnStepSelected(idx)
                            }
                        }
                    }
                    .pointerInput(steps.size) {
                        detectHorizontalDragGestures { change, _ ->
                            change.consume()
                            if (steps.size > 1 && size.width > 0) {
                                val idx = ((change.position.x / size.width.toFloat()) * (steps.size - 1)).roundToInt().coerceIn(0, steps.lastIndex)
                                currentOnStepSelected(idx)
                            }
                        }
                    },
            )
        }

        // Aligned Step Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val selectedTeal = Color(0xFF00897B)
            val unselectedSlate = Color(0xFF475569)

            steps.forEachIndexed { i, stepLabel ->
                val isSelected = i == safeIndex
                Box(
                    modifier = Modifier.clickable { currentOnStepSelected(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stepLabel,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) selectedTeal else unselectedSlate,
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
 * Modernized white pill container with gold active segment selector for zone color options.
 */
@Composable
fun KarooSegmentedPill(
    selectedZoneFormat: ZoneFormat,
    fieldName: String,
    colors: KustomColorScheme,
    onZoneFormatSelected: (ZoneFormat) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnOptionSelected by rememberUpdatedState(onZoneFormatSelected)

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
                .height(46.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White)
                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(50))
                .padding(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val options = listOf(
                    Triple(ZoneFormat.NONE, "None", 0.9f),
                    Triple(ZoneFormat.TEXT, "Text", 0.9f),
                    Triple(ZoneFormat.BACKGROUND, "Fill", 1.2f),
                )

                val activeGold = Color(0xFFF4C414)
                val activeDarkText = Color(0xFF1E2225)
                val unselectedSlate = Color(0xFF475569)

                options.forEach { (format, labelText, weight) ->
                    val isSelected = selectedZoneFormat == format

                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .background(if (isSelected) activeGold else Color.Transparent)
                            .clickable { currentOnOptionSelected(format) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = labelText,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) activeDarkText else unselectedSlate,
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
 * Helper finding list index closest in value to a target float.
 */
fun findNearestIndex(values: List<Float>, target: Float): Int {
    return values.minByOrNull { abs(it - target) }?.let { values.indexOf(it) } ?: 0
}
