package fanteract.connect.adapter

import fanteract.connect.entity.UserChatroomHistory
import fanteract.connect.enumerate.ChatroomJoinStatus
import fanteract.connect.repo.UserChatroomHistoryRepo
import org.springframework.stereotype.Component
import kotlin.Long

@Component
class UserChatroomHistoryWriter(
    private val userChatroomHistoryRepo: UserChatroomHistoryRepo,
) {
    fun create(
        userId: Long,
        chatroomId: Long,
        chatroomJoinStatus: ChatroomJoinStatus,
    ): UserChatroomHistory {
        val userChatroomHistory =
            userChatroomHistoryRepo.save(
                UserChatroomHistory(
                    userId = userId,
                    chatroomId = chatroomId,
                    chatroomJoinStatus = chatroomJoinStatus,
                )
            )

        return userChatroomHistory
    }
}