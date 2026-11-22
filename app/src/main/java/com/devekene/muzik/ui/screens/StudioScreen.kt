package com.devekene.muzik.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddLink
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SettingsSuggest
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.devekene.muzik.data.local.UserAudioPreferences
import com.devekene.muzik.data.model.BluetoothSyncState
import com.devekene.muzik.data.model.EqBandUi
import com.devekene.muzik.data.model.EqPreset
import com.devekene.muzik.data.model.EqualizerState
import com.devekene.muzik.data.model.ThemeMode
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun StudioScreen(
    eqState: EqualizerState,
    bluetoothState: BluetoothSyncState,
    preferences: UserAudioPreferences,
    bottomContentPadding: Dp,
    onToggleEqEnabled: (Boolean) -> Unit,
    onChangeBandLevel: (Int, Int) -> Unit,
    onChangeBassBoost: (Int) -> Unit,
    onChangeVirtualizer: (Int) -> Unit,
    onApplyPreset: (EqPreset) -> Unit,
    onSaveCustomPreset: (String) -> Unit,
    onDeleteCustomPreset: (EqPreset) -> Unit,
    onSelectCrossfadeMs: (Int) -> Unit,
    onToggleGapless: (Boolean) -> Unit,
    onOpenBluetoothSheet: () -> Unit,
    onToggleBluetoothAutoResume: (Boolean) -> Unit,
    onToggleBluetoothAutoEq: (Boolean, String) -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onToggleDynamicColor: (Boolean) -> Unit,
    onScanAudio: () -> Unit,
    onImportAudio: () -> Unit,
    onOpenAddStream: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    if (onBack != null) {
        BackHandler(onBack = onBack)
    }
    var showSavePresetDialog by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = bottomContentPadding
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Column {
                    Text("Settings & Audio", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Equalizer, Bluetooth synchronization & theme",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Equalizer Section
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.GraphicEq,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("5-Band Equalizer", style = MaterialTheme.typography.titleLarge)
                                Text(
                                    text = if (eqState.isHardwareAttached) {
                                        "Active DSP • ${eqState.activePresetName}"
                                    } else {
                                        "Preset: ${eqState.activePresetName}"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = eqState.isEnabled,
                            onCheckedChange = onToggleEqEnabled,
                            modifier = Modifier.testTag("eq_master_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Presets Strip
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(eqState.presets, key = { it.id }) { preset ->
                            FilterChip(
                                selected = preset.name == eqState.activePresetName,
                                onClick = { onApplyPreset(preset) },
                                label = { Text(preset.name) },
                                trailingIcon = if (!preset.isBuiltIn) {
                                    {
                                        IconButton(
                                            onClick = { onDeleteCustomPreset(preset) },
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Icon(
                                                Icons.Rounded.Close,
                                                contentDescription = "Delete preset",
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                } else null
                            )
                        }
                        item {
                            OutlinedButton(
                                onClick = { showSavePresetDialog = true },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Frequency Response Curve Canvas
                    EqFrequencyCurve(
                        bands = eqState.bands,
                        minMb = eqState.minBandLevelMb,
                        maxMb = eqState.maxBandLevelMb,
                        enabled = eqState.isEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(84.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Interactive Vertical Band Faders
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        eqState.bands.forEach { band ->
                            EqBandFader(
                                band = band,
                                minMb = eqState.minBandLevelMb,
                                maxMb = eqState.maxBandLevelMb,
                                enabled = eqState.isEnabled,
                                onLevelChange = { level -> onChangeBandLevel(band.index, level) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Bass Boost & Virtualizer
                    Text(
                        text = "Bass Boost (${eqState.bassBoostStrength / 10}%)",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Slider(
                        value = eqState.bassBoostStrength.toFloat(),
                        onValueChange = { onChangeBassBoost(it.roundToInt()) },
                        valueRange = 0f..1000f,
                        enabled = eqState.isEnabled,
                        modifier = Modifier.testTag("bass_boost_slider")
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Soundstage Virtualizer (${eqState.virtualizerStrength / 10}%)",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Slider(
                        value = eqState.virtualizerStrength.toFloat(),
                        onValueChange = { onChangeVirtualizer(it.roundToInt()) },
                        valueRange = 0f..1000f,
                        enabled = eqState.isEnabled,
                        modifier = Modifier.testTag("virtualizer_slider")
                    )
                }
            }
        }

        // Smooth Transitions & Gapless Playback
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Transitions & Playback", style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Smooth Track Fade", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Gradually blends volume on track changes and resume",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0 to "Off", 2000 to "2s", 4000 to "4s", 6000 to "6s").forEach { (ms, label) ->
                            FilterChip(
                                selected = preferences.crossfadeDurationMs == ms,
                                onClick = { onSelectCrossfadeMs(ms) },
                                label = { Text(label) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Gapless Playback", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Eliminate silence gaps between continuous album tracks",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = preferences.gaplessPlayback,
                            onCheckedChange = onToggleGapless
                        )
                    }
                }
            }
        }

        // Bluetooth Synchronization Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.BluetoothConnected,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Bluetooth Sync", style = MaterialTheme.typography.titleLarge)
                                Text(
                                    text = bluetoothState.activeDevice?.let { "${it.name} (${it.type.label})" }
                                        ?: "Device Speaker",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        OutlinedButton(onClick = onOpenBluetoothSheet) {
                            Text("Outputs")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-Resume on Connect", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Resume queued music when Bluetooth audio pairs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = bluetoothState.autoResumeOnConnect,
                            onCheckedChange = onToggleBluetoothAutoResume
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Bluetooth Auto-EQ", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Apply ${bluetoothState.bluetoothEqPresetName} on Bluetooth route",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = bluetoothState.autoEqOnBluetooth,
                            onCheckedChange = {
                                onToggleBluetoothAutoEq(it, bluetoothState.bluetoothEqPresetName)
                            }
                        )
                    }
                }
            }
        }

        // Theme & Contrast Settings
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Display & Theme", style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "High-contrast Light mode is primary for crisp readability, with Studio Dark available anytime.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = preferences.themeMode == mode,
                                onClick = { onSelectThemeMode(mode) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = when (mode) {
                                            ThemeMode.LIGHT -> Icons.Rounded.LightMode
                                            ThemeMode.DARK -> Icons.Rounded.DarkMode
                                            ThemeMode.SYSTEM -> Icons.Rounded.SettingsSuggest
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text(mode.label) },
                                modifier = Modifier.testTag("theme_chip_${mode.name.lowercase()}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Material You Dynamic Color", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Adapt accent hues from wallpaper on Android 12+",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = preferences.useDynamicColor,
                            onCheckedChange = onToggleDynamicColor
                        )
                    }
                }
            }
        }

        // Local Sources & Streaming Utility Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("Audio Sources", style = MaterialTheme.typography.titleLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = onScanAudio, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scan Device")
                        }
                        OutlinedButton(onClick = onImportAudio, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import Files")
                        }
                    }
                    OutlinedButton(onClick = onOpenAddStream, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.AddLink, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add High-Quality Audio Stream URL")
                    }
                }
            }
        }
    }

    if (showSavePresetDialog) {
        var presetName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSavePresetDialog = false },
            title = { Text("Save EQ Preset") },
            text = {
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    label = { Text("Preset name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (presetName.isNotBlank()) {
                            onSaveCustomPreset(presetName)
                            showSavePresetDialog = false
                        }
                    },
                    enabled = presetName.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSavePresetDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun EqFrequencyCurve(
    bands: List<EqBandUi>,
    minMb: Int,
    maxMb: Int,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val lineColor = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val bgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
            val midY = size.height / 2f
            drawLine(
                color = gridColor,
                start = Offset(0f, midY),
                end = Offset(size.width, midY),
                strokeWidth = 1.5.dp.toPx()
            )
            if (bands.size < 2) return@Canvas

            val span = (maxMb - minMb).coerceAtLeast(1).toFloat()
            val points = bands.mapIndexed { idx, band ->
                val x = (idx.toFloat() / (bands.size - 1).toFloat()) * size.width
                val norm = ((band.levelMb - minMb).toFloat() / span).coerceIn(0f, 1f)
                val y = size.height - (norm * size.height)
                Offset(x, y)
            }

            val strokePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 0 until points.lastIndex) {
                    val p0 = points[i]
                    val p1 = points[i + 1]
                    val cx = (p0.x + p1.x) / 2f
                    cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                }
            }

            val fillPath = Path().apply {
                addPath(strokePath)
                lineTo(points.last().x, size.height)
                lineTo(points.first().x, size.height)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.28f), lineColor.copy(alpha = 0.02f))
                )
            )
            drawPath(
                path = strokePath,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
            points.forEach { pt ->
                drawCircle(color = lineColor, radius = 4.dp.toPx(), center = pt)
            }
        }
    }
}

@Composable
private fun EqBandFader(
    band: EqBandUi,
    minMb: Int,
    maxMb: Int,
    enabled: Boolean,
    onLevelChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val trackBg = MaterialTheme.colorScheme.surfaceVariant
    val activeColor = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val thumbColor = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.surface
    var heightPx by remember { mutableStateOf(1f) }
    val span = (maxMb - minMb).coerceAtLeast(1)
    val norm = ((band.levelMb - minMb).toFloat() / span.toFloat()).coerceIn(0f, 1f)

    fun updateFromY(y: Float) {
        if (!enabled || heightPx <= 0f) return
        val fractionFromBottom = (1f - (y / heightPx)).coerceIn(0f, 1f)
        val rawMb = minMb + (fractionFromBottom * span).roundToInt()
        val snapped = (rawMb / 50) * 50
        onLevelChange(snapped.coerceIn(minMb, maxMb))
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        val db = band.levelMb / 100f
        Text(
            text = String.format(Locale.US, "%+.1f", db),
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(48.dp)
                .height(152.dp)
                .onSizeChanged { heightPx = it.height.toFloat().coerceAtLeast(1f) }
                .pointerInput(enabled, minMb, maxMb) {
                    detectTapGestures { offset -> updateFromY(offset.y) }
                }
                .pointerInput(enabled, minMb, maxMb) {
                    detectVerticalDragGestures { change, _ ->
                        change.consume()
                        updateFromY(change.position.y)
                    }
                }
                .testTag("eq_band_${band.index}")
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val trackW = 8.dp.toPx()
                val left = (size.width - trackW) / 2f
                drawRoundRect(
                    color = trackBg,
                    topLeft = Offset(left, 0f),
                    size = Size(trackW, size.height),
                    cornerRadius = CornerRadius(trackW / 2f, trackW / 2f)
                )

                val thumbY = size.height - (norm * size.height)
                val centerY = size.height / 2f
                val fillTop = minOf(thumbY, centerY)
                val fillHeight = kotlin.math.abs(centerY - thumbY)
                drawRoundRect(
                    color = activeColor,
                    topLeft = Offset(left, fillTop),
                    size = Size(trackW, fillHeight.coerceAtLeast(2f)),
                    cornerRadius = CornerRadius(trackW / 2f, trackW / 2f)
                )

                drawCircle(
                    color = activeColor,
                    radius = 11.dp.toPx(),
                    center = Offset(size.width / 2f, thumbY.coerceIn(11.dp.toPx(), size.height - 11.dp.toPx()))
                )
                drawCircle(
                    color = thumbColor,
                    radius = 4.dp.toPx(),
                    center = Offset(size.width / 2f, thumbY.coerceIn(11.dp.toPx(), size.height - 11.dp.toPx()))
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = band.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
