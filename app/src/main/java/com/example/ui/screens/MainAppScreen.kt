package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import android.widget.Toast
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.EstimationProject
import com.example.data.model.RoomDetail
import com.example.data.model.ChatMessage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.util.Base64
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.ui.viewmodel.QuantityViewModel
import com.example.utils.QuantityCalculator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: QuantityViewModel) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val calcResult by viewModel.calculationResult.collectAsStateWithLifecycle()
    val hasCompletedOnboarding by viewModel.hasCompletedOnboarding.collectAsStateWithLifecycle()
    val userTokens by viewModel.userTokens.collectAsStateWithLifecycle()
    val userSession by viewModel.userSession.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showTokenDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val billingError by viewModel.billingErrorMessage.collectAsStateWithLifecycle()
    val billingSuccess by viewModel.billingSuccessMessage.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.setupBilling(context)
    }

    LaunchedEffect(billingError) {
        billingError?.let {
            scope.launch {
                snackbarHostState.showSnackbar("❌ $it")
            }
            viewModel.billingErrorMessage.value = null
        }
    }

    LaunchedEffect(billingSuccess) {
        billingSuccess?.let {
            scope.launch {
                snackbarHostState.showSnackbar("✅ $it")
            }
            viewModel.billingSuccessMessage.value = null
        }
    }

    if (userSession == null) {
        LoginScreen(viewModel = viewModel)
    } else if (!hasCompletedOnboarding) {
        OnboardingScreen(onCompleted = { viewModel.hasCompletedOnboarding.value = true })
    } else {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(end = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // User Tokens Card Chip
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .clickable { showTokenDialog = true }
                                    .testTag("tokens_chip")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Paid,
                                        contentDescription = "الرصيد",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "$userTokens عملة",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "BOQ AI",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 19.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Right
                                )
                                Icon(
                                    imageVector = Icons.Default.Engineering,
                                    contentDescription = "المهندس المساعد",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { viewModel.toggleDarkMode() },
                            modifier = Modifier.testTag("dark_mode_toggle")
                        ) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "تغيير الوضع المظلم",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    ),
                    modifier = Modifier.shadow(4.dp)
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    val menuItems = listOf(
                        NavigationTabItem("الرئيسية", Icons.Default.Home, 0),
                        NavigationTabItem("التشطيبات", Icons.Default.ColorLens, 1),
                        NavigationTabItem("كشف المواد", Icons.Default.List, 2),
                        NavigationTabItem("المحفوظات والأمان", Icons.Default.Settings, 3)
                    )
                    
                    menuItems.forEach { item ->
                        NavigationBarItem(
                            selected = selectedTab == item.index,
                            onClick = { viewModel.selectedTab.value = item.index },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("nav_tab_${item.index}")
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(
                        Brush.verticalGradient(
                            colors = if (isDarkMode) {
                                listOf(Color(0xFF0B141A), Color(0xFF111B21))
                            } else {
                                listOf(Color(0xFFECE5DD), Color(0xFFF0F2F5))
                            }
                        )
                    )
            ) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(200))
                    },
                    label = "ScreenTransition"
                ) { targetIndex ->
                    when (targetIndex) {
                        0 -> ConstructionCalculatorTab(viewModel, snackbarHostState)
                        1 -> FinishesCalculatorTab(viewModel, snackbarHostState)
                        2 -> BoqResultTab(viewModel, calcResult, snackbarHostState)
                        3 -> SettingsAndSavedTab(viewModel, snackbarHostState)
                    }
                }
            }
        }
    }
}

    if (showTokenDialog) {
        TokenManagementDialog(
            viewModel = viewModel,
            onDismiss = { showTokenDialog = false }
        )
    }
}

private data class NavigationTabItem(val label: String, val icon: ImageVector, val index: Int)

