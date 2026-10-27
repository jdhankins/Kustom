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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import com.github.dhaval2404.colorpicker.ColorPickerDialog
import com.github.dhaval2404.colorpicker.MaterialColorPickerDialog
import com.github.dhaval2404.colorpicker.model.ColorShape
import com.github.dhaval2404.colorpicker.model.ColorSwatch
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
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
import com.hank.kustom.data.model.GlobalColorConfig
import com.hank.kustom.data.model.GlobalTypographyConfig
import com.hank.kustom.data.model.KustomFieldCatalog
import com.hank.kustom.data.model.KustomFieldConfig
import com.hank.kustom.data.model.KustomFieldDefinition
import com.hank.kustom.data.model.KustomFont
import com.hank.kustom.data.model.KustomSettings
import com.hank.kustom.data.model.SmoothingOption
import com.hank.kustom.data.model.TypographyConfig
import com.hank.kustom.data.model.ZoneFormat
import com.hank.kustom.data.model.ZonePalette
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
    var expandedCategory by remember { mutableStateOf<FieldCategory?>(null) }
    var showHelpDialog by remember { mutableStateOf(false) }

    if (showHelpDialog) {
        KustomHelpDialog(
            colors = colors,
            onDismiss = { showHelpDialog = false },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        // 1. Sleek Compact App Title Header (Logo + "Kustom" + Right Help Circle)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(id = com.hank.kustom.R.drawable.ic_kustom_field),
                    contentDescription = "Kustom Logo",
                    modifier = Modifier.size(22.dp),
                    tint = colors.textPrimary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Kustom",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    letterSpacing = 0.5.sp,
                )
            }

            // Right-aligned Circular Help Icon Button
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(colors.trackBg)
                    .border(BorderStroke(1.dp, colors.border), CircleShape)
                    .clickable { showHelpDialog = true },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "?",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                )
            }
        }

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
    val globalTypo = settings.globalTypography

    val genericMockValues = remember { listOf("123", "145", "168", "182", "154") }

    var mockStepIndex by remember(selectedTab) { mutableIntStateOf(0) }
    LaunchedEffect(selectedTab) {
        while (true) {
            delay(1200L)
            mockStepIndex = (mockStepIndex + 1) % genericMockValues.size
        }
    }

    val sampleValueText = genericMockValues.getOrElse(mockStepIndex) { "123" }
    var previewIsDark by remember { mutableStateOf(colors.isDark) }

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

            // 2. Sticky Live Field Preview Box Centered with Circular Theme Toggle Icon (GLOBAL TAB ONLY)
            if (selectedTab == 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
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
                            label = "PREVIEW",
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
                            colorMode = ColorMode.NONE,
                            mappedColor = null,
                            isDarkTheme = previewIsDark,
                            globalColors = settings.globalColors,
                        )

                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Live Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Small Circular Theme Toggle Icon Button
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(colors.trackBg)
                            .border(BorderStroke(1.dp, colors.border), CircleShape)
                            .clickable { previewIsDark = !previewIsDark },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (previewIsDark) "🌙" else "☀️",
                            fontSize = 15.sp,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Detailed cyclist onboarding and quick-start guide popup dialog.
 */
@Composable
fun KustomHelpDialog(
    colors: KustomColorScheme,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.cardBg,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painter = painterResource(id = com.hank.kustom.R.drawable.ic_kustom_field),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = colors.accent,
                )
                Text(
                    text = "Kustom Cyclist Guide",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Kustom gives you total control over how data metrics appear on your Hammerhead Karoo screen.",
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                )

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "1. Global Typography",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                    )
                    Text(
                        text = "Under the Global tab, pick your preferred font family (Karoo, Wahoo, Garmin, Coros, etc.) and adjust Value/Label size and weight sliders for maximum readability on the bike.",
                        fontSize = 13.sp,
                        color = colors.textSecondary,
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "2. Field Background Colors",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                    )
                    Text(
                        text = "In Field Background, select custom colors for Light Mode and Dark Mode. Toggle the sun/moon icon next to the preview box to test both themes.",
                        fontSize = 13.sp,
                        color = colors.textSecondary,
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "3. Smoothing & Zone Colors",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                    )
                    Text(
                        text = "In the Fields tab, set smoothing (0s to 30s) to stabilize fluctuating power or cadence numbers, and choose Zone Color (Text or Fill) for colored zone highlights.",
                        fontSize = 13.sp,
                        color = colors.textSecondary,
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "4. Add Fields to Ride Profile",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                    )
                    Text(
                        text = "Select a profile (Profiles -> Choose Profile) and tap a page or add a new page to edit. Tap Add Field, scroll down to the Kustom category, and select your customized metrics for your ride screens!",
                        fontSize = 13.sp,
                        color = colors.textSecondary,
                    )
                }
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .clickable { onDismiss() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "GOT IT",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.accent,
                )
            }
        },
    )
}

