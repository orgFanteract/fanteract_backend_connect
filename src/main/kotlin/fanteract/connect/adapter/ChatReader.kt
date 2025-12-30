package fanteract.connect.adapter

import fanteract.connect.entity.Chat
import fanteract.connect.enumerate.RiskLevel
import fanteract.connect.repo.ChatRepo
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class ChatReader(
    private val chatRepo: ChatRepo,
) {
    fun findByUserIdAndChatroomId(
        userId: Long,
        chatroomId: Long,
        pageable: Pageable,
    ): Page<Chat> = chatRepo.findByUserIdAndChatroomId(userId, chatroomId, pageable)

    fun findByUserIdAndChatroomIdAnd(
        userId: Long,
        chatroomId: Long,
        content: String,
        pageable: PageRequest,
    ): Page<Chat> = chatRepo.findByUserIdAndChatroomIdAndContentContaining(userId, chatroomId, content, pageable)

    fun countByUserId(userId: Long): Long = chatRepo.countByUserId(userId)

    fun countByUserIdAndRiskLevel(
        userId: Long,
        riskLevel: RiskLevel,
    ): Long = chatRepo.countByUserIdAndRiskLevel(userId, riskLevel)

    fun findByUserIdAndRiskLevel(
        userId: Long,
        riskLevel: RiskLevel,
        pageable: Pageable,
    ): Page<Chat> = chatRepo.findByUserIdAndRiskLevel(userId, riskLevel, pageable)
}
