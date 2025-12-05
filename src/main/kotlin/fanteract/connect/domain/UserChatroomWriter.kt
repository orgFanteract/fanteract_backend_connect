package fanteract.connect.domain

import fanteract.connect.entity.UserChatroom
import fanteract.connect.enumerate.ChatroomJoinStatus
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
            .orElseThrow{NoSuchElementException("조건에 맞는 채팅 기록이 존재하지 않습니다")}

        userChatroom.userId = userId
        userChatroom.chatroomId = chatroomId
        userChatroom.chatroomJoinStatus = chatroomJoinStatus

        return userChatroomRepo.save(userChatroom)
    }
}