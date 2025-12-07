package fanteract.connect.domain

import fanteract.connect.repo.UserChatroomHistoryRepo
import org.springframework.stereotype.Component

@Component
class UserChatroomHistoryReader(
    private val userChatroomHistoryRepo: UserChatroomHistoryRepo,
) {

}