package com.example.robotcontroller.ai

/**
 * Data class to store conversation history
 */
data class ConversationEntry(
    val timestamp: Long,
    val userInput: String,
    val aiResponse: String,
    val isMovementCommand: Boolean = false
)

/**
 * Manages conversation memory for the AI robot
 */
class ConversationMemory {
    private val conversations = mutableListOf<ConversationEntry>()
    private val maxConversations = 8

    /**
     * Add a new conversation to memory
     */
    fun addConversation(userInput: String, aiResponse: String, isMovementCommand: Boolean = false) {
        val entry = ConversationEntry(
            timestamp = System.currentTimeMillis(),
            userInput = userInput,
            aiResponse = aiResponse,
            isMovementCommand = isMovementCommand
        )

        conversations.add(entry)

        // Keep only the last 8 conversations
        if (conversations.size > maxConversations) {
            conversations.removeAt(0)
        }
    }

    /**
     * Get conversation history as context for AI
     */
    fun getContextString(): String {
        if (conversations.isEmpty()) return ""

        val contextBuilder = StringBuilder()
        contextBuilder.append("Previous conversation history:\n")

        conversations.forEach { entry ->
            contextBuilder.append("User: ${entry.userInput}\n")
            if (!entry.isMovementCommand) {
                contextBuilder.append("Robo: ${entry.aiResponse}\n")
            } else {
                contextBuilder.append("Robo: [Executed movement: ${entry.aiResponse}]\n")
            }
            contextBuilder.append("---\n")
        }

        return contextBuilder.toString()
    }

    /**
     * Get recent conversations for display
     */
    fun getRecentConversations(count: Int = 3): List<ConversationEntry> {
        return conversations.takeLast(count)
    }

    /**
     * Clear all conversation history
     */
    fun clear() {
        conversations.clear()
    }

    /**
     * Get the last user input for context
     */
    fun getLastUserInput(): String? {
        return conversations.lastOrNull()?.userInput
    }

    /**
     * Check if user recently mentioned something specific
     */
    fun hasRecentMention(keyword: String, lastNConversations: Int = 3): Boolean {
        return conversations.takeLast(lastNConversations)
            .any { it.userInput.lowercase().contains(keyword.lowercase()) }
    }
}