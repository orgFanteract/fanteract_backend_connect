package fanteract.connect.api.outer

import fanteract.connect.annotation.LoginRequired
import io.swagger.v3.oas.annotations.Operation
import jakarta.servlet.http.HttpServletRequest
import fanteract.connect.config.JwtParser
import fanteract.connect.dto.outer.*
import fanteract.connect.service.ChatService
import org.springframework.http.ResponseEntity
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.security.Principal

@RestController
@RequestMapping("/api/chats")
class ChatAPI(
    private val chatService: ChatService,
) {
    // 채팅방 생성
    
    @Operation(summary = "채팅방 생성")
    @PostMapping("/chatroom")
    fun createChatroom(
        @RequestHeader("X-User-Id") userId: Long,
        @RequestBody createChatroomOuterRequest: CreateChatroomOuterRequest,
        ): ResponseEntity<CreateChatroomOuterResponse> {
        
        val response = chatService.createChatroom(userId, createChatroomOuterRequest)

        return ResponseEntity.ok().body(response)
    }

    // 유저 기반 채팅방 조회
    
    @Operation(summary = "사용자가 제작한 채팅방 조회")
    @GetMapping("/chatroom/user")
    fun readChatroomListByUserId(
        @RequestHeader("X-User-Id") userId: Long,
    ): ResponseEntity<ReadChatroomListOuterResponse> {
        val response = chatService.readChatroomListByUserId(userId)

        return ResponseEntity.ok().body(response)
    }

    // 이름 기반 채팅방 조회
    
    @Operation(summary = "이름 기반 채팅방 조회")
    @GetMapping("/chatroom")
    fun readChatroomListByUserIdAndTitleContaining(
        @RequestParam("title") title: String,
        @RequestHeader("X-User-Id") userId: Long,
    ): ResponseEntity<ReadChatroomListOuterResponse> {
        
        val response = chatService.readChatroomListByUserIdAndTitleContaining(userId, title)

        return ResponseEntity.ok().body(response)
    }

    // 채팅방 채팅내역 조회
    
    @Operation(summary = "채팅 내역 조회")
    @GetMapping("{chatroomId}/chat")
    fun readChatByChatroomId(
        @RequestHeader("X-User-Id") userId: Long,
        @PathVariable chatroomId: Long,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "10") size: Int,
    ): ResponseEntity<ReadChatListOuterResponse> {
        

        val response = chatService.readChatByChatroomId(
            userId = userId,
            chatroomId = chatroomId,
            page = page,
            size = size
        )

        return ResponseEntity.ok().body(response)
    }

    // 채팅방 채팅내역 조회
    
    @Operation(summary = "채팅 내역 기반 채팅 조회")
    @PostMapping("{chatroomId}/chat")
    fun readChatContainingByChatroomId(
        @RequestHeader("X-User-Id") userId: Long,
        @PathVariable chatroomId: Long,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestBody readChatContainingContentOuterRequest: ReadChatContainingContentOuterRequest
    ): ResponseEntity<ReadChatListOuterResponse> {
        

        val response =
            chatService.readChatContainingByChatroomId(
                userId = userId,
                chatroomId = chatroomId,
                readChatContainingContentOuterRequest = readChatContainingContentOuterRequest,
                page = page,
                size = 1 // 한 개씩 찾기 위해 1로 고정
            )

        return ResponseEntity.ok().body(response)
    }

    // 특정 채팅방 조회
    
    @Operation(summary = "아이디 기반 채팅방 조회")
    @GetMapping("{chatroomId}/chatroom/summary")
    fun readChatroomSummaryById(
        @RequestHeader("X-User-Id") userId: Long,
        @PathVariable chatroomId: Long,
    ): ResponseEntity<ReadChatroomOuterResponse> {
        
        val response = chatService.readChatroomSummaryById(userId, chatroomId)

        return ResponseEntity.ok().body(response)
    }

    // 특정 채팅방 접속
    
    @Operation(summary = "채팅방 입장")
    @PostMapping("/{chatroomId}/join")
    fun joinChatroom(
        @RequestHeader("X-User-Id") userId: Long,
        @PathVariable chatroomId: Long,
    ): ResponseEntity<JoinChatroomOuterResponse> {
        
        val response = chatService.joinChatroom(userId, chatroomId)

        return ResponseEntity.ok().body(response)
    }

    // 특정 채팅방 탈퇴
    
    @Operation(summary = "채팅방 퇴장")
    @PostMapping("/{chatroomId}/leave")
    fun leaveChatroom(
        @RequestHeader("X-User-Id") userId: Long,
        @PathVariable chatroomId: Long,
    ): ResponseEntity<LeaveChatroomOuterResponse> {
        
        val response = chatService.leaveChatroom(userId, chatroomId)

        return ResponseEntity.ok().body(response)
    }

    // 특정 채팅방에 채팅 전송
    @MessageMapping("/chat.{chatroomId}") // 다음 url path를 통해 발동
    @SendTo("/subscribe/chat.{chatroomId}") // 해당 결과는 다음 path를 구독하는 클라이언트에게 전달
    fun sendChat(
        principal: Principal,
        sendChatRequest: SendChatRequest,
        @DestinationVariable chatroomId: Long
    ): SendChatResponse {
        println("send Chat")
        val userId = principal.name.toLong()
        val response = chatService.sendChatNew(sendChatRequest, chatroomId, userId)
//        val response = chatService.sendChat(sendChatRequest, chatroomId, userId)
//        val response = chatService.sendChatOrigin(sendChatRequest, chatroomId, userId)

        return response
    }
}