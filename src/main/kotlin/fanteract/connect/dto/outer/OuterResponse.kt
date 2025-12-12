package fanteract.connect.dto.outer

import fanteract.connect.enumerate.RiskLevel
import java.time.LocalDateTime

data class CreateChatroomOuterResponse(
    val chatroomId: Long,
)

data class ReadChatroomListOuterResponse(
    val response: List<ReadChatroomOuterResponse>
)

data class ReadChatroomOuterResponse(
    val chatroomId: Long,
    val title: String,
    val description: String?,
)

data class ReadChatOuterResponse(
    val chatId: Long,
    val userName: String,
    val content: String,
    val createdAt: LocalDateTime,
)

data class ReadChatListOuterResponse(
    val contents: List<ReadChatOuterResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
)

data class JoinChatroomOuterResponse(
    val userChatroomId: Long,
)

data class LeaveChatroomOuterResponse(
    val userChatroomId: Long,
)

data class SendChatResponse(
    val chatId: Long? = null,
    val userName: String? = null,
    val content: String? = null,
    val createdAt: LocalDateTime? = null,
    val riskLevel: RiskLevel,
    val sentAt: Long? = null,
)