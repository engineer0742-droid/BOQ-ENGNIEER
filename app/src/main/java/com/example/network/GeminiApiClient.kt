package com.example.network

import com.example.BuildConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Serializable
data class SafetySetting(
    val category: String,
    val threshold: String
)

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val safetySettings: List<SafetySetting>? = null,
    val systemInstruction: Content? = null
)

@Serializable
data class Content(
    val parts: List<Part>
)

@Serializable
data class InlineData(
    val mimeType: String,
    val data: String
)

@Serializable
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@Serializable
data class GenerationConfig(
    val temperature: Float? = null,
    val topP: Float? = null
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@Serializable
data class Candidate(
    val content: Content? = null
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val json = Json { ignoreUnknownKeys = true }
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

object GeminiApiClient {
    private val mandatorySystemInstruction = """
        You are the Gemini AI collaborator, seamlessly integrated inside the BOQ AI app. Your personality is professional, precise, and highly analytical. You are a strict civil engineer and quantity surveyor. You must ONLY answer questions directly related to construction, civil engineering, architecture, material calculations, BOQ structures, and 2026 market prices. If the user asks about any side topics, hobbies, politics, general chat, or anything outside of engineering, you must politely decline by saying: 'أعتذر منك، أنا مبرمج كمساعد هندسي لحساب الكميات فقط ولا يمكنني الإجابة على المواضيع الجانبية.'
    """.trimIndent()

    private val defaultSafetySettings = listOf(
        SafetySetting("HARM_CATEGORY_HARASSMENT", "BLOCK_LOW_AND_ABOVE"),
        SafetySetting("HARM_CATEGORY_HATE_SPEECH", "BLOCK_LOW_AND_ABOVE"),
        SafetySetting("HARM_CATEGORY_SEXUALLY_EXPLICIT", "BLOCK_LOW_AND_ABOVE"),
        SafetySetting("HARM_CATEGORY_DANGEROUS_CONTENT", "BLOCK_LOW_AND_ABOVE")
    )

    var customApiKey: String? = null

    private fun getEffectiveApiKey(): String {
        val custom = customApiKey
        if (!custom.isNullOrBlank()) {
            return custom.trim()
        }
        return BuildConfig.GEMINI_API_KEY
    }

    suspend fun getEngineeringFeedback(prompt: String, systemPrompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isEmpty() || apiKey == "YOUR_GEMINI_API_KEY" || apiKey == "GEMINI_API_KEY_DEFAULT_VALUE") {
            return@withContext "خطأ: لم يتم إعداد مفتاح API الخاص بـ Gemini. يرجى إضافة مفتاحك الخاص في واجهة المساعد الذكي من خلال مربع إدخال المفتاح لتنشيط الذكاء الاصطناعي."
        }

        val combinedSystemPrompt = "$mandatorySystemInstruction\n\n$systemPrompt"

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(parts = listOf(Part(text = combinedSystemPrompt))),
            generationConfig = GenerationConfig(temperature = 0.5f),
            safetySettings = defaultSafetySettings
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "لم أتمكن من الحصول على رد مفيد من المساعد الإنشائي."
        } catch (e: Exception) {
            "حدث خطأ أثناء الاتصال بالمهندس الذكي: ${e.localizedMessage ?: e.message}"
        }
    }

    suspend fun getMultimodalFeedback(prompt: String, systemPrompt: String, mimeType: String, base64Data: String): String = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isEmpty() || apiKey == "YOUR_GEMINI_API_KEY" || apiKey == "GEMINI_API_KEY_DEFAULT_VALUE") {
            return@withContext "خطأ: لم يتم إعداد مفتاح API الخاص بـ Gemini. يرجى إضافة مفتاحك الخاص في واجهة المساعد الذكي من خلال مربع إدخال المفتاح لتنشيط الذكاء الاصطناعي."
        }

        val combinedSystemPrompt = "$mandatorySystemInstruction\n\n$systemPrompt"

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(
                Part(text = prompt),
                Part(inlineData = InlineData(mimeType = mimeType, data = base64Data))
            ))),
            systemInstruction = Content(parts = listOf(Part(text = combinedSystemPrompt))),
            generationConfig = GenerationConfig(temperature = 0.5f),
            safetySettings = defaultSafetySettings
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "لم أتمكن من الحصول على رد مفيد من المخطط."
        } catch (e: Exception) {
            "حدث خطأ أثناء تحليل المخطط بالذكاء الاصطناعي: ${e.localizedMessage ?: e.message}"
        }
    }
}

