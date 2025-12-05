package fanteract.connect.domain

import fanteract.connect.entity.Chatroom
import fanteract.connect.repo.ChatroomRepo
import org.springframework.stereotype.Component

@Component
class ChatroomWriter(
    private val chatroomRepo: ChatroomRepo,
) {
    fun create(
        title: String,
        description: String?,
        userId: Long
    ): Chatroom {
        return chatroomRepo.save(
            Chatroom(
                title = title,
                description = description,
                userId = userId,
            )
        )

    }
}