package fanteract.connect.repo

import fanteract.connect.entity.UserChatroom
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserChatroomRepo : JpaRepository<UserChatroom, Long> {
    fun findByUserIdAndChatroomId(
        userId: Long,
        chatroomId: Long,
    ): List<UserChatroom>
}
