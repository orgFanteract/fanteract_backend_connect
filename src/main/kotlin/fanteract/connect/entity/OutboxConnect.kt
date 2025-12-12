package fanteract.connect.entity

import fanteract.connect.entity.constant.BaseEntity
import fanteract.connect.enumerate.OutboxStatus
import fanteract.connect.enumerate.TopicService
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table


@Entity
@Table(name = "outbox_connect")
data class OutboxConnect(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val outboxId: Long = 0L,
    val content: String, // 전송 내용. JSON을 base64로 인코딩 후 적재
    val topic: String, // 구독 대상 (대상 서비스 + 메서드 이름)
    @Enumerated(EnumType.STRING)
    var outboxStatus: OutboxStatus,
): BaseEntity()