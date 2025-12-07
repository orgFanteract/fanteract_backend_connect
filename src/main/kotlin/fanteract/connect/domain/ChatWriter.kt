package fanteract.connect.domain

import fanteract.connect.entity.Chat
import fanteract.connect.entity.Chatroom
import fanteract.connect.enumerate.RiskLevel
import fanteract.connect.repo.ChatRepo
import org.springframework.stereotype.Component
import kotlin.String

@Component
class ChatWriter(
    private val chatRepo: ChatRepo,
) {
    fun create(
        content: String,
        chatroomId: Long,
        userId: Long,
        riskLevel: RiskLevel,
    ): Chat {
        return chatRepo.save(
            Chat(
                content = content,
                chatroomId = chatroomId,
                userId = userId,
                riskLevel = riskLevel,
            )
        )
    }

}