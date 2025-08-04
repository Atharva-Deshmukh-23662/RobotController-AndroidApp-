package com.example.robotcontroller.ai

import android.content.Context
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.TextPart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AiManager(context: Context, apiKey: String) {
    private val model = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = apiKey,
        systemInstruction = Content(
            role = "system",
            parts = listOf(TextPart(text = """
                You are Robo, an AI robot-controller assistant.
                • Answer concisely unless detailed is requested.
            """.trimIndent()))
        )
    )

    suspend fun interpret(text: String): String = withContext(Dispatchers.IO) {
        model.generateContent(text).text ?: ""
    }
}
