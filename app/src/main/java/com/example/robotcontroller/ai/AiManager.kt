package com.example.robotcontroller.ai

import android.content.Context
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.TextPart
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException

class AiManager(context: Context, apiKey: String) {
    private val model = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = apiKey,
        systemInstruction = Content(
            role = "system",
            parts = listOf(TextPart(text = """
                You are Robo, an AI robot-controller assistant that converts voice commands into structured action sequences.
                
                Available Actions:
                - go_straight(duration_ms): Move forward for specified milliseconds
                - go_backward(duration_ms): Move backward for specified milliseconds  
                - turn_left(): Turn left (takes ~1 second)
                - turn_right(): Turn right (takes ~1 second)
                - stop(): Stop all movement
                
                IMPORTANT: Always respond with ONLY a valid JSON array of actions. No explanations or additional text.
                
                Examples:
                Input: "move forward for 2 seconds and turn left"
                Output: [{"action": "go_straight", "params": [2000]}, {"action": "turn_left", "params": []}]
                
                Input: "go back 3 seconds then turn right"
                Output: [{"action": "go_backward", "params": [3000]}, {"action": "turn_right", "params": []}]
                
                Input: "stop the robot"
                Output: [{"action": "stop", "params": []}]
                
                Convert time references to milliseconds (1 sec = 1000ms, 2 sec = 2000ms, etc.)
            """.trimIndent()))
        ),
        generationConfig = generationConfig {
            responseMimeType = "application/json"
        }
    )

    suspend fun interpretToActions(text: String): List<RobotAction> = withContext(Dispatchers.IO) {
        try {
            val response = model.generateContent(text).text ?: "[]"
            parseActions(response)
        } catch (e: Exception) {
            // Fallback: try to extract basic commands manually
            extractBasicCommands(text)
        }
    }

    suspend fun interpret(text: String): String = withContext(Dispatchers.IO) {
        model.generateContent("Respond conversationally to: $text").text ?: ""
    }

    private fun parseActions(jsonString: String): List<RobotAction> {
        val actions = mutableListOf<RobotAction>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val actionObj = jsonArray.getJSONObject(i)
                val actionName = actionObj.getString("action")
                val params = mutableListOf<Any>()

                if (actionObj.has("params")) {
                    val paramsArray = actionObj.getJSONArray("params")
                    for (j in 0 until paramsArray.length()) {
                        params.add(paramsArray.get(j))
                    }
                }

                actions.add(RobotAction(actionName, params))
            }
        } catch (e: JSONException) {
            // If JSON parsing fails, return empty list
            return emptyList()
        }
        return actions
    }

    private fun extractBasicCommands(text: String): List<RobotAction> {
        val lowerText = text.lowercase()
        val actions = mutableListOf<RobotAction>()

        // Extract time duration (look for numbers followed by "sec" or "second")
        val timeRegex = Regex("""(\d+)\s*(?:sec|second)s?""")
        val timeMatch = timeRegex.find(lowerText)
        val duration = timeMatch?.groupValues?.get(1)?.toIntOrNull()?.times(1000) ?: 1000

        when {
            lowerText.contains("forward") || lowerText.contains("ahead") -> {
                actions.add(RobotAction("go_straight", listOf(duration)))
            }
            lowerText.contains("backward") || lowerText.contains("back") -> {
                actions.add(RobotAction("go_backward", listOf(duration)))
            }
            lowerText.contains("left") -> {
                actions.add(RobotAction("turn_left", emptyList()))
            }
            lowerText.contains("right") -> {
                actions.add(RobotAction("turn_right", emptyList()))
            }
            lowerText.contains("stop") -> {
                actions.add(RobotAction("stop", emptyList()))
            }
        }

        return actions
    }
}

data class RobotAction(
    val action: String,
    val params: List<Any>
)