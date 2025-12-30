package fanteract.connect.entity

import fanteract.connect.entity.constant.BaseEntity
import fanteract.connect.enumerate.RiskLevel
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "chats",
    indexes = [
        Index(name = "idx_chats_user_status", columnList = "user_id, status"),
        Index(name = "idx_chats_user_risk_status", columnList = "user_id, risk_level, status"),
    ],
)
class Chat(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val chatId: Long = 0L,
    val content: String,
    val chatroomId: Long,
    val userId: Long,
    @Enumerated(EnumType.STRING)
    val riskLevel: RiskLevel,
) : BaseEntity()