/**
 * Detailed 7-Zone vs 5-Zone Power & HR training guide popup dialog.
 */
@Composable
fun ZoneHelpDialog(
    colors: KustomColorScheme,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.cardBg,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painter = painterResource(id = com.hank.kustom.R.drawable.ic_category_power),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = colors.accent,
                )
                Text(
                    text = "Power & HR Zone Guide",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Understanding 7-Zone vs 5-Zone models for Power & Heart Rate training on your bike computer:",
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                )

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "7-Zone Power Model (Default)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                    )
                    Text(
                        text = "Based on Dr. Andrew Coggan's training model used across Garmin, Wahoo, Karoo, TrainingPeaks, and Strava:\n• Z1 Active Recovery (<55% FTP)\n• Z2 Endurance (55-75% FTP)\n• Z3 Tempo (76-90% FTP)\n• Z4 Threshold (91-105% FTP)\n• Z5 VO2 Max (106-120% FTP)\n• Z6 Anaerobic (121-150% FTP)\n• Z7 Neuromuscular (>150% FTP)",
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                        lineHeight = 16.sp,
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "5-Zone Power Model",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                    )
                    Text(
                        text = "Used by simplified training systems (e.g. British Cycling / Joe Friel) to combine high-intensity VO2/Anaerobic efforts into a 5-zone structure:\n• Z1 Recovery (<60% FTP)\n• Z2 Endurance (60-75% FTP)\n• Z3 Tempo (76-90% FTP)\n• Z4 Threshold (91-105% FTP)\n• Z5 Max Effort (>105% FTP)",
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                        lineHeight = 16.sp,
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Heart Rate Zones (5-Zone Standard)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                    )
                    Text(
                        text = "Heart rate training is universally 5 zones across all devices (Z1 Easy, Z2 Fat Burn, Z3 Aerobic, Z4 Threshold, Z5 Redline).",
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                        lineHeight = 16.sp,
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Automatic Rider Profile Sync",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                    )
                    Text(
                        text = "⚡ Zone thresholds (FTP & Max HR) are automatically synchronized in real-time from your Hammerhead Karoo Rider Profile.",
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                        lineHeight = 16.sp,
                    )
                }
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .clickable { onDismiss() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "GOT IT",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.accent,
                )
            }
        },
    )
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
    var isTypographyExpanded by remember { mutableStateOf(false) }
    var isFieldBackgroundExpanded by remember { mutableStateOf(false) }
    var isZoneExpanded by remember { mutableStateOf(false) }
    var showLightColorDialog by remember { mutableStateOf(false) }
    var showDarkColorDialog by remember { mutableStateOf(false) }
    var showZoneHelpDialog by remember { mutableStateOf(false) }

    if (showZoneHelpDialog) {
        ZoneHelpDialog(
            colors = colors,
            onDismiss = { showZoneHelpDialog = false },
        )
    }

    if (showLightColorDialog) {
        FieldBackgroundColorDialog(
            title = "Light Mode Background",
            selectedHex = settings.globalColors.lightBgColorHex,
            presetHexes = GlobalColorConfig.LIGHT_PRESETS,
            colors = colors,
            onColorSelected = { hex ->
                viewModel.updateLightBgColor(hex)
            },
            onDismiss = { showLightColorDialog = false },
        )
    }

    if (showDarkColorDialog) {
        FieldBackgroundColorDialog(
            title = "Dark Mode Background",
            selectedHex = settings.globalColors.darkBgColorHex,
            presetHexes = GlobalColorConfig.DARK_PRESETS,
            colors = colors,
            onColorSelected = { hex ->
                viewModel.updateDarkBgColor(hex)
            },
            onDismiss = { showDarkColorDialog = false },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // --- ACCORDION 1: TYPOGRAPHY ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.cardBg),
                shape = RoundedCornerShape(10.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isTypographyExpanded = !isTypographyExpanded }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                painter = painterResource(id = com.hank.kustom.R.drawable.ic_category_typography),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = colors.textPrimary,
                            )
                            Text(
                                text = "TYPOGRAPHY",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                            )
                        }

                        Text(
                            text = if (isTypographyExpanded) "▼" else "◀",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
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

        // --- ACCORDION 2: FIELD BACKGROUND ---
        item {
            var isFieldBackgroundExpanded by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.cardBg),
                shape = RoundedCornerShape(10.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isFieldBackgroundExpanded = !isFieldBackgroundExpanded }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                painter = painterResource(id = com.hank.kustom.R.drawable.ic_category_palette),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = colors.textPrimary,
                            )
                            Text(
                                text = "FIELD BACKGROUND",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                            )
                        }

                        Text(
                            text = if (isFieldBackgroundExpanded) "▼" else "◀",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                        )
                    }

                    AnimatedVisibility(visible = isFieldBackgroundExpanded) {
                        val localContext = LocalContext.current
                        val globalColors = settings.globalColors
                        val selectedLightColor = runCatching {
                            Color(android.graphics.Color.parseColor(globalColors.lightBgColorHex))
                        }.getOrDefault(Color.White)
                        val selectedDarkColor = runCatching {
                            Color(android.graphics.Color.parseColor(globalColors.darkBgColorHex))
                        }.getOrDefault(Color.Black)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            // Row 1: Light Mode
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(colors.accent)
                                        .clickable {
                                            showLightColorDialog = true
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "Light Mode",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Selected Light Mode Color Indicator Swatch
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(selectedLightColor)
                                        .border(BorderStroke(2.dp, colors.border), CircleShape),
                                )
                            }

                            // Row 2: Dark Mode
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(colors.accent)
                                        .clickable {
                                            showDarkColorDialog = true
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "Dark Mode",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Selected Dark Mode Color Indicator Swatch
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(selectedDarkColor)
                                        .border(BorderStroke(2.dp, colors.border), CircleShape),
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- ACCORDION 3: ZONE ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.cardBg),
                shape = RoundedCornerShape(10.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isZoneExpanded = !isZoneExpanded }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                painter = painterResource(id = com.hank.kustom.R.drawable.ic_category_power),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = colors.textPrimary,
                            )
                            Text(
                                text = "ZONE",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                            )
                        }

                        Text(
                            text = if (isZoneExpanded) "▼" else "◀",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                        )
                    }

                    AnimatedVisibility(visible = isZoneExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "GLOBAL POWER ZONE MODEL",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.headerText,
                                )

                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(colors.trackBg)
                                        .border(BorderStroke(1.dp, colors.border), CircleShape)
                                        .clickable { showZoneHelpDialog = true },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "?",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary,
                                    )
                                }
                            }

                            val globalZoneCount = settings.globalColors.globalPowerZoneCount

                            // 5 Zones vs 7 Zones Selector Pill
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(colors.trackBg)
                                    .border(1.dp, colors.border, RoundedCornerShape(50))
                                    .padding(3.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    val activeBg = Color(0xFF00C4B4)

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(50))
                                            .background(if (globalZoneCount == 7) activeBg else Color.Transparent)
                                            .clickable { viewModel.updateGlobalPowerZoneCount(7) },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "7 Zones",
                                            fontSize = 13.sp,
                                            fontWeight = if (globalZoneCount == 7) FontWeight.Bold else FontWeight.Medium,
                                            color = if (globalZoneCount == 7) Color.White else colors.textSecondary,
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(50))
                                            .background(if (globalZoneCount == 5) activeBg else Color.Transparent)
                                            .clickable { viewModel.updateGlobalPowerZoneCount(5) },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "5 Zones",
                                            fontSize = 13.sp,
                                            fontWeight = if (globalZoneCount == 5) FontWeight.Bold else FontWeight.Medium,
                                            color = if (globalZoneCount == 5) Color.White else colors.textSecondary,
                                        )
                                    }
                                }
                            }

                            // PALETTE STYLE DROPDOWN
                            Text(
                                text = "PALETTE STYLE",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.headerText,
                            )

                            var paletteExpanded by remember { mutableStateOf(false) }
                            val currentPalette = settings.globalColors.globalZonePalette

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(50))
                                    .background(colors.trackBg)
                                    .clickable { paletteExpanded = true }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = currentPalette.displayName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary,
                                    )
                                    Text(
                                        text = if (paletteExpanded) "▲" else "▼",
                                        fontSize = 13.sp,
                                        color = colors.textSecondary,
                                    )
                                }

                                DropdownMenu(
                                    expanded = paletteExpanded,
                                    onDismissRequest = { paletteExpanded = false },
                                    modifier = Modifier.background(colors.cardBg),
                                ) {
                                    ZonePalette.entries.forEach { palette ->
                                        DropdownMenuItem(
                                            text = { Text(text = palette.displayName, fontSize = 14.sp, color = colors.textPrimary) },
                                            onClick = {
                                                viewModel.updateGlobalZonePalette(palette)
                                                paletteExpanded = false
                                            },
                                        )
                                    }
                                }
                            }

                            // Dynamic Live Swatch Preview (5 or 7 swatches matching selected Palette)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(colors.trackBg)
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                val paletteColors = if (globalZoneCount == 5) {
                                    currentPalette.get5ZoneColors().map { Color(it) }
                                } else {
                                    currentPalette.get7ZoneColors().map { Color(it) }
                                }
                                paletteColors.forEach { zoneCol ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(zoneCol),
                                    )
                                }
                            }
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
 * Custom Compose dialog displaying color presets in a 3x6 scrollable grid.
 */
