package fanteract.connect.dto

import fanteract.connect.enumerate.RiskLevel


data class FilterResult(
    val action: RiskLevel,
    val reason: String? = null,
    val score: Double? = null, // ML 토크시티 점수 등
)

data class ExistsUserResponse(
    val exists: Boolean,
)

data class UpdateBalanceRequest(
    val balance: Int,
)

data class UpdateActivePointRequest(
    val activePoint: Int,
)

data class UpdateAbusePointRequest(
    val abusePoint: Int,
)