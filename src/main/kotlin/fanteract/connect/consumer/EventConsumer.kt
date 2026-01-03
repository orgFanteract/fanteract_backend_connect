package fanteract.connect.consumer

import fanteract.connect.dto.client.CreateChatResponse
import fanteract.connect.dto.client.MessageWrapper
import fanteract.connect.entity.Chat
import fanteract.connect.repo.ChatRepo
import fanteract.connect.util.BaseUtil
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import java.util.Base64
import kotlin.String

@Component
class EventConsumer(
    private val chatRepo: ChatRepo,
) {
    @KafkaListener(
        topics = ["CONNECT_SERVICE.createChat"],
        groupId = "connect-service",
    )
    fun consumeCreateChat(message: String) {
        val decodedJson = String(Base64.getDecoder().decode(message))

        val response = BaseUtil.fromJson<MessageWrapper<CreateChatResponse>>(decodedJson)

        chatRepo.save(
            Chat(
                content = response.content.content,
                chatroomId = response.content.chatroomId,
                userId = response.content.userId,
                riskLevel = response.content.riskLevel,
            ),
        )
    }
}
