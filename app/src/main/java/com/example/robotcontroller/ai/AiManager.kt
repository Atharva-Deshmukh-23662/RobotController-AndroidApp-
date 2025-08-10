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
import android.util.Log

class AiManager(context: Context, apiKey: String) {

    companion object {
        private const val TAG = "AiManager"
    }

    // Model for action-based commands
    private val actionModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = apiKey,
        systemInstruction = Content(
            role = "system",
            parts = listOf(TextPart(text = """
                You are Robo, an AI robot-controller assistant that converts movement commands into structured action sequences.
                
                Available Actions:
                - go_straight(duration_ms): Move forward for specified milliseconds
                - go_backward(duration_ms): Move backward for specified milliseconds 
                - turn_left(): Turn left (takes ~1 second)
                - turn_right(): Turn right (takes ~1 second)
                - stop(): Stop all movement
                
                IMPORTANT: 
                - Only respond with JSON arrays for MOVEMENT/ACTION commands
                - For conversational questions, greetings, or non-movement queries, respond with: {"type": "conversation"}
                
                Movement Command Examples:
                Input: "move forward for 2 seconds and turn left"
                Output: [{"action": "go_straight", "params": [2000]}, {"action": "turn_left", "params": []}]
                
                Input: "go back 3 seconds then turn right"
                Output: [{"action": "go_backward", "params": [3000]}, {"action": "turn_right", "params": []}]
                
                Non-Movement Examples:
                Input: "what's your name"
                Output: {"type": "conversation"}
                
                Input: "hello"
                Output: {"type": "conversation"}
                
                Convert time references to milliseconds (1 sec = 1000ms, 2 sec = 2000ms, etc.)
                If no duration is specified for movement commands, use 1000ms as default.
            """.trimIndent()))
        ),
        generationConfig = generationConfig {
            responseMimeType = "application/json"
        }
    )

    // Model for conversational responses
    private val conversationalModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = apiKey,
        systemInstruction = Content(
            role = "system",
            parts = listOf(TextPart(text = """
                You are Robo, a friendly AI assistant controlling a mobile robot. 
                Respond naturally and conversationally to questions and greetings.
                Keep responses concise but friendly, be creative with your responses.
                
                Some information about yourself:
                - Your name is Robo
                - You're an AI assistant that controls a mobile robot
                - You can move forward, backward, turn left, turn right, and stop
                - You use voice commands to control movement
                - You're part of a student's final year project
                
                Be helpful and engaging!
            """.trimIndent()))
        )
    )

    /**
     * Main interpretation method that determines if input is action-based or conversational
     */
    suspend fun interpretToActions(text: String): List<RobotAction> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Processing voice command: $text")
        try {
            val response = actionModel.generateContent(text).text ?: "{\"type\": \"conversation\"}"
            Log.d(TAG, "AI Response: $response")

            // Check if it's a conversational response
            if (response.contains("\"type\": \"conversation\"")) {
                Log.d(TAG, "Detected conversational input, returning empty actions")
                return@withContext emptyList()
            }

            val actions = parseActions(response)
            Log.d(TAG, "Parsed ${actions.size} actions: ${actions.map { it.action }}")
            actions
        } catch (e: Exception) {
            Log.e(TAG, "AI processing failed, using fallback", e)
            // Fallback: try to extract basic commands manually
            val fallbackActions = extractBasicCommands(text)
            Log.d(TAG, "Fallback extracted ${fallbackActions.size} actions: ${fallbackActions.map { it.action }}")
            fallbackActions
        }
    }

    /**
     * Get conversational response for non-action queries
     */
    suspend fun interpret(text: String): String = withContext(Dispatchers.IO) {
        Log.d(TAG, "Getting conversational response for: $text")
        try {
            val response = conversationalModel.generateContent(text).text ?: ""
            Log.d(TAG, "Conversational response: $response")
            response
        } catch (e: Exception) {
            Log.e(TAG, "Conversational AI failed", e)
            "I heard '$text' but couldn't process it properly."
        }
    }

    /**
     * Check if the input is likely a conversational query rather than a movement command
     */
    private fun isConversationalQuery(text: String): Boolean {
        val lowerText = text.lowercase().trim()

        val conversationalKeywords = listOf(
            "what's your name", "what is your name", "who are you",
            "hello", "hi", "hey", "greetings",
            "how are you", "what can you do", "help",
            "what's up", "good morning", "good evening",
            "nice to meet you", "tell me about yourself"
        )

        val movementKeywords = listOf(
            "move", "go", "turn", "forward", "backward", "back",
            "left", "right", "stop", "ahead", "straight"
        )

        // Check for exact conversational matches first
        if (conversationalKeywords.any { keyword -> lowerText.contains(keyword) }) {
            return true
        }

        // Check if it contains movement keywords
        if (movementKeywords.any { keyword -> lowerText.contains(keyword) }) {
            return false
        }

        // If unclear, default to conversational for questions (containing ?)
        return lowerText.contains("?") || lowerText.startsWith("what") || lowerText.startsWith("who")
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
            Log.e(TAG, "JSON parsing failed: $jsonString", e)
            // If JSON parsing fails, return empty list
            return emptyList()
        }
        return actions
    }

    private fun extractBasicCommands(text: String): List<RobotAction> {
        val lowerText = text.lowercase()
        val actions = mutableListOf<RobotAction>()

        // Check if it's conversational first
        if (isConversationalQuery(text)) {
            Log.d(TAG, "Fallback detected conversational input: $text")
            return emptyList()
        }

        // Extract time duration (look for numbers followed by "sec" or "second")
        val timeRegex = Regex("""(\d+)\s*(?:sec|second)s?""")
        val timeMatch = timeRegex.find(lowerText)
        val duration = timeMatch?.groupValues?.get(1)?.toIntOrNull()?.times(1000) ?: 1000

        Log.d(TAG, "Fallback parsing: '$lowerText', extracted duration: ${duration}ms")

        when {
            lowerText.contains("forward") || lowerText.contains("ahead") || lowerText.contains("straight") -> {
                actions.add(RobotAction("go_straight", listOf(duration)))
            }
            lowerText.contains("backward") || lowerText.contains("back") -> {
                actions.add(RobotAction("go_backward", listOf(duration)))
            }
            lowerText.contains("turn left") || (lowerText.contains("left") && lowerText.contains("turn")) -> {
                actions.add(RobotAction("turn_left", emptyList()))
            }
            lowerText.contains("turn right") || (lowerText.contains("right") && lowerText.contains("turn")) -> {
                actions.add(RobotAction("turn_right", emptyList()))
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