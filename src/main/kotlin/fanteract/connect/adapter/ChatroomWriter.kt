package fanteract.connect.adapter

import fanteract.connect.entity.Chatroom
import fanteract.connect.exception.ExceptionType
import fanteract.connect.exception.MessageType
import fanteract.connect.repo.ChatroomRepo
import org.springframework.stereotype.Component

@Component
class ChatroomWriter(
    private val chatroomRepo: ChatroomRepo,
) {
    fun create(
        title: String,
        description: String?,
        userId: Long,
    ): Chatroom =
        chatroomRepo.save(
            Chatroom(
                title = title,
                description = description,
                userId = userId,
            ),
        )

    fun increaseChatCount(chatroomId: Long) {
        val chatroom =
            chatroomRepo
                .findById(chatroomId)
                .orElseThrow { ExceptionType.withType(MessageType.NOT_EXIST) }

        chatroom.chatCount += 1

        chatroomRepo.save(chatroom)
    }

    fun incrementChatCount(
        chatroomId: Long,
        delta: Long,
    ) {
        chatroomRepo.incrementChatCount(chatroomId, delta)
    }
}
