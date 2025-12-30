package fanteract.connect.adapter

import fanteract.connect.dto.client.CreateChatRequest
import fanteract.connect.dto.client.MessageWrapper
import fanteract.connect.entity.Chat
import fanteract.connect.enumerate.RiskLevel
import fanteract.connect.enumerate.TopicService
import fanteract.connect.repo.ChatRepo
import fanteract.connect.util.BaseUtil
import fanteract.connect.util.ChatCountAccumulator
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.util.Base64
import kotlin.String

@Component
class ChatWriter(
    private val chatRepo: ChatRepo,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val chatCountAccumulator: ChatCountAccumulator,
) {
    fun create(
        content: String,
        chatroomId: Long,
        userId: Long,
        riskLevel: RiskLevel,
    ): Chat =
        chatRepo.save(
            Chat(
                content = content,
                chatroomId = chatroomId,
                userId = userId,
                riskLevel = riskLevel,
            ),
        )

    fun <T : Any> sendMessageUsingMessage(
        content: T,
        topicService: TopicService,
        methodName: String,
    ) {
        val jsonContent = BaseUtil.toJson(content)

        val base64Content = Base64.getEncoder().encodeToString(jsonContent.toByteArray())

        kafkaTemplate
            .send(
                "$topicService.$methodName",
                base64Content,
            ).get()
    }
}
