package fanteract.connect.adapter

import fanteract.connect.repo.UserChatroomHistoryRepo
import org.springframework.stereotype.Component

@Component
class UserChatroomHistoryReader(
    private val userChatroomHistoryRepo: UserChatroomHistoryRepo,
)