@Composable
fun FieldBackgroundColorDialog(
    title: String,
    selectedHex: String,
    presetHexes: List<String>,
    colors: KustomColorScheme,
    onColorSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val rows: List<List<String>> = presetHexes.chunked(3)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.cardBg,
        title = {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                rows.forEach { rowHexes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        rowHexes.forEach { hexStr ->
                            val isSelected = selectedHex.equals(hexStr, ignoreCase = true)
                            val parsedColor = runCatching { Color(android.graphics.Color.parseColor(hexStr)) }.getOrDefault(Color.White)
                            val dotColor = if (hexStr.equals("#000000", ignoreCase = true) || hexStr.startsWith("#0") || hexStr.startsWith("#1") || hexStr.startsWith("#2")) Color.White else Color.Black

                            Box(
                                modifier = Modifier
                                    .size(62.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(parsedColor)
                                    .border(
                                        border = BorderStroke(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) colors.accent else Color(0xFFCBD5E1),
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                    )
                                    .clickable {
                                        onColorSelected(hexStr)
                                        onDismiss()
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(dotColor),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .clickable { onDismiss() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "CLOSE",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.accent,
                )
            }
        },
    )
}

/**
 * Dynamic configuration controls for Universal 3-in-1 Custom Combo field (typeId = "custom-combo").
 * Automatically adapts controls (Center Metric, Left Metric, Right Metric, Zone Mode, Smoothing)
 * based on the capabilities of the selected metrics.
 */
@Composable
fun UniversalFieldComboControls(
    fieldConfig: KustomFieldConfig,
    colors: KustomColorScheme,
    viewModel: KustomSettingsViewModel,
    typeId: String,
) {
    val centerOptions = remember {
        listOf(
            "power" to "Power",
            "hr" to "Heart Rate",
            "cadence" to "Cadence",
            "speed" to "Speed",
            "grade" to "Road Grade %",
            "vam" to "VAM Ascent",
            "np" to "Normalized Power",
            "avg-power" to "Avg Power",
            "avg-speed" to "Avg Speed",
            "avg-hr" to "Avg HR",
            "distance" to "Distance",
            "ride-time" to "Ride Time",
            "clock-time" to "Clock Time",
        )
    }

    val cornerOptions = remember {
        listOf(
            "np" to "NP",
            "max-power" to "Max Power",
            "avg-power" to "Avg Power",
            "power-to-weight" to "W/kg",
            "power-kj" to "kJ",
            "percent-ftp" to "% FTP",
            "avg-hr" to "Avg HR",
            "max-hr" to "Max HR",
            "hr-zone" to "HR Zone",
            "avg-cadence" to "Avg Cadence",
            "max-cadence" to "Max Cadence",
            "avg-torque" to "Avg Torque",
            "max-torque" to "Max Torque",
            "avg-speed" to "Avg Speed",
            "max-speed" to "Max Speed",
            "distance" to "Distance",
            "ascent" to "Ascent",
            "grade" to "Grade %",
            "none" to "None",
        )
    }

    val selectedCenterId = fieldConfig.comboCenterMetric.ifEmpty { "power" }
    val centerDef = remember(selectedCenterId) { KustomFieldCatalog.findByTypeId(selectedCenterId) ?: KustomFieldCatalog.POWER }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // --- SECTION 1: CENTER METRIC (PRIMARY) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(colors.trackBg)
                .border(BorderStroke(1.dp, colors.border), RoundedCornerShape(10.dp))
                .padding(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                var centerExpanded by remember { mutableStateOf(false) }
                val currentCenterLabel = centerOptions.firstOrNull { it.first == selectedCenterId }?.second ?: centerDef.childTitle

                Text(
                    text = "CENTER METRIC (PRIMARY)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.headerText,
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(colors.cardBg)
                        .clickable { centerExpanded = true }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = currentCenterLabel,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                        )
                        Text(
                            text = if (centerExpanded) "▲" else "▼",
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                        )
                    }

                    DropdownMenu(
                        expanded = centerExpanded,
                        onDismissRequest = { centerExpanded = false },
                        modifier = Modifier.background(colors.cardBg),
                    ) {
                        centerOptions.forEach { (id, name) ->
                            DropdownMenuItem(
                                text = { Text(text = name, fontSize = 13.sp, color = colors.textPrimary) },
                                onClick = {
                                    viewModel.updateComboCenterMetric(typeId, id)
                                    centerExpanded = false
                                },
                            )
                        }
                    }
                }

                // Dynamic Zone Format Controls for Center Metric
                if (centerDef.supportsZones) {
                    val supportedFormats = if (selectedCenterId == "power" || selectedCenterId == "power-combo" || selectedCenterId == "custom-combo") {
                        listOf(ZoneFormat.NONE, ZoneFormat.TEXT, ZoneFormat.BACKGROUND, ZoneFormat.BAR)
                    } else {
                        listOf(ZoneFormat.NONE, ZoneFormat.TEXT, ZoneFormat.BACKGROUND)
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "ZONE FORMAT",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textSecondary,
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .clip(RoundedCornerShape(50))
                                .background(colors.cardBg)
                                .border(1.dp, colors.border, RoundedCornerShape(50))
                                .padding(3.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                supportedFormats.forEach { format ->
                                    val isSelected = fieldConfig.zoneFormat == format
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(50))
                                            .background(if (isSelected) Color(0xFF00C4B4) else Color.Transparent)
                                            .clickable { viewModel.updateFieldZoneFormat(typeId, format) },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = format.displayLabel,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else colors.textSecondary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Dynamic Smoothing Controls for Center Metric
                if (centerDef.supportsSmoothing) {
                    val smoothingSteps = listOf("0s", "3s", "5s", "10s", "30s")
                    val currentSmoothIdx = SmoothingOption.entries.indexOf(fieldConfig.smoothingOption).coerceAtLeast(0)
                    KarooSteppedSlider(
                        label = "Smoothing",
                        steps = smoothingSteps,
                        selectedIndex = currentSmoothIdx,
                        colors = colors,
                        onStepSelected = { idx ->
                            val option = SmoothingOption.entries.getOrElse(idx) { SmoothingOption.THREE_SEC }
                            viewModel.updateFieldSmoothing(typeId, option)
                        },
                    )
                }
            }
        }

        // --- SECTION 2: LEFT METRIC ---
        val selectedLeftId = fieldConfig.comboLeftMetric.ifEmpty { "np" }
        val leftDef = remember(selectedLeftId) { KustomFieldCatalog.findByTypeId(selectedLeftId) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(colors.trackBg)
                .border(BorderStroke(1.dp, colors.border), RoundedCornerShape(10.dp))
                .padding(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                var leftExpanded by remember { mutableStateOf(false) }
                val currentLeftLabel = cornerOptions.firstOrNull { it.first == selectedLeftId }?.second ?: (leftDef?.childTitle ?: "NP")

                Text(
                    text = "LEFT METRIC",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.headerText,
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(colors.cardBg)
                        .clickable { leftExpanded = true }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = currentLeftLabel,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                        )
                        Text(
                            text = if (leftExpanded) "▲" else "▼",
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                        )
                    }

                    DropdownMenu(
                        expanded = leftExpanded,
                        onDismissRequest = { leftExpanded = false },
                        modifier = Modifier.background(colors.cardBg),
                    ) {
                        cornerOptions.forEach { (id, name) ->
                            DropdownMenuItem(
                                text = { Text(text = name, fontSize = 13.sp, color = colors.textPrimary) },
                                onClick = {
                                    viewModel.updateComboLeftMetric(typeId, id)
                                    leftExpanded = false
                                },
                            )
                        }
                    }
                }

                // Dynamic Zone Format Controls if Left Metric supports zones
                if (leftDef?.supportsZones == true) {
                    val supportedFormats = listOf(ZoneFormat.NONE, ZoneFormat.TEXT, ZoneFormat.BACKGROUND)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "LEFT ZONE FORMAT",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textSecondary,
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .clip(RoundedCornerShape(50))
                                .background(colors.cardBg)
                                .border(1.dp, colors.border, RoundedCornerShape(50))
                                .padding(3.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                supportedFormats.forEach { format ->
                                    val isSelected = fieldConfig.comboLeftZoneFormat == format
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(50))
                                            .background(if (isSelected) Color(0xFF00C4B4) else Color.Transparent)
                                            .clickable { viewModel.updateComboLeftZoneFormat(typeId, format) },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = format.displayLabel,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else colors.textSecondary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Dynamic Left Smoothing Controls if Left Metric supports smoothing
                if (leftDef?.supportsSmoothing == true) {
                    val smoothingSteps = listOf("0s", "3s", "5s", "10s", "30s")
                    val currentSmoothIdx = SmoothingOption.entries.indexOf(fieldConfig.comboLeftSmoothing).coerceAtLeast(0)
                    KarooSteppedSlider(
                        label = "Left Smoothing",
                        steps = smoothingSteps,
                        selectedIndex = currentSmoothIdx,
                        colors = colors,
                        onStepSelected = { idx ->
                            val option = SmoothingOption.entries.getOrElse(idx) { SmoothingOption.NONE }
                            viewModel.updateComboLeftSmoothing(typeId, option)
                        },
                    )
                }
            }
        }

        // --- SECTION 3: RIGHT METRIC ---
        val selectedRightId = fieldConfig.comboRightMetric.ifEmpty { "max-power" }
        val rightDef = remember(selectedRightId) { KustomFieldCatalog.findByTypeId(selectedRightId) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(colors.trackBg)
                .border(BorderStroke(1.dp, colors.border), RoundedCornerShape(10.dp))
                .padding(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                var rightExpanded by remember { mutableStateOf(false) }
                val currentRightLabel = cornerOptions.firstOrNull { it.first == selectedRightId }?.second ?: (rightDef?.childTitle ?: "Max Power")

                Text(
                    text = "RIGHT METRIC",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.headerText,
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(colors.cardBg)
                        .clickable { rightExpanded = true }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = currentRightLabel,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                        )
                        Text(
                            text = if (rightExpanded) "▲" else "▼",
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                        )
                    }

                    DropdownMenu(
                        expanded = rightExpanded,
                        onDismissRequest = { rightExpanded = false },
                        modifier = Modifier.background(colors.cardBg),
                    ) {
                        cornerOptions.forEach { (id, name) ->
                            DropdownMenuItem(
                                text = { Text(text = name, fontSize = 13.sp, color = colors.textPrimary) },
                                onClick = {
                                    viewModel.updateComboRightMetric(typeId, id)
                                    rightExpanded = false
                                },
                            )
                        }
                    }
                }

                // Dynamic Zone Format Controls if Right Metric supports zones
                if (rightDef?.supportsZones == true) {
                    val supportedFormats = listOf(ZoneFormat.NONE, ZoneFormat.TEXT, ZoneFormat.BACKGROUND)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "RIGHT ZONE FORMAT",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textSecondary,
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .clip(RoundedCornerShape(50))
                                .background(colors.cardBg)
                                .border(1.dp, colors.border, RoundedCornerShape(50))
                                .padding(3.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                supportedFormats.forEach { format ->
                                    val isSelected = fieldConfig.comboRightZoneFormat == format
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(50))
                                            .background(if (isSelected) Color(0xFF00C4B4) else Color.Transparent)
                                            .clickable { viewModel.updateComboRightZoneFormat(typeId, format) },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = format.displayLabel,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else colors.textSecondary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Dynamic Right Smoothing Controls if Right Metric supports smoothing
                if (rightDef?.supportsSmoothing == true) {
                    val smoothingSteps = listOf("0s", "3s", "5s", "10s", "30s")
                    val currentSmoothIdx = SmoothingOption.entries.indexOf(fieldConfig.comboRightSmoothing).coerceAtLeast(0)
                    KarooSteppedSlider(
                        label = "Right Smoothing",
                        steps = smoothingSteps,
                        selectedIndex = currentSmoothIdx,
                        colors = colors,
                        onStepSelected = { idx ->
                            val option = SmoothingOption.entries.getOrElse(idx) { SmoothingOption.NONE }
                            viewModel.updateComboRightSmoothing(typeId, option)
                        },
                    )
                }
            }
        }
    }
}

/**
 * Configuration controls for Special Field combo blocks (corner metric dropdowns and 5/7 zone count selector).
 */
@Composable
fun SpecialFieldComboControls(
    fieldConfig: KustomFieldConfig,
    colors: KustomColorScheme,
    viewModel: KustomSettingsViewModel,
    typeId: String,
) {
    val options = remember {
        listOf(
            "avg-power" to "Avg Power",
            "np" to "NP",
            "max-power" to "Max Power",
            "power-to-weight" to "W/kg",
            "power-kj" to "kJ",
            "percent-ftp" to "% FTP",
            "none" to "None",
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            var leftExpanded by remember { mutableStateOf(false) }
            val currentLeftLabel = options.firstOrNull { it.first == fieldConfig.comboLeftMetric }?.second ?: "NP"

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "LEFT METRIC",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.headerText,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(colors.trackBg)
                        .clickable { leftExpanded = true }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = currentLeftLabel,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                        )
                        Text(
                            text = if (leftExpanded) "▲" else "▼",
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                        )
                    }

                    DropdownMenu(
                        expanded = leftExpanded,
                        onDismissRequest = { leftExpanded = false },
                        modifier = Modifier.background(colors.cardBg),
                    ) {
                        options.forEach { (id, name) ->
                            DropdownMenuItem(
                                text = { Text(text = name, fontSize = 13.sp, color = colors.textPrimary) },
                                onClick = {
                                    viewModel.updateComboLeftMetric(typeId, id)
                                    leftExpanded = false
                                },
                            )
                        }
                    }
                }
            }

            var rightExpanded by remember { mutableStateOf(false) }
            val currentRightLabel = options.firstOrNull { it.first == fieldConfig.comboRightMetric }?.second ?: "Max Power"

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "RIGHT METRIC",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.headerText,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(colors.trackBg)
                        .clickable { rightExpanded = true }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = currentRightLabel,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                        )
                        Text(
                            text = if (rightExpanded) "▲" else "▼",
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                        )
                    }

                    DropdownMenu(
                        expanded = rightExpanded,
                        onDismissRequest = { rightExpanded = false },
                        modifier = Modifier.background(colors.cardBg),
                    ) {
                        options.forEach { (id, name) ->
                            DropdownMenuItem(
                                text = { Text(text = name, fontSize = 13.sp, color = colors.textPrimary) },
                                onClick = {
                                    viewModel.updateComboRightMetric(typeId, id)
                                    rightExpanded = false
                                },
                            )
                        }
                    }
                }
            }
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
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        configurableGrouped.forEach { (category, fields) ->
            item {
                val isExpanded = expandedCategory == category

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = colors.cardBg),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCategoryToggled(category) }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(
                                    painter = painterResource(id = category.iconResId),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = colors.textPrimary,
                                )
                                Text(
                                    text = "${category.displayName.uppercase()} FIELDS",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary,
                                )
                            }

                            Text(
                                text = if (isExpanded) "▼" else "◀",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
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
    val settings by viewModel.settingsState.collectAsState()

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

    val activePalette = settings.globalColors.globalZonePalette
    val mappedColor = if (field.supportsZones) {
        when (field.typeId) {
            "power", "power-combo", "np", "avg-power", "max-power", "power-20m", "power-1hr", "lap-power", "lap-np", "last-lap-power", "last-lap-np", "percent-ftp", "power-balance", "power-zone", "power-to-weight", "workout-target-power", "workout-primary-target" -> KustomColorGradeMapper.getPowerZoneColor(currentMockVal, null, fieldConfig.colorMode, palette = activePalette)
            "hr", "avg-hr", "max-hr", "lap-hr", "last-lap-hr", "percent-max-hr", "hr-zone" -> KustomColorGradeMapper.getHeartRateZoneColor(currentMockVal, null, fieldConfig.colorMode, palette = activePalette)
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
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // 1. Bold Detailed Title Header Above Preview Box
            Text(
                text = field.cardHeaderTitle,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
            )

            val isFullWidthPreview = field.typeId == "power-combo" || field.category == FieldCategory.SPECIAL

            // 2. Centered Live Preview Box
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                BoxWithConstraints(
                    modifier = if (isFullWidthPreview) {
                        Modifier
                            .fillMaxWidth()
                            .height(84.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(BorderStroke(1.dp, colors.border), RoundedCornerShape(12.dp))
                    } else {
                        Modifier
                            .size(width = 150.dp, height = 70.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(BorderStroke(1.dp, colors.border), RoundedCornerShape(12.dp))
                    },
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

                    val bitmap = if (field.typeId == "power-combo" || field.typeId == "custom-combo") {
                        val centerDef = if (field.typeId == "custom-combo") {
                            KustomFieldCatalog.findByTypeId(fieldConfig.comboCenterMetric) ?: KustomFieldCatalog.POWER
                        } else {
                            KustomFieldCatalog.POWER
                        }

                        val centerVal = centerDef.mockValues.getOrElse(mockStepIndex) { centerDef.mockValues.first() }
                        val primaryValText = centerDef.formatNumericValueOnly(centerVal)
                        val dynamicCenterLabel = centerDef.resolveDynamicLabel(fieldConfig.smoothingOption)

                        val leftDef = KustomFieldCatalog.findByTypeId(fieldConfig.comboLeftMetric) ?: KustomFieldCatalog.NP
                        val rightDef = KustomFieldCatalog.findByTypeId(fieldConfig.comboRightMetric) ?: KustomFieldCatalog.MAX_POWER

                        val leftMock = leftDef.mockValues.firstOrNull() ?: (centerVal * 0.95)
                        val rightMock = rightDef.mockValues.firstOrNull() ?: (centerVal * 1.05)

                        val comboMappedColor = if (centerDef.supportsZones) {
                            when (centerDef.typeId) {
                                "power", "power-combo", "custom-combo", "np", "avg-power", "max-power", "percent-ftp" -> KustomColorGradeMapper.getPowerZoneColor(centerVal, null, fieldConfig.colorMode, palette = activePalette)
                                "hr", "avg-hr", "max-hr", "percent-max-hr" -> KustomColorGradeMapper.getHeartRateZoneColor(centerVal, null, fieldConfig.colorMode, palette = activePalette)
                                "grade" -> KustomColorGradeMapper.getGradeColor(centerVal, fieldConfig.colorMode)
                                else -> null
                            }
                        } else null

                        val leftZoneFormat = fieldConfig.comboLeftZoneFormat
                        val leftMappedColor = if (leftDef.supportsZones && leftZoneFormat != ZoneFormat.NONE) {
                            when (leftDef.typeId) {
                                "power", "power-combo", "custom-combo", "np", "avg-power", "max-power", "percent-ftp" -> KustomColorGradeMapper.getPowerZoneColor(leftMock, null, leftZoneFormat.toColorMode(), palette = activePalette)
                                "hr", "avg-hr", "max-hr", "percent-max-hr" -> KustomColorGradeMapper.getHeartRateZoneColor(leftMock, null, leftZoneFormat.toColorMode(), palette = activePalette)
                                "grade" -> KustomColorGradeMapper.getGradeColor(leftMock, leftZoneFormat.toColorMode())
                                else -> null
                            }
                        } else null

                        val rightZoneFormat = fieldConfig.comboRightZoneFormat
                        val rightMappedColor = if (rightDef.supportsZones && rightZoneFormat != ZoneFormat.NONE) {
                            when (rightDef.typeId) {
                                "power", "power-combo", "custom-combo", "np", "avg-power", "max-power", "percent-ftp" -> KustomColorGradeMapper.getPowerZoneColor(rightMock, null, rightZoneFormat.toColorMode(), palette = activePalette)
                                "hr", "avg-hr", "max-hr", "percent-max-hr" -> KustomColorGradeMapper.getHeartRateZoneColor(rightMock, null, rightZoneFormat.toColorMode(), palette = activePalette)
                                "grade" -> KustomColorGradeMapper.getGradeColor(rightMock, rightZoneFormat.toColorMode())
                                else -> null
                            }
                        } else null

                        KustomFieldRenderer.renderComboBlock(
                            context = context,
                            config = previewViewConfig,
                            label = dynamicCenterLabel,
                            primaryValueText = primaryValText,
                            smoothingLabel = "",
                            leftLabel = if (fieldConfig.comboLeftMetric == "none") "" else leftDef.childTitle,
                            leftValueText = if (fieldConfig.comboLeftMetric == "none") "" else leftDef.formatNumericValueOnly(leftMock),
                            rightLabel = if (fieldConfig.comboRightMetric == "none") "" else rightDef.childTitle,
                            rightValueText = if (fieldConfig.comboRightMetric == "none") "" else rightDef.formatNumericValueOnly(rightMock),
                            powerValueWatts = centerVal,
                            ftpWatts = 250.0,
                            zoneCount = settings.globalColors.globalPowerZoneCount,
                            zoneFormat = if (centerDef.supportsZones) fieldConfig.zoneFormat else ZoneFormat.NONE,
                            mappedColor = comboMappedColor,
                            leftZoneFormat = leftZoneFormat,
                            leftMappedColor = leftMappedColor,
                            rightZoneFormat = rightZoneFormat,
                            rightMappedColor = rightMappedColor,
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
                            isDarkTheme = colors.isDark,
                            globalColors = settings.globalColors,
                        )
                    } else {
                        KustomFieldRenderer.render(
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
                            globalColors = settings.globalColors,
                        )
                    }

                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Metric Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Recommendation Notice Disclaimer Box directly under the field preview
            if (isFullWidthPreview) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.trackBg)
                        .border(BorderStroke(1.dp, colors.border), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                ) {
                    Text(
                        text = "💡 Best Results: This special field is designed to fill a full-width row on your Karoo ride screen.",
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                        lineHeight = 16.sp,
                    )
                }
            }

            // Controls Below
            if (field.typeId == "custom-combo") {
                UniversalFieldComboControls(
                    fieldConfig = fieldConfig,
                    colors = colors,
                    viewModel = viewModel,
                    typeId = field.typeId,
                )
            } else if (field.category == FieldCategory.SPECIAL) {
                SpecialFieldComboControls(
                    fieldConfig = fieldConfig,
                    colors = colors,
                    viewModel = viewModel,
                    typeId = field.typeId,
                )
            }

            // Controls Below (For standard fields, not for 3-in-1 custom-combo which has per-slot smoothing)
            if (field.supportsSmoothing && field.typeId != "custom-combo") {
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

            // Zone Color Controls (For standard fields and power-combo, NOT for custom-combo which has per-slot zone formatting)
            if (field.supportsZones && field.typeId != "custom-combo") {
                KarooSegmentedPill(
                    selectedZoneFormat = fieldConfig.zoneFormat,
                    colors = colors,
                    onZoneFormatSelected = { format ->
                        viewModel.updateFieldZoneFormat(field.typeId, format)
                    },
                    isComboBlock = field.typeId == "power-combo",
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
    colors: KustomColorScheme,
    onZoneFormatSelected: (ZoneFormat) -> Unit,
    modifier: Modifier = Modifier,
    isComboBlock: Boolean = false,
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(50))
                .background(colors.trackBg)
                .border(1.dp, colors.border, RoundedCornerShape(50))
                .padding(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val options = if (isComboBlock) {
                    listOf(
                        Triple(ZoneFormat.NONE, "None", 1.0f),
                        Triple(ZoneFormat.TEXT, "Text", 1.0f),
                        Triple(ZoneFormat.BACKGROUND, "Fill", 1.0f),
                        Triple(ZoneFormat.BAR, "Bar", 1.0f),
                    )
                } else {
                    listOf(
                        Triple(ZoneFormat.NONE, "None", 0.9f),
                        Triple(ZoneFormat.TEXT, "Text", 0.9f),
                        Triple(ZoneFormat.BACKGROUND, "Fill", 1.2f),
                    )
                }

                val activeBg = Color(0xFF00C4B4) // Same teal accent as the slider
                val activeText = Color.White
                val unselectedSlate = colors.textSecondary

                options.forEach { (format, labelText, weight) ->
                    val isSelected = selectedZoneFormat == format

                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .background(if (isSelected) activeBg else Color.Transparent)
                            .clickable { currentOnOptionSelected(format) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = labelText,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) activeText else unselectedSlate,
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
