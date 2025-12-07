package fanteract.connect.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import fanteract.connect.entity.constant.BaseEntity
import fanteract.connect.enumerate.RiskLevel

@Entity
@Table(name = "chats")
class Chat (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val chatId: Long = 0L,
    val content: String,
    val chatroomId: Long,
    val userId: Long,
    @Enumerated(EnumType.STRING)
    val riskLevel: RiskLevel,
): BaseEntity()