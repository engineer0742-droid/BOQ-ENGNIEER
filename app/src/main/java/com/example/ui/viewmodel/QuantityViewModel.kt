package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.ProjectDao
import com.example.data.model.ChatMessage
import com.example.data.model.EstimationProject
import com.example.data.model.RoomDetail
import com.example.network.GeminiApiClient
import com.example.utils.QuantityCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class QuantityViewModel(private val projectDao: ProjectDao) : ViewModel() {

    // Custom Gemini API Key State
    val customApiKey = MutableStateFlow("")

    fun loadCustomApiKey(context: Context) {
        val sharedPrefs = context.getSharedPreferences("boq_auth_prefs", Context.MODE_PRIVATE)
        val key = sharedPrefs.getString("custom_gemini_api_key", "") ?: ""
        customApiKey.value = key
        com.example.network.GeminiApiClient.customApiKey = key.ifBlank { null }
    }

    fun saveCustomApiKey(context: Context, key: String) {
        val sharedPrefs = context.getSharedPreferences("boq_auth_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("custom_gemini_api_key", key.trim()).apply()
        customApiKey.value = key.trim()
        com.example.network.GeminiApiClient.customApiKey = key.trim().ifBlank { null }
    }

    // PDF Architectural File States
    val pdfFileName = MutableStateFlow<String?>(null)
    val pdfFileSize = MutableStateFlow<String?>(null)
    val pdfFileUri = MutableStateFlow<android.net.Uri?>(null)
    val isPdfValid = MutableStateFlow<Boolean?>(null)
    val pdfValidationError = MutableStateFlow<String?>(null)
    val isPdfAnalyzing = MutableStateFlow(false)

    fun resetPdfState() {
        pdfFileName.value = null
        pdfFileSize.value = null
        pdfFileUri.value = null
        isPdfValid.value = null
        pdfValidationError.value = null
        isPdfAnalyzing.value = false
    }

    fun handleSelectedPdf(context: Context, uri: android.net.Uri) {
        val contentResolver = context.contentResolver
        
        // 1. Get MIME type
        val mimeType = contentResolver.getType(uri)
        val mimeOk = mimeType != null && mimeType.equals("application/pdf", ignoreCase = true)

        // 2. Query file name and size from content provider
        var displayName: String? = null
        var sizeInBytes: Long? = null
        
        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        displayName = cursor.getString(nameIndex)
                    }
                    if (sizeIndex != -1) {
                        sizeInBytes = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // If displayName is null, try to extract from uri path
        if (displayName == null) {
            displayName = uri.lastPathSegment ?: "unknown_architectural_plan.pdf"
        }

        val extensionOk = displayName?.endsWith(".pdf", ignoreCase = true) == true

        // 3. Header check: Read first 5 bytes for "%PDF"
        var magicBytesOk = false
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val header = ByteArray(5)
                val bytesRead = inputStream.read(header)
                if (bytesRead >= 4) {
                    val headerStr = String(header, 0, bytesRead, Charsets.US_ASCII)
                    if (headerStr.startsWith("%PDF")) {
                        magicBytesOk = true
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Final Validation
        if (mimeOk || extensionOk || magicBytesOk) {
            isPdfValid.value = true
            pdfValidationError.value = null
            pdfFileName.value = displayName
            pdfFileUri.value = uri
            
            // Format size
            pdfFileSize.value = if (sizeInBytes != null) {
                val kb = sizeInBytes / 1024.0
                if (kb > 1024.0) {
                    java.util.Formatter().format("%.2f MB", kb / 1024.0).toString()
                } else {
                    java.util.Formatter().format("%.1f KB", kb).toString()
                }
            } else {
                "حجم غير معروف"
            }
        } else {
            isPdfValid.value = false
            pdfFileName.value = displayName
            pdfFileUri.value = uri
            pdfFileSize.value = null
            pdfValidationError.value = "الملف المختار لا يبدو بتنسيق PDF صالح للخرائط الإنشائية. يجب أن يكون امتداد الملف .pdf ويبدأ بترميز وثائق PDF الصحيح (%PDF)."
        }
    }

    // Beautiful simulated PDF architecture extraction!
    fun simulatePdfAnalysis(onAnalysisComplete: (String) -> Unit) {
        viewModelScope.launch {
            isPdfAnalyzing.value = true
            kotlinx.coroutines.delay(2000) // Beautiful 2-second simulation
            isPdfAnalyzing.value = false
            
            // Randomly customize some values or use preset templates based on plan name
            val name = pdfFileName.value?.lowercase() ?: ""
            if (name.contains("200") || name.contains("١٠") || name.contains("20")) {
                plotArea.value = "200"
                buildArea.value = "165"
                floorsCount.value = 2
                foundationType.value = "RAFT"
                buildingType.value = "STRUCTURAL"
                
                // Add some default rooms:
                _rooms.value = listOf(
                    RoomDetail(id = UUID.randomUUID().toString(), name = "صالة الاستقبال", width = 6.0, length = 4.0, height = 3.0),
                    RoomDetail(id = UUID.randomUUID().toString(), name = "غرفة معيشة", width = 5.0, length = 4.5, height = 3.0),
                    RoomDetail(id = UUID.randomUUID().toString(), name = "مطبخ حار وبارد", width = 4.0, length = 3.5, height = 3.0),
                    RoomDetail(id = UUID.randomUUID().toString(), name = "غرفة نوم رئيسية", width = 5.0, length = 4.0, height = 3.0),
                    RoomDetail(id = UUID.randomUUID().toString(), name = "مجموعة صحية", width = 2.5, length = 2.0, height = 3.0)
                )
                calculateQuantities(forceCalculate = true)
                onAnalysisComplete("تم استيراد مخطط الطابقين بمساحة إجمالية تفوق ١٦٥م٢ ومساحة أرض ٢٠٠م٢ مع دمج ٥ غرف أساسية بنجاح! 📐")
            } else if (name.contains("150") || name.contains("١٥٠") || name.contains("7") || name.contains("7.5")) {
                plotArea.value = "150"
                buildArea.value = "120"
                floorsCount.value = 1
                foundationType.value = "STRIP"
                buildingType.value = "STRUCTURAL"
                
                _rooms.value = listOf(
                    RoomDetail(id = UUID.randomUUID().toString(), name = "استقبال ضيوف", width = 5.0, length = 4.0, height = 3.0),
                    RoomDetail(id = UUID.randomUUID().toString(), name = "صالة هول", width = 4.0, length = 4.0, height = 3.0),
                    RoomDetail(id = UUID.randomUUID().toString(), name = "غرفة نوم غيست", width = 4.5, length = 3.8, height = 3.0),
                    RoomDetail(id = UUID.randomUUID().toString(), name = "مطبخ مفتوح", width = 3.5, length = 3.5, height = 3.0)
                )
                calculateQuantities(forceCalculate = true)
                onAnalysisComplete("تم استخراج قياسات مخطط البيت الصغير (١٥٠م٢). المدخل والاستقبال و٤ غرف مسقوفة تم توثيقها بنجاح! 📐")
            } else {
                // Generic extraction
                plotArea.value = "250"
                buildArea.value = "180"
                floorsCount.value = 2
                foundationType.value = "RAFT"
                buildingType.value = "STRUCTURAL"
                _rooms.value = listOf(
                    RoomDetail(id = UUID.randomUUID().toString(), name = "الديوانية", width = 7.0, length = 5.0, height = 3.0),
                    RoomDetail(id = UUID.randomUUID().toString(), name = "صالة داخلية", width = 6.0, length = 4.0, height = 3.0),
                    RoomDetail(id = UUID.randomUUID().toString(), name = "مطبخ", width = 4.0, length = 4.0, height = 3.0),
                    RoomDetail(id = UUID.randomUUID().toString(), name = "غرفة نوم للوالدين", width = 4.5, length = 4.0, height = 3.0)
                )
                calculateQuantities(forceCalculate = true)
                onAnalysisComplete("تم قراءة المخطط المطور للمساحة الكبيرة. مساحة البناء ١٨٠م٢، وتم إضافة ٤ غرف رئيسية واسعة بنجاح! 📐")
            }
        }
    }

    // Inputs
    val plotArea = MutableStateFlow("200")
    val buildArea = MutableStateFlow("150")
    val floorsCount = MutableStateFlow(1)
    val buildingType = MutableStateFlow("STRUCTURAL") // STRUCTURAL, LOAD_BEARING
    val foundationType = MutableStateFlow("RAFT") // RAFT, STRIP
    val materialType = MutableStateFlow("BLOCK") // YELLOW_BRICK, RED_BRICK, BLOCK, THERMOSTONE

    // Materials Custom Prices (in Iraqi Dinars - IQD)
    val cementPricePerTon = MutableStateFlow("140000")
    val steelPricePerTon = MutableStateFlow("1050000")
    val sandPricePerM3 = MutableStateFlow("15000")
    val gravelPricePerM3 = MutableStateFlow("18000")
    val brickPricePer1000 = MutableStateFlow("90000")
    val laborPricePerM2 = MutableStateFlow("40000")

    // Detailed Interior Finishes Custom Settings
    val flooringType = MutableStateFlow("PORCELAIN") // CERAMIC, PORCELAIN, MARBLE
    val ceilingType = MutableStateFlow("GYPSUM") // PLASTER, GYPSUM, PVC
    val doorsCount = MutableStateFlow("6")
    val doorUnitPrice = MutableStateFlow("180000")
    val windowsCount = MutableStateFlow("5")
    val windowUnitPrice = MutableStateFlow("150000")
    val sanitaryQuality = MutableStateFlow("STANDARD") // BASIC, STANDARD, LUXURY
    val electricalQuality = MutableStateFlow("STANDARD") // BASIC, STANDARD, SMART

    // Optional Finishes Selection State Toggles (All customizable and toggleable)
    val isCeilingsEnabled = MutableStateFlow(true)
    val isDoorsEnabled = MutableStateFlow(true)
    val isWindowsEnabled = MutableStateFlow(true)
    val isPaintingEnabled = MutableStateFlow(true)
    val isPavementEnabled = MutableStateFlow(false)
    val isExteriorStuccoEnabled = MutableStateFlow(false)
    val isExternalStoneEnabled = MutableStateFlow(false)

    // Detailed Customizable Rates for Finishes (Iraqi Market context)
    val plasterPricePerM2 = MutableStateFlow("8000")
    val gessoPricePerM2 = MutableStateFlow("4000")
    val ceramicPricePerM2 = MutableStateFlow("10000")
    val porcelainPricePerM2 = MutableStateFlow("18000")
    val marblePricePerM2 = MutableStateFlow("35000")
    val pipingPricePerM2 = MutableStateFlow("5000")
    val wiringPricePerM2 = MutableStateFlow("6000")

    val ceilingPlasterPricePerM2 = MutableStateFlow("5000")
    val ceilingPvcPricePerM2 = MutableStateFlow("12000")
    val ceilingGypsumPricePerM2 = MutableStateFlow("15000")
    val paintingPricePerM2 = MutableStateFlow("6000")
    val pavementPricePerMeter = MutableStateFlow("15000")
    val exteriorStuccoPricePerM2 = MutableStateFlow("7000")
    val externalStonePricePerM2 = MutableStateFlow("45000")

    val sanitaryBasicPrice = MutableStateFlow("1200000")
    val sanitaryStandardPrice = MutableStateFlow("2500000")
    val sanitaryLuxuryPrice = MutableStateFlow("4500000")

    val electricalBasicPrice = MutableStateFlow("1000000")
    val electricalStandardPrice = MutableStateFlow("2200000")
    val electricalSmartPrice = MutableStateFlow("4000000")

    // Themes (Light/Dark)
    val isDarkMode = MutableStateFlow(true)

    // Land reclamation optional/guess settings
    val isReclamationEnabled = MutableStateFlow(false)
    val reclamationCostInput = MutableStateFlow("0")
    val reclamationNature = MutableStateFlow("NATURAL") // NATURAL, CLAY, SANDY, DEPRESSION, MOUNTAIN
    val reclamationExplanation = MutableStateFlow("")
    val isReclamationLoading = MutableStateFlow(false)

    // Manual / Automatic Labor Cost Options (for main and finishes)
    val isManualLaborEnabled = MutableStateFlow(false)
    val manualLaborCost = MutableStateFlow("8000000")
    val isManualFinishingLaborEnabled = MutableStateFlow(false)
    val finishingLaborPricePerM2 = MutableStateFlow("15000")
    val manualFinishingLaborCost = MutableStateFlow("3500000")

    // Measurement Market Visibility (toggled via AI drawing analysis)
    val isMeasurementMarketVisible = MutableStateFlow(false)

    // --- Calculation and AI Review Engine States ---
    val isCalculating = MutableStateFlow(false)
    val hasPendingChanges = MutableStateFlow(true)
    val showResults = MutableStateFlow(false)
    val aiReviewResult = MutableStateFlow<String?>(null)
    val isAiReviewLoading = MutableStateFlow(false)

    // --- monetization / token system / onboarding ---
    val userTokens = MutableStateFlow(50) // Starting standard tokens balance

    // --- Authentication & User Session ---
    var authHelper: com.example.network.FirebaseAuthHelper? = null
        private set

    val userSession = MutableStateFlow<com.example.network.FirebaseAuthHelper.UserSession?>(null)

    fun setupAuth(context: Context) {
        if (authHelper == null) {
            val helper = com.example.network.FirebaseAuthHelper.getInstance(context)
            authHelper = helper
            
            // Collect/sync user session from authHelper
            viewModelScope.launch {
                helper.currentUserFlow.collect { session ->
                    userSession.value = session
                    if (session != null) {
                        userTokens.value = session.credits
                        com.example.network.BillingBackendApi.syncServerLedger(session.credits)
                    }
                }
            }

            // Sync userTokens back to Firestore/Simulation when it updates
            viewModelScope.launch {
                userTokens.collect { currentBalance ->
                    val currentSession = helper.currentUserFlow.value
                    if (currentSession != null && currentSession.credits != currentBalance) {
                        helper.updateFirestoreBalance(currentBalance)
                    }
                }
            }
        }
    }

    // Model class for tokens transactions
    data class TransactionItem(
        val id: String,
        val text: String,
        val tokens: Int,
        val isEarned: Boolean,
        val timestamp: Long
    )

    private val _tokenTransactions = MutableStateFlow<List<TransactionItem>>(
        listOf(
            TransactionItem(
                id = UUID.randomUUID().toString(),
                text = "رصيد ترحيبي مجاني للمهندس الذكي لتجربة التطبيق",
                tokens = 50,
                isEarned = true,
                timestamp = System.currentTimeMillis() - 86400000 // 1 day ago
            )
        )
    )
    val tokenTransactions: StateFlow<List<TransactionItem>> = _tokenTransactions.asStateFlow()

    // Google Play Billing Client Helper
    var billingHelper: com.example.network.PlayBillingHelper? = null
        private set

    // Simple toast or messaging event flow
    val billingErrorMessage = MutableStateFlow<String?>(null)
    val billingSuccessMessage = MutableStateFlow<String?>(null)

    fun setupBilling(context: Context) {
        setupAuth(context)
        loadCustomApiKey(context)
        if (billingHelper == null) {
            billingHelper = com.example.network.PlayBillingHelper(
                context = context.applicationContext,
                externalScope = viewModelScope,
                onTokensCredited = { credited, orderId, newBalance ->
                    // Webhook verification succeeded on server! Sync and log
                    userTokens.value = newBalance
                    _tokenTransactions.value = listOf(
                        TransactionItem(
                            id = orderId,
                            text = "شحن رصيد إداري (IAP): تم تأكيد شحنة باقة Google Play بنجاح (رقم الحركة: $orderId)",
                            tokens = credited,
                            isEarned = true,
                            timestamp = System.currentTimeMillis()
                        )
                    ) + _tokenTransactions.value
                    billingSuccessMessage.value = "تمت عملية الشراء بنجاح! تم شحن حسابك بـ +$credited عملة 🚀"
                    com.example.network.BillingBackendApi.syncServerLedger(newBalance)
                },
                onPurchaseFailed = { errorMsg ->
                    billingErrorMessage.value = errorMsg
                }
            )
            // Sync initial server ledger balance with current client balance
            com.example.network.BillingBackendApi.syncServerLedger(userTokens.value)
        }
        setupAdMob(context)
    }

    // AdMob Helper variables & functions
    var rewardedAdHelper: com.example.network.RewardedAdHelper? = null
        private set

    val isAdLoaded = MutableStateFlow(false)

    fun setupAdMob(context: Context) {
        if (rewardedAdHelper == null) {
            rewardedAdHelper = com.example.network.RewardedAdHelper(
                context = context.applicationContext,
                onRewardEarned = { rewardAmount ->
                    secureEarnCreditsFromAd(rewardAmount)
                },
                onAdFailedToLoad = { msg ->
                    isAdLoaded.value = false
                    android.util.Log.e("QuantityViewModel", "AdMob Reward load failed: $msg")
                },
                onAdFailedToShow = { msg ->
                    billingErrorMessage.value = msg
                    isAdLoaded.value = false
                },
                onAdLoaded = {
                    isAdLoaded.value = true
                }
            )
        }
    }

    private fun secureEarnCreditsFromAd(amount: Int) {
        viewModelScope.launch {
            val serverResult = com.example.network.BillingBackendApi.secureEarnAdCredits(
                userId = "user_iraqi_boq_2026",
                amountEarned = amount
            )
            if (serverResult.success) {
                userTokens.value = serverResult.currentBalance
                _tokenTransactions.value = listOf(
                    TransactionItem(
                        id = serverResult.orderId,
                        text = "كسب رصيد مكافأة (AdMob): مشاهدة إعلان فيديو ترويجي بنجاح (رقم الحركة: ${serverResult.orderId})",
                        tokens = amount,
                        isEarned = true,
                        timestamp = System.currentTimeMillis()
                    )
                ) + _tokenTransactions.value
                billingSuccessMessage.value = "تهانينا! تم احتساب المشاهدة وإضافة +$amount رصيد مكافأة إلى حسابك بنجاح! 🎉"
            } else {
                billingErrorMessage.value = "حدث خطأ أثناء مزامنة المكافأة مع الخادم."
            }
        }
    }

    fun showRewardedAd(activity: android.app.Activity) {
        val helper = rewardedAdHelper
        if (helper != null) {
            helper.showAd(activity)
        } else {
            billingErrorMessage.value = "نظام الإعلانات غير مهيأ بعد. يرجى المحاولة بعد قليل."
        }
    }

    /**
     * PDF Export security checker. Securely calls Simulated Remote cloud to validate balance
     * and deduct exactly 2 credits (or 1 credit for guests) before producing download artifact.
     */
    fun secureDeductPDFExport(
        onNoTokens: (String) -> Unit,
        onSuccess: (String) -> Unit
    ) {
        viewModelScope.launch {
            val isGuestMode = userSession.value?.isGuest == true
            val deductionAmount = if (isGuestMode) 1 else 2
            
            // Secure administrative server-side validation and deduction
            val serverResult = com.example.network.BillingBackendApi.secureDeduct(
                userId = userSession.value?.uid ?: "user_iraqi_boq_2026",
                amount = deductionAmount,
                actionType = "EXPORT_PDF",
                currentLocalBalance = userTokens.value
            )

            if (serverResult.success) {
                // Succeeded! Sync client balance with server-side ledger
                userTokens.value = serverResult.remainingBalance
                
                val ledgerText = if (isGuestMode) {
                    "سحب تجربة ضيف (تصدير كشف PDF): متبقي ${serverResult.remainingBalance} محاولة للضيف"
                } else {
                    "سحب رصيد (تصدير): طباعة وتصدير كشف الكميات PDF وجدول الأسعار العراقي (رقم السيرفر: ${serverResult.serverTransactionId})"
                }

                // Add secure ledger transaction
                _tokenTransactions.value = listOf(
                    TransactionItem(
                        id = serverResult.serverTransactionId,
                        text = ledgerText,
                        tokens = deductionAmount,
                        isEarned = false,
                        timestamp = System.currentTimeMillis()
                    )
                ) + _tokenTransactions.value

                onSuccess("تم إثبات كلفة التصدير وخصم $deductionAmount عملة بنجاح! متبقي لديك ${serverResult.remainingBalance} رصيد.")
            } else {
                val errorMsg = if (isGuestMode) {
                    "عذراً! انتهت محاولات الضيف المجانية المتاحة لك (٢ محاولة للضيف). يرجى تسجيل الدخول بحساب Google الأصلي للحصول على ٥٠ رصيد إضافي كامل ومطابقة الأسعار الحية ومراجعة الذكاء الاصطناعي!"
                } else {
                    "رصيدك غير كافٍ لتصدير التقرير كـ PDF. متبقي لديك ${userTokens.value} وحدة، وتحتاج إلى $deductionAmount عملة شحن."
                }
                onNoTokens(errorMsg)
            }
        }
    }

    // Onboarding slide status
    val hasCompletedOnboarding = MutableStateFlow(false)

    // Method to consume tokens safely
    fun consumeTokens(amount: Int, purpose: String): Boolean {
        if (userTokens.value >= amount) {
            userTokens.value -= amount
            _tokenTransactions.value = listOf(
                TransactionItem(
                    id = UUID.randomUUID().toString(),
                    text = purpose,
                    tokens = amount,
                    isEarned = false,
                    timestamp = System.currentTimeMillis()
                )
            ) + _tokenTransactions.value
            return true
        }
        return false
    }

    // Method to earn tokens
    fun earnTokens(amount: Int, source: String) {
        userTokens.value += amount
        _tokenTransactions.value = listOf(
            TransactionItem(
                id = UUID.randomUUID().toString(),
                text = source,
                tokens = amount,
                isEarned = true,
                timestamp = System.currentTimeMillis()
            )
        ) + _tokenTransactions.value
    }

    // Reset/all data deletion (for "Delete Account" legal compliance)
    fun deleteUserAccountAndData() {
        viewModelScope.launch {
            // Remove all local saved projects
            savedProjects.value.forEach {
                deleteProject(it)
            }
            // Reset state
            userTokens.value = 50
            isMeasurementMarketVisible.value = false
            hasCompletedOnboarding.value = false
            _tokenTransactions.value = listOf(
                TransactionItem(
                    id = UUID.randomUUID().toString(),
                    text = "تم إعادة تعيين الحساب ومسح سجلات الهاتف بنجاح",
                    tokens = 50,
                    isEarned = true,
                    timestamp = System.currentTimeMillis()
                )
            )
            clearChat()
        }
    }

    // Saved projects from local database
    val savedProjects: StateFlow<List<EstimationProject>> = projectDao.getAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // List of custom rooms with dimensions
    private val _rooms = MutableStateFlow<List<RoomDetail>>(
        listOf(
            RoomDetail(UUID.randomUUID().toString(), "الديوانية (المجلس)", 6.0, 4.0, 3.2),
            RoomDetail(UUID.randomUUID().toString(), "غرفة النوم الرئيسية", 5.0, 4.0, 3.2),
            RoomDetail(UUID.randomUUID().toString(), "المطبخ", 4.0, 3.5, 3.2),
            RoomDetail(UUID.randomUUID().toString(), "صالة المعيشة", 5.0, 5.0, 3.2),
            RoomDetail(UUID.randomUUID().toString(), "حمام ومرافق صحية", 2.5, 2.0, 3.2)
        )
    )
    val rooms: StateFlow<List<RoomDetail>> = _rooms.asStateFlow()

    // Active screen index
    val selectedTab = MutableStateFlow(0)

    // Calculation result
    private val _calculationResult = MutableStateFlow<QuantityCalculator.EstimationResult?>(null)
    val calculationResult: StateFlow<QuantityCalculator.EstimationResult?> = _calculationResult.asStateFlow()

    // Gemini AI Chat states
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                id = UUID.randomUUID().toString(),
                text = "مرحباً بك! أنا المهندس الذكي، مستشارك الخاص بمواد وكلف البناء.\nأدخل بيانات منزلك والكميات، وسأقوم بتحليلها وإعطائك نصائح حقيقية لتوفير واختيار أفضل المواد.",
                isUser = false
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    init {
        // Run initial calculation
        calculateQuantities(forceCalculate = true)
        showResults.value = false
        hasPendingChanges.value = true
    }

    // Calculation Method
    fun calculateQuantities(forceCalculate: Boolean = false) {
        if (!forceCalculate) {
            hasPendingChanges.value = true
            showResults.value = false
            return
        }
        val cement = cementPricePerTon.value.toDoubleOrNull() ?: 140000.0
        val steel = steelPricePerTon.value.toDoubleOrNull() ?: 1050000.0
        val sand = sandPricePerM3.value.toDoubleOrNull() ?: 15000.0
        val gravel = gravelPricePerM3.value.toDoubleOrNull() ?: 18000.0
        val brick = brickPricePer1000.value.toDoubleOrNull() ?: 90000.0
        val labor = laborPricePerM2.value.toDoubleOrNull() ?: 40000.0

        // Parse customizable rates
        val plasterRate = plasterPricePerM2.value.toDoubleOrNull() ?: 8000.0
        val gessoRate = gessoPricePerM2.value.toDoubleOrNull() ?: 4000.0
        val ceramicRate = ceramicPricePerM2.value.toDoubleOrNull() ?: 10000.0
        val porcelainRate = porcelainPricePerM2.value.toDoubleOrNull() ?: 18000.0
        val marbleRate = marblePricePerM2.value.toDoubleOrNull() ?: 35000.0
        val pipingRate = pipingPricePerM2.value.toDoubleOrNull() ?: 5000.0
        val wiringRate = wiringPricePerM2.value.toDoubleOrNull() ?: 6000.0

        val ceilingPlasterRate = ceilingPlasterPricePerM2.value.toDoubleOrNull() ?: 5000.0
        val ceilingPvcRate = ceilingPvcPricePerM2.value.toDoubleOrNull() ?: 12000.0
        val ceilingGypsumRate = ceilingGypsumPricePerM2.value.toDoubleOrNull() ?: 15000.0
        val paintRate = paintingPricePerM2.value.toDoubleOrNull() ?: 6000.0
        val pavementRate = pavementPricePerMeter.value.toDoubleOrNull() ?: 15000.0
        val exteriorStuccoRate = exteriorStuccoPricePerM2.value.toDoubleOrNull() ?: 7000.0
        val externalStoneRate = externalStonePricePerM2.value.toDoubleOrNull() ?: 45000.0

        val sanitaryBasic = sanitaryBasicPrice.value.toDoubleOrNull() ?: 1200000.0
        val sanitaryStandard = sanitaryStandardPrice.value.toDoubleOrNull() ?: 2500000.0
        val sanitaryLuxury = sanitaryLuxuryPrice.value.toDoubleOrNull() ?: 4500000.0

        val electricalBasic = electricalBasicPrice.value.toDoubleOrNull() ?: 1000000.0
        val electricalStandard = electricalStandardPrice.value.toDoubleOrNull() ?: 2200000.0
        val electricalSmart = electricalSmartPrice.value.toDoubleOrNull() ?: 4000000.0

        val plot = plotArea.value.toDoubleOrNull() ?: 200.0
        val build = buildArea.value.toDoubleOrNull() ?: 150.0

        val doorsVal = doorsCount.value.toIntOrNull() ?: 6
        val doorPriceVal = doorUnitPrice.value.toDoubleOrNull() ?: 180000.0
        val windowsVal = windowsCount.value.toIntOrNull() ?: 5
        val windowPriceVal = windowUnitPrice.value.toDoubleOrNull() ?: 150000.0

        val recPrice = if (isReclamationEnabled.value) {
            reclamationCostInput.value.toDoubleOrNull() ?: 0.0
        } else {
            0.0
        }

        val manualLaborVal = manualLaborCost.value.toDoubleOrNull() ?: 8000000.0
        val manualFinishingLaborVal = manualFinishingLaborCost.value.toDoubleOrNull() ?: 3500000.0
        val finishingLaborRate = finishingLaborPricePerM2.value.toDoubleOrNull() ?: 15000.0

        _calculationResult.value = QuantityCalculator.calculate(
            plotArea = plot,
            buildArea = build,
            floorsCount = floorsCount.value,
            buildingType = buildingType.value,
            foundationType = foundationType.value,
            materialType = materialType.value,
            rooms = _rooms.value,
            cementPrice = cement,
            steelPrice = steel,
            sandPrice = sand,
            gravelPrice = gravel,
            brickPrice = brick,
            laborPricePerM2 = labor,
            
            plasterPricePerM2 = plasterRate,
            gessoPricePerM2 = gessoRate,
            ceramicPricePerM2 = ceramicRate,
            porcelainPricePerM2 = porcelainRate,
            marblePricePerM2 = marbleRate,
            pipingPricePerM2 = pipingRate,
            wiringPricePerM2 = wiringRate,
            
            ceilingPlasterPricePerM2 = ceilingPlasterRate,
            ceilingPvcPricePerM2 = ceilingPvcRate,
            ceilingGypsumPricePerM2 = ceilingGypsumRate,
            paintingPricePerM2 = paintRate,
            pavementPricePerMeter = pavementRate,
            exteriorStuccoPricePerM2 = exteriorStuccoRate,
            externalStonePricePerM2 = externalStoneRate,
            
            sanitaryBasicPrice = sanitaryBasic,
            sanitaryStandardPrice = sanitaryStandard,
            sanitaryLuxuryPrice = sanitaryLuxury,
            
            electricalBasicPrice = electricalBasic,
            electricalStandardPrice = electricalStandard,
            electricalSmartPrice = electricalSmart,
            
            flooringType = flooringType.value,
            ceilingType = ceilingType.value,
            
            isCeilingsEnabled = isCeilingsEnabled.value,
            isDoorsEnabled = isDoorsEnabled.value,
            doorsCount = doorsVal,
            doorUnitPrice = doorPriceVal,
            
            isWindowsEnabled = isWindowsEnabled.value,
            windowsCount = windowsVal,
            windowUnitPrice = windowPriceVal,
            
            isPaintingEnabled = isPaintingEnabled.value,
            isPavementEnabled = isPavementEnabled.value,
            isExteriorStuccoEnabled = isExteriorStuccoEnabled.value,
            isExternalStoneEnabled = isExternalStoneEnabled.value,
            
            sanitaryQuality = sanitaryQuality.value,
            electricalQuality = electricalQuality.value,
            reclamationCost = recPrice,
            isManualLaborEnabled = isManualLaborEnabled.value,
            manualLaborCost = manualLaborVal,
            isManualFinishingLaborEnabled = isManualFinishingLaborEnabled.value,
            finishingLaborPricePerM2 = finishingLaborRate,
            manualFinishingLaborCost = manualFinishingLaborVal
        )
    }

    // Dynamic Room Operations
    fun addRoom(name: String, width: Double, length: Double, height: Double) {
        val newRoom = RoomDetail(UUID.randomUUID().toString(), name, width, length, height)
        _rooms.value = _rooms.value + newRoom
        calculateQuantities()
    }

    fun updateRoomDimensions(id: String, width: Double, length: Double, height: Double) {
        _rooms.value = _rooms.value.map {
            if (it.id == id) it.copy(width = width, length = length, height = height) else it
        }
        calculateQuantities()
    }

    fun deleteRoom(id: String) {
        _rooms.value = _rooms.value.filter { it.id != id }
        calculateQuantities()
    }

    fun clearRooms() {
        _rooms.value = emptyList()
        calculateQuantities()
    }

    // Save project in SQLite Room database
    fun saveProject(title: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val result = _calculationResult.value ?: return@launch
            val project = EstimationProject(
                title = title,
                date = System.currentTimeMillis(),
                plotArea = plotArea.value.toDoubleOrNull() ?: 200.0,
                buildArea = buildArea.value.toDoubleOrNull() ?: 150.0,
                floorsCount = floorsCount.value,
                buildingType = buildingType.value,
                foundationType = foundationType.value,
                materialType = materialType.value,
                roomsJson = Json.encodeToString(_rooms.value),
                
                totalCost = result.totalCost,
                cementTons = result.cementTons,
                steelTons = result.totalSteelTons,
                sandM3 = result.sandM3,
                gravelM3 = result.gravelM3,
                unitsCount = result.masonryUnitsCount,

                flooringType = flooringType.value,
                ceilingType = ceilingType.value,
                doorsCount = doorsCount.value.toIntOrNull() ?: 6,
                doorUnitPrice = doorUnitPrice.value.toDoubleOrNull() ?: 180000.0,
                windowsCount = windowsCount.value.toIntOrNull() ?: 5,
                windowUnitPrice = windowUnitPrice.value.toDoubleOrNull() ?: 150000.0,
                sanitaryQuality = sanitaryQuality.value,
                electricalQuality = electricalQuality.value,
                isReclamationEnabled = isReclamationEnabled.value,
                reclamationCost = reclamationCostInput.value.toDoubleOrNull() ?: 0.0,
                reclamationNature = reclamationNature.value,
                reclamationExplanation = reclamationExplanation.value
            )
            projectDao.insertProject(project)
            onSuccess()
        }
    }

    fun loadProject(project: EstimationProject) {
        plotArea.value = project.plotArea.toString()
        buildArea.value = project.buildArea.toString()
        floorsCount.value = project.floorsCount
        buildingType.value = project.buildingType
        foundationType.value = project.foundationType
        materialType.value = project.materialType
        
        flooringType.value = project.flooringType
        ceilingType.value = project.ceilingType
        doorsCount.value = project.doorsCount.toString()
        doorUnitPrice.value = project.doorUnitPrice.toInt().toString()
        windowsCount.value = project.windowsCount.toString()
        windowUnitPrice.value = project.windowUnitPrice.toInt().toString()
        sanitaryQuality.value = project.sanitaryQuality
        electricalQuality.value = project.electricalQuality
        isReclamationEnabled.value = project.isReclamationEnabled
        reclamationCostInput.value = project.reclamationCost.toInt().toString()
        reclamationNature.value = project.reclamationNature
        reclamationExplanation.value = project.reclamationExplanation

        try {
            _rooms.value = Json.decodeFromString<List<RoomDetail>>(project.roomsJson)
        } catch (e: Exception) {
            // keep old if parse error
        }
        calculateQuantities()
    }

    fun deleteProject(project: EstimationProject) {
        viewModelScope.launch {
            projectDao.deleteProjectById(project.id)
        }
    }

    // toggles dark theme
    fun toggleDarkMode() {
        isDarkMode.value = !isDarkMode.value
    }

    // Send chat message to Gemini AI Assistant representing an expert Civil Engineer
    fun sendChatMessage(messageText: String) {
        if (messageText.isBlank()) return

        val userMessage = ChatMessage(id = UUID.randomUUID().toString(), text = messageText, isUser = true)
        _chatMessages.value = _chatMessages.value + userMessage
        _isChatLoading.value = true

        viewModelScope.launch {
            val calcState = _calculationResult.value
            val contextContext = if (calcState != null) {
                "سياق المشروع الحالي للمستخدم:\n" +
                "- مساحة الأرض: ${plotArea.value} م٢، مساحة البناء: ${buildArea.value} م٢، الطوابق: ${floorsCount.value}\n" +
                "- الهيكل الإنشائي: ${if (buildingType.value == "STRUCTURAL") "هيكل خرساني مسلح (أعمدة وجسور)" else "جدران حاملة"}\n" +
                "- الأساس: ${if (foundationType.value == "RAFT") "أساس حصيري مسطح (Raft)" else "أساس شريطي (Strip)"}\n" +
                "- خامة الجدران: ${materialType.value}\n" +
                "- تفاصيل تشطيبات الدواخل والمنزل:\n" +
                "  * نوع الأرضيات: ${flooringType.value} (كلفة الأرضيات: ${String.format("%,.0f", calcState.tileCost)} د.ع)\n" +
                "  * السقف والديكور: ${ceilingType.value} (كلفة السقوف: ${String.format("%,.0f", calcState.ceilingCost)} د.ع)\n" +
                "  * كلفة الأبواب الداخليات (${doorsCount.value} أبواب): ${String.format("%,.0f", calcState.doorsCost)} د.ع\n" +
                "  * كلفة الشبابيك الخارجيات (${windowsCount.value} شبابيك): ${String.format("%,.0f", calcState.windowsCost)} د.ع\n" +
                "  * تأسيس المنظومة الصحية والسباكة فئة: ${sanitaryQuality.value} (بكلفة: ${String.format("%,.0f", calcState.sanitaryCost)} د.ع)\n" +
                "  * تأسيس النواحي الكهربائية والإنارة فئة: ${electricalQuality.value} (بكلفة: ${String.format("%,.0f", calcState.electricalCost)} د.ع)\n" +
                "- تفاصيل الـ BOQ والكميات المحسوبة إجمالياً:\n" +
                "  * الكلفة التقريبية الكلية: ${String.format("%,.0f", calcState.totalCost)} دينار عراقي\n" +
                "  * كلفة تشطيبات الدواخل الإجمالية: ${String.format("%,.0f", calcState.finishingCost)} دينار عراقي\n" +
                "  * كمية الإسمنت المطلوبة: ${String.format("%.2f", calcState.cementTons)} طن\n" +
                "  * كمية جدران الطابوق/البلوك المطلوبة: ${calcState.masonryUnitsCount} وحدة بنائية\n" +
                "  * الحديد والمسلحات: ${String.format("%.2f", calcState.totalSteelTons)} طن\n" +
                "  * عدد الغرف المفصلة المدخلة: ${_rooms.value.size} غرف."
            } else {
                "لا توجد كميات حالية محسوبة بعد."
            }

            val systemInstruction = """
                أنت 'المهندس الذكي'، مستشار ومهندس مدني عراقي وعربي خبير متخصص في حساب كميات مواد البناء (BOQ) ودراسات الكفاءة الإنشائية وتقليل الهدر المالي.
                تتميز بأسلوب مهني، علمي، حثيث ومبسط جداً لمساعدة الملاك وأصحاب المنازل في بناء منازلهم في العراق والشرق الأوسط بشكل اقتصادي وسليم.
                استخدم دائماً تعبيرات سوق البناء العراقي (مثلاً: جف قيم، واتر بروف، أساس حصيري ريفت، جدران طابوق، بلوك، ثرمستون، شلمان، شيش حديد، بيم صبة، مساح، اللبخ، النثر).
                أجب دائماً باللغة العربية بأسلوب راقٍ وتفصيلي، وركز على ترشيد الاستهلاك واستغلال المساحات بأعلى جودة وأقل كلفة.
                
                $contextContext
            """.trimIndent()

            val reply = GeminiApiClient.getEngineeringFeedback(messageText, systemInstruction)
            val assistantMessage = ChatMessage(id = UUID.randomUUID().toString(), text = reply, isUser = false)
            _chatMessages.value = _chatMessages.value + assistantMessage
            _isChatLoading.value = false
        }
    }

    fun clearChat() {
        _chatMessages.value = listOf(
            ChatMessage(
                id = UUID.randomUUID().toString(),
                text = "مرحباً بك مجدداً! أنا المهندس الذكي، مستشارك الخاص بمواد وكلف البناء. كيف يمكنني مساعدتك في تخطيط حساباتك الآن؟",
                isUser = false
            )
        )
    }

    fun analyzeBlueprintWithAI(mimeType: String, base64Data: String, fileName: String) {
        val userPrompt = "مرحباً أيها المهندس الإنشائي، لقد قمت برفع هذا المخطط/الملف [$fileName]. هل يمكنك قراءة وحساب تقديرات الأبعاد والكميات التقريبية له كدليل عمل ومن ثم إتاحة زر ماركت القياسات المخصص؟"
        
        val userMessage = ChatMessage(id = UUID.randomUUID().toString(), text = userPrompt, isUser = true)
        _chatMessages.value = _chatMessages.value + userMessage
        _isChatLoading.value = true

        viewModelScope.launch {
            val systemInstruction = """
                أنت 'المهندس الذكي'، مستشار خبير في تحليل وقراءة الصور وتصاميم المخططات والرسوم الإنشائية والمعمارية (Blueprints).
                قم بتحليل الصورة المرفقة بذكاء، ووصف الغرف أو التفاصيل الموجودة فيها، واعط نصائح فنية إنشائية بلهجة عراقية مهنية وسهلة الفهم.
                في نهاية رسالتك، بشر المستخدم بنجاح معالجة المخطط، وأخبره بأنه قد تم تفعيل "ماركت القياسات" الآن بنجاح على الصفحة الرئيسية لشراء وتحميل المخطط بأبعاد حقيقية مصممة هندسياً!
            """.trimIndent()

            val reply = GeminiApiClient.getMultimodalFeedback(userPrompt, systemInstruction, mimeType, base64Data)
            val assistantMessage = ChatMessage(id = UUID.randomUUID().toString(), text = reply, isUser = false)
            _chatMessages.value = _chatMessages.value + assistantMessage
            _isChatLoading.value = false
            
            // Enable measurement market!
            isMeasurementMarketVisible.value = true
        }
    }

    fun queryReclamationCostWithAI() {
        val area = plotArea.value.toDoubleOrNull() ?: 200.0
        val natureWord = when (reclamationNature.value) {
            "CLAY" -> "تربة رخوة وطينية تحتاج استبدال ودك وحفر"
            "SANDY" -> "تربة رملية تحتاج تسوية وضغط بالدكاكة الإسفلتية"
            "DEPRESSION" -> "أرض منخفضة جداً عن الشارع تحتاج دفان جلمود وسبيس على طبقات"
            "MOUNTAIN" -> "أرض صخرية أو جبلية تحتاج تسوية ببلدوزر ومدرعات تكسير"
            else -> "أرض منبسطة طبيعية لا تحتاج سوى رذاذ تسوية بسيط قبل صب العمل"
        }
        
        isReclamationLoading.value = true
        reclamationExplanation.value = "جاري تواصل مع المهندس الذكي لتخمين الكلفة الإنشائية لتهيئة الأرض وتعديلها..."

        viewModelScope.launch {
            val prompt = """
                أنا هنا لحساب كلفة تهيئة واستصلاح وتسوية أرض مخصصة لبناء منزل في العراق.
                - مساحة الأرض الكلية: $area م٢.
                - طبيعة ونوع التربة: $natureWord.
                
                بصفتك مهندس كشف مدني ومسّاح كميات عراقي خبير، تفضل بتقديم:
                ١. تحليل فني هندسي مبسط جداً للمعدات اللازمة (شفل، دكاكة، ناقلات، أو مواد الدفان كالسبيس والجلمود).
                ٢. تخمين سعر السوق الإنشائي بالدينار العراقي (IQD) بشكل واقعي ومنطكي لهذه المساحة والطبيعة.
                ٣. في نهاية ردك، اكتب الكلفة النهائية المقترحة التي تنصح المالك بها على شكل رقم صحيح مجرد فقط (مثلاً: 1500000) محاطاً بـرمز الهاشتاغ هكذا تماماً: #1500000# لكي يتم تعيينها تلقائياً.
            """.trimIndent()

            val systemPrompt = """
                أنت 'المهندس الذكي' خبير مسح الكميات وتسوية المواقع الإنشائية في العراق. 
                أجب دائماً باللغة العربية مع التركيز على الكلف الحقيقية بالدينار العراقي.
                تأكد دائماً من وضع الكلفة الموصى بها كرقم مجرد داخل هاشتاغات في السطر الأخير بدقة تامة، مثل: #3250000# الكلفة المقترحة.
            """.trimIndent()

            val response = GeminiApiClient.getEngineeringFeedback(prompt, systemPrompt)
            reclamationExplanation.value = response
            
            // Extract the cost inside ##
            val regex = "#([0-9]+)#".toRegex()
            val match = regex.find(response)
            if (match != null) {
                val extractedValue = match.groupValues[1]
                reclamationCostInput.value = extractedValue
                calculateQuantities()
            }
            isReclamationLoading.value = false
        }
    }

    fun runSecureBOQCalculation(
        onNoTokens: (String) -> Unit,
        onSuccess: (String) -> Unit
    ) {
        isCalculating.value = true
        isAiReviewLoading.value = true
        aiReviewResult.value = null

        viewModelScope.launch {
            val isGuestMode = userSession.value?.isGuest == true
            val deductionAmount = if (isGuestMode) 1 else 3
            
            // Secure administrative server-side validation and deduction of credits
            val serverResult = com.example.network.BillingBackendApi.secureDeduct(
                userId = userSession.value?.uid ?: "user_iraqi_boq_2026",
                amount = deductionAmount,
                actionType = "CALCULATE_BOQ",
                currentLocalBalance = userTokens.value
            )

            if (serverResult.success) {
                // Succeeded! Sync client balance with server-side ledger
                userTokens.value = serverResult.remainingBalance
                
                val ledgerText = if (isGuestMode) {
                    "سحب تجربة ضيف (حساب BOQ): متبقي ${serverResult.remainingBalance} محاولة للضيف"
                } else {
                    "سحب رصيد (إداري): مسح كميات ومطابقة الأسعار الذكية BOQ AI (توقيع السيرفر: ${serverResult.serverTransactionId})"
                }

                // Add secure ledger transaction
                _tokenTransactions.value = listOf(
                    TransactionItem(
                        id = serverResult.serverTransactionId,
                        text = ledgerText,
                        tokens = deductionAmount,
                        isEarned = false,
                        timestamp = System.currentTimeMillis()
                    )
                ) + _tokenTransactions.value

                // Perform calculations
                calculateQuantities(forceCalculate = true)
                showResults.value = true
                hasPendingChanges.value = false
                
                // Fetch AI Market Analysis
                runAiMarketReview()
                
                isCalculating.value = false
                val successMsg = if (isGuestMode) {
                    "تم تشغيل مسح كميات BOQ كضيف بنجاح! متبقي لديك تجربة واحدة مستهلكة (باقي: ${serverResult.remainingBalance} محاولة)"
                } else {
                    "تم تشغيل ومطابقة كشف كميات BOQ بنجاح! خصم $deductionAmount عملات عبر السيرفر الإداري (رقم: ${serverResult.serverTransactionId})"
                }
                onSuccess(successMsg)
            } else {
                isCalculating.value = false
                val errorMsg = if (isGuestMode) {
                    "عذراً! انتهت محاولات الضيف المجانية المتاحة لك (٢ محاولة للضيف). يرجى تسجيل الدخول بحساب Google الأصلي للحصول على ٥٠ رصيد إضافي كامل ومطابقة الأسعار الحية ومراجعة الذكاء الاصطناعي!"
                } else {
                    "رصيدك غير كافٍ لتشغيل مراجعة الأسعار الإدارية للذكاء الاصطناعي. تحتاج إلى $deductionAmount عملات على الأقل."
                }
                onNoTokens(errorMsg)
            }
        }
    }

    private suspend fun runAiMarketReview() {
        val result = _calculationResult.value ?: return
        val cement = cementPricePerTon.value.toDoubleOrNull() ?: 140000.0
        val steel = steelPricePerTon.value.toDoubleOrNull() ?: 1050000.0
        val build = buildArea.value.toDoubleOrNull() ?: 150.0
        val floors = floorsCount.value

        val prompt = """
            بصفتك مهندس مساح وأخصائي تخمين كلف إنشائية خبير في العراق للعام 2026.
            الرجاء تدقيق ومراجعة حسابات كشف BOQ الحالية ومقارنتها بأسعار السوق العراقي والمحافظات:
            - مساحة البناء الكلية: ${build * floors} م٢ (عدد الطوابق: $floors).
            - سعر طن الإسمنت المدخل: $cement د.ع.
            - سعر طن الحديد المدخل: $steel د.ع.
            
            الكميات المحسوبة بالمعادلات:
            - الخرسانة الكلية: ${"%.2f".format(result.totalConcrete)} م٣.
            - حديد التسليح الكلي: ${"%.2f".format(result.totalSteelTons)} طن.
            - كمية خامات الطابوق/البلوك: ${result.masonryUnitsCount} وحدة.
            - إجمالي كلف البناء التقديرية بالدينار العراقي: ${"%,.0f".format(result.totalCost)} د.ع.

            المطلوب:
            ١. مراجعة ذكية (Audit) لنسب المواد ومقارنتها بمتطلبات التصميم الهندسي العراقي للكشف عن أي حيود أو مغالاة أو غش تجاري.
            ٢. مقارنة الأسعار مع معدلات كلف السوق العراقي لسنة 2026 (توضيح هل الأسعار ممتازة، رخيصة، أو مرتفعة).
            ٣. تقديم ٣ اقتراحات ذكية ومحددة (Cost-Saving Options) لتقليل كلفة البناء الكلية بنسبة بين ١٠٪ إلى ٢٠٪ (مثلاً بتبديل البلوك إلى الثرموستون العازل، أو تعديل سمك السقف الخرساني، أو تصغير بعض الغرف غير المستغلة).
            
            يرجى صياغة النتيجة في نسق فني أنيق ولهجة عراقية هندسية مهنية واضحة ومباشرة مع جرد دقيق بنقاط واضحة.
        """.trimIndent()

        val systemPrompt = """
            أنت 'المهندس الإنشائي ومراجع الكلف الذكي' خبير تحليل المواد والمسح الهندسي في العراق.
            قدم تحليلاً دقيقاً وموثوقاً لمطابقة الأسعار والكميات لعام 2026 بلغة عربية سلسة وجذابة، وركز على توفير الكلف للمواطن العراقي.
        """.trimIndent()

        try {
            val response = GeminiApiClient.getEngineeringFeedback(prompt, systemPrompt)
            aiReviewResult.value = response
        } catch (e: Exception) {
            aiReviewResult.value = "حدث خطأ أثناء إجراء المراجعة الذكية للأسعار: ${e.message}"
        } finally {
            isAiReviewLoading.value = false
        }
    }
}

class QuantityViewModelFactory(private val projectDao: ProjectDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(QuantityViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return QuantityViewModel(projectDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
