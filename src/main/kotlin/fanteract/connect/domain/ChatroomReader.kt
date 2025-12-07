package fanteract.connect.domain

import fanteract.connect.entity.Chatroom
import fanteract.connect.enumerate.Status
import fanteract.connect.exception.ExceptionType
import fanteract.connect.exception.MessageType
import fanteract.connect.repo.ChatroomRepo
import org.springframework.stereotype.Component

@Component
class ChatroomReader(
    private val chatroomRepo: ChatroomRepo,
) {
    fun findByUserId(userId: Long): List<Chatroom> {
        return chatroomRepo.findByUserId(userId)
    }
    fun findByChatroomIdAndUserId(
        chatroomId: Long,
        userId: Long
    ): Chatroom {
        val chatroom =
            chatroomRepo.findById(chatroomId)
                .orElseThrow{ExceptionType.withType(MessageType.NOT_EXIST)}

        if (chatroom.userId != userId){
            throw ExceptionType.withType(MessageType.NOT_EXIST)
        }

        return chatroom
    }

    fun existsById(chatroomId: Long){
        val chatroom = chatroomRepo.findById(chatroomId).
            orElseThrow { ExceptionType.withType(MessageType.NOT_EXIST) }

        if (chatroom.status == Status.DELETED){
            throw ExceptionType.withType(MessageType.NOT_EXIST)
        }
    }

    fun findByUserIdAndTitleContaining(userId: Long, title: String): List<Chatroom> {
        return chatroomRepo.findByUserIdAndTitleContaining(userId, title)
    }

    fun countByUserId(userId: Long): Long {
        return chatroomRepo.countByUserId(userId)
    }

}
