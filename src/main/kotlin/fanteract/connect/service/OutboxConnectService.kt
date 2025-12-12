package fanteract.connect.service

import fanteract.connect.domain.ChatReader
import fanteract.connect.domain.ChatWriter
import fanteract.connect.domain.ChatroomWriter
import fanteract.connect.dto.client.CreateChatRequest
import fanteract.connect.dto.client.MessageWrapper
import fanteract.connect.entity.OutboxConnect
import fanteract.connect.enumerate.OutboxStatus
import fanteract.connect.enumerate.RiskLevel
import fanteract.connect.enumerate.TopicService
import fanteract.connect.repo.OutboxConnectRepo
import fanteract.connect.util.BaseUtil
import org.springframework.stereotype.Service
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.transaction.annotation.Transactional
import java.util.Base64
import kotlin.String

@Service

class OutboxConnectService(
    private val outboxConnectRepo: OutboxConnectRepo,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val chatReader: ChatReader,
    private val chatWriter: ChatWriter,
    private val chatroomWriter: ChatroomWriter,
) {
    /*
    @Scheduled(fixedDelay = 1000)
    fun sendToMessageBroker(){
        println("sendToMessageBroker begin")
        // 토픽 : 서비스 이름, 벨류 : 대상 서비스 메서드 이름 + 매개변수
        // 아직 처리되지 않은 메세지 조회
        val outboxMessageList = outboxConnectRepo.findAllByOutboxStatusOrderByCreatedAtDesc(OutboxStatus.NEW)

        for (message in outboxMessageList) {
            sendOneMessage(message)
        }
    }

    @Transactional
    fun sendOneMessage(message: OutboxConnect){
        try {
            // 메세지 브로커로 메세지 전송 및 동기적으로 결과 확인
            kafkaTemplate.send(
                message.topic,
                message.content
            ).get()

            // 전송 성공 시
            message.outboxStatus = OutboxStatus.SENT

        } catch (ex: Exception) {
            // 전송 실패 시
            message.outboxStatus = OutboxStatus.FAILED
        }

        outboxConnectRepo.save(message)
    }*/

    /*
    @Scheduled(fixedDelay = 1000)
    @Transactional
    fun sendToMessageBrokerNew() {
        val messages =
            outboxConnectRepo.findTop500ByOutboxStatusOrderByCreatedAtAsc(OutboxStatus.NEW)

        if (messages.isEmpty()) return

        val successIds = mutableListOf<Long>()
        val failedIds = mutableListOf<Long>()

        for (msg in messages) {
            try {
                kafkaTemplate.send(msg.topic, msg.content)
                successIds += msg.outboxId
            } catch (e: Exception) {
                failedIds += msg.outboxId
            }
        }

        // 한 번에 bulk UPDATE
        if (successIds.isNotEmpty()) {
            outboxConnectRepo.bulkUpdateStatus(OutboxStatus.SENT, successIds)
        }
        if (failedIds.isNotEmpty()) {
            outboxConnectRepo.bulkUpdateStatus(OutboxStatus.FAILED, failedIds)
        }
    }*/

    @Transactional
    fun <T: Any> createMessage(
        content: T,
        topicService: TopicService,
        methodName: String,
    ){
        // 컨텐츠를 JSON으로 변경
        val jsonContent = BaseUtil.toJson(content)

        // BASE64로 인코딩
        val baseContent = Base64.getEncoder().encodeToString(jsonContent.toByteArray())

        // 토픽 생성
        val topic = "${topicService}.${methodName}"

        // 생성
        outboxConnectRepo.save(
            OutboxConnect(
                content = baseContent,
                topic = topic,
                outboxStatus = OutboxStatus.NEW,
            )
        )
    }

    @Transactional
    fun increaseChatCountAndCreateChat(
        chatroomId: Long,
        content: String,
        userId: Long,
        riskLevel: RiskLevel,
    ) {
        // 채팅창 갱신
        chatroomWriter.increaseChatCount(chatroomId)

        // 아웃박스에 채팅 내역 저장을 메세지 형태로 전송
        createMessage(
            content =
                MessageWrapper(
                    methodName = "createChat",
                    content =
                        CreateChatRequest(
                            content = content,
                            chatroomId = chatroomId,
                            userId = userId,
                            riskLevel = riskLevel,
                        ),
                ),
            topicService = TopicService.CONNECT_SERVICE,
            methodName = "createChat",
        )
    }
}