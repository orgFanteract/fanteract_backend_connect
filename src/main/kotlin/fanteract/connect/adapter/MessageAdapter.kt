package fanteract.connect.adapter

import fanteract.connect.dto.client.MessageWrapper
import fanteract.connect.enumerate.TopicService
import fanteract.connect.util.BaseUtil
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.util.Base64
import kotlin.text.toByteArray

@Component
class MessageAdapter(
    private val kafkaTemplate: KafkaTemplate<String, String>,
) {
    fun <T> sendMessageUsingBroker(
        message: T,
        topicService: TopicService,
        methodName: String,
    ) {
        val content =
            MessageWrapper(
                methodName = methodName,
                content = message,
            )

        val baseContent = Base64.getEncoder().encodeToString(BaseUtil.Companion.toJson(content).toByteArray())

        kafkaTemplate
            .send(
                "$topicService.$methodName",
                baseContent,
            ).get()
    }
}
