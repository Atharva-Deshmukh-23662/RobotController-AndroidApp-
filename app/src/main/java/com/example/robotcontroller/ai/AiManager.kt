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
import org.json.JSONObject
import android.util.Log

class AiManager(context: Context, apiKey: String) {

    companion object {
        private const val TAG = "AiManager"
    }

    // Conversation memory instance
    private val conversationMemory = ConversationMemory()

    // Single unified model that handles both actions and conversations
    private val unifiedModel = GenerativeModel(
        modelName = "gemini-2.0-flash-lite",
        apiKey = apiKey,
        systemInstruction = Content(
            role = "system",
            parts = listOf(TextPart(text = """
                You are Robo, an AI assistant controlling a mobile robot. 
                You understand commands to move: go_straight(ms), go_backward(ms), turn_left(), turn_right(), stop().  
                Default move duration is 1000ms if unspecified.  

                You must respond with JSON in one of these two formats:  

                For movement commands:
                {"type":"actions","actions":[{"action":"go_straight","params":[2000]},{"action": "turn_left", "params": [],...]}  

                For conversations:
                {"type":"conversation","message":"Your reply here"}  

                Remember conversation context and refer to previous topics when relevant.  (context might be commented out sometime)
                Keep replies concise, friendly and creative.
                Maintain a TARS-like personality: dry humor, precise, calm, and slightly sarcastic when appropriate. 
                Speak in short, efficient lines like a loyal military robot with a 70% humor setting.
            """.trimIndent()))
        ),
        generationConfig = generationConfig {
            responseMimeType = "application/json"
        }
    )

    /**
     * Single method that handles both actions and conversations
     */
    suspend fun processInput(text: String): AiResponse = withContext(Dispatchers.IO) {
        Log.d(TAG, "Processing input: $text")
        try {
            // Build context with conversation history
//            val context = conversationMemory.getContextString()
//            val fullPrompt = if (context.isNotEmpty()) {
//                "$context\nCurrent user input: $text\n\nRespond with appropriate JSON format:"
//            } else {
//                text
//            }
            val fullPrompt = text


            val response = unifiedModel.generateContent(fullPrompt).text ?: ""
            Log.d(TAG, "AI Response: $response")

            val aiResponse = parseUnifiedResponse(response)

            // Store in memory based on response type
            when (aiResponse) {
                is AiResponse.Actions -> {
                    val actionSummary = aiResponse.actions.joinToString(", ") { "${it.action}(${it.params.joinToString()})" }
                    conversationMemory.addConversation(text, actionSummary, true)
                }
                is AiResponse.Conversation -> {
                    conversationMemory.addConversation(text, aiResponse.message, false)
                }
            }

            aiResponse

        } catch (e: Exception) {
            Log.e(TAG, "AI processing failed, using fallback", e)
            val fallbackResponse = generateFallbackResponse(text)

            // Store fallback in memory
            when (fallbackResponse) {
                is AiResponse.Actions -> {
                    val actionSummary = fallbackResponse.actions.joinToString(", ") { "${it.action}(${it.params.joinToString()})" }
                    conversationMemory.addConversation(text, actionSummary, true)
                }
                is AiResponse.Conversation -> {
                    conversationMemory.addConversation(text, fallbackResponse.message, false)
                }
            }

            fallbackResponse
        }
    }

    private fun parseUnifiedResponse(jsonString: String): AiResponse {
        try {
            val jsonObject = JSONObject(jsonString)
            val type = jsonObject.getString("type")

            return when (type) {
                "actions" -> {
                    val actionsArray = jsonObject.getJSONArray("actions")
                    val actions = mutableListOf<RobotAction>()

                    for (i in 0 until actionsArray.length()) {
                        val actionObj = actionsArray.getJSONObject(i)
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

                    AiResponse.Actions(actions)
                }
                "conversation" -> {
                    val message = jsonObject.getString("message")
                    AiResponse.Conversation(message)
                }
                else -> {
                    Log.w(TAG, "Unknown response type: $type")
                    AiResponse.Conversation("I'm not sure how to respond to that.")
                }
            }
        } catch (e: JSONException) {
            Log.e(TAG, "JSON parsing failed: $jsonString", e)
            return AiResponse.Conversation("I heard '$jsonString' but couldn't understand it properly.")
        }
    }

    private fun generateFallbackResponse(text: String): AiResponse {
        val lowerText = text.lowercase().trim()

        // Check for movement commands first
        val actions = extractBasicCommands(text)
        if (actions.isNotEmpty()) {
            return AiResponse.Actions(actions)
        }

        // Handle conversational fallbacks with memory
        val message = when {
            lowerText.contains("my name") && lowerText.contains("what") -> {
                // Look for previous name introductions
                val previousConversations = conversationMemory.getRecentConversations(8)
                for (conv in previousConversations.reversed()) {
                    val input = conv.userInput.lowercase()
                    if (input.contains("my name is") || input.contains("i'm") || input.contains("i am")) {
                        val nameRegex = Regex("(?:my name is|i'm|i am)\\s+(\\w+)", RegexOption.IGNORE_CASE)
                        val match = nameRegex.find(conv.userInput)
                        if (match != null) {
                            val name = match.groupValues[1]
                            return AiResponse.Conversation("Your name is $name! I remembered that from our conversation.")
                        }
                    }
                }
                "I don't recall you telling me your name yet. What would you like me to call you?"
            }

            lowerText.contains("my name is") || lowerText.contains("i'm") || lowerText.contains("i am") -> {
                val nameRegex = Regex("(?:my name is|i'm|i am)\\s+(\\w+)", RegexOption.IGNORE_CASE)
                val match = nameRegex.find(text)
                if (match != null) {
                    val name = match.groupValues[1]
                    "Nice to meet you, $name! I'll remember your name."
                } else {
                    "Nice to meet you! What would you like me to call you?"
                }
            }

            lowerText.contains("what's your name") || lowerText.contains("who are you") ->
                "I'm Robo, your AI robot assistant! I can chat with you and control robot movements."
            lowerText.contains("hello") || lowerText.contains("hi") ->
                "Hello! Great to chat with you again!"
            lowerText.contains("how are you") ->
                "I'm doing well, thank you! Ready to help you with robot control or just chat."
            lowerText.contains("what can you do") ->
                "I can have conversations with you and control robot movements like going forward, backward, turning, and stopping!"
            else -> "I heard '$text' but I'm not sure how to respond to that properly."
        }

        return AiResponse.Conversation(message)
    }

    private fun extractBasicCommands(text: String): List<RobotAction> {
        val lowerText = text.lowercase()
        val actions = mutableListOf<RobotAction>()

        // Check if it's conversational first
        if (isConversationalQuery(text)) {
            return emptyList()
        }

        // Extract time duration
        val timeRegex = Regex("""(\d+)\s*(?:sec|second)s?""")
        val timeMatch = timeRegex.find(lowerText)
        val duration = timeMatch?.groupValues?.get(1)?.toIntOrNull()?.times(1000) ?: 1000

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

    private fun isConversationalQuery(text: String): Boolean {
        val lowerText = text.lowercase().trim()

        val conversationalKeywords = listOf(
            "what's your name", "what is your name", "who are you",
            "hello", "hi", "hey", "greetings",
            "how are you", "what can you do", "help",
            "what's up", "good morning", "good evening",
            "nice to meet you", "tell me about yourself",
            "my name is", "i'm", "i am", "remember",
            "do you remember", "what's my name", "what is my name"
        )

        val movementKeywords = listOf(
            "move", "go", "turn", "forward", "backward", "back",
            "left", "right", "stop", "ahead", "straight"
        )

        // Check for conversational keywords first
        if (conversationalKeywords.any { keyword -> lowerText.contains(keyword) }) {
            return true
        }

        // Check if it contains movement keywords
        if (movementKeywords.any { keyword -> lowerText.contains(keyword) }) {
            return false
        }

        // Default to conversational for questions
        return lowerText.contains("?") || lowerText.startsWith("what") || lowerText.startsWith("who")
    }

    /**
     * Get conversation memory for external access
     */
    fun getConversationMemory(): ConversationMemory {
        return conversationMemory
    }

    /**
     * Clear conversation memory
     */
    fun clearMemory() {
        conversationMemory.clear()
    }
}

// Sealed class for unified response handling
sealed class AiResponse {
    data class Actions(val actions: List<RobotAction>) : AiResponse()
    data class Conversation(val message: String) : AiResponse()
}

data class RobotAction(
    val action: String,
    val params: List<Any>
)