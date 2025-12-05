package fanteract.connect.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import fanteract.connect.entity.constant.BaseEntity

@Entity
@Table(name = "chatrooms")
class Chatroom (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val chatroomId: Long = 0L,
    val title: String,
    val description: String?,
    val userId: Long,
): BaseEntity()