package com.lighttool.dengyu

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.lighttool.dengyu.data.AppPage
import com.lighttool.dengyu.data.LampUiState
import com.lighttool.dengyu.data.MessageMode
import com.lighttool.dengyu.reader.CenterLightAnalyzer
import com.lighttool.dengyu.reader.CenterLightSample
import com.lighttool.dengyu.ui.theme.LightSignalTheme
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LightSignalTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val uiState by viewModel.uiState.collectAsState()
                    PermissionGate {
                        LampScreen(
                            state = uiState,
                            onPageChange = viewModel::setCurrentPage,
                            onToggleTorch = viewModel::toggleTorch,
                            onIntervalChange = viewModel::setInterval,
                            onStartFlashing = viewModel::startFlashing,
                            onStopFlashing = viewModel::stopFlashing,
                            onMessageModeChange = viewModel::setMessageMode,
                            onMorseInputChange = viewModel::setMorseInput,
                            onCustomPatternTextChange = viewModel::setCustomPatternText,
                            onShortOnChange = viewModel::setShortOn,
                            onLongOnChange = viewModel::setLongOn,
                            onGapChange = viewModel::setGap,
                            onWordGapChange = viewModel::setWordGap,
                            onRepeatCountChange = viewModel::setRepeatCount,
                            onSendPattern = viewModel::sendPattern,
                            onStopPatternSending = viewModel::stopPatternSending,
                            onAutoOffHoursChange = viewModel::setAutoOffHours,
                            onAutoOffMinutesChange = viewModel::setAutoOffMinutes,
                            onStartTorchWithAutoOff = viewModel::startTorchWithAutoOff,
                            onStopTorchNow = viewModel::stopTorchNow,
                            onReaderThresholdChange = viewModel::setReaderThreshold,
                            onApplySuggestedReaderThreshold = viewModel::applySuggestedReaderThreshold,
                            onReaderStart = viewModel::startReadingLight,
                            onReaderStop = viewModel::stopReadingLight,
                            onReaderClear = viewModel::clearReaderResult,
                            onReaderSample = viewModel::onReaderSample
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionGate(content: @Composable () -> Unit) {
    val permissions = buildList {
        add(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }
    LaunchedEffect(Unit) {
        launcher.launch(permissions)
    }
    content()
}

@Composable
private fun LampScreen(
    state: LampUiState,
    onPageChange: (AppPage) -> Unit,
    onToggleTorch: () -> Unit,
    onIntervalChange: (Float) -> Unit,
    onStartFlashing: () -> Unit,
    onStopFlashing: () -> Unit,
    onMessageModeChange: (MessageMode) -> Unit,
    onMorseInputChange: (String) -> Unit,
    onCustomPatternTextChange: (String) -> Unit,
    onShortOnChange: (Float) -> Unit,
    onLongOnChange: (Float) -> Unit,
    onGapChange: (Float) -> Unit,
    onWordGapChange: (Float) -> Unit,
    onRepeatCountChange: (String) -> Unit,
    onSendPattern: () -> Unit,
    onStopPatternSending: () -> Unit,
    onAutoOffHoursChange: (String) -> Unit,
    onAutoOffMinutesChange: (String) -> Unit,
    onStartTorchWithAutoOff: () -> Unit,
    onStopTorchNow: () -> Unit,
    onReaderThresholdChange: (Float) -> Unit,
    onApplySuggestedReaderThreshold: () -> Unit,
    onReaderStart: () -> Unit,
    onReaderStop: () -> Unit,
    onReaderClear: () -> Unit,
    onReaderSample: (CenterLightSample) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFF4F8FF), Color(0xFFEAF2FF))
                )
            )
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HeaderCard(state.statusMessage)
        PageSegmentedRow(
            selected = state.currentPage,
            onSelect = onPageChange
        )
        if (state.currentPage == AppPage.CONTROL) {
            ScheduleCard(
                state = state,
                onAutoOffHoursChange = onAutoOffHoursChange,
                onAutoOffMinutesChange = onAutoOffMinutesChange,
                onStartTorchWithAutoOff = onStartTorchWithAutoOff,
                onStopTorchNow = onStopTorchNow
            )
            PowerCard(
                state = state,
                onToggleTorch = onToggleTorch,
                onIntervalChange = onIntervalChange,
                onStartFlashing = onStartFlashing,
                onStopFlashing = onStopFlashing
            )
            PatternCard(
                state = state,
                onMessageModeChange = onMessageModeChange,
                onMorseInputChange = onMorseInputChange,
                onCustomPatternTextChange = onCustomPatternTextChange,
                onShortOnChange = onShortOnChange,
                onLongOnChange = onLongOnChange,
                onGapChange = onGapChange,
                onWordGapChange = onWordGapChange,
                onRepeatCountChange = onRepeatCountChange,
                onSendPattern = onSendPattern,
                onStopPatternSending = onStopPatternSending
            )
        } else {
            ReaderCard(
                state = state,
                onReaderThresholdChange = onReaderThresholdChange,
                onApplySuggestedReaderThreshold = onApplySuggestedReaderThreshold,
                onReaderStart = onReaderStart,
                onReaderStop = onReaderStop,
                onReaderClear = onReaderClear,
                onReaderSample = onReaderSample
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun HeaderCard(status: String) {
    FancyCard {
        Text(
            text = "灯语助手 读灯版",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF122033)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "保留原有发灯功能，并新增用摄像头读取灯语的页面",
            color = Color(0xFF52627A)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF66E6DD))
                    .width(10.dp)
                    .height(10.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = status, color = Color(0xFF243447))
        }
    }
}

@Composable
private fun PageSegmentedRow(
    selected: AppPage,
    onSelect: (AppPage) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ModeChip(
            title = "发灯功能",
            selected = selected == AppPage.CONTROL,
            onClick = { onSelect(AppPage.CONTROL) },
            modifier = Modifier.weight(1f)
        )
        ModeChip(
            title = "读灯功能",
            selected = selected == AppPage.READER,
            onClick = { onSelect(AppPage.READER) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ReaderCard(
    state: LampUiState,
    onReaderThresholdChange: (Float) -> Unit,
    onApplySuggestedReaderThreshold: () -> Unit,
    onReaderStart: () -> Unit,
    onReaderStop: () -> Unit,
    onReaderClear: () -> Unit,
    onReaderSample: (CenterLightSample) -> Unit
) {
    val readerState = state.readerState
    FancyCard {
        Text("读灯语", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF102033))
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "把闪烁光源对准取景框中央，系统会根据亮灭时长尝试识别 . - 和摩斯码文本。",
            color = Color(0xFF58677F)
        )
        Spacer(modifier = Modifier.height(14.dp))
        CameraPreviewPanel(onSample = onReaderSample)
        Spacer(modifier = Modifier.height(14.dp))
        ReaderMetricsPanel(
            signalStrength = readerState.signalStrength,
            centerBrightness = readerState.centerBrightness,
            surroundBrightness = readerState.surroundBrightness,
            noiseFloor = readerState.noiseFloor,
            lightDetected = readerState.lightDetected,
            pulseMs = readerState.currentPulseMs
        )
        Spacer(modifier = Modifier.height(14.dp))
        PreviewPanel(title = "读取提示", text = readerState.guideMessage)
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "识别阈值 ${readerState.detectionThreshold.toInt()}",
            color = Color(0xFF102033),
            fontWeight = FontWeight.Medium
        )
        Slider(
            value = readerState.detectionThreshold,
            onValueChange = onReaderThresholdChange,
            valueRange = 5f..60f
        )
        Text(
            text = "建议阈值 ${readerState.suggestedThreshold.toInt()}，可先点击自动校准再开始读取",
            color = Color(0xFF58677F)
        )
        Spacer(modifier = Modifier.height(10.dp))
        SecondaryButton(onClick = onApplySuggestedReaderThreshold, modifier = Modifier.fillMaxWidth()) {
            Text("自动校准阈值")
        }
        Spacer(modifier = Modifier.height(10.dp))
        if (readerState.decodedSymbols.isNotBlank()) {
            PreviewPanel(title = "已读取灯语", text = readerState.decodedSymbols)
            Spacer(modifier = Modifier.height(10.dp))
        }
        if (readerState.decodedMessage.isNotBlank()) {
            PreviewPanel(title = "摩斯码翻译", text = readerState.decodedMessage)
            Spacer(modifier = Modifier.height(10.dp))
        }
        Text(
            text = "建议先用另一台手机的闪光灯在室内测试，中近距离识别更稳定。",
            color = Color(0xFF58677F)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (readerState.isReading) {
                SecondaryButton(onClick = onReaderStop, modifier = Modifier.weight(1f)) {
                    Text("停止读取")
                }
            } else {
                PrimaryButton(onClick = onReaderStart, modifier = Modifier.weight(1f)) {
                    Text("开始读取")
                }
            }
            SecondaryButton(onClick = onReaderClear, modifier = Modifier.weight(1f)) {
                Text("清空结果")
            }
        }
    }
}

@Composable
private fun CameraPreviewPanel(
    onSample: (CenterLightSample) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val onSampleState = rememberUpdatedState(onSample)
    val executor = remember { Executors.newSingleThreadExecutor() }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    val hasCameraPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
        }
    }

    DisposableEffect(previewView, lifecycleOwner, hasCameraPermission) {
        val currentPreviewView = previewView
        if (!hasCameraPermission || currentPreviewView == null) {
            onDispose { }
        } else {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            val mainExecutor = ContextCompat.getMainExecutor(context)
            val listener = Runnable {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().apply {
                    surfaceProvider = currentPreviewView.surfaceProvider
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .apply {
                        setAnalyzer(
                            executor,
                            CenterLightAnalyzer { sample ->
                                onSampleState.value(sample)
                            }
                        )
                    }
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
            }
            cameraProviderFuture.addListener(listener, mainExecutor)
            onDispose {
                runCatching {
                    cameraProviderFuture.get().unbindAll()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF09111F))
    ) {
        if (hasCameraPermission) {
            AndroidView(
                factory = {
                    PreviewView(it).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        previewView = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(160.dp)
                    .border(
                        width = 3.dp,
                        color = Color(0xFF66E6DD),
                        shape = RoundedCornerShape(22.dp)
                    )
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("未获得摄像头权限", color = Color.White, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("请允许相机权限后再进入读灯功能。", color = Color(0xFFBFD0E8))
            }
        }
    }
}

@Composable
private fun ReaderMetricsPanel(
    signalStrength: Float,
    centerBrightness: Float,
    surroundBrightness: Float,
    noiseFloor: Float,
    lightDetected: Boolean,
    pulseMs: Long
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MetricCard(
            title = "信号强度",
            value = signalStrength.toInt().toString(),
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            title = "中心亮度",
            value = centerBrightness.toInt().toString(),
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            title = "周围亮度",
            value = surroundBrightness.toInt().toString(),
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(modifier = Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MetricCard(
            title = "噪声水平",
            value = noiseFloor.toInt().toString(),
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            title = if (lightDetected) "已亮灯" else "未亮灯",
            value = "${pulseMs}ms",
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFF4F8FD))
            .padding(14.dp)
    ) {
        Text(title, color = Color(0xFF5B6B82), fontSize = 13.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, color = Color(0xFF122033), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PowerCard(
    state: LampUiState,
    onToggleTorch: () -> Unit,
    onIntervalChange: (Float) -> Unit,
    onStartFlashing: () -> Unit,
    onStopFlashing: () -> Unit
) {
    FancyCard {
        Text(
            text = "闪灯与手动控制",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF102033)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "这里的常亮开关只负责手动立即开灯/关灯，不会影响上面的延时关灯。",
            color = Color(0xFF58677F)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("常亮开关", color = Color(0xFF102033), fontWeight = FontWeight.Medium)
                Text(
                    text = if (state.isTorchOn) "当前已开启" else "当前已关闭",
                    color = Color(0xFF58677F)
                )
            }
            Switch(
                checked = state.isTorchOn,
                onCheckedChange = { onToggleTorch() }
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text("闪灯间隔 ${state.intervalMs.toInt()} ms", color = Color(0xFF102033))
        Slider(
            value = state.intervalMs,
            onValueChange = onIntervalChange,
            valueRange = 80f..1500f
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton(onClick = onStartFlashing, modifier = Modifier.weight(1f)) {
                Text("开始闪灯")
            }
            SecondaryButton(onClick = onStopFlashing, modifier = Modifier.weight(1f)) {
                Text("停止任务")
            }
        }
    }
}

@Composable
private fun ScheduleCard(
    state: LampUiState,
    onAutoOffHoursChange: (String) -> Unit,
    onAutoOffMinutesChange: (String) -> Unit,
    onStartTorchWithAutoOff: () -> Unit,
    onStopTorchNow: () -> Unit
) {
    FancyCard {
        Text("延时关灯", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF102033))
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "设置好延时时间后，点击“确认开灯”，手电筒会立即开启，并在指定时间后自动关闭。",
            color = Color(0xFF58677F)
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (state.autoOffRemainingSeconds != null) {
            PreviewPanel(
                title = "自动关灯倒计时",
                text = formatSeconds(state.autoOffRemainingSeconds)
            )
            Spacer(modifier = Modifier.height(14.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DurationInput(
                label = "小时",
                value = state.scheduleState.autoOffHours.toString(),
                onValueChange = onAutoOffHoursChange,
                modifier = Modifier.weight(1f)
            )
            DurationInput(
                label = "分钟",
                value = state.scheduleState.autoOffMinutes.toString(),
                onValueChange = onAutoOffMinutesChange,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SecondaryButton(onClick = onStopTorchNow, modifier = Modifier.weight(1f)) {
                Text("立即关灯")
            }
            PrimaryButton(onClick = onStartTorchWithAutoOff, modifier = Modifier.weight(1f)) {
                Text("确认开灯")
            }
        }
    }
}

@Composable
private fun PatternCard(
    state: LampUiState,
    onMessageModeChange: (MessageMode) -> Unit,
    onMorseInputChange: (String) -> Unit,
    onCustomPatternTextChange: (String) -> Unit,
    onShortOnChange: (Float) -> Unit,
    onLongOnChange: (Float) -> Unit,
    onGapChange: (Float) -> Unit,
    onWordGapChange: (Float) -> Unit,
    onRepeatCountChange: (String) -> Unit,
    onSendPattern: () -> Unit,
    onStopPatternSending: () -> Unit
) {
    FancyCard {
        Text("灯语模式", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF102033))
        Spacer(modifier = Modifier.height(8.dp))
        Text("支持国际摩斯码自动编码，也支持你自己定义长短闪节奏。", color = Color(0xFF58677F))
        Spacer(modifier = Modifier.height(12.dp))
        SegmentedModeRow(
            selected = state.messageMode,
            onSelect = onMessageModeChange
        )
        Spacer(modifier = Modifier.height(14.dp))
        if (state.messageMode == MessageMode.MORSE) {
            OutlinedTextField(
                value = state.morseInput,
                onValueChange = onMorseInputChange,
                label = { Text("输入英文字母或数字") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            PreviewPanel(
                title = "摩斯码预览",
                text = state.morsePreview
            )
        } else {
            Text("输入 . 表示短闪，- 表示长闪，空格表示单词间隔", color = Color(0xFF58677F))
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = state.customPatternText,
                onValueChange = onCustomPatternTextChange,
                label = { Text("自定义节奏") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = Color(0xFFE4EAF4))
        Spacer(modifier = Modifier.height(16.dp))
        DurationInput(
            label = "循环播放次数",
            value = state.repeatCount.toString(),
            onValueChange = onRepeatCountChange,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text("时长设置", color = Color(0xFF102033), fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))
        DurationSlider("短闪", state.shortOnMs, 100f..600f, onShortOnChange)
        DurationSlider("长闪", state.longOnMs, 300f..1400f, onLongOnChange)
        DurationSlider("符号间隔", state.gapMs, 80f..800f, onGapChange)
        DurationSlider("词间隔", state.wordGapMs, 300f..1800f, onWordGapChange)
        Spacer(modifier = Modifier.height(12.dp))
        Text("循环之间已增加停顿，默认播放 1 次。", color = Color(0xFF58677F))
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton(onClick = onSendPattern, modifier = Modifier.weight(1f)) {
                Text("发送灯语")
            }
            SecondaryButton(
                onClick = onStopPatternSending,
                modifier = Modifier.weight(1f)
            ) {
                Text("停止发送")
            }
        }
    }
}

@Composable
private fun DurationSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Text("$label ${value.toInt()} ms", color = Color(0xFF102033))
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange)
    }
}

@Composable
private fun FancyCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, Color(0xFFD9E6F7)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFEFF))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            content = content
        )
    }
}

@Composable
private fun SegmentedModeRow(
    selected: MessageMode,
    onSelect: (MessageMode) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ModeChip(
            title = "摩斯码",
            selected = selected == MessageMode.MORSE,
            onClick = { onSelect(MessageMode.MORSE) },
            modifier = Modifier.weight(1f)
        )
        ModeChip(
            title = "自定义",
            selected = selected == MessageMode.CUSTOM,
            onClick = { onSelect(MessageMode.CUSTOM) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ModeChip(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) Color(0xFF24C3C7) else Color(0xFFF2F6FB))
            .border(
                width = 1.dp,
                color = if (selected) Color(0xFF24C3C7) else Color(0xFFD5E1F0),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (selected) Color.White else Color(0xFF34445C),
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun PreviewPanel(title: String, text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFF4F8FD))
            .padding(14.dp)
    ) {
        Text(text = title, color = Color(0xFF5B6B82), fontSize = 13.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = text,
            color = Color(0xFF122033),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun DurationInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit).take(2)) },
        label = { Text(label) },
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

private fun formatSeconds(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}

@Composable
private fun PrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(54.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF24C3C7),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
        content = { content() }
    )
}

@Composable
private fun SecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(54.dp),
        border = BorderStroke(1.dp, Color(0xFFB9C9DD)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Color(0xFF29405A)
        ),
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
        content = { content() }
    )
}
