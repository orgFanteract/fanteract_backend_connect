package fanteract.connect.dto.client

import fanteract.connect.enumerate.RiskLevel
import fanteract.connect.enumerate.WriteStatus

data class UpdateBalanceInnerRequest(
    val balance: Int,
)

data class UpdateActivePointInnerRequest(
    val activePoint: Int,
)

data class UpdateAbusePointInnerRequest(
    val abusePoint: Int,
)

data class MessageWrapper<T>(
    val methodName: String,
    val content: T,
)

data class UpdateActivePointSendRequest(
    val userId: Long,
    val activePoint: Int,
)

data class CreateChatRequest(
    val content: String,
    val chatroomId: Long,
    val userId: Long,
    val riskLevel: RiskLevel,
)

data class WriteCommentForUserRequest(
    val userId: Long,
    val writeStatus: WriteStatus,
    val riskLevel: RiskLevel,
)
