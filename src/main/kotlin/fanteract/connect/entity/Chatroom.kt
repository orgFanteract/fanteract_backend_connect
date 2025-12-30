package fanteract.connect.entity

import fanteract.connect.entity.constant.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "chatrooms",
    indexes = [
        Index(name = "idx_chatrooms_user_status", columnList = "user_id, status"),
    ],
)
class Chatroom(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val chatroomId: Long = 0L,
    val title: String,
    val description: String?,
    var chatCount: Long = 0L,
    val userId: Long,
) : BaseEntity()