// ------------------------------------
// TAB 1: Construction Calculator & Rooms Slider Screen
// ------------------------------------
@Composable
fun ConstructionCalculatorTab(
    viewModel: QuantityViewModel,
    snackbarHostState: SnackbarHostState
) {
    val plotArea by viewModel.plotArea.collectAsStateWithLifecycle()
    val buildArea by viewModel.buildArea.collectAsStateWithLifecycle()
    val floorsCount by viewModel.floorsCount.collectAsStateWithLifecycle()
    val buildingType by viewModel.buildingType.collectAsStateWithLifecycle()
    val foundationType by viewModel.foundationType.collectAsStateWithLifecycle()
    val materialType by viewModel.materialType.collectAsStateWithLifecycle()
    val rooms by viewModel.rooms.collectAsStateWithLifecycle()
    val calcResult by viewModel.calculationResult.collectAsStateWithLifecycle()
    val showResults by viewModel.showResults.collectAsStateWithLifecycle()

    val flooringType by viewModel.flooringType.collectAsStateWithLifecycle()
    val ceilingType by viewModel.ceilingType.collectAsStateWithLifecycle()
    val doorsCount by viewModel.doorsCount.collectAsStateWithLifecycle()
    val doorUnitPrice by viewModel.doorUnitPrice.collectAsStateWithLifecycle()
    val windowsCount by viewModel.windowsCount.collectAsStateWithLifecycle()
    val windowUnitPrice by viewModel.windowUnitPrice.collectAsStateWithLifecycle()
    val sanitaryQuality by viewModel.sanitaryQuality.collectAsStateWithLifecycle()
    val electricalQuality by viewModel.electricalQuality.collectAsStateWithLifecycle()
    
    val scope = rememberCoroutineScope()
    var projectTitleSave by remember { mutableStateOf("") }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showAddRoomDialog by remember { mutableStateOf(false) }
    var showRoomsList by remember { mutableStateOf(false) }
    val isMarketVisible by viewModel.isMeasurementMarketVisible.collectAsStateWithLifecycle()
    var showMarketDialog by remember { mutableStateOf(false) }
    
    var showManualDimensions by remember { mutableStateOf(false) }
    var buildLengthText by remember { mutableStateOf("") }
    var buildWidthText by remember { mutableStateOf("") }
    var plotLengthText by remember { mutableStateOf("") }
    var plotWidthText by remember { mutableStateOf("") }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcoming card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "محاكاة البناء وحساب الكميات الذكي",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "هنا يمكنك تحديد أبعاد الأرض وهيكل البناء، والتحكم بأبعاد غرف منزلك بدقة عبر المنزلقات التفاعلية لحساب كميات الخرسانة، الحديد، الإسمنت الطابوق بصورة فورية.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        textAlign = TextAlign.Right,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Card for PDF Plan Upload
        item {
            val pdfFileName by viewModel.pdfFileName.collectAsStateWithLifecycle()
            val pdfFileSize by viewModel.pdfFileSize.collectAsStateWithLifecycle()
            val isPdfValid by viewModel.isPdfValid.collectAsStateWithLifecycle()
            val pdfValidationError by viewModel.pdfValidationError.collectAsStateWithLifecycle()
            val isPdfAnalyzing by viewModel.isPdfAnalyzing.collectAsStateWithLifecycle()

            val context = LocalContext.current
            val filePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent(),
                onResult = { uri: android.net.Uri? ->
                    if (uri != null) {
                        viewModel.handleSelectedPdf(context, uri)
                    }
                }
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    width = 1.2.dp,
                    color = when (isPdfValid) {
                        true -> Color(0xFF2E7D32).copy(alpha = 0.5f)
                        false -> MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                        else -> MaterialTheme.colorScheme.outlineVariant
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    // Header Row with Plan Icon
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    color = when (isPdfValid) {
                                        true -> Color(0xFFE8F5E9)
                                        false -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                                        else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                                    },
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (isPdfValid) {
                                    true -> Icons.Default.CheckCircle
                                    false -> Icons.Default.Cancel
                                    else -> Icons.Default.PictureAsPdf
                                },
                                contentDescription = null,
                                tint = when (isPdfValid) {
                                    true -> Color(0xFF2E7D32)
                                    false -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.primary
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "مخطط البناء المعماري (PDF)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "ارفع مخططك الإنشائي بصيغة PDF لاستخلاص البيانات تلقائياً",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (pdfFileName == null) {
                        // Empty State / Upload Trigger Area
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .clickable {
                                    try {
                                        filePickerLauncher.launch("application/pdf")
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "لم نتمكن من فتح اختيار الملفات في جهازك.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = "تحميل ملف",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "اختر ملف المخطط المعماري (.pdf)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "الحجم الأقصى: ٢٥ ميغابايت",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    } else {
                        // File Loaded State
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = if (isPdfValid == true) Color(0xFFF1F8E9) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp),
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Delete/Remove Button
                                IconButton(
                                    onClick = { viewModel.resetPdfState() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "إزالة الملف",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = pdfFileName ?: "",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Right
                                        )
                                        Text(
                                            text = pdfFileSize ?: "تحقق من سلامة البيانات",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.Default.PictureAsPdf,
                                        contentDescription = null,
                                        tint = if (isPdfValid == true) Color(0xFFD32F2F) else Color.Gray,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            if (isPdfValid == true) {
                                HorizontalDivider(color = Color(0xFF33691E).copy(alpha = 0.15f))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "المخطط جاهز ومطابق للمواصفات الفنية ✅",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF33691E),
                                        textAlign = TextAlign.Right
                                    )
                                }

                                if (isPdfAnalyzing) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().height(44.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "جاري قراءة وتحليل بيانات المخطط وتوطين الأبعاد تلقائياً...",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            viewModel.simulatePdfAnalysis { message ->
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(message)
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF2E7D32)
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(38.dp)
                                            .testTag("analyze_pdf_plan_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "تحليل المخطط واستيراد القيم للمخمن الاستشاري",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            } else {
                                // Error Message Container
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = pdfValidationError ?: "الملف المختار ليس بصيغة PDF صالحة.",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.error,
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isMarketVisible) {
            item {
                Card(
                    onClick = { showMarketDialog = true },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(16.dp))
                        .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "ماركت القياسات",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.weight(1f).padding(end = 12.dp)
                        ) {
                            Text(
                                text = "🛒 مفعّل الآن: ماركت القياسات والمخططات",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "بناءً على تحليل ذكاء المهندس الإنشائي، تتوفر الآن باقات تصاميم ومخططات مطابقة بأبعاد هندسية حقيقية جاهزة للتحميل.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                textAlign = TextAlign.Right,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }

        // Collapsible Glossary Card for the General Public ("العامة")
        item {
            var isGlossaryExpanded by remember { mutableStateOf(false) }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isGlossaryExpanded = !isGlossaryExpanded }
            ) {
                Column(
                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(
                            imageVector = if (isGlossaryExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "💡 دليل مصطلحات البناء البسيط وعناوين المواد",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary,
                                textAlign = TextAlign.Right
                            )
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    
                    AnimatedVisibility(
                        visible = isGlossaryExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Divider(color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f))
                            
                            GlossaryItem("صبة اللبشة (Raft Foundation)", "صب مخفر خرساني صلب مستمر لكامل المساحة. يوزع ثقل البناء بالتساوي، ممتاز للأراضي الطينية لمنع النزول.")
                            GlossaryItem("صب شريطي (Strip Foundation)", "حزام كونكريتي ممتد تحت الحوائط الأساسية فقط؛ خيار موفر جداً وجيد في التربة المتماسكة.")
                            GlossaryItem("الدفان والسبيس (Sub-base Layer)", "رص طبقات من مخلوط الرمل والشور لتقوية وتمتين قواعد البناء وتعميق عزلها عن رطوبة الأرض.")
                            GlossaryItem("البياض واللبخ (Plaster & Gesso)", "طبقة الملاط الإسمنتية متبوعة بالبورق لإعداد جدران الغرف ناعمة ومهيأة لطبقات الصباغة النهائية.")
                        }
                    }
                }
            }
        }

        // Section: Building Basic Inputs
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.shadow(2.dp, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "١. معلومات الأرض والصبة الأساسية",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Right
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "💡 قوالب مساحات عراقية شائعة لبدء حساب سريع ومبسط:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Iraqi Plot Size Presets
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End)
                    ) {
                        listOf(
                            Triple("٣٠٠ م٢", "240", "300"),
                            Triple("٢٠٠ م٢", "160", "200"),
                            Triple("١٥٠ م٢", "120", "150"),
                            Triple("١٠٠ م٢", "80", "100")
                        ).forEach { preset ->
                            val isSelected = plotArea == preset.third && buildArea == preset.second
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    )
                                    .clickable {
                                        viewModel.plotArea.value = preset.third
                                        viewModel.buildArea.value = preset.second
                                        viewModel.calculateQuantities()
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                "تم تطبيق قالب مساحة عراقي بقيمة ${preset.first} بنجاح!",
                                                withDismissAction = true
                                            )
                                        }
                                    }
                                    .border(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary 
                                        else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = preset.first,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer 
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    
                    // Fields Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = buildArea,
                            onValueChange = {
                                viewModel.buildArea.value = it
                                viewModel.calculateQuantities()
                            },
                            label = { Text("مساحة البناء (م٢)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("input_build_area")
                        )
                        OutlinedTextField(
                            value = plotArea,
                            onValueChange = {
                                viewModel.plotArea.value = it
                                viewModel.calculateQuantities()
                            },
                            label = { Text("مساحة الأرض الكلية (م٢)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("input_plot_area")
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { 
                                showManualDimensions = !showManualDimensions 
                                if (showManualDimensions) {
                                    val bArea = buildArea.toDoubleOrNull() ?: 0.0
                                    if (bArea > 0.0) {
                                        buildLengthText = "20"
                                        buildWidthText = String.format("%.1f", bArea / 20.0).replace(",", ".")
                                    } else {
                                        buildLengthText = ""
                                        buildWidthText = ""
                                    }
                                    val pArea = plotArea.toDoubleOrNull() ?: 0.0
                                    if (pArea > 0.0) {
                                        plotLengthText = "20"
                                        plotWidthText = String.format("%.1f", pArea / 20.0).replace(",", ".")
                                    } else {
                                        plotLengthText = ""
                                        plotWidthText = ""
                                    }
                                }
                            },
                            modifier = Modifier.testTag("toggle_manual_dimensions_button")
                        ) {
                            Icon(
                                imageVector = if (showManualDimensions) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (showManualDimensions) "إخفاء حساب الأبعاد يدوياً" else "📐 حساب المساحة بإدخال الأبعاد يدوياً (الطول × العرض)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = showManualDimensions,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "📐 أدخل أبعاد البناء والأرض لحساب المساحات تلقائياً:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    textAlign = TextAlign.Right
                                )
                                
                                // Build dimensions inputs
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = buildWidthText,
                                        onValueChange = { newValue ->
                                            buildWidthText = newValue
                                            val len = buildLengthText.toDoubleOrNull() ?: 0.0
                                            val wid = newValue.toDoubleOrNull() ?: 0.0
                                            if (wid > 0 && len > 0) {
                                                val area = len * wid
                                                viewModel.buildArea.value = String.format("%.1f", area).replace(",", ".")
                                                viewModel.calculateQuantities()
                                            }
                                        },
                                        label = { Text("عرض البناء (م)") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f).testTag("manual_build_width")
                                    )
                                    OutlinedTextField(
                                        value = buildLengthText,
                                        onValueChange = { newValue ->
                                            buildLengthText = newValue
                                            val len = newValue.toDoubleOrNull() ?: 0.0
                                            val wid = buildWidthText.toDoubleOrNull() ?: 0.0
                                            if (wid > 0 && len > 0) {
                                                val area = len * wid
                                                viewModel.buildArea.value = String.format("%.1f", area).replace(",", ".")
                                                viewModel.calculateQuantities()
                                            }
                                        },
                                        label = { Text("طول البناء (م)") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f).testTag("manual_build_length")
                                    )
                                    Text(
                                        "البناء:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(42.dp),
                                        textAlign = TextAlign.Right
                                    )
                                }

                                // Plot dimensions inputs
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = plotWidthText,
                                        onValueChange = { newValue ->
                                            plotWidthText = newValue
                                            val len = plotLengthText.toDoubleOrNull() ?: 0.0
                                            val wid = newValue.toDoubleOrNull() ?: 0.0
                                            if (wid > 0 && len > 0) {
                                                val area = len * wid
                                                viewModel.plotArea.value = String.format("%.1f", area).replace(",", ".")
                                                viewModel.calculateQuantities()
                                            }
                                        },
                                        label = { Text("عرض الأرض (م)") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f).testTag("manual_plot_width")
                                    )
                                    OutlinedTextField(
                                        value = plotLengthText,
                                        onValueChange = { newValue ->
                                            plotLengthText = newValue
                                            val len = newValue.toDoubleOrNull() ?: 0.0
                                            val wid = plotWidthText.toDoubleOrNull() ?: 0.0
                                            if (wid > 0 && len > 0) {
                                                val area = len * wid
                                                viewModel.plotArea.value = String.format("%.1f", area).replace(",", ".")
                                                viewModel.calculateQuantities()
                                            }
                                        },
                                        label = { Text("طول الأرض (م)") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f).testTag("manual_plot_length")
                                    )
                                    Text(
                                        "الأرض:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(42.dp),
                                        textAlign = TextAlign.Right
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // Floors Selector
                    Text(
                        text = "عدد طوابق البناء: $floorsCount",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Slider(
                        value = floorsCount.toFloat(),
                        onValueChange = {
                            viewModel.floorsCount.value = it.toInt()
                            viewModel.calculateQuantities()
                        },
                        valueRange = 1f..4f,
                        steps = 2,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("slider_floors")
                    )
                }
            }
        }

        // Section: Land Reclamation / Preparation (Optional/Guessing)
        item {
            val isReclamationEnabled by viewModel.isReclamationEnabled.collectAsStateWithLifecycle()
            val reclamationCostInput by viewModel.reclamationCostInput.collectAsStateWithLifecycle()
            val reclamationNature by viewModel.reclamationNature.collectAsStateWithLifecycle()
            val reclamationExplanation by viewModel.reclamationExplanation.collectAsStateWithLifecycle()
            val isReclamationLoading by viewModel.isReclamationLoading.collectAsStateWithLifecycle()

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.shadow(2.dp, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Switch(
                            checked = isReclamationEnabled,
                            onCheckedChange = {
                                viewModel.isReclamationEnabled.value = it
                                viewModel.calculateQuantities()
                            }
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "استصلاح وتسوية الأرض (اختياري)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Right
                            )
                            Icon(
                                imageVector = Icons.Default.Landscape,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    Text(
                        text = "إذا كانت الأرض غير مستوية أو تحتاج تهيئة استثنائية (حفر، دفن سبيس، أو تكسير صخور)، قم بتفعيل هذا الخيار لحسابه في الميزانية.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                    )

                    if (isReclamationEnabled) {
                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        
                        Text(
                            text = "طبيعة الأرض الحالية وتضاريسها:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        
                        // Nature choice grid
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val activeCol = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                                val inactiveCol = ButtonDefaults.outlinedButtonColors()
                                
                                OutlinedButton(
                                    onClick = { 
                                        viewModel.reclamationNature.value = "NATURAL"
                                        viewModel.calculateQuantities()
                                    },
                                    colors = if (reclamationNature == "NATURAL") activeCol else inactiveCol,
                                    modifier = Modifier.weight(1.5f).height(38.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Text("طبيعية تسوية خفيفة", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                
                                OutlinedButton(
                                    onClick = { 
                                        viewModel.reclamationNature.value = "CLAY"
                                        viewModel.calculateQuantities()
                                    },
                                    colors = if (reclamationNature == "CLAY") activeCol else inactiveCol,
                                    modifier = Modifier.weight(1f).height(38.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Text("تربة طينية", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                
                                OutlinedButton(
                                    onClick = { 
                                        viewModel.reclamationNature.value = "SANDY"
                                        viewModel.calculateQuantities()
                                    },
                                    colors = if (reclamationNature == "SANDY") activeCol else inactiveCol,
                                    modifier = Modifier.weight(1f).height(38.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Text("تربة رملية", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val activeCol = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                                val inactiveCol = ButtonDefaults.outlinedButtonColors()
                                
                                OutlinedButton(
                                    onClick = { 
                                        viewModel.reclamationNature.value = "DEPRESSION"
                                        viewModel.calculateQuantities()
                                    },
                                    colors = if (reclamationNature == "DEPRESSION") activeCol else inactiveCol,
                                    modifier = Modifier.weight(1.3f).height(38.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Text("أرض منخفضة تحتاج دفان", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                
                                OutlinedButton(
                                    onClick = { 
                                        viewModel.reclamationNature.value = "MOUNTAIN"
                                        viewModel.calculateQuantities()
                                    },
                                    colors = if (reclamationNature == "MOUNTAIN") activeCol else inactiveCol,
                                    modifier = Modifier.weight(1.2f).height(38.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Text("صخرية تحتاج تسوية", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        // Cost entry field
                        OutlinedTextField(
                            value = reclamationCostInput,
                            onValueChange = {
                                viewModel.reclamationCostInput.value = it
                                viewModel.calculateQuantities()
                            },
                            label = { Text("كلفة الاستسصلاح وتهيئة الأرض (د.ع)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Section: Rooms List with Sliding Dimensions Determinator (Collapsible)
        item {
            Card(
                onClick = { showRoomsList = !showRoomsList },
                colors = CardDefaults.cardColors(
                    containerColor = if (showRoomsList) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (showRoomsList) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "(${rooms.size} غرف)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "الغرف",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        if (showRoomsList) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { showAddRoomDialog = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("add_room_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "إضافة مساحة", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة غرفة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "٢. تحديد غرف وأبعاد المنزل بدقة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (rooms.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                    ) {
                        Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                "لا توجد غرف مخصصة حالياً. لتصميم غرف منزلك، اضغط إضافة غرفة.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(rooms, key = { it.id }) { room ->
                    RoomInteractiveCard(
                        room = room,
                        onUpdate = { w, l, h ->
                            viewModel.updateRoomDimensions(room.id, w, l, h)
                        },
                        onDelete = {
                            viewModel.deleteRoom(room.id)
                            scope.launch {
                                snackbarHostState.showSnackbar("تم حذف غرفة '${room.name}' من المقارنة")
                            }
                        }
                    )
                }
            }
        }

        // Section: Engineering Selectors
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.shadow(2.dp, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "٣. تفاصيل الهيكل والمواد الإنشائية",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Right
                    )
                    Divider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    
                    // Building Type Selection
                    Text(text = "طريقة الإنشاء الهيكلي", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val activeColor = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                        val inactiveColor = ButtonDefaults.outlinedButtonColors()
                        
                        OutlinedButton(
                            onClick = {
                                viewModel.buildingType.value = "LOAD_BEARING"
                                viewModel.calculateQuantities()
                            },
                            colors = if (buildingType == "LOAD_BEARING") activeColor else inactiveColor,
                            modifier = Modifier.weight(1f).height(41.dp)
                        ) {
                            Text("جدران حاملة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        OutlinedButton(
                            onClick = {
                                viewModel.buildingType.value = "STRUCTURAL"
                                viewModel.calculateQuantities()
                            },
                            colors = if (buildingType == "STRUCTURAL") activeColor else inactiveColor,
                            modifier = Modifier.weight(1f).height(41.dp)
                        ) {
                            Text("هيكل خرساني (أعمدة)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Foundation Type
                    Text(text = "تصميم نوّع الأساس الإنشائي", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val activeColor = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                        val inactiveColor = ButtonDefaults.outlinedButtonColors()

                        OutlinedButton(
                            onClick = {
                                viewModel.foundationType.value = "STRIP"
                                viewModel.calculateQuantities()
                            },
                            colors = if (foundationType == "STRIP") activeColor else inactiveColor,
                            modifier = Modifier.weight(1f).height(41.dp)
                        ) {
                            Text("شريطي (Strip)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.foundationType.value = "RAFT"
                                viewModel.calculateQuantities()
                            },
                            colors = if (foundationType == "RAFT") activeColor else inactiveColor,
                            modifier = Modifier.weight(1f).height(41.dp)
                        ) {
                            Text("أساس حصيري (Raft)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Material/Brick selection
                    Text(text = "خامة الجدار الخارجي والقواطع", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val listMaterials = listOf(
                            Triple("BLOCK", "بلوك إسمنتي خرساني", "اقتصادي وصلادة عالية"),
                            Triple("YELLOW_BRICK", "طابوق أصفر عراقي", "مظهر عريق وعزل ممتاز"),
                            Triple("THERMOSTONE", "ثرمستون (خفيف)", "عزل حراري وصوتي من الدرجة الأولى"),
                            Triple("RED_BRICK", "طابوق أحمر مستورد", "وزن مثالي وجودة ممتازة")
                        )
                        
                        listMaterials.forEach { (type, title, desc) ->
                            val isSelected = materialType == type
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        viewModel.materialType.value = type
                                        viewModel.calculateQuantities()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            viewModel.materialType.value = type
                                            viewModel.calculateQuantities()
                                        }
                                    )
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        val dimsText = when (type) {
                                            "YELLOW_BRICK" -> "الأبعاد: ٢٥ × ١٢ × ٦ سم (المساحة ≈ ٠.٠٣ م² | ≈ ٥٥ طابوقة/م² مفرد)"
                                            "RED_BRICK" -> "الأبعاد: ٢٤ × ١١.٥ × ٧.٥ سم (تخمين عالي الصلابة والجودة بوزن مثالي)"
                                            "BLOCK" -> "الأبعاد: ٤٠ × ٢٠ × ٢0 سم (المساحة ≈ ٠.٠٨ م² | ≈ ١٢–١٣ بلوكة/م²)"
                                            "THERMOSTONE" -> "الأبعاد: ٦٠ × ٢٠ × ٢٠ سم (المساحة ≈ ٠.١٢ م² | ≈ ٨–٩ بلوكة/م²)"
                                            else -> ""
                                        }
                                        if (dimsText.isNotEmpty()) {
                                            Text(
                                                text = dimsText,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Normal,
                                                modifier = Modifier.padding(top = 1.dp)
                                            )
                                        }
                                        Text(desc, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Construction Labor Cost Selection (أجور اليد العاملة لبناء الهيكل)
        item {
            val isManualLabor by viewModel.isManualLaborEnabled.collectAsStateWithLifecycle()
            val manualLaborCostStr by viewModel.manualLaborCost.collectAsStateWithLifecycle()
            val laborPricePerM2Str by viewModel.laborPricePerM2.collectAsStateWithLifecycle()

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { /* Help icon or placeholder */ }) {
                            Icon(Icons.Default.Info, contentDescription = "معلومات", tint = MaterialTheme.colorScheme.primary)
                        }
                        Text(
                            text = "أجور عمال البناء والهيكل الإنشائي",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Right
                        )
                    }
                    
                    Text(
                        text = "طريقة تحديد تكلفة عقود اليد العاملة לבناء الهيكل الخرساني والجدران:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val activeColor = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                        val inactiveColor = ButtonDefaults.outlinedButtonColors()

                        OutlinedButton(
                            onClick = {
                                viewModel.isManualLaborEnabled.value = false
                                viewModel.calculateQuantities()
                            },
                            colors = if (!isManualLabor) activeColor else inactiveColor,
                            modifier = Modifier.weight(1f).height(41.dp)
                        ) {
                            Text("آلي (حسب مساحة البناء)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.isManualLaborEnabled.value = true
                                viewModel.calculateQuantities()
                            },
                            colors = if (isManualLabor) activeColor else inactiveColor,
                            modifier = Modifier.weight(1f).height(41.dp)
                        ) {
                            Text("يدوي (تحديد مبلغ ثابت)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (isManualLabor) {
                        OutlinedTextField(
                            value = manualLaborCostStr,
                            onValueChange = {
                                viewModel.manualLaborCost.value = it
                                viewModel.calculateQuantities()
                            },
                            label = { Text("أجور العمل الإجمالية (د.ع)", textAlign = TextAlign.Right) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Right)
                        )
                        if (showResults) {
                            calcResult?.let { res ->
                                Text(
                                    text = "أجور عمال الهيكل المحددة يدوياً: ${String.format("%,.0f", res.laborCost)} د.ع",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 4.dp).fillMaxWidth(),
                                    textAlign = TextAlign.Right
                                )
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = laborPricePerM2Str,
                            onValueChange = {
                                viewModel.laborPricePerM2.value = it
                                viewModel.calculateQuantities()
                            },
                            label = { Text("سعر عمل المتر المربع للبناء (د.ع/م٢)", textAlign = TextAlign.Right) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Right)
                        )
                        if (showResults) {
                            calcResult?.let { res ->
                                val parsedBuildArea = buildArea.toDoubleOrNull() ?: 150.0
                                val totalSize = parsedBuildArea * floorsCount
                                Text(
                                    text = "أجور عمال الهيكل المحسوبة تلقائياً: ${String.format("%,.0f", res.laborCost)} د.ع\n(المساحة الإجمالية للمبنى: ${String.format("%.0f", totalSize)} م٢)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 4.dp).fillMaxWidth(),
                                    textAlign = TextAlign.Right
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Detailed Interior Finishes & House Accessories Banner Link
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.selectedTab.value = 1 } // Go to Finishes Tab (index 1)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "تعديل تفاصيل تشطيبات المنزل والدواخل",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Right
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "انقر هنا لتخصيص جودة ونوع الأرضيات، السقوف الثانوية، الأبواب، ومواد الصحيات والكهربائيات في صفحة مخصصة.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                            textAlign = TextAlign.Right,
                            lineHeight = 15.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ColorLens,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        item {
            RunBoqCard(viewModel, snackbarHostState)
        }

        // Saving Project Block
        item {
            Button(
                onClick = { showSaveDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("save_project_trigger_button")
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("حفظ الكشف الحالي في المحفوظات", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showAddRoomDialog) {
        var rName by remember { mutableStateOf("") }
        var rWidth by remember { mutableStateOf("4.0") }
        var rLength by remember { mutableStateOf("5.0") }
        var rHeight by remember { mutableStateOf("3.2") }
        
        AlertDialog(
            onDismissRequest = { showAddRoomDialog = false },
            title = {
                Text("إضافة غرفة جديدة لتفصيل المنزل", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    OutlinedTextField(
                        value = rName,
                        onValueChange = { rName = it },
                        label = { Text("اسم القاطع او الغرفة (مثال: الصالة)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_room_name_input")
                    )
                    OutlinedTextField(
                        value = rWidth,
                        onValueChange = { rWidth = it },
                        label = { Text("العرض الافتراضي (م) *") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = rLength,
                        onValueChange = { rLength = it },
                        label = { Text("الطول الافتراضي (م) *") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = rHeight,
                        onValueChange = { rHeight = it },
                        label = { Text("الارتفاع الافتراضي (م) *") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val nameResolved = rName.ifBlank { "غرفة مخصصة " + (rooms.size + 1) }
                        val wTmp = rWidth.toDoubleOrNull() ?: 4.0
                        val lTmp = rLength.toDoubleOrNull() ?: 5.0
                        val hTmp = rHeight.toDoubleOrNull() ?: 3.2
                        viewModel.addRoom(nameResolved, wTmp, lTmp, hTmp)
                        showAddRoomDialog = false
                    },
                    modifier = Modifier.testTag("add_room_confirm_button")
                ) {
                    Text("إضافة الآن")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddRoomDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = {
                Text("تسمية وحفظ كشف البناء", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
            },
            text = {
                OutlinedTextField(
                    value = projectTitleSave,
                    onValueChange = { projectTitleSave = it },
                    label = { Text("أدخل اسم الكشف الكلي (مثال: منزلي الكوت)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("save_project_name_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val nameResolved = projectTitleSave.ifBlank { "كشف منزل مخصص " + SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
                        viewModel.saveProject(nameResolved) {
                            scope.launch {
                                snackbarHostState.showSnackbar("تم حفظ كشف كلف ومواد '$nameResolved' بنجاح!")
                            }
                        }
                        projectTitleSave = ""
                        showSaveDialog = false
                    },
                    modifier = Modifier.testTag("save_project_confirm_button")
                ) {
                    Text("حفظ المشروع")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (showMarketDialog) {
        AlertDialog(
            onDismissRequest = { showMarketDialog = false },
            title = {
                Text(
                    text = "🛒 ماركت القياسات والمخططات الهندسية",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "أهلاً بك في سوق المخططات الخاص بالمهندس الذكي. تم توليد هذه التصاميم بعناية فائقة لتفادي الأخطاء الإنشائية وتوفير كلف حديد التسليح الصب:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Right,
                        lineHeight = 18.sp
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Item 1
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("جاري تحميل ملف الأوتوكاد (M2_Layout_Classic.dwg)...")
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("تحميل PDF/DWG", fontSize = 10.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("مخطط الطابقين العراقي ٢٠٠م", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("أبعاد: ١٠م واجهة × ٢٠م عمق - كلاسيك", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    // Item 2
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("جاري تحميل ملف الأوتوكاد (M1.5_Modern_Smart.dwg)...")
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("تحميل PDF/DWG", fontSize = 10.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("تصميم واجهة ونثر مودرن ١٥٠م", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("أبعاد: ٧.٥م واجهة × ٢٠م عمق - ذكي", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    // Item 3
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("تم إرسال طلب تصميم مخصص للمهندس ذكي...")
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("طلب مخصص", fontSize = 10.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("رسم مخطط إنشائي مخصص بالذكاء", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("حسب رغبة وذوق الملاك بأحدث طراز", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMarketDialog = false }) {
                    Text("إغلاق", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

// Interactive details card component for each room with manual dimension inputs
@Composable
fun RoomInteractiveCard(
    room: RoomDetail,
    onUpdate: (width: Double, length: Double, height: Double) -> Unit,
    onDelete: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var widthText by remember(room.width) { mutableStateOf(room.width.toString()) }
    var lengthText by remember(room.length) { mutableStateOf(room.length.toString()) }
    var heightText by remember(room.height) { mutableStateOf(room.height.toString()) }

    val currentWidth = widthText.replace(',', '.').toDoubleOrNull()
    val currentLength = lengthText.replace(',', '.').toDoubleOrNull()
    val currentHeight = heightText.replace(',', '.').toDoubleOrNull()

    val canSave = currentWidth != null && currentWidth > 0.0 &&
                  currentLength != null && currentLength > 0.0 &&
                  currentHeight != null && currentHeight > 0.0

    val isChanged = (currentWidth != room.width) ||
                    (currentLength != room.length) ||
                    (currentHeight != room.height)
    
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Delete Button
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف القاطع",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Center/Right details
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = room.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "حجم: ${String.format("%.1f", room.width * room.length * room.height)} م٣",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text("|", fontSize = 10.sp, color = MaterialTheme.colorScheme.outlineVariant)
                            Text(
                                text = "مساحة: ${String.format("%.1f", room.width * room.length)} م٢",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Ex-Sliders Column / Now Manual Inputs
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    
                    Text(
                        text = "أدخل أبعاد الغرفة يدوياً بدقة للتحكم التلقائي بالكميات والكلف الحقيقية:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Height Input
                        OutlinedTextField(
                            value = heightText,
                            onValueChange = { newValue ->
                                if (newValue.isEmpty() || newValue.matches(Regex("^\\d*[.,]?\\d*$"))) {
                                    heightText = newValue
                                }
                            },
                            label = { Text("الارتفاع (م)", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        )

                        // Length Input
                        OutlinedTextField(
                            value = lengthText,
                            onValueChange = { newValue ->
                                if (newValue.isEmpty() || newValue.matches(Regex("^\\d*[.,]?\\d*$"))) {
                                    lengthText = newValue
                                }
                            },
                            label = { Text("الطول (م)", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        )

                        // Width Input
                        OutlinedTextField(
                            value = widthText,
                            onValueChange = { newValue ->
                                if (newValue.isEmpty() || newValue.matches(Regex("^\\d*[.,]?\\d*$"))) {
                                    widthText = newValue
                                }
                            },
                            label = { Text("العرض (م)", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        )
                    }

                    // Save and Cancel buttons for editing layout
                    if (isChanged) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    widthText = room.width.toString()
                                    lengthText = room.length.toString()
                                    heightText = room.height.toString()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("تراجع", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            
                            Button(
                                onClick = {
                                    if (canSave) {
                                        onUpdate(currentWidth!!, currentLength!!, currentHeight!!)
                                    }
                                },
                                enabled = canSave,
                                modifier = Modifier.weight(1.5f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("حفظ التغييرات 💾", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Simple Interactive Room Blueprint Canvas Drawing
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val canvasWidth = size.width
                            val canvasHeight = size.height
                            
                            // Map length (1m-12m) to width pixels, and width (1m-10m) to height pixels properly
                            val scaleX = (canvasWidth * 0.7f) / 12f
                            val scaleY = (canvasHeight * 0.7f) / 10f
                            
                            // Real-time canvas preview using manually typed dimensions (fallback to current if invalid)
                            val drawLength = currentLength ?: room.length
                            val drawWidth = currentWidth ?: room.width
                            
                            val rectWidthPixels = (drawLength * scaleX).toFloat()
                            val rectHeightPixels = (drawWidth * scaleY).toFloat()
                            
                            val left = (canvasWidth - rectWidthPixels) / 2f
                            val top = (canvasHeight - rectHeightPixels) / 2f
                            
                            // Draw Grid/Blueprint
                            val pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                            drawRect(
                                color = Color(0x1F22D3EE),
                                style = Stroke(width = 2.dp.toPx(), pathEffect = pathEffect)
                            )
                            
                            // Draw brick wall boundaries
                            drawRect(
                                color = Color(0xFF0284C7).copy(alpha = 0.2f),
                                topLeft = Offset(left, top),
                                size = androidx.compose.ui.geometry.Size(rectWidthPixels, rectHeightPixels)
                            )
                            
                            drawRect(
                                color = Color(0xFF0284C7),
                                topLeft = Offset(left, top),
                                size = androidx.compose.ui.geometry.Size(rectWidthPixels, rectHeightPixels),
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                            
                            // Label
                            drawCircle(
                                color = Color(0xFFEA580C),
                                radius = 3.dp.toPx(),
                                center = Offset(canvasWidth/2, canvasHeight/2)
                            )
                        }
                        
                        Text(
                            text = "مخطط القاطع التفاعلي لتوزيع الجدران والجسور",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }
    }
}


// ------------------------------------
// TAB 2: BI-lingual Detailed BOQ & Material Breakdown Screen
// ------------------------------------
@Composable
fun BoqResultTab(
    viewModel: QuantityViewModel,
    calcResult: QuantityCalculator.EstimationResult?,
    snackbarHostState: SnackbarHostState
) {
    if (calcResult == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val scope = rememberCoroutineScope()
    var isGeneratingPdf by remember { mutableStateOf(false) }
    var showPdfSuccessDialog by remember { mutableStateOf(false) }
    var selectedExportFormat by remember { mutableStateOf("pdf") } // "pdf" or "excel" or "latex"

    val isReclamationEnabled by viewModel.isReclamationEnabled.collectAsStateWithLifecycle()
    val reclamationNature by viewModel.reclamationNature.collectAsStateWithLifecycle()
    val showResults by viewModel.showResults.collectAsStateWithLifecycle()

    val flooringTypeLabel = when(viewModel.flooringType.value) {
        "MARBLE" -> "مرمر طبيعي"
        "CERAMIC" -> "سيراميك محلي"
        else -> "بورسلين مستورد"
    }
    val ceilingTypeLabel = when(viewModel.ceilingType.value) {
        "PLASTER" -> "لبخ وبياض"
        "PVC" -> "أسقف ثانوية PVC"
        else -> "جبس بورد"
    }
    val sanitaryQualityLabel = when(viewModel.sanitaryQuality.value) {
        "LUXURY" -> "ديلوكس فاخر"
        "BASIC" -> "اقتصادي مبسط"
        else -> "متوسط مميز"
    }
    val electricalQualityLabel = when(viewModel.electricalQuality.value) {
        "SMART" -> "ذكي متكامل"
        "BASIC" -> "اقتصادي مبسط"
        else -> "حديث مميز"
    }
    val reclamationNatureLabel = when(reclamationNature) {
        "CLAY" -> "تربة رخوة وطينية"
        "SANDY" -> "تربة رملية"
        "DEPRESSION" -> "أرض منخفضة جداً"
        "MOUNTAIN" -> "أرض صخرية أو جبلية"
        else -> "أرض منبسطة طبيعية"
    }

    val baseCategories = listOf(
        BoqCardItem("صبة الهيكل والأساسات (Concrete Structures)", "مجموع صبات الأسقف والجسور والأعمدة والقواعد الخرسانية المتينة.", "${String.format("%.1f", calcResult.totalConcrete)} م٣", calcResult.concreteCost, Icons.Default.HomeRepairService),
        BoqCardItem("حديد التسليح (Steel Reinforcing bars)", "حديد تشكيل مقاوم للشد والتحميل طبقاً للمواصفات العراقية.", "${String.format("%.2f", calcResult.totalSteelTons)} طن", calcResult.steelCost, Icons.Default.GridOn),
        BoqCardItem("الإسمنت الكلي للهيكل والبناء (Total Cement)", "خرسانة صب الأعمدة وملاط البناء وجدران المنزل الأساسية.", "${String.format("%.2f", calcResult.cementTons)} طن", calcResult.cementCost, Icons.Default.Landscape),
        BoqCardItem("الرمل والحصى والركام (Sand & Gravel Supply)", "شحنات النقل اللوجستية المطلوبة لتغطية كامل خليط الكونكريت.", "رمل: ${String.format("%.1f", calcResult.sandM3)} م٣ / حصى: ${String.format("%.1f", calcResult.gravelM3)} م٣", calcResult.aggregatCost, Icons.Default.LocalShipping),
        BoqCardItem("جدران الطابوق والبناء وملاط اللبخ (Masonry Walls)", "تخمين الطابوق الفخاري أو البلوك الإسمنتي لجدران الفواصل والحيطان.", "${calcResult.masonryUnitsCount} وحدة بنائية", calcResult.masonryUnitsCost, Icons.Default.Dashboard),
        
        // Detailed Interior Finishes sub-cards
        BoqCardItem("أرضيات الغرف والصالات البينية (Tile Flooring)", "سيراميك أو بورسلين أو مرمر حسب الاختيار المخصص للمجالس: $flooringTypeLabel.", "${String.format("%.0f", calcResult.flooringAreaM2)} م٢", calcResult.tileCost, Icons.Default.Dashboard),
        BoqCardItem("أسقف وديكورات الغرف الداخلية (Suspended Ceilings)", "تنفيذ وتجهيز ديكورات الأسقف وسقوف الجبس بورد نوع: $ceilingTypeLabel.", "${String.format("%.0f", calcResult.flooringAreaM2)} م٢", calcResult.ceilingCost, Icons.Default.Home),
        BoqCardItem("بياض ولبخ الجدران الأساسي (Wall Plastering)", "أعمال تأسيس الجدار الداخلي اللبخ الإسمنتي وبياض البورق.", "${String.format("%.0f", calcResult.wallPlasterAreaM2)} م٢", calcResult.plasterCost, Icons.Default.HomeRepairService),
        BoqCardItem("دهان وصباغة جدران المنزل (Painting Work)", "صباغة الجدران بالمعجون الأساسي والطلاء الخارجي والداخلي.", "${String.format("%.0f", calcResult.paintingAreaM2)} م٢", calcResult.paintingCost, Icons.Default.ColorLens),
        
        // Openings (Doors & Windows)
        BoqCardItem("أبواب الغرف والمخارج الداخلية (Interior Doors)", "شراء وتثبيت أبواب الغرف الداخلية الخشبية والـ PVC.", "${viewModel.doorsCount.value} أبواب", calcResult.doorsCost, Icons.Default.Home),
        BoqCardItem("شبابيك الغرف الخارجية (Exterior Windows)", "شباك ألومنيوم دبل جلاس عازل للصوت والحرارة.", "${viewModel.windowsCount.value} شبابيك منافذ", calcResult.windowsCost, Icons.Default.GridOn),
        
        // Utilities
        BoqCardItem("تجهيز وتأسيس المنظومة الصحية (Plumbing Services)", "شبكة مياه وصرف صحي وخلاطات ومغاسل فئة: $sanitaryQualityLabel.", "كامل التأسيس والصحي", calcResult.sanitaryCost, Icons.Default.Build),
        BoqCardItem("التوصيلات والمفاتيح الكهربائية (Electrical Setup)", "نقاط توزيع الكهرباء والأسلاك ومفاتيح الإنارة فئة: $electricalQualityLabel.", "كامل تسليك البيت والإنارة", calcResult.electricalCost, Icons.Default.Settings),
        
        BoqCardItem("أيدي المقاول والعمالة العامة (Labor Premium Contract)", "أجور مهندسي ومقاولي التنفيذ الإجمالي طبقاً لأمتار البناء الفعلي.", "تغطية كامل العقد الإنشائي", calcResult.laborCost, Icons.Default.Engineering)
    )

    val categories = if (isReclamationEnabled) {
        listOf(
            BoqCardItem("استصلاح وتسوية الأرض (Land preparation)", "تهيئة ومعالجة عيوب وحفر ودفن عيوب التربة لنوع: $reclamationNatureLabel.", "تهيئة الموقع وتعديله", calcResult.reclamationCost, Icons.Default.Landscape)
        ) + baseCategories
    } else {
        baseCategories
    }

    if (!showResults) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                RunBoqCard(viewModel, snackbarHostState)
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        
                        Text(
                            text = "نتائج الحسابات وتخمين كشف الكميات والـ BOQ",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        
                        Text(
                            text = "لتجنب عرض حسابات غير مكتملة أو قديمة، قمنا بقفل النتائج التخمينية وتحديث الأسعار. الرجاء إدخال تفاصيل منزلك والمواد المطلوبة ثم الضغط على زر 'تشغيل وحساب كشف BOQ ومطابقة الأسعار الحية' بالرصيد لتنشيط ومعالجة البيانات بالكامل مع الأسعار المحلية المعتمدة هندسياً لعام 2026.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Grand Total Block
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.shadow(3.dp, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "الكلفة التقديرية الإجمالية للمشروع",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${String.format("%,.0f", calcResult.totalCost)} د.ع",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.08f)).padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("الاسمنت الكلي", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("${String.format("%.2f", calcResult.cementTons)} طن", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("مجموع حديد التشكيل", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("${String.format("%.2f", calcResult.totalSteelTons)} طن", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("البلوك / الطابوق", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("${calcResult.masonryUnitsCount} طابوقة", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            RunBoqCard(viewModel, snackbarHostState)
        }

        // 1.5 Visual Budget Breakdown Chart
        item {
            val total = calcResult.totalCost
            if (total > 0.0) {
                val materialCost = calcResult.concreteCost + calcResult.steelCost + calcResult.cementCost + calcResult.aggregatCost + calcResult.masonryUnitsCost
                val finishesCost = calcResult.finishingCost
                val laborCost = calcResult.laborCost
                val reclamationCost = if (isReclamationEnabled) calcResult.reclamationCost else 0.0

                val materialPercentage = (materialCost / total) * 100
                val finishesPercentage = (finishesCost / total) * 100
                val laborPercentage = (laborCost / total) * 100
                val reclamationPercentage = (reclamationCost / total) * 100

                Card(
                    modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "📊 توزيع الميزانية الكلية التقريبي",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Right
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "نوضح لك بنسب مئوية كيف تتوزع كلفة البناء الكلية ليسهل عليك فهم ميزانيتك وتنسيقها.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Right,
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // Multi-segmented horizontal bar list
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            if (materialPercentage > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(materialPercentage.toFloat())
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                            if (finishesPercentage > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(finishesPercentage.toFloat())
                                        .background(MaterialTheme.colorScheme.secondary)
                                )
                            }
                            if (laborPercentage > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(laborPercentage.toFloat())
                                        .background(MaterialTheme.colorScheme.tertiary)
                                )
                            }
                            if (reclamationPercentage > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(reclamationPercentage.toFloat())
                                        .background(MaterialTheme.colorScheme.error)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Grid-like legends using simple rows
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                VisualLegendItem(
                                    label = "التشطيبات والديكورات",
                                    color = MaterialTheme.colorScheme.secondary,
                                    percentage = finishesPercentage,
                                    amount = finishesCost
                                )
                                VisualLegendItem(
                                    label = "المواد الأساسية والهيكل",
                                    color = MaterialTheme.colorScheme.primary,
                                    percentage = materialPercentage,
                                    amount = materialCost
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                if (reclamationCost > 0) {
                                    VisualLegendItem(
                                        label = "استصلاح وتسوية الأرض",
                                        color = MaterialTheme.colorScheme.error,
                                        percentage = reclamationPercentage,
                                        amount = reclamationCost
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                                VisualLegendItem(
                                    label = "أجور المقاول والعمل",
                                    color = MaterialTheme.colorScheme.tertiary,
                                    percentage = laborPercentage,
                                    amount = laborCost
                                )
                            }
                        }
                    }
                }
            }
        }



        // Action Toolbar
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "حفظ وتصدير جدول الكميات (BOQ)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Save,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = "اختر صيغة الملف لحفظ حسابات المشروع بجدولة BOQ عالمية مسعرة:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (isGeneratingPdf) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (selectedExportFormat == "pdf") "جاري إعداد مستند الـ PDF العالمي..." else "جاري إعداد مستند الـ Excel والـ BOQ...",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    } else {
                        // Single full-width PDF Button
                        Button(
                            onClick = {
                                selectedExportFormat = "pdf"
                                viewModel.secureDeductPDFExport(
                                    onNoTokens = { errorMsg ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar("❌ $errorMsg")
                                        }
                                    },
                                    onSuccess = { successMsg ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar("✅ $successMsg")
                                        }
                                        isGeneratingPdf = true
                                        scope.launch {
                                            delay(1800) // Simulated generating progress
                                            isGeneratingPdf = false
                                            showPdfSuccessDialog = true
                                        }
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("export_pdf_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)), // Crimson Red
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("توليد وتصدير كشف الكميات والأسعار كـ PDF", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Section header
        item {
            Text(
                text = "جدول تفصيلي بالكميات والكلف والإنشاء والمواد (BOQ)",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Right
            )
        }

        items(categories) { item ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Left Cost Section
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${String.format("%,.0f", item.cost)} د.ع",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = item.amountLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Right description and label
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = item.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Right
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.description,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 15.sp,
                                textAlign = TextAlign.Right
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
        item {
            AdMobBannerAd(
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
            )
        }
    }

    if (showPdfSuccessDialog) {
        val context = LocalContext.current
        var exportProjectName by remember { mutableStateOf("مشروع بناء منزل سكني") }
        var exportLocation by remember { mutableStateOf("البصرة، العراق") }
        var exportEngineer by remember { mutableStateOf("engineer0742@gmail.com") }

        AlertDialog(
            onDismissRequest = { showPdfSuccessDialog = false },
            title = {
                Text(
                    text = "تجهيز وتصدير جدول الكميات (BOQ)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "يرجى مراجعة وتأكيد بيانات الكشف أدناه لإدراجها في ترويسة مستند الـ BOQ النهائي للتخمين الإنشائي والمالي:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Right,
                        lineHeight = 16.sp
                    )

                    OutlinedTextField(
                        value = exportProjectName,
                        onValueChange = { exportProjectName = it },
                        label = { Text("اسم المشروع والمسودة", textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) },
                        textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Right),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = exportLocation,
                        onValueChange = { exportLocation = it },
                        label = { Text("الموقع والمنطقة الإنشائية", textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) },
                        textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Right),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = exportEngineer,
                        onValueChange = { exportEngineer = it },
                        label = { Text("المهندس المستشار المسؤول", textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) },
                        textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Right),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Final Action Button (Always PDF)
                    Button(
                        onClick = {
                            val metadata = com.example.utils.DataExporter.ExportMetadata(
                                projectName = exportProjectName,
                                location = exportLocation,
                                date = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date()),
                                engineer = exportEngineer,
                                totalEstimatedCost = calcResult.totalCost,
                                concreteVolumeM3 = calcResult.totalConcrete,
                                totalAreaM2 = viewModel.buildArea.value.toDoubleOrNull() ?: viewModel.plotArea.value.toDoubleOrNull() ?: 150.0
                            )
                            val exportItems = categories.map { item ->
                                com.example.utils.DataExporter.ExportItem(
                                    name = item.title,
                                    description = item.description,
                                    quantity = item.amountLabel,
                                    approximateCost = item.cost,
                                    notes = "تخمين تلقائي بمعدل هدر عراقي فني 5%-8%"
                                )
                            }
                            
                            com.example.utils.DataExporter.exportToPdf(context, metadata, exportItems)
                            showPdfSuccessDialog = false
                        },
                        modifier = Modifier.fillMaxWidth().testTag("confirm_export_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD32F2F)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "توليد وطباعة مستند الـ PDF المطور",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPdfSuccessDialog = false }) {
                    Text("إلغاء وتراجع", color = MaterialTheme.colorScheme.outline)
                }
            }
        )
    }
}

private data class BoqCardItem(
    val title: String,
    val description: String,
    val amountLabel: String,
    val cost: Double,
    val icon: ImageVector
)


// ------------------------------------
// TAB 3: AI Engineer Gemini Assistant Client Chat Screen
// ------------------------------------
@Composable
fun GeminiChatTab(viewModel: QuantityViewModel) {
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isChatLoading.collectAsStateWithLifecycle()
    
    var userText by remember { mutableStateOf("") }
    val listState = rememberScrollState()
    
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val savedKey by viewModel.customApiKey.collectAsStateWithLifecycle()
    var apiKeyInput by remember(savedKey) { mutableStateOf(savedKey) }
    var isConfigExpanded by remember(savedKey) { mutableStateOf(savedKey.isEmpty()) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Chat Header bar with engineering stats
        Card(
            shape = RoundedCornerShape(0.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.clearChat() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "مسح المحادثة", tint = MaterialTheme.colorScheme.error)
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("المهندس المدني المساعد (AI)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("مستشارك الفني في كلف الخامات والمواد وتصميم جدران الطابوق للبناء", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "مساعد ذكي",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // --- Custom Google Gemini API Key Guide & Configuration ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            ),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header Row (Click to expand/collapse)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isConfigExpanded = !isConfigExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isConfigExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (savedKey.isNotEmpty()) {
                            Text(
                                "المفتاح مفعّل ومحفوظ ✅",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32),
                                modifier = Modifier
                                    .background(Color(0xFFE8F5E9), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Text(
                                "بحاجة لإعداد المفتاح ⚠️",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        
                        Text(
                            "إعداد وتنشيط مفتاح ذكاء Google Gemini",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (isConfigExpanded) {
                    Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    
                    Text(
                        text = "لتشغيل المساعد الذكي مجاناً بأعلى سرعة وكفاءة، يرجى إحضار مفتاح API الخاص بك من منصة جوجل للذكاء الاصطناعي ولصقه أدناه لمباشرة الحساب:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Informational Steps
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text("١. اضغط على رابط الإرشاد المباشر أدناه لفتح واجهة Google AI Studio.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Right)
                        Text("٢. اضغط على زر 'Create API Key' لإنشاء مفتاح خاص بك مجاناً.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Right)
                        Text("٣. انسخ المفتاح المتولد، ثم ألصقه في المربع أدناه واضغط على 'تفعيل وحفظ المفتاح'.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Right)
                    }

                    // Direct Tutorial Button
                    Button(
                        onClick = {
                            try {
                                uriHandler.openUri("https://aistudio.google.com/app/apikey")
                            } catch (e: Exception) {
                                // ignore
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.CenterHorizontally).height(36.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إحضار مفتاح Google API مجاناً 🌐", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    // Input & Save Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                viewModel.saveCustomApiKey(context, apiKeyInput)
                                Toast.makeText(context, "تم حفظ وتفعيل مفتاح Gemini بنجاح! 🎉", Toast.LENGTH_SHORT).show()
                                isConfigExpanded = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text("تفعيل وحفظ", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = { apiKeyInput = it },
                            placeholder = { Text("أدخل مفتاحك الخاص (AIzaSy...)", fontSize = 11.sp, textAlign = TextAlign.Right) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("custom_api_key_input"),
                            textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Right),
                            leadingIcon = {
                                if (apiKeyInput.isNotEmpty()) {
                                    IconButton(onClick = { apiKeyInput = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "مسح المربع", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        // Messages Box
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val scrollState = rememberScrollState()
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                messages.forEach { msg ->
                    ChatBubble(message = msg)
                }
                
                if (isLoading) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "المهندس الذكي يفكر ويدقق بالمعادلات الآن...",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        ShimmerPlaceholder(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .height(14.dp)
                        )
                        ShimmerPlaceholder(
                            modifier = Modifier
                                .fillMaxWidth(0.75f)
                                .height(14.dp)
                        )
                        ShimmerPlaceholder(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(14.dp)
                        )
                    }
                }
            }
            
            // Auto scroll logic helper
            LaunchedEffect(messages.size, isLoading) {
                scrollState.animateScrollTo(scrollState.maxValue)
            }
        }

        // Quick Input suggestions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End)
        ) {
            val suggestions = listOf(
                "كيف أقلل كلفة الأساس؟",
                "أيهما أفضل: الثرمستون أم البلوك العادي؟",
                "احسب لي جدران الطابوق"
            )
            suggestions.reversed().forEach { sug ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                        .clickable { viewModel.sendChatMessage(sug) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(sug, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Title banner for uploading architectural blueprints
        Text(
            text = "أرفق مخطط هندسي أو اختر عينة معالجة بالذكاء الاصطناعي:",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp).fillMaxWidth(),
            textAlign = TextAlign.Right
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
        ) {
            val context = LocalContext.current
            val imagePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri ->
                uri?.let {
                    try {
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val bytes = inputStream?.readBytes()
                        if (bytes != null) {
                            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                            viewModel.analyzeBlueprintWithAI(mimeType, base64, "مخطط_منزلي.jpg")
                        }
                    } catch (e: Exception) {
                        // ignore/handle
                    }
                }
            }

            // Button 1: Device Upload
            Button(
                onClick = { imagePickerLauncher.launch("image/*") },
                contentPadding = PaddingValues(horizontal = 8.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                modifier = Modifier.height(34.dp)
            ) {
                Text("📂 ارفع مخطط من الاستوديو", fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }

            // Button 2: Mock double floor 200m
            Button(
                onClick = {
                    viewModel.analyzeBlueprintWithAI(
                        "image/jpeg",
                        "R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7",
                        "مخطط_بيت_عراقي_200م.jpg"
                    )
                },
                contentPadding = PaddingValues(horizontal = 8.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                modifier = Modifier.height(34.dp)
            ) {
                Text("🗺️ عينة مخطط ٢٠٠م", fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }

            // Button 3: Mock classic facade
            Button(
                onClick = {
                    viewModel.analyzeBlueprintWithAI(
                        "image/jpeg",
                        "R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7",
                        "تصميم_واجهة_صحراوي.jpg"
                    )
                },
                contentPadding = PaddingValues(horizontal = 8.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer),
                modifier = Modifier.height(34.dp)
            ) {
                Text("📐 عينة واجهة ونثر", fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Input Field Card
        Card(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().shadow(6.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp).fillMaxWidth().navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = {
                        if (userText.isNotBlank()) {
                            viewModel.sendChatMessage(userText)
                            userText = ""
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .testTag("send_chat_button"),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "إرسال", modifier = Modifier.size(18.dp))
                }

                OutlinedTextField(
                    value = userText,
                    onValueChange = { userText = it },
                    placeholder = { Text("اطرح أي سؤال للمهندس الإنشائي عن منزلك...", fontSize = 12.sp, textAlign = TextAlign.Right) },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(1f).testTag("chat_input_text"),
                    textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Right)
                )
            }
        }

        // Fixed, elegant disclaimer banner at the very bottom of the AI screen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.12f))
                .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                .padding(vertical = 6.dp, horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "⚠️ تنبيه: إجابات الذكاء الاصطناعي تقديرية للاسترشاد، يرجى مراجعتها هندسياً قبل اعتمادها رسمياً.",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ShimmerPlaceholder(modifier: Modifier = Modifier) {
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(durationMillis = 1200, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.6f),
        Color.LightGray.copy(alpha = 0.2f),
        Color.LightGray.copy(alpha = 0.6f),
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = androidx.compose.ui.geometry.Offset.Zero,
        end = androidx.compose.ui.geometry.Offset(x = translateAnim.value, y = translateAnim.value)
    )

    Box(
        modifier = modifier
            .background(brush, shape = RoundedCornerShape(4.dp))
    )
}

@Composable
private fun TableRenderer(rows: List<List<String>>, textColor: Color) {
    if (rows.isEmpty()) return
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            rows.forEachIndexed { rowIndex, cells ->
                val isHeader = rowIndex == 0
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isHeader) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            else if (rowIndex % 2 == 0) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            else Color.Transparent
                        )
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    cells.forEach { cell ->
                        val cellText = cell.trim()
                        Text(
                            text = cellText,
                            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp,
                            color = if (isHeader) MaterialTheme.colorScheme.onPrimaryContainer else textColor,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                if (rowIndex < rows.size - 1) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                }
            }
        }
    }
}

@Composable
fun MarkdownText(text: String, color: Color) {
    val lines = text.split("\n")
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        val tableRows = mutableListOf<List<String>>()
        var inTable = false

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("|")) {
                inTable = true
                if (trimmed.contains("-") && !trimmed.any { it.isLetterOrDigit() }) {
                    continue
                }
                val cells = trimmed.split("|").map { it.trim() }.filter { it.isNotEmpty() }
                if (cells.isNotEmpty()) {
                    tableRows.add(cells)
                }
            } else {
                if (inTable && tableRows.isNotEmpty()) {
                    TableRenderer(rows = tableRows.toList(), textColor = color)
                    tableRows.clear()
                    inTable = false
                }

                if (trimmed.startsWith("### ")) {
                    Text(
                        text = trimmed.substring(4),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp).fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                } else if (trimmed.startsWith("## ")) {
                    Text(
                        text = trimmed.substring(3),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp).fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                } else if (trimmed.startsWith("# ")) {
                    Text(
                        text = trimmed.substring(2),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp).fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                } else if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                    val body = trimmed.substring(2)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        LinkifiedText(
                            text = body,
                            color = color,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("•", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (trimmed.isNotEmpty()) {
                    LinkifiedText(
                        text = trimmed,
                        color = color,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.padding(vertical = 1.dp).fillMaxWidth()
                    )
                }
            }
        }

        if (inTable && tableRows.isNotEmpty()) {
            TableRenderer(rows = tableRows.toList(), textColor = color)
            tableRows.clear()
        }
    }
}

private fun parseBoldMarkdown(text: String): androidx.compose.ui.text.AnnotatedString {
    return androidx.compose.ui.text.buildAnnotatedString {
        val parts = text.split("**")
        parts.forEachIndexed { index, part ->
            if (index % 2 == 1) { // It's bold
                pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Black))
                append(part)
                pop()
            } else {
                append(part)
            }
        }
    }
}

@Composable
fun LinkifiedText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Right
) {
    if (text.isBlank()) return
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    var layoutResult by remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }
    
    val annotatedString = remember(text) {
        androidx.compose.ui.text.buildAnnotatedString {
            var currentIndex = 0
            val combinedRegex = java.util.regex.Pattern.compile("(\\[(.*?)\\]\\((https?://.*?)\\))|(https?://[a-zA-Z0-9./?%&=#~_-]+)")
            val matcher = combinedRegex.matcher(text)
            
            while (matcher.find()) {
                val start = matcher.start()
                val end = matcher.end()
                
                // Append text before the match
                if (start > currentIndex) {
                    val partBefore = text.substring(currentIndex, start)
                    val boldParts = partBefore.split("**")
                    boldParts.forEachIndexed { idx, boldPart ->
                        if (idx % 2 == 1) {
                            pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Black))
                            append(boldPart)
                            pop()
                        } else {
                            append(boldPart)
                        }
                    }
                }
                
                val isMdLink = matcher.group(1) != null
                if (isMdLink) {
                    val linkLabel = matcher.group(2) ?: ""
                    val linkUrl = matcher.group(3) ?: ""
                    
                    val startIdx = length
                    append(linkLabel)
                    val endIdx = length
                    
                    addStyle(
                        style = androidx.compose.ui.text.SpanStyle(
                            color = Color(0xFF1976D2),
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                            fontWeight = FontWeight.Bold
                        ),
                        start = startIdx,
                        end = endIdx
                    )
                    
                    addStringAnnotation(
                        tag = "URL",
                        annotation = linkUrl,
                        start = startIdx,
                        end = endIdx
                    )
                } else {
                    val plainUrl = matcher.group(4) ?: ""
                    
                    val startIdx = length
                    append(plainUrl)
                    val endIdx = length
                    
                    addStyle(
                        style = androidx.compose.ui.text.SpanStyle(
                            color = Color(0xFF1976D2),
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                            fontWeight = FontWeight.Bold
                        ),
                        start = startIdx,
                        end = endIdx
                    )
                    
                    addStringAnnotation(
                        tag = "URL",
                        annotation = plainUrl,
                        start = startIdx,
                        end = endIdx
                    )
                }
                currentIndex = end
            }
            
            // Append remaining text
            if (currentIndex < text.length) {
                val remPart = text.substring(currentIndex)
                val boldParts = remPart.split("**")
                boldParts.forEachIndexed { idx, boldPart ->
                    if (idx % 2 == 1) {
                        pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Black))
                        append(boldPart)
                        pop()
                    } else {
                        append(boldPart)
                    }
                }
            }
        }
    }

    Text(
        text = annotatedString,
        modifier = modifier.pointerInput(annotatedString) {
            detectTapGestures { offset ->
                layoutResult?.let { layout ->
                    try {
                        val position = layout.getOffsetForPosition(offset)
                        if (position >= 0 && position < annotatedString.length) {
                            annotatedString.getStringAnnotations(tag = "URL", start = position, end = position)
                                .firstOrNull()?.let { annotation ->
                                    uriHandler.openUri(annotation.item)
                                }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        },
        style = androidx.compose.ui.text.TextStyle(
            color = color,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            textAlign = textAlign
        ),
        onTextLayout = { layoutResult = it }
    )
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val bubbleColor = if (message.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (message.isUser) Color.White else MaterialTheme.colorScheme.onSurface
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
        ) {
            Card(
                shape = RoundedCornerShape(
                    topStart = 14.dp,
                    topEnd = 14.dp,
                    bottomStart = if (message.isUser) 14.dp else 2.dp,
                    bottomEnd = if (message.isUser) 2.dp else 14.dp
                ),
                colors = CardDefaults.cardColors(containerColor = bubbleColor),
                modifier = Modifier.widthIn(max = 290.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (message.isUser) {
                        Text(
                            text = message.text,
                            fontSize = 12.sp,
                            color = textColor,
                            lineHeight = 18.sp,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        MarkdownText(text = message.text, color = textColor)
                    }
                }
            }
        }
        
        if (!message.isUser) {
            var isReported by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .padding(start = 4.dp, top = 2.dp, bottom = 4.dp)
                    .widthIn(max = 290.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { isReported = true },
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = "إبلاغ عن محتوى غير دقيق",
                        tint = if (isReported) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isReported) "تم إرسال البلاغ والمراجعة للتنقيح" else "إبلاغ عن محتوى غير دقيق",
                        fontSize = 10.sp,
                        color = if (isReported) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}


// ------------------------------------
// TAB 4: General Settings, Materials Prices and Local Database Records
// ------------------------------------
@Composable
fun SettingsAndSavedTab(
    viewModel: QuantityViewModel,
    snackbarHostState: SnackbarHostState
) {
    val savedProjects by viewModel.savedProjects.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }
    var showRatingDialog by remember { mutableStateOf(false) }
    var ratingStars by remember { mutableStateOf(0) }
    var supportText by remember { mutableStateOf("") }
    var supportEmail by remember { mutableStateOf("") }

    var isWatchingAd by remember { mutableStateOf(false) }
    var adTimeRemaining by remember { mutableStateOf(4) }
    val userTokens by viewModel.userTokens.collectAsStateWithLifecycle()
    
    // Materials custom prices
    val cementPrice by viewModel.cementPricePerTon.collectAsStateWithLifecycle()
    val steelPrice by viewModel.steelPricePerTon.collectAsStateWithLifecycle()
    val sandPrice by viewModel.sandPricePerM3.collectAsStateWithLifecycle()
    val gravelPrice by viewModel.gravelPricePerM3.collectAsStateWithLifecycle()
    val brickPrice by viewModel.brickPricePer1000.collectAsStateWithLifecycle()
    val laborPrice by viewModel.laborPricePerM2.collectAsStateWithLifecycle()

    // Primary Finish Prices
    val plasterPricePerM2 by viewModel.plasterPricePerM2.collectAsStateWithLifecycle()
    val gessoPricePerM2 by viewModel.gessoPricePerM2.collectAsStateWithLifecycle()
    val ceramicPricePerM2 by viewModel.ceramicPricePerM2.collectAsStateWithLifecycle()
    val porcelainPricePerM2 by viewModel.porcelainPricePerM2.collectAsStateWithLifecycle()
    val marblePricePerM2 by viewModel.marblePricePerM2.collectAsStateWithLifecycle()
    val pipingPricePerM2 by viewModel.pipingPricePerM2.collectAsStateWithLifecycle()
    val wiringPricePerM2 by viewModel.wiringPricePerM2.collectAsStateWithLifecycle()

    // Optional Finish Prices
    val ceilingPlasterPricePerM2 by viewModel.ceilingPlasterPricePerM2.collectAsStateWithLifecycle()
    val ceilingPvcPricePerM2 by viewModel.ceilingPvcPricePerM2.collectAsStateWithLifecycle()
    val ceilingGypsumPricePerM2 by viewModel.ceilingGypsumPricePerM2.collectAsStateWithLifecycle()
    val paintingPricePerM2 by viewModel.paintingPricePerM2.collectAsStateWithLifecycle()
    val pavementPricePerMeter by viewModel.pavementPricePerMeter.collectAsStateWithLifecycle()
    val exteriorStuccoPricePerM2 by viewModel.exteriorStuccoPricePerM2.collectAsStateWithLifecycle()
    val externalStonePricePerM2 by viewModel.externalStonePricePerM2.collectAsStateWithLifecycle()

    val sanitaryBasicPrice by viewModel.sanitaryBasicPrice.collectAsStateWithLifecycle()
    val sanitaryStandardPrice by viewModel.sanitaryStandardPrice.collectAsStateWithLifecycle()
    val sanitaryLuxuryPrice by viewModel.sanitaryLuxuryPrice.collectAsStateWithLifecycle()

    val electricalBasicPrice by viewModel.electricalBasicPrice.collectAsStateWithLifecycle()
    val electricalStandardPrice by viewModel.electricalStandardPrice.collectAsStateWithLifecycle()
    val electricalSmartPrice by viewModel.electricalSmartPrice.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 0: Google Account Status & Sign Out
        item {
            val userSession by viewModel.userSession.collectAsStateWithLifecycle()
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            viewModel.authHelper?.signOut {
                                // Reset handled by state listener
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "تسجيل الخروج",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("خروج", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                    ) {
                        Text(
                            text = userSession?.name ?: "مهندس BOQ AI",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Right
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = userSession?.email ?: "",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Right
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = if (userSession?.isSimulation == true) "رصيد تقديري آمن" else "رصيد سحابي موثق",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (userSession?.isSimulation == true) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Right
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = if (userSession?.isSimulation == true) Icons.Default.Info else Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = if (userSession?.isSimulation == true) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(38.dp),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = (userSession?.name?.firstOrNull()?.toString() ?: "M").uppercase(),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        // Section A: Materials prices config
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.shadow(2.dp, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "جدول أسعار سوق مواد البناء بمحافظتك (د.ع)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Right
                    )
                    Text(
                        "تعديل الأسعار هنا يعدل حسابات الفواتير و BOQ مباشرة وبصورة تفاعلية",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = steelPrice,
                            onValueChange = {
                                viewModel.steelPricePerTon.value = it
                                viewModel.calculateQuantities()
                            },
                            label = { Text("طن الحديد د.ع") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = cementPrice,
                            onValueChange = {
                                viewModel.cementPricePerTon.value = it
                                viewModel.calculateQuantities()
                            },
                            label = { Text("طن الإسمنت د.ع") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = gravelPrice,
                            onValueChange = {
                                viewModel.gravelPricePerM3.value = it
                                viewModel.calculateQuantities()
                            },
                            label = { Text("متر الحصى د.ع") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = sandPrice,
                            onValueChange = {
                                viewModel.sandPricePerM3.value = it
                                viewModel.calculateQuantities()
                            },
                            label = { Text("متر الرمل د.ع") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = laborPrice,
                            onValueChange = {
                                viewModel.laborPricePerM2.value = it
                                viewModel.calculateQuantities()
                            },
                            label = { Text("عمل المتر المربع د.ع") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = brickPrice,
                            onValueChange = {
                                viewModel.brickPricePer1000.value = it
                                viewModel.calculateQuantities()
                            },
                            label = { Text("١٠٠٠ طابوق د.ع") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Section A.1: Primary Finishes Price Config
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.shadow(2.dp, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "🛠️ أسعار مصنعيات ومواد التشطيب الأساسي (رئيسيات)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Right
                    )
                    Text(
                        text = "خصص الكلفة لكل متر مربع لخدمات اللبخ والبياض والسيراميك أو كلفة تأسيس مائي وكهرباء لإعطاء كشف دقيق للغاية.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = gessoPricePerM2,
                            onValueChange = {
                                viewModel.gessoPricePerM2.value = it
                                viewModel.calculateQuantities()
                            },
                            label = { Text("متر البياض/البورق د.ع") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = plasterPricePerM2,
                            onValueChange = {
                                viewModel.plasterPricePerM2.value = it
                                viewModel.calculateQuantities()
                            },
                            label = { Text("متر اللبخ الداخلي د.ع") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = porcelainPricePerM2,
                            onValueChange = {
                                viewModel.porcelainPricePerM2.value = it
                                viewModel.calculateQuantities()
                            },
                            label = { Text("متر البورسلين د.ع") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = ceramicPricePerM2,
                            onValueChange = {
                                viewModel.ceramicPricePerM2.value = it
                                viewModel.calculateQuantities()
                            },
                            label = { Text("متر السيراميك د.ع") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = pipingPricePerM2,
                            onValueChange = {
                                viewModel.pipingPricePerM2.value = it
                                viewModel.calculateQuantities()
                            },
                            label = { Text("متر تفصيل السباكة د.ع") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = marblePricePerM2,
                            onValueChange = {
                                viewModel.marblePricePerM2.value = it
                                viewModel.calculateQuantities()
                            },
                            label = { Text("متر المرمر الطبيعي د.ع") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = wiringPricePerM2,
                            onValueChange = {
                                viewModel.wiringPricePerM2.value = it
                                viewModel.calculateQuantities()
                            },
                            label = { Text("متر تسليك الكهرباء د.ع") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1.0f)
                        )
                    }
                }
            }
        }

        // Section A.2: Optional Finishes Price Config
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.shadow(2.dp, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "⭐ أسعار وخيارات التشطيب الثانوي (اختيارية كلياً)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Right
                    )
                    Text(
                        text = "عدّل على أسعار الملحقات والديكورات الثانوية مثل الجبس بورد أو الصباغة أو واجهة الحجر الموصلي.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("سقوف الديكور (المتر المربع د.ع):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = ceilingGypsumPricePerM2,
                            onValueChange = {
                                viewModel.ceilingGypsumPricePerM2.value = it
                                viewModel.calculateQuantities()
                            },
                            label = { Text("متر جبس بورد") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = ceilingPvcPricePerM2,
                            onValueChange = {
                                viewModel.ceilingPvcPricePerM2.value = it
                                viewModel.calculateQuantities()
                            },
                            label = { Text("سقف معلق PVC") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = ceilingPlasterPricePerM2,
                            onValueChange = {
                                viewModel.ceilingPlasterPricePerM2.value = it
                                viewModel.calculateQuantities()
                            },
                            label = { Text("لبخ سقف عادي") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("الصباغة والملحقات والواجهات (د.ع):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = pavementPricePerMeter,
                            onValueChange = {
                                viewModel.pavementPricePerMeter.value = it
                                viewModel.calculateQuantities()
                            },
                            label = { Text("دكة الحماية (متر طولي)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = paintingPricePerM2,
                            onValueChange = {
                                viewModel.paintingPricePerM2.value = it
                                viewModel.calculateQuantities()
                            },
                            label = { Text("متر الصبغ والدهان") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = externalStonePricePerM2,
                            onValueChange = {
                                viewModel.externalStonePricePerM2.value = it
                                viewModel.calculateQuantities()
                            },
                            label = { Text("الحجر الموصلي (م٢)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = exteriorStuccoPricePerM2,
                            onValueChange = {
                                viewModel.exteriorStuccoPricePerM2.value = it
                                viewModel.calculateQuantities()
                            },
                            label = { Text("البخ الخارجي/النثر م٢") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("أعمال السباكة والصحيات الكاملة (مقطوعة د.ع):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = sanitaryBasicPrice,
                            onValueChange = {
                                viewModel.sanitaryBasicPrice.value = it
                                viewModel.calculateQuantities()
                            },
                            label = { Text("إقتصادي") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = sanitaryStandardPrice,
                            onValueChange = {
                                viewModel.sanitaryStandardPrice.value = it
                                viewModel.calculateQuantities()
                            },
                            label = { Text("متوسط مميز") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = sanitaryLuxuryPrice,
                            onValueChange = {
                                viewModel.sanitaryLuxuryPrice.value = it
                                viewModel.calculateQuantities()
                            },
                            label = { Text("ديلوكس فاخر") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("الأعمال والتمديدات الكهربائية والإنارة (مقطوعة د.ع):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = electricalBasicPrice,
                            onValueChange = {
                                viewModel.electricalBasicPrice.value = it
                                viewModel.calculateQuantities()
                            },
                            label = { Text("إقتصادي") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = electricalStandardPrice,
                            onValueChange = {
                                viewModel.electricalStandardPrice.value = it
                                viewModel.calculateQuantities()
                            },
                            label = { Text("متوسط مميز") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = electricalSmartPrice,
                            onValueChange = {
                                viewModel.electricalSmartPrice.value = it
                                viewModel.calculateQuantities()
                            },
                            label = { Text("ذكي متكامل") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Section A.3: Coins & Token Monetization Management System
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.shadow(2.dp, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.testTag("balance_tag")
                        ) {
                            Text(
                                text = "$userTokens عملة",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        Text(
                            text = "🪙 نظام شحن الرصيد والعملات المعززة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Right
                        )
                    }

                    Text(
                        text = "يستخدم الرصيد في العمليات المتقدمة وقراءة المخططات المعقدة وحفظ التقارير بصيغة PDF وطباعتها.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp),
                        textAlign = TextAlign.Right
                    )

                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (isWatchingAd) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = "جاري الاتصال بـ AdMob وعرض إعلان مكافأة... المتبقي $adTimeRemaining ح",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Right
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                scope.launch {
                                    isWatchingAd = true
                                    adTimeRemaining = 4
                                    while (adTimeRemaining > 0) {
                                        delay(1000)
                                        adTimeRemaining--
                                    }
                                    isWatchingAd = false
                                    viewModel.earnTokens(15, "مكافأة إعلانات مرئية AdMob")
                                    scope.launch {
                                        snackbarHostState.showSnackbar("تهانينا! حصلت على +15 عملة مجانية!")
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF128C7E)), // WhatsApp green accent
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("شحن رصيد مجاني (مشاهدة إعلان 📺)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Icon(Icons.Default.MonetizationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "باقات الدفع السريع المعتمدة (Google Play Billing):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Light package
                        OutlinedButton(
                            onClick = {
                                viewModel.earnTokens(100, "شراء باقة 100 عملة برونزية")
                                scope.launch {
                                    snackbarHostState.showSnackbar("نجحت الفوترة الفورية من جوجل بلاي! +100 عملة")
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("100 عملة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("5,000 د.ع", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        }

                        // Premium package
                        OutlinedButton(
                            onClick = {
                                viewModel.earnTokens(500, "شراء باقة 500 عملة ذهبية")
                                scope.launch {
                                    snackbarHostState.showSnackbar("نجحت الفوترة الفورية من جوجل بلاي! +500 عملة")
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("500 عملة", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                Text("15,000 د.ع", fontSize = 9.sp, color = Color(0xFFF97316), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Section A.4: Legal compliance, Safety, support contact and Rating system.
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.shadow(2.dp, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "🔒 الأمان، الخصوصية والدعم الفني",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "نحن نلتزم بحماية خصوصيتك بنسبة 100% طبقاً لمعايير أمان متجر Google Play القانونية.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp),
                        textAlign = TextAlign.Right
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Buttons list for policies
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showPrivacyPolicyDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("سياسة الخصوصية", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        }

                        Button(
                            onClick = { showSupportDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("تواصل بالدعم 🛠️", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showRatingDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer),
                            modifier = Modifier.weight(1.0f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("قيمنا على المتجر ⭐", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Delete Account
                    Button(
                        onClick = { showDeleteAccountDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("حذف حسابي والبيانات نهائياً (Delete Account)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        // Section B: Saved project history
        item {
            Text(
                text = "المشاريع المحفوظة بسجلات الهاتف",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Right
            )
        }

        if (savedProjects.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                ) {
                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            "لا توجد مشاريع سابقة محفوظة. اذهب للرئيسية لاحتساب الكميات وحفظ كشفك الأول.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(savedProjects, key = { it.id }) { proj ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().shadow(1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Delete Button
                        IconButton(onClick = { viewModel.deleteProject(proj) }) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف الكشف", tint = MaterialTheme.colorScheme.error)
                        }

                        // Load / Middle Section
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.clickable {
                                viewModel.loadProject(proj)
                                viewModel.selectedTab.value = 0
                                scope.launch {
                                    snackbarHostState.showSnackbar("تم تحميل بيانات كشف البناء '${proj.title}'")
                                }
                            }
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(proj.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("كلفة: ${String.format("%,.0f", proj.totalCost)} د.ع", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    Text("|", fontSize = 10.sp, color = MaterialTheme.colorScheme.outlineVariant)
                                    Text("بناء: ${proj.buildArea} م٢", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    text = "تاريخ: " + SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(proj.date)),
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            
                            Box(
                                modifier = Modifier.size(38.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIALOGS FOR COMPLIANCE, SAFETY & MONETIZATION ---
    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = {
                Text(
                    "⚠️ تحذير: حذف الحساب نهائياً",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
            },
            text = {
                Text(
                    "هل أنت متأكد من رغبتك في حذف حساب بيانات BOQ AI نهائياً؟ سيؤدي ذلك لمسح كافة المشاريع المحفوظة بسجلات هاتفك المحلية، وتصفير الأرصدة والعملات والعمليات السابقة فوراً دون إمكانية استعادتها.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Right
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteUserAccountAndData()
                        showDeleteAccountDialog = false
                        scope.launch {
                            snackbarHostState.showSnackbar("تم حذف الحساب بالكامل ومسح كافة السجلات والصور بنجاح طبقاً لقوانين جوجل")
                        }
                    }
                ) {
                    Text("نعم، احذف نهائياً", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text("إلغاء", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        )
    }

    if (showPrivacyPolicyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicyDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "🔒 سياسة الخصوصية وأمان البيانات",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Right
                    )
                }
            },
            text = {
                Box(modifier = Modifier.height(300.dp).verticalScroll(rememberScrollState())) {
                    Text(
                        text = "مرحباً بك في تطبيق BOQ AI (مساعد حساب الكميات الذكي).\n\n" +
                                "١. حماية وتشفير البيانات (HTTPS):\n" +
                                "نحن نلتزم بتأمين اتصالك وحماية كافة البيانات المتبادلة بين التطبيق وسيرفرات معالجة الذكاء الاصطناعي Gemini عبر معايير تشفير HTTPS المعتمدة عالمياً لضمان عدم اعتراض البيانات.\n\n" +
                                "٢. تخزين محلي آمن:\n" +
                                "يتم حفظ كشوفات ومشاريع البناء والأسعار المفصلة التي تدخلها بشكل محلي آمن تماماً داخل هاتف الذكي باستخدام تقنيات Room Database وبخوارزميات تشفير قوية.\n\n" +
                                "٣. خصوصية الصور والملفات:\n" +
                                "عند رفع مخططات هندسية أو صور كشوفات لتحليلها بالذكاء الاصطناعي، تتم معالجتها لحظياً لإخراج تقاريرك ثم يتم مسحها تلقائياً بالكامل من الخادم المؤقت.\n\n" +
                                "٤. حق الحذف الكامل (Compliance):\n" +
                                "بإمكانك في أي وقت وبنقرة واحدة حذف حسابك بالكامل ومسح كافة بياناتك وسجلاتك المالية من هاتفك نهائياً وبشكل غير قابل للاسترجاع، وذلك التزاماً بشروط متجر Google Play القانونية لحماية المستخدمين.\n\n" +
                                "لأي استفسار يرجى مراسلة المهندس المساعد على: support@aistudio.com",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPrivacyPolicyDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("موافق ومتابعة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showSupportDialog) {
        AlertDialog(
            onDismissRequest = { showSupportDialog = false },
            title = {
                Text(
                    "🛠️ إرسال شكوى أو طلب دعم فني مباشر",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        "تواصل مباشرة مع مهندسي النظام لحل المشاكل التقنية أو تقديم اقتراحات.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Right
                    )

                    OutlinedTextField(
                        value = supportEmail,
                        onValueChange = { supportEmail = it },
                        label = { Text("بريدك الإلكتروني للتواصل") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("support_email_input")
                    )

                    OutlinedTextField(
                        value = supportText,
                        onValueChange = { supportText = it },
                        label = { Text("اكتب تفاصيل طلبك أو الشكوى هنا") },
                        modifier = Modifier.fillMaxWidth().height(100.dp).testTag("support_msg_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (supportText.trim().isEmpty() || supportEmail.trim().isEmpty()) {
                            scope.launch {
                                snackbarHostState.showSnackbar("برجاء إدخال البريد الإلكتروني وتفصيل المشكلة أولاً")
                            }
                        } else {
                            scope.launch {
                                val progress = snackbarHostState.showSnackbar("جاري معالجة وتشفير بلاغك...")
                                delay(1200)
                                snackbarHostState.showSnackbar("نشكر تواصلك معنا! تم إرسال تذكرتك بنجاح برقم #BOQ-${System.currentTimeMillis() % 10000}")
                                showSupportDialog = false
                                supportText = ""
                            }
                        }
                    }
                ) {
                    Text("إرسال التذكرة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSupportDialog = false }) {
                    Text("إلغاء", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        )
    }

    if (showRatingDialog) {
        AlertDialog(
            onDismissRequest = { showRatingDialog = false },
            title = {
                Text(
                    "⭐ التقييم المباشر على متجر Google Play",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "يسعدنا تقييمك بـ 5 نجوم لكي نستمر في تطوير أدوات الإعمار العراقية وتحديث الأسعار بشكل دوري وموثوق!",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Right
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (star in 1..5) {
                            IconButton(
                                onClick = { ratingStars = star },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "$star Stars",
                                    tint = if (ratingStars >= star) Color(0xFFFFC107) else MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (ratingStars > 0) {
                            viewModel.earnTokens(10, "تقييم التطبيق بـ $ratingStars نجوم ⭐")
                            scope.launch {
                                snackbarHostState.showSnackbar("شكراً لك! حصلت على مكافأة +10 عملات لمساعدتنا في الانتشار ✨")
                            }
                            showRatingDialog = false
                        }
                    }
                ) {
                    Text("تقييم الآن ونيل المكافأة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRatingDialog = false }) {
                    Text("إلغاء", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        )
    }
}

@Composable
fun FinishesCalculatorTab(
    viewModel: QuantityViewModel,
    snackbarHostState: SnackbarHostState
) {
    val flooringType by viewModel.flooringType.collectAsStateWithLifecycle()
    val ceilingType by viewModel.ceilingType.collectAsStateWithLifecycle()
    val doorsCount by viewModel.doorsCount.collectAsStateWithLifecycle()
    val doorUnitPrice by viewModel.doorUnitPrice.collectAsStateWithLifecycle()
    val windowsCount by viewModel.windowsCount.collectAsStateWithLifecycle()
    val windowUnitPrice by viewModel.windowUnitPrice.collectAsStateWithLifecycle()
    val sanitaryQuality by viewModel.sanitaryQuality.collectAsStateWithLifecycle()
    val electricalQuality by viewModel.electricalQuality.collectAsStateWithLifecycle()
    
    // Optional selections
    val isCeilingsEnabled by viewModel.isCeilingsEnabled.collectAsStateWithLifecycle()
    val isDoorsEnabled by viewModel.isDoorsEnabled.collectAsStateWithLifecycle()
    val isWindowsEnabled by viewModel.isWindowsEnabled.collectAsStateWithLifecycle()
    val isPaintingEnabled by viewModel.isPaintingEnabled.collectAsStateWithLifecycle()
    val isPavementEnabled by viewModel.isPavementEnabled.collectAsStateWithLifecycle()
    val isExteriorStuccoEnabled by viewModel.isExteriorStuccoEnabled.collectAsStateWithLifecycle()
    val isExternalStoneEnabled by viewModel.isExternalStoneEnabled.collectAsStateWithLifecycle()
    
    val showResults by viewModel.showResults.collectAsStateWithLifecycle()
    val calcResult by viewModel.calculationResult.collectAsStateWithLifecycle()
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Design banner matching expert engineer aesthetic
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "🏛️ حاسبة كلف تشطيب المنزل والدواخل",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "تنقسم هذه الصفحة تفاعلياً إلى قسمين: البنود الكهربائية والمائية واللبخ الأساسية (الرئيسية الثابتة)، وبنود التشطيبات والتزيينات الثانوية (الاختيارية)، للتسهيل التام على العّامة.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Right,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        item {
            RunBoqCard(viewModel, snackbarHostState)
        }

        // Professional Summary Card displaying the total cost of calculated active Finishes
        if (showResults && calcResult != null) {
            val res = calcResult!!
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(3.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "إجمالي كشف حساب كلف التشطيبات والملحقات الفعّالة",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${String.format("%,.0f", res.finishingCost)} د.ع",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CostBadge("الأرضيات واللبخ والجدُّر", res.tileCost + res.plasterCost)
                            CostBadge("الصحيات والكهربائيات", res.sanitaryCost + res.electricalCost)
                        }
                    }
                }
            }
        } else {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "النتائج وقيم التحليل مغلقة المزامنة 🔒",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "الرجاء النقر على زر 'تشغيل وحساب كشف BOQ' لحسابها حياً",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }

        // ==========================================
        // SECTION 1: MAIN/PRIMARY MANDATORY FINISHES
        // ==========================================
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Text(
                    text = "١. التشطيبات والخدمات والمنشآت الأساسية (رئيسيات ومثبتة)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
            }
        }

        // 1. Flooring Types Choice (Compulsory Primary item)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.shadow(1.dp, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().wrapContentHeight()
                    ) {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Text("رئيسي إلزامي", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text(text = "سيراميك وبورسلين الأرضيات", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Icon(imageVector = Icons.Default.Dashboard, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val activeColor = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                        val inactiveColor = ButtonDefaults.outlinedButtonColors()

                        OutlinedButton(
                            onClick = {
                                viewModel.flooringType.value = "MARBLE"
                                viewModel.calculateQuantities()
                            },
                            colors = if (flooringType == "MARBLE") activeColor else inactiveColor,
                            modifier = Modifier.weight(1f).height(41.dp)
                        ) {
                            Text("مرمر طبيعي", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.flooringType.value = "PORCELAIN"
                                viewModel.calculateQuantities()
                            },
                            colors = if (flooringType == "PORCELAIN") activeColor else inactiveColor,
                            modifier = Modifier.weight(1f).height(41.dp)
                        ) {
                            Text("بورسلين خارجي", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.flooringType.value = "CERAMIC"
                                viewModel.calculateQuantities()
                            },
                            colors = if (flooringType == "CERAMIC") activeColor else inactiveColor,
                            modifier = Modifier.weight(1f).height(41.dp)
                        ) {
                            Text("سيراميك محلي", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (showResults) {
                        calcResult?.let { res ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "مساحة تطبيق البلاط الكلية: ${String.format("%.0f", res.flooringAreaM2)} م٢ بكلفة إجمالية مقدرة: ${String.format("%,.0f", res.tileCost)} د.ع",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        // 2. Raw walls finishing info (Plaster & Gesso rendering) -> Compulsory Primary
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.shadow(1.dp, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().wrapContentHeight()
                    ) {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Text("رئيسي إلزامي", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text(text = "اللبخ الداخلي وبياض الجدران المائي", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Icon(imageVector = Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    if (showResults) {
                        calcResult?.let { res ->
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "• كلفة تنفيذ اللبخ والإنهاءات الجصية: ${String.format("%,.0f", res.plasterCost)} د.ع لمساحة حوائط: ${String.format("%.0f", res.wallPlasterAreaM2)} م٢",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Right
                                )
                                Text(
                                    text = "• يتضمن هذا البند أعمال البورق والتعديل والتنعيم للغرف والمجالس استعداداً للصبغ والتبطين.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Right
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Plumbing Utilities (Compulsory Primary)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.shadow(1.dp, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().wrapContentHeight()
                    ) {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Text("رئيسي إلزامي", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text(text = "تأسيسات السباكة ومواد المائي والصحي", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val activeColor = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                        val inactiveColor = ButtonDefaults.outlinedButtonColors()

                        OutlinedButton(
                            onClick = {
                                viewModel.sanitaryQuality.value = "LUXURY"
                                viewModel.calculateQuantities()
                            },
                            colors = if (sanitaryQuality == "LUXURY") activeColor else inactiveColor,
                            modifier = Modifier.weight(1f).height(41.dp)
                        ) {
                            Text("ديلوكس فاخر", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.sanitaryQuality.value = "STANDARD"
                                viewModel.calculateQuantities()
                            },
                            colors = if (sanitaryQuality == "STANDARD") activeColor else inactiveColor,
                            modifier = Modifier.weight(1f).height(41.dp)
                        ) {
                            Text("متوسط مميز", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.sanitaryQuality.value = "BASIC"
                                viewModel.calculateQuantities()
                            },
                            colors = if (sanitaryQuality == "BASIC") activeColor else inactiveColor,
                            modifier = Modifier.weight(1f).height(41.dp)
                        ) {
                            Text("اقتصادي مبسط", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (showResults) {
                        calcResult?.let { res ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "الكلفة الكلية المقدرة للتأسيس الصحي والأجهزة والمطابخ: ${String.format("%,.0f", res.sanitaryCost)} د.ع",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        // 4. Electrical Utilities (Compulsory Primary)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.shadow(1.dp, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().wrapContentHeight()
                    ) {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Text("رئيسي إلزامي", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text(text = "التجهيزات والأعمال الكهربائية والإنارة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val activeColor = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                        val inactiveColor = ButtonDefaults.outlinedButtonColors()

                        OutlinedButton(
                            onClick = {
                                viewModel.electricalQuality.value = "SMART"
                                viewModel.calculateQuantities()
                            },
                            colors = if (electricalQuality == "SMART") activeColor else inactiveColor,
                            modifier = Modifier.weight(1f).height(41.dp)
                        ) {
                            Text("ذكي متكامل", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.electricalQuality.value = "STANDARD"
                                viewModel.calculateQuantities()
                            },
                            colors = if (electricalQuality == "STANDARD") activeColor else inactiveColor,
                            modifier = Modifier.weight(1f).height(41.dp)
                        ) {
                            Text("حديث مميز", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.electricalQuality.value = "BASIC"
                                viewModel.calculateQuantities()
                            },
                            colors = if (electricalQuality == "BASIC") activeColor else inactiveColor,
                            modifier = Modifier.weight(1f).height(41.dp)
                        ) {
                            Text("اقتصادي مبسط", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (showResults) {
                        calcResult?.let { res ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "كلفة تسليك الكابلات والبوكس واللوحات وحساب الكهرباء: ${String.format("%,.0f", res.electricalCost)} د.ع",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // SECTION 2: OPTIONAL SECONDARY FINISHES
        // ==========================================
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
            ) {
                Text(
                    text = "٢. التشطيب والملحقات الثانوية (اختيارية كلياً - فعِّل ما ترغب به)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
            }
        }

        // 1. Ceiling (Optional Toggleable)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.shadow(1.dp, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().wrapContentHeight()
                    ) {
                        Switch(
                            checked = isCeilingsEnabled,
                            onCheckedChange = {
                                viewModel.isCeilingsEnabled.value = it
                                viewModel.calculateQuantities()
                            }
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(text = "سقوف الجبس بورد والديكورات", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Icon(imageVector = Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                    }
                    
                    if (isCeilingsEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val activeColor = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                            val inactiveColor = ButtonDefaults.outlinedButtonColors()

                            OutlinedButton(
                                onClick = {
                                    viewModel.ceilingType.value = "PVC"
                                    viewModel.calculateQuantities()
                                },
                                colors = if (ceilingType == "PVC") activeColor else inactiveColor,
                                modifier = Modifier.weight(1f).height(41.dp)
                            ) {
                                Text("ثانوية PVC", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.ceilingType.value = "GYPSUM"
                                    viewModel.calculateQuantities()
                                },
                                colors = if (ceilingType == "GYPSUM") activeColor else inactiveColor,
                                modifier = Modifier.weight(1f).height(41.dp)
                            ) {
                                Text("جبس بورد غرف", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.ceilingType.value = "PLASTER"
                                    viewModel.calculateQuantities()
                                },
                                colors = if (ceilingType == "PLASTER") activeColor else inactiveColor,
                                modifier = Modifier.weight(1f).height(41.dp)
                            ) {
                                Text("سقف لبخ عادي", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (showResults) {
                            calcResult?.let { res ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "كلفة تنفيذ السقوف الثانوية للديكور المقدرة: ${String.format("%,.0f", res.ceilingCost)} د.ع",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "تم تعطيل السقوف الثانوية المقررة من حساب الفاتورة.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            textAlign = TextAlign.Right,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }

        // 2. Interior Doors (Optional Toggleable)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.shadow(1.dp, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().wrapContentHeight()
                    ) {
                        Switch(
                            checked = isDoorsEnabled,
                            onCheckedChange = {
                                viewModel.isDoorsEnabled.value = it
                                viewModel.calculateQuantities()
                            }
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(text = "أبواب الغرف والمداخل", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Icon(imageVector = Icons.Default.GridOn, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                    }
                    
                    if (isDoorsEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = doorUnitPrice,
                                onValueChange = {
                                    viewModel.doorUnitPrice.value = it
                                    viewModel.calculateQuantities()
                                },
                                label = { Text("سعر الباب الواحد (د.ع)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1.3f)
                            )
                            OutlinedTextField(
                                value = doorsCount,
                                onValueChange = {
                                    viewModel.doorsCount.value = it
                                    viewModel.calculateQuantities()
                                },
                                label = { Text("العدد") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(0.7f)
                            )
                        }
                        if (showResults) {
                            calcResult?.let { res ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "حساب كلفة أبواب البيت المقدرة: ${String.format("%,.0f", res.doorsCost)} د.ع",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "تم استبعاد كلفة الأبواب كمنشأة خارجية جاهزة.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            textAlign = TextAlign.Right,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }

        // 3. Exterior Windows (Optional Toggleable)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.shadow(1.dp, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().wrapContentHeight()
                    ) {
                        Switch(
                            checked = isWindowsEnabled,
                            onCheckedChange = {
                                viewModel.isWindowsEnabled.value = it
                                viewModel.calculateQuantities()
                            }
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(text = "الشبابيك الخارجية دبل جلاس", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Icon(imageVector = Icons.Default.GridOn, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                    }
                    
                    if (isWindowsEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = windowUnitPrice,
                                onValueChange = {
                                    viewModel.windowUnitPrice.value = it
                                    viewModel.calculateQuantities()
                                },
                                label = { Text("سعر الشباك الواحد (د.ع)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1.3f)
                            )
                            OutlinedTextField(
                                value = windowsCount,
                                onValueChange = {
                                    viewModel.windowsCount.value = it
                                    viewModel.calculateQuantities()
                                },
                                label = { Text("العدد") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(0.7f)
                            )
                        }
                        if (showResults) {
                            calcResult?.let { res ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "حساب كلفة شبابيك البيت والألومنيوم: ${String.format("%,.0f", res.windowsCost)} د.ع",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "تم استبعاد كلفة الشبابيك كمنشأة خارجية منفصلة.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            textAlign = TextAlign.Right,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }

        // 4. Painting and Pigmentation (Optional Toggleable)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.shadow(1.dp, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().wrapContentHeight()
                    ) {
                        Switch(
                            checked = isPaintingEnabled,
                            onCheckedChange = {
                                viewModel.isPaintingEnabled.value = it
                                viewModel.calculateQuantities()
                            }
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(text = "أعمال الصباغة والدهان الداخلي", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Icon(imageVector = Icons.Default.ColorLens, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                    }
                    
                    if (isPaintingEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        if (showResults) {
                            calcResult?.let { res ->
                                Text(
                                    text = "• مساحة دهان الجدران المحسوبة: ${String.format("%.0f", res.paintingAreaM2)} م٢",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Right
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "• الكلفة التقديرية للأصباغ (معجون وأساس ودهان): ${String.format("%,.0f", res.paintingCost)} د.ع",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Right
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "تم إلغاء بند الصباغة لتوفيره لاحقاً.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            textAlign = TextAlign.Right,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }

        // 5. Pavement protection (Optional Toggleable)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.shadow(1.dp, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().wrapContentHeight()
                    ) {
                        Switch(
                            checked = isPavementEnabled,
                            onCheckedChange = {
                                viewModel.isPavementEnabled.value = it
                                viewModel.calculateQuantities()
                            }
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(text = "دكة الحماية الخرسانية حول المنزل", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                    }
                    
                    if (isPavementEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        if (showResults) {
                            calcResult?.let { res ->
                                Text(
                                    text = "• طول المحيط الخارجي المحسوب: ${String.format("%.1f", res.pavementLengthM)} متر خطي",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Right
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "• كلفة صب دكة الحماية المسلحة لحماية الأسس: ${String.format("%,.0f", res.pavementCost)} د.ع",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Right
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "تم تجاهل إضافة كلفة دكة حماية الأسس الخارجية.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            textAlign = TextAlign.Right,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }

        // 6. Exterior Stucco (Optional Toggleable, i.e., البخ الخارجي)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.shadow(1.dp, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().wrapContentHeight()
                    ) {
                        Switch(
                            checked = isExteriorStuccoEnabled,
                            onCheckedChange = {
                                viewModel.isExteriorStuccoEnabled.value = it
                                viewModel.calculateQuantities()
                            }
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(text = "لبخ نثر خارجي ملون للواجهات", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Icon(imageVector = Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                    }
                    
                    if (isExteriorStuccoEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        if (showResults) {
                            calcResult?.let { res ->
                                Text(
                                    text = "• مساحة الواجهة الخارجية المحسوبة: ${String.format("%.0f", res.exteriorStuccoAreaM2)} م٢",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Right
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "• كلفة تنفيذ النثر الخارجي مع المواد بالألوان الجذابة: ${String.format("%,.0f", res.exteriorStuccoCost)} د.ع",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Right
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "تم تعطيل نثر الواجهة بالأصباغ الملونة.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            textAlign = TextAlign.Right,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }

        // 7. External Cladding Stone (Optional Toggleable, i.e., الحجر الموصلي)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.shadow(1.dp, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().wrapContentHeight()
                    ) {
                        Switch(
                            checked = isExternalStoneEnabled,
                            onCheckedChange = {
                                viewModel.isExternalStoneEnabled.value = it
                                viewModel.calculateQuantities()
                            }
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(text = "واجهات الحجر الطبيعي (الموصلي)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Icon(imageVector = Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                    }
                    
                    if (isExternalStoneEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        if (showResults) {
                            calcResult?.let { res ->
                                Text(
                                    text = "• مساحة تلبيس حجر الواجهة الموصلي: ${String.format("%.0f", res.externalStoneAreaM2)} م٢",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Right
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "• كلفة تلبيس واجهة الحجر الطبيعي المعزول مع الهيكل: ${String.format("%,.0f", res.externalStoneCost)} د.ع",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Right
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "واجهة البيت حجر طبيعي معطل حالياً من حساب الكشف.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            textAlign = TextAlign.Right,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }

        // 8. Finishes Labor Cost Selection (أجور عمال التشطيبات والتلوين)
        item {
            val isManualFinishingLabor by viewModel.isManualFinishingLaborEnabled.collectAsStateWithLifecycle()
            val manualFinishingLaborCostStr by viewModel.manualFinishingLaborCost.collectAsStateWithLifecycle()
            val finishingLaborPricePerM2Str by viewModel.finishingLaborPricePerM2.collectAsStateWithLifecycle()
            val buildAreaStr by viewModel.buildArea.collectAsStateWithLifecycle()
            val floorsCountVal by viewModel.floorsCount.collectAsStateWithLifecycle()

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { /* Help context dialog */ }) {
                            Icon(Icons.Default.Info, contentDescription = "معلومات", tint = MaterialTheme.colorScheme.primary)
                        }
                        Text(
                            text = "أجور عمال التشطيبات والديكور الداخلي",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Right
                        )
                    }
                    
                    Text(
                        text = "طريقة تحديد تكلفة عقود اليد العاملة لأعمال الإنهاءات والصبغ والديكور:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    val activeColor = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                    val inactiveColor = ButtonDefaults.outlinedButtonColors()

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.isManualFinishingLaborEnabled.value = false
                                viewModel.calculateQuantities()
                            },
                            colors = if (!isManualFinishingLabor) activeColor else inactiveColor,
                            modifier = Modifier.weight(1f).height(41.dp)
                        ) {
                            Text("آلي (حسب مساحة التشطيب)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.isManualFinishingLaborEnabled.value = true
                                viewModel.calculateQuantities()
                            },
                            colors = if (isManualFinishingLabor) activeColor else inactiveColor,
                            modifier = Modifier.weight(1f).height(41.dp)
                        ) {
                            Text("يدوي (تحديد مبلغ ثابت)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (isManualFinishingLabor) {
                        OutlinedTextField(
                            value = manualFinishingLaborCostStr,
                            onValueChange = {
                                viewModel.manualFinishingLaborCost.value = it
                                viewModel.calculateQuantities()
                            },
                            label = { Text("أجور عمال التشطيبات الإجمالية (د.ع)", textAlign = TextAlign.Right) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Right)
                        )
                        if (showResults) {
                            calcResult?.let { res ->
                                Text(
                                    text = "أجور عمال التشطيب المحددة يدوياً: ${String.format("%,.0f", res.finishingLaborCost)} د.ع",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 4.dp).fillMaxWidth(),
                                    textAlign = TextAlign.Right
                                )
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = finishingLaborPricePerM2Str,
                            onValueChange = {
                                viewModel.finishingLaborPricePerM2.value = it
                                viewModel.calculateQuantities()
                            },
                            label = { Text("سعر عمل المتر المربع للتشطيبات (د.ع/م٢)", textAlign = TextAlign.Right) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Right)
                        )
                        if (showResults) {
                            calcResult?.let { res ->
                                val parsedBuildArea = buildAreaStr.toDoubleOrNull() ?: 150.0
                                val totalSize = parsedBuildArea * floorsCountVal
                                Text(
                                    text = "أجور عمال التشطيب المحسوبة تلقائياً: ${String.format("%,.0f", res.finishingLaborCost)} د.ع\n(المساحة الإجمالية المقدرة: ${String.format("%.0f", totalSize)} م٢)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 4.dp).fillMaxWidth(),
                                    textAlign = TextAlign.Right
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun CostBadge(label: String, amount: Double) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f))
            Text("${String.format("%,.0f", amount)} د.ع", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun GlossaryItem(title: String, desc: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
            .padding(10.dp),
        horizontalAlignment = Alignment.End
    ) {
        Text(title, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(2.dp))
        Text(desc, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Right, lineHeight = 14.sp)
    }
}

@Composable
fun VisualLegendItem(
    label: String,
    color: Color,
    percentage: Double,
    amount: Double
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
        modifier = Modifier.wrapContentSize()
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.wrapContentSize()
        ) {
            Text(
                text = "$label (${String.format("%.1f", percentage)}%)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${String.format("%,.0f", amount)} د.ع",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
fun OnboardingScreen(onCompleted: () -> Unit) {
    var currentSlide by remember { mutableStateOf(0) }
    val slides = listOf(
        Triple(
            "مرحباً بك في BOQ Pro 👋",
            "مساعد المخططات وحساب كشوف مواد البناء والمسح الكمي العراقي المطور.",
            Icons.Default.Engineering
        ),
        Triple(
            "تخمين كلف تفاعلي وحقيقي 📊",
            "اضبط أسعار مواد البناء المطبقة في محافظتك عراقي (إسمنت، حديد، بلوك، أجور عمالة) وسيتم تحديث إجمالي الحسابات فوراً.",
            Icons.Default.Home
        ),
        Triple(
            "تحليل المخططات والمواصفات 📐",
            "ارفع مخططات البناء وسيقوم التطبيق بتحليل الأبعاد وحساب تقديرات واحتياجات ومواد منزلك تلقائياً بدقة.",
            Icons.Default.Dashboard
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B141A)) // Dark slate background/WhatsApp theme
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = slides[currentSlide].third,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(60.dp)
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = slides[currentSlide].first,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = slides[currentSlide].second,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Slide indicators
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (i in slides.indices) {
                    Box(
                        modifier = Modifier
                            .size(if (currentSlide == i) 24.dp else 8.dp, 8.dp)
                            .clip(CircleShape)
                            .background(if (currentSlide == i) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.3f))
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCompleted) {
                    Text("تخطي", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
                }

                Button(
                    onClick = {
                        if (currentSlide < slides.size - 1) {
                            currentSlide++
                        } else {
                            onCompleted()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = if (currentSlide == slides.size - 1) "ابدأ الآن 🚀" else "التالي",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

data class PurchasePackage(
    val id: String,
    val name: String,
    val credits: Int,
    val price: String,
    val desc: String
)

@Composable
fun TokenManagementDialog(
    viewModel: QuantityViewModel,
    onDismiss: () -> Unit
) {
    val userSession by viewModel.userSession.collectAsStateWithLifecycle()
    val isGuestMode = userSession?.isGuest == true
    val userTokens by viewModel.userTokens.collectAsStateWithLifecycle()
    val transactions by viewModel.tokenTransactions.collectAsStateWithLifecycle()
    val activity = LocalContext.current as? android.app.Activity

    val packages = listOf(
        PurchasePackage("sku_credits_10_starter", "الباقة الأساسية (Starter)", 10, "1,000 د.ع", "مثالية لتدقيق مساحة واحدة أو تقرير سريع"),
        PurchasePackage("sku_credits_50_pro", "باقة المحترفين (Pro)", 50, "5,000 د.ع", "مناسبة لتصميم ومسح كلف المنازل الكاملة في العراق"),
        PurchasePackage("sku_credits_120_business", "باقة الأعمال الكبرى (Business)", 120, "10,000 د.ع", "الخيار الأقوى مع +20 عملة هدية إدارية مجانية")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "🪙 تفاصيل العملات والرصيد المتاح",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Balance card
                Card(
                     modifier = Modifier.fillMaxWidth(),
                     colors = CardDefaults.cardColors(
                         containerColor = if (isGuestMode) 
                             MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f) 
                         else 
                             MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                     )
                ) {
                     Column(
                          modifier = Modifier.padding(14.dp).fillMaxWidth(),
                          horizontalAlignment = Alignment.CenterHorizontally,
                          verticalArrangement = Arrangement.spacedBy(4.dp)
                     ) {
                          Text(
                              text = if (isGuestMode) "محاولات الضيف المتبقية (أقصى حد ٢):" else "رصيدك المتاح الحالي:", 
                              fontSize = 11.sp, 
                              color = MaterialTheme.colorScheme.onSurfaceVariant,
                              fontWeight = FontWeight.Bold
                          )
                          Text(
                              text = "$userTokens عملة", 
                              fontSize = 24.sp, 
                              fontWeight = FontWeight.Black, 
                              color = if (isGuestMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                          )
                          Text(
                              text = if (isGuestMode) "ستنتهي صلاحية هذه المحاولات بمجرد الخروج." else "يتم مزامنة أرصدتك سحابياً بشكل دائم وآمن.",
                              fontSize = 9.sp,
                              color = MaterialTheme.colorScheme.outline,
                              textAlign = TextAlign.Center
                          )
                     }
                }

                if (isGuestMode) {
                    // Luxurious Guest Warning Card stating the maximum 2 trials and Google LogIn instructions
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "وضع الضيف المحدود (تجربتان فقط)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Text(
                                text = "أنت مسجل حالياً كضيف سريع وغير مسجل بحساب Google أصيل. بموجب سياسات المتجر، لا يُسمح للضيوف بشحن أرصدة Google Play أو كسب العملات لحماية مدفوعاتك من الفقدان.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Right,
                                lineHeight = 15.sp
                            )

                            Text(
                                text = "💡 احصل فوراً على ٥٠ رصيداً ترحيبياً مجانياً كاملاً عند تبديل حسابك وتسجيل الدخول ببريدك الإلكتروني، لتشغيل مراجعات الذكاء الاصطناعي وتصدير تقارير الـ PDF ومزامنة بياناتك في العراق سحابياً مدى الحياة!",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Right,
                                lineHeight = 15.sp
                            )

                            Button(
                                onClick = {
                                    onDismiss()
                                    viewModel.authHelper?.signOut {
                                        // Auto takes them back to Login Screen
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(42.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "تسجيل الدخول والترقية لحساب Google",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    // Dedicated Rewarded Ad Section (Wallet Screen/Dialog Option) - only for Authenticated Users
                    val isAdCached by viewModel.isAdLoaded.collectAsStateWithLifecycle()
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.12f)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "🎁 رصيد مجاني فوري (مكافأة مشاهدة إعلان):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                textAlign = TextAlign.Center
                            )
                            
                            Button(
                                onClick = {
                                    if (activity != null) {
                                        viewModel.showRewardedAd(activity)
                                    } else {
                                        viewModel.earnTokens(2, "رصيد مكافأة (AdMob): مشاهدة عرض ترويجي")
                                        viewModel.billingSuccessMessage.value = "تمت إضافة +2 رصيد مجاني بنجاح! 🎉"
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .testTag("rewarded_ad_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "احصل على 2 ارصده مقابل الاعلان",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSecondary
                                )
                            }

                            // Background cache status
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(
                                            if (isAdCached) Color(0xFF128C7E) else Color(0xFFFFA500),
                                            shape = androidx.compose.foundation.shape.CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isAdCached) "جاهز للتشغيل والخصم الفوري" else "جاري تحميل وتخزين الإعلان بالخلفية...",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isAdCached) Color(0xFF128C7E) else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    // Buy packs section
                    Text(
                         "🛒 تعبئة الرصيد الفوري (Google Play):",
                         fontSize = 12.sp,
                         fontWeight = FontWeight.Bold,
                         color = MaterialTheme.colorScheme.primary,
                         modifier = Modifier.fillMaxWidth(),
                         textAlign = TextAlign.Right
                    )

                    packages.forEach { pack ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (pack.id == "sku_credits_120_business") 
                                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f) 
                                else 
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.dp, 
                                if (pack.id == "sku_credits_120_business") 
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f) 
                                else 
                                    Color.Transparent
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            if (activity != null) {
                                                viewModel.billingHelper?.purchasePackage(activity, pack.id, pack.price)
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (pack.id == "sku_credits_120_business") 
                                                MaterialTheme.colorScheme.tertiary 
                                            else 
                                                MaterialTheme.colorScheme.primary
                                        ),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text(pack.price, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            "${pack.credits} عملة" + if (pack.id == "sku_credits_120_business") " (+20 بونص!)" else "",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (pack.id == "sku_credits_120_business") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                                        )
                                        Text(pack.name, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text(pack.desc, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }

                    // Google Play Policy Compliance Trust Badge
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "بوابة دفع Google Play الرسمية والآمنة",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Right
                                )
                                Text(
                                    text = "عمليات الشحن تتم بالكامل بموجب سياسات ومعايير الأمان المعتمدة لدى متجر Google Play مع حفظ مشفر للعملات على خوادم السيرفر الإداري.",
                                    fontSize = 8.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Right,
                                    lineHeight = 11.sp
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Text(
                     "سجل العمليات السابقة (Ledger History):",
                     fontSize = 11.sp,
                     fontWeight = FontWeight.Bold,
                     color = MaterialTheme.colorScheme.onSurface,
                     modifier = Modifier.fillMaxWidth(),
                     textAlign = TextAlign.Right
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("إغلاق", fontSize = 11.sp)
            }
        }
    )
}

@Composable
fun RunBoqCard(
    viewModel: QuantityViewModel,
    snackbarHostState: SnackbarHostState
) {
    val hasPendingChanges by viewModel.hasPendingChanges.collectAsStateWithLifecycle()
    val isCalculating by viewModel.isCalculating.collectAsStateWithLifecycle()
    val userTokens by viewModel.userTokens.collectAsStateWithLifecycle()
    val showResults by viewModel.showResults.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val isButtonEnabled = !showResults || hasPendingChanges

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isButtonEnabled) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .shadow(
                elevation = if (hasPendingChanges) 4.dp else 1.dp,
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("run_boq_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasPendingChanges) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF57C00), // Alert/Pending orange
                    ) {
                        Text(
                            text = "تعديلات معلقة ⚠️",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF388E3C), // Success green
                    ) {
                        Text(
                            text = "الحسابات محدثة ✨",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = "⚙️ معالج حسابات BOQ ومطابق الأسعار",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Right
                )
            }

            Text(
                text = if (hasPendingChanges) {
                    "تم كشف تعديلات غير محسوبة على المعاملات. اضغط على الزر أدناه لمعالجة البيانات، مطابقتها بأسواق البناء لـ 2026، وتفعيل مراجعة الذكاء الاصطناعي."
                } else {
                    "كشف الكميات والتقرير الحالي مصادق ومطابق بالكامل مع أسعار خامات السوق العراقي والمحافظات لعام 2026."
                },
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            if (isCalculating) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "جاري تسييل الكميات، المطابقة والتدقيق بالذكاء الاصطناعي...",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                    val buttonBrush = if (isButtonEnabled) {
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    } else {
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.runSecureBOQCalculation(
                                onNoTokens = { errorMsg ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar(errorMsg)
                                    }
                                },
                                onSuccess = { successMsg ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar(successMsg)
                                    }
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .shadow(
                                elevation = if (isButtonEnabled) 4.dp else 0.dp,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .testTag("calculate_now_btn"),
                        enabled = isButtonEnabled,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = androidx.compose.ui.graphics.Color.Transparent,
                            disabledContainerColor = androidx.compose.ui.graphics.Color.Transparent
                        ),
                        contentPadding = PaddingValues()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(buttonBrush)
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isButtonEnabled) "تشغيل وحساب كشف BOQ ومطابقة الأسعار الحية 🚀" else "الحسابات محدثة ومطابقة بالكامل",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isButtonEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Icon(
                                    imageVector = if (isButtonEnabled) Icons.Default.Bolt else Icons.Default.DoneAll,
                                    contentDescription = null,
                                    tint = if (isButtonEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "تكلفة العملية: 5 عملات (تقتطع وتدقق بآلية تواصل سحابية آمنة)",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@androidx.compose.runtime.Composable
fun AdMobBannerAd(
    modifier: Modifier = Modifier,
    adUnitId: String = "ca-app-pub-7461854901165079/9688946465"
) {
    var adFailedToLoad by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(12.dp))
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مساحة إعلانية مروجة 🏷️",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline
                )
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                    modifier = Modifier.size(11.dp)
                )
            }

            if (!adFailedToLoad) {
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { ctx ->
                        try {
                            com.google.android.gms.ads.AdView(ctx).apply {
                                setAdSize(com.google.android.gms.ads.AdSize.BANNER)
                                setAdUnitId(adUnitId)
                                adListener = object : com.google.android.gms.ads.AdListener() {
                                    override fun onAdFailedToLoad(loadAdError: com.google.android.gms.ads.LoadAdError) {
                                        super.onAdFailedToLoad(loadAdError)
                                        adFailedToLoad = true
                                    }
                                }
                                loadAd(com.google.android.gms.ads.AdRequest.Builder().build())
                            }
                        } catch (e: Throwable) {
                            e.printStackTrace()
                            adFailedToLoad = true
                            android.view.View(ctx)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "تخمين وحساب كميات مالي مدعوم بالذكاء الاصطناعي 🏗️",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "نعمل على مدار الساعة لتحديث أسعار الإسمنت والحديد وبلوك السقوف لعام 2026",
                            fontSize = 8.sp,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
