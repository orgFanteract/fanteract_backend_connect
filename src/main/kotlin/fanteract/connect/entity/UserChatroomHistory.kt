package fanteract.connect.entity

import fanteract.connect.entity.constant.BaseEntity
import fanteract.connect.enumerate.ChatroomJoinStatus
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "user_chatroom_histories")
class UserChatroomHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val userChatroomId: Long = 0L,
    val userId: Long,
    val chatroomId: Long,
    @Enumerated(EnumType.STRING)
    val chatroomJoinStatus: ChatroomJoinStatus,
) : BaseEntity()
