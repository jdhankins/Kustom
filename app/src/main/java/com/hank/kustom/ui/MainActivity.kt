package com.hank.kustom.ui

import android.content.Context
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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

// Karoo OS Native Color Palette Constants
val KarooBg = Color(0xFFDCE1E3)
val KarooHeaderCardBg = Color(0xFFF4F6F7)
val KarooTextPrimary = Color(0xFF1E2225)
val KarooTextSecondary = Color(0xFF4A5258)
val KarooSlateAccent = Color(0xFF91A6B0)
val KarooTrackBg = Color(0xFFEEF1F2)

class MainActivity : ComponentActivity() {

    private val viewModel: KustomSettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        KustomLogger.installCrashHandler(this)
        super.onCreate(savedInstanceState)
        KustomLogger.d("MainActivity created with Karoo Native UI theme")

        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = KarooBg.toArgb()
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = true

        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = KarooBg,
            ) {
                KustomKarooNativeScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun KustomKarooNativeScreen(viewModel: KustomSettingsViewModel) {
    val settings by viewModel.settingsState.collectAsState()
    var selectedTopNavTab by remember { mutableIntStateOf(0) } // 0 = Global, 1 = Fields

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KarooBg),
    ) {
        // Top 2-Segment Navigation Bar (Strictly Global and Fields Only)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(50))
                .background(KarooTrackBg)
                .padding(3.dp),
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(if (selectedTopNavTab == 0) KarooSlateAccent else Color.Transparent)
                        .clickable { selectedTopNavTab = 0 },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Global",
                        fontSize = 14.sp,
                        fontWeight = if (selectedTopNavTab == 0) FontWeight.Bold else FontWeight.Medium,
                        color = KarooTextPrimary,
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(if (selectedTopNavTab == 1) KarooSlateAccent else Color.Transparent)
                        .clickable { selectedTopNavTab = 1 },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Fields",
                        fontSize = 14.sp,
                        fontWeight = if (selectedTopNavTab == 1) FontWeight.Bold else FontWeight.Medium,
                        color = KarooTextPrimary,
                    )
                }
            }
        }

        // Screen Body
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            if (selectedTopNavTab == 0) {
                GlobalFontsTab(
                    settings = settings,
                    onUpdateGlobalTypography = { viewModel.updateGlobalTypography(it) },
                )
            } else {
                FieldsTab(
                    settings = settings,
                    onUpdateFieldConfig = { typeId, config ->
                        viewModel.updateFieldConfig(typeId, config)
                    },
                )
            }
        }
    }
}

@Composable
fun GlobalFontsTab(
    settings: KustomSettings,
    onUpdateGlobalTypography: (GlobalTypographyConfig) -> Unit,
) {
    val context = LocalContext.current
    val global = settings.globalTypography

    // Live Mock Ticker every 1200ms for Global Preview
    val powerField = KustomFieldCatalog.POWER
    var mockStepIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1200L)
            mockStepIndex = (mockStepIndex + 1) % powerField.mockValues.size
        }
    }

    val currentMockVal = powerField.mockValues.getOrElse(mockStepIndex) { powerField.mockValues.first() }
    val powerConfig = settings.getFieldConfig(powerField.typeId)
    val sampleValueText = powerField.formatNumericValueOnly(currentMockVal)
    val dynamicLabel = powerField.resolveDynamicLabel(powerConfig.smoothingOption)
    val mappedColor = KustomColorGradeMapper.getPowerZoneColor(currentMockVal, null, powerConfig.colorMode)

    val previewViewConfig = remember {
        ViewConfig(
            gridSize = Pair(60, 20),
            viewSize = Pair(264, 136),
            textSize = 38,
            alignment = ViewConfig.Alignment.CENTER,
            preview = true,
        )
    }

    val bitmap = KustomFieldRenderer.render(
        context = context,
        config = previewViewConfig,
        label = dynamicLabel,
        valueText = sampleValueText,
        labelTypography = TypographyConfig(
            font = global.fontFamily,
            fontWeight = global.labelWeight,
            fontSizeSp = global.labelSizeSp.toFloat(),
        ),
        valueTypography = TypographyConfig(
            font = global.fontFamily,
            fontWeight = global.valueWeight,
            fontSizeSp = global.valueSizeSp.toFloat(),
        ),
        colorMode = powerConfig.colorMode,
        mappedColor = mappedColor,
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Top Global Live Preview Card (#F4F6F7)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = KarooHeaderCardBg),
                shape = RoundedCornerShape(14.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "Power",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = KarooTextPrimary,
                            maxLines = 1,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Current power output",
                            fontSize = 13.sp,
                            color = KarooTextSecondary,
                            maxLines = 2,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(width = 132.dp, height = 68.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(BorderStroke(1.dp, Color(0xFF5A6268)), RoundedCornerShape(12.dp)),
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

        // FONT FAMILY DROPDOWN
        item {
            KarooFontDropdown(
                selectedFont = global.fontFamily,
                onFontSelected = { font ->
                    onUpdateGlobalTypography(global.copy(fontFamily = font))
                },
            )
        }

        // VALUE FONT SIZE (36sp .. 72sp)
        item {
            val valueSizes = listOf(36f, 42f, 48f, 56f, 64f, 72f)
            val valueSizeSteps = listOf("36sp", "42sp", "48sp", "56sp", "64sp", "72sp")
            val currentIdx = findNearestIndex(valueSizes, global.valueSizeSp.toFloat())
            KarooSteppedSlider(
                label = "Value Font Size",
                steps = valueSizeSteps,
                selectedIndex = currentIdx,
                onStepSelected = { idx ->
                    val newSize = valueSizes.getOrElse(idx) { 48f }.toInt()
                    onUpdateGlobalTypography(global.copy(valueSizeSp = newSize))
                },
            )
        }

        // VALUE FONT WEIGHT
        item {
            val weights = listOf(300, 400, 500, 700, 900)
            val weightSteps = listOf("300", "400", "500", "700", "900")
            val currentIdx = findNearestIndex(weights.map { it.toFloat() }, global.valueWeight.toFloat())
            KarooSteppedSlider(
                label = "Value Font Weight",
                steps = weightSteps,
                selectedIndex = currentIdx,
                onStepSelected = { idx ->
                    val newWeight = weights.getOrElse(idx) { 700 }
                    onUpdateGlobalTypography(global.copy(valueWeight = newWeight))
                },
            )
        }

        // LABEL FONT SIZE
        item {
            val labelSizes = listOf(10f, 12f, 14f, 16f, 18f)
            val labelSizeSteps = listOf("10sp", "12sp", "14sp", "16sp", "18sp")
            val currentIdx = findNearestIndex(labelSizes, global.labelSizeSp.toFloat())
            KarooSteppedSlider(
                label = "Label Font Size",
                steps = labelSizeSteps,
                selectedIndex = currentIdx,
                onStepSelected = { idx ->
                    val newSize = labelSizes.getOrElse(idx) { 14f }.toInt()
                    onUpdateGlobalTypography(global.copy(labelSizeSp = newSize))
                },
            )
        }

        // LABEL FONT WEIGHT
        item {
            val weights = listOf(300, 400, 500, 700, 900)
            val weightSteps = listOf("300", "400", "500", "700", "900")
            val currentIdx = findNearestIndex(weights.map { it.toFloat() }, global.labelWeight.toFloat())
            KarooSteppedSlider(
                label = "Label Font Weight",
                steps = weightSteps,
                selectedIndex = currentIdx,
                onStepSelected = { idx ->
                    val newWeight = weights.getOrElse(idx) { 700 }
                    onUpdateGlobalTypography(global.copy(labelWeight = newWeight))
                },
            )
        }
    }
}

@Composable
fun FieldsTab(
    settings: KustomSettings,
    onUpdateFieldConfig: (String, KustomFieldConfig) -> Unit,
) {
    val configurableGrouped = remember {
        KustomFieldCatalog.configurableFields.groupBy { it.category }
    }

    var expandedCategory by remember { mutableStateOf<FieldCategory?>(FieldCategory.POWER) }

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
                    colors = CardDefaults.cardColors(containerColor = KarooHeaderCardBg),
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
                                .clickable {
                                    expandedCategory = if (isExpanded) null else category
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "${category.displayName.uppercase()} FIELDS",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = KarooTextPrimary,
                            )
                            Text(
                                text = if (isExpanded) "▲" else "▼",
                                fontSize = 14.sp,
                                color = KarooTextSecondary,
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

@Composable
fun FieldCardItem(
    field: KustomFieldDefinition,
    fieldConfig: KustomFieldConfig,
    globalTypography: GlobalTypographyConfig,
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
            "power", "np", "avg-power", "max-power", "lap-power", "last-lap-power" -> KustomColorGradeMapper.getPowerZoneColor(currentMockVal, null, fieldConfig.colorMode)
            "hr", "avg-hr", "max-hr", "lap-hr" -> KustomColorGradeMapper.getHeartRateZoneColor(currentMockVal, null, fieldConfig.colorMode)
            "grade" -> KustomColorGradeMapper.getGradeColor(currentMockVal, fieldConfig.colorMode)
            else -> null
        }
    } else null

    val previewViewConfig = remember {
        ViewConfig(
            gridSize = Pair(60, 20),
            viewSize = Pair(264, 136),
            textSize = 38,
            alignment = ViewConfig.Alignment.CENTER,
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
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = KarooHeaderCardBg),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFCFD8DC)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Top Row: Title + Description on Left, 132.dp x 68.dp Preview Box on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = field.childTitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = KarooTextPrimary,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = field.description,
                        fontSize = 13.sp,
                        color = KarooTextSecondary,
                    )
                }

                Box(
                    modifier = Modifier
                        .size(width = 132.dp, height = 68.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(BorderStroke(1.dp, Color(0xFF5A6268)), RoundedCornerShape(12.dp)),
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Metric Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds,
                    )
                }
            }

            // Controls Below Top Row
            if (field.supportsSmoothing) {
                val smoothingSteps = listOf("0s", "3s", "5s", "10s", "30s")
                val currentSmoothIdx = SmoothingOption.entries.indexOf(fieldConfig.smoothingOption).coerceAtLeast(0)
                KarooSteppedSlider(
                    label = "Smoothing",
                    steps = smoothingSteps,
                    selectedIndex = currentSmoothIdx,
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
                    onZoneFormatSelected = { format ->
                        onUpdateFieldConfig(field.typeId, fieldConfig.copy(zoneFormat = format))
                    },
                )
            }
        }
    }
}

@Composable
fun KarooFontDropdown(
    selectedFont: KustomFont,
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
            fontWeight = FontWeight.Medium,
            color = KarooTextSecondary,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(KarooTrackBg)
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
                    color = KarooTextPrimary,
                )
                Text(
                    text = if (expanded) "▲" else "▼",
                    fontSize = 14.sp,
                    color = KarooTextSecondary,
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .background(KarooHeaderCardBg),
            ) {
                KustomFont.entries.forEach { font ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = font.displayName,
                                fontSize = 14.sp,
                                fontWeight = if (font == selectedFont) FontWeight.Bold else FontWeight.Normal,
                                color = KarooTextPrimary,
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

@Composable
fun KarooSteppedSlider(
    label: String,
    steps: List<String>,
    selectedIndex: Int,
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
            fontWeight = FontWeight.Medium,
            color = KarooTextSecondary,
        )

        val safeIndex = selectedIndex.coerceIn(0, (steps.size - 1).coerceAtLeast(0))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            val totalWidthPx = constraints.maxWidth.toFloat()

            fun updateFromPx(xPx: Float) {
                if (steps.size <= 1 || totalWidthPx <= 0f) return
                val targetFraction = (xPx / totalWidthPx).coerceIn(0f, 1f)
                val nearestIdx = (targetFraction * (steps.size - 1)).roundToInt().coerceIn(0, steps.lastIndex)
                if (nearestIdx != safeIndex) {
                    onStepSelected(nearestIdx)
                }
            }

            // Visible Pill Track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFCFD8DC)),
            )

            // Step Target Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(steps.size) {
                        detectTapGestures { offset ->
                            updateFromPx(offset.x)
                        }
                    }
                    .pointerInput(steps.size) {
                        detectHorizontalDragGestures { change, _ ->
                            change.consume()
                            updateFromPx(change.position.x)
                        }
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                steps.forEachIndexed { i, _ ->
                    val isSelected = i == safeIndex
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) KarooSlateAccent else Color.Transparent)
                            .clickable { onStepSelected(i) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) {
                            Text(
                                text = "⚡",
                                fontSize = 18.sp,
                                color = Color.White,
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF6E828C)),
                            )
                        }
                    }
                }
            }
        }

        // Aligned Step Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            steps.forEachIndexed { i, stepLabel ->
                Text(
                    text = stepLabel,
                    fontSize = 13.sp,
                    fontWeight = if (i == safeIndex) FontWeight.Bold else FontWeight.Normal,
                    color = if (i == safeIndex) KarooTextPrimary else KarooTextSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier
                        .width(42.dp)
                        .clickable { onStepSelected(i) },
                )
            }
        }
    }
}

@Composable
fun KarooSegmentedPill(
    selectedZoneFormat: ZoneFormat,
    fieldName: String,
    onZoneFormatSelected: (ZoneFormat) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "ZONE FORMATTING",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = KarooTextSecondary,
        )

        val descriptionText = when (selectedZoneFormat) {
            ZoneFormat.NONE -> "Your ${fieldName.lowercase()} will display in numbers only."
            ZoneFormat.TEXT -> "Your ${fieldName.lowercase()} will display in numbers with the corresponding zone as the text color."
            ZoneFormat.BACKGROUND -> "Your ${fieldName.lowercase()} will display in numbers with the corresponding zone as the background color."
        }

        Text(
            text = descriptionText,
            fontSize = 13.sp,
            color = KarooTextSecondary,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(50))
                .background(KarooTrackBg)
                .padding(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val options = listOf(
                    Triple(ZoneFormat.NONE, "None", 0.85f),
                    Triple(ZoneFormat.TEXT, "Text", 0.85f),
                    Triple(ZoneFormat.BACKGROUND, "Background", 1.3f),
                )

                options.forEach { (format, labelText, weight) ->
                    val isSelected = selectedZoneFormat == format

                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .background(if (isSelected) KarooSlateAccent else Color.Transparent)
                            .clickable { onZoneFormatSelected(format) }
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = labelText,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = KarooTextPrimary,
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

fun findNearestIndex(values: List<Float>, target: Float): Int {
    return values.minByOrNull { abs(it - target) }?.let { values.indexOf(it) } ?: 0
}
