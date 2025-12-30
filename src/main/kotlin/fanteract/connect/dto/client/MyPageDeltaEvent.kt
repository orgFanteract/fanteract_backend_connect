package fanteract.connect.dto.client

import java.util.UUID

data class MyPageDeltaEvent(
    val eventId: String = UUID.randomUUID().toString(),
    val userId: Long,
    val deltas: Map<String, Long>, // 예: boardCount:+1, commentCount:+2 ...
    val createdAtEpochMs: Long = System.currentTimeMillis(),
)
