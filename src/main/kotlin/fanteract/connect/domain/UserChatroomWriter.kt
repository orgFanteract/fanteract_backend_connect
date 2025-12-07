package fanteract.connect.domain

import fanteract.connect.entity.UserChatroom
import fanteract.connect.enumerate.ChatroomJoinStatus
import fanteract.connect.exception.ExceptionType
import fanteract.connect.exception.MessageType
import fanteract.connect.repo.UserChatroomRepo
import fanteract.connect.repo.UserChatroomHistoryRepo
import org.springframework.stereotype.Component
import kotlin.Long

@Component
class UserChatroomWriter(
    private val userChatroomRepo: UserChatroomRepo,
    private val userChatroomHistoryRepo: UserChatroomHistoryRepo,
) {
    fun create(
        userId: Long,
        chatroomId: Long,
        chatroomJoinStatus: ChatroomJoinStatus,
    ): UserChatroom {
        val userChatroom =
            userChatroomRepo.save(
                UserChatroom(
                    userId = userId,
                    chatroomId = chatroomId,
                    chatroomJoinStatus = chatroomJoinStatus,
                )
            )

        return userChatroom
    }

    fun update(
        userChatroomId: Long, 
        userId: Long, 
        chatroomId: Long, 
        chatroomJoinStatus: ChatroomJoinStatus
    ): UserChatroom {
        val userChatroom = userChatroomRepo.findById(userChatroomId)
            .orElseThrow{ExceptionType.withType(MessageType.NOT_EXIST)}

        userChatroom.userId = userId
        userChatroom.chatroomId = chatroomId
        userChatroom.chatroomJoinStatus = chatroomJoinStatus

        return userChatroomRepo.save(userChatroom)
    }
}