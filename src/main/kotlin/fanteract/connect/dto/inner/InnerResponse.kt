package fanteract.connect.dto.inner

import fanteract.connect.enumerate.RiskLevel

data class ReadChatCountInnerResponse(
    val count: Long,
)

data class ReadChatroomCountInnerResponse(
    val count: Long,
)

data class ReadChatPageInnerResponse(
    val contents: List<ReadChatInnerResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
)

data class ReadChatInnerResponse(
    val chatId: Long = 0L,
    val content: String,
    val chatroomId: Long,
    val userId: Long,
    val riskLevel: RiskLevel,
)
