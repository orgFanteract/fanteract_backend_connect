package fanteract.connect.repo

import fanteract.connect.entity.UserChatroom
import fanteract.connect.entity.UserChatroomHistory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserChatroomHistoryRepo : JpaRepository<UserChatroomHistory, Long>
