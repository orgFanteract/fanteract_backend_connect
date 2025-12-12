package fanteract.connect.dto.outer

import java.time.LocalDateTime

data class CreateChatroomOuterRequest(
    val title: String,
    val description: String?,
)

data class SendChatRequest(
    val content: String,
    val sentAt: Long? = null,
)

data class ReadChatContainingContentOuterRequest(
    val content: String,
)