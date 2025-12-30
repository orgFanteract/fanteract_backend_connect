package fanteract.connect.api.inner

import fanteract.connect.dto.inner.*
import fanteract.connect.entity.Chat
import fanteract.connect.enumerate.RiskLevel
import fanteract.connect.service.ChatService
import io.swagger.v3.oas.annotations.Hidden
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Hidden
@RestController
@RequestMapping("/internal/chats")
class ChatInnerAPI(
    private val chatService: ChatService,
) {
    @GetMapping("/{userId}/chat/count")
    fun readChatCountByUserId(
        @PathVariable userId: Long,
    ): ResponseEntity<ReadChatCountInnerResponse> {
        val response = chatService.countChatByUserId(userId)

        return ResponseEntity.ok().body(response)
    }

    @GetMapping("/{userId}/chatroom/count")
    fun readChatroomCountByUserId(
        @PathVariable userId: Long,
    ): ResponseEntity<ReadChatroomCountInnerResponse> {
        val response = chatService.countChatroomByUserId(userId)

        return ResponseEntity.ok().body(response)
    }

    @GetMapping("/{userId}/user/count")
    fun readChatCountByUserIdAndRiskLevel(
        @PathVariable userId: Long,
        @RequestParam riskLevel: RiskLevel,
    ): ResponseEntity<ReadChatCountInnerResponse> {
        val response = chatService.countChatByUserIdAndRiskLevel(userId, riskLevel)

        return ResponseEntity.ok().body(response)
    }

    @GetMapping("/{userId}/user")
    fun readChatByUserIdAndRiskLevel(
        @PathVariable userId: Long,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "10") size: Int,
        @RequestParam riskLevel: RiskLevel,
    ): ResponseEntity<ReadChatPageInnerResponse> {
        val response = chatService.findChatByUserIdAndRiskLevel(page, size, userId, riskLevel)

        return ResponseEntity.ok().body(response)
    }
}
