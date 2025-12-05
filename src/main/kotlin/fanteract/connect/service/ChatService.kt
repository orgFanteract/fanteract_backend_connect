package fanteract.connect.service

import fanteract.connect.client.UserClient
import fanteract.connect.domain.ChatReader
import fanteract.connect.domain.ChatWriter
import fanteract.connect.domain.ChatroomReader
import fanteract.connect.domain.ChatroomWriter
import fanteract.connect.domain.UserChatroomHistoryReader
import fanteract.connect.domain.UserChatroomHistoryWriter
import fanteract.connect.domain.UserChatroomReader
import fanteract.connect.domain.UserChatroomWriter
import fanteract.connect.dto.outer.*
import fanteract.connect.dto.inner.*
import fanteract.connect.entity.UserChatroom
import fanteract.connect.enumerate.ActivePoint
import fanteract.connect.enumerate.Balance
import fanteract.connect.enumerate.ChatroomJoinStatus
import fanteract.connect.enumerate.RiskLevel
import fanteract.connect.filter.ProfanityFilterService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.Long
import kotlin.collections.associateBy
import kotlin.collections.map

@Transactional
@Service
class ChatService(
    private val chatroomReader: ChatroomReader,
    private val chatroomWriter: ChatroomWriter,
    private val chatWriter: ChatWriter,
    private val chatReader: ChatReader,
    private val userChatroomReader: UserChatroomReader,
    private val userChatroomWriter: UserChatroomWriter,
    private val userChatroomHistoryReader: UserChatroomHistoryReader,
    private val userChatroomHistoryWriter: UserChatroomHistoryWriter,
    private val userClient: UserClient,
    private val profanityFilterService: ProfanityFilterService
) {
    fun createChatroom(
        userId: Long,
        createChatroomOuterRequest: CreateChatroomOuterRequest
    ): CreateChatroomOuterResponse {
        val chatroom =
            chatroomWriter.create(
                title = createChatroomOuterRequest.title,
                description = createChatroomOuterRequest.description,
                userId = userId,
            )

        // TODO: 해당 채팅방에 참여 상태로 변경


        return CreateChatroomOuterResponse(
            chatroomId = chatroom.chatroomId,
        )
    }

    fun readChatroomListByUserId(
        userId: Long
    ):ReadChatroomListOuterResponse {
        val chatroomList = chatroomReader.findByUserId(userId)

        return ReadChatroomListOuterResponse(
            chatroomList.map {
                ReadChatroomOuterResponse(
                    chatroomId = it.chatroomId,
                    title = it.title,
                    description = it.description,
                )
            }
        )
    }
    fun readChatroomSummaryById(
        userId: Long,
        chatroomId: Long,
    ): ReadChatroomOuterResponse {
        val chatroom = chatroomReader.findByChatroomIdAndUserId(chatroomId, userId)

        return ReadChatroomOuterResponse(
            chatroomId = chatroom.chatroomId,
            title = chatroom.title,
            description = chatroom.description,
        )
    }

    fun joinChatroom(
        userId: Long,
        chatroomId: Long
    ): JoinChatroomOuterResponse {
        // 채팅방 존재여부 확인
        chatroomReader.existsById(chatroomId)

        // 사용자 존재여부 확인
        userClient.existsById(userId)

        // 채팅방 접속기록 확인
        val preUserChatroom = userChatroomReader.findByUserIdAndChatroomId(userId, chatroomId)

        // 접속 기록이 존재하는지 확인
        if (preUserChatroom != null && preUserChatroom.chatroomJoinStatus == ChatroomJoinStatus.JOIN){
            throw kotlin.NoSuchElementException("이미 참여중인 채팅방입니다")
        }

        // 채팅방 입장 기록 생성
        val userChatroom =
            joinUserChatroom(
                preUserChatroom = preUserChatroom,
                userId = userId,
                chatroomId = chatroomId,
            )

        return JoinChatroomOuterResponse(userChatroom.userChatroomId)
    }
    fun leaveChatroom(
        userId: Long,
        chatroomId: Long
    ): LeaveChatroomOuterResponse {
        // 채팅방 존재여부 확인
        chatroomReader.existsById(chatroomId)

        // 사용자 존재여부 확인
        userClient.existsById(userId)

        // 채팅방 접속기록 확인
        val preUserChatroom = userChatroomReader.findByUserIdAndChatroomId(userId, chatroomId)

        // 접속 기록이 존재하는지 확인
        if (preUserChatroom == null || preUserChatroom.chatroomJoinStatus == ChatroomJoinStatus.LEAVE){
            throw kotlin.NoSuchElementException("이미 탈퇴했거나 입장하지 않은 채팅방입니다")
        }

        // 채팅방 입장 기록 변경
        val userChatroom =
            leaveUserChatroom(
                preUserChatroom = preUserChatroom,
                userId = preUserChatroom.userId,
                chatroomId = preUserChatroom.chatroomId,
            )

        return LeaveChatroomOuterResponse(userChatroom.userChatroomId)
    }
    fun sendChat(
        sendChatRequest: SendChatRequest,
        chatroomId: Long,
        userId: Long
    ): SendChatResponse {
        // 비용 검증 및 차감
        val user = userClient.findById(userId)
        
        if (user.balance < Balance.CHAT.cost){
            throw kotlin.IllegalArgumentException("비용이 부족합니다")
        }

        userClient.updateBalance(userId, -Balance.CHAT.cost)
        
        // 게시글 필터링 진행
        val riskLevel =
            profanityFilterService.checkProfanityAndUpdateAbusePoint(
                userId = userId,
                text = sendChatRequest.content,
            )

        val chat =
            chatWriter.create(
                content = sendChatRequest.content,
                chatroomId = chatroomId,
                userId = userId,
                riskLevel = riskLevel,
            )

        // 활동 점수 변경
        if (riskLevel != RiskLevel.BLOCK) {
            userClient.updateActivePoint(
                userId = userId,
                activePoint = ActivePoint.CHAT.point
            )
        }

        return SendChatResponse(
            chatId = chat.chatId,
            userName = user.name,
            content = chat.content,
            createdAt = chat.createdAt!!,
            riskLevel = riskLevel,
        )
    }

    fun leaveUserChatroom(
        preUserChatroom: UserChatroom,
        userId: Long,
        chatroomId: Long,
    ): UserChatroom {
        val userChatroom =
            userChatroomWriter.update(
                userChatroomId = preUserChatroom.userChatroomId,
                userId = preUserChatroom.userId,
                chatroomId = preUserChatroom.chatroomId,
                chatroomJoinStatus = ChatroomJoinStatus.LEAVE
            )

        userChatroomHistoryWriter.create(
            userId = userId,
            chatroomId = chatroomId,
            chatroomJoinStatus = ChatroomJoinStatus.LEAVE
        )

        return userChatroom
    }
    fun joinUserChatroom(
        preUserChatroom: UserChatroom?,
        userId: Long,
        chatroomId: Long,
    ): UserChatroom {
        val userChatroom =
            if (preUserChatroom == null) {
                // 입장한 경험이 없는 경우
                userChatroomWriter.create(
                    userId = userId,
                    chatroomId = chatroomId,
                    chatroomJoinStatus = ChatroomJoinStatus.JOIN
                )
            } else {
                // 입장 후 떠난 상태인 경우
                userChatroomWriter.update(
                    userChatroomId = preUserChatroom.userChatroomId,
                    userId = preUserChatroom.userId,
                    chatroomId = preUserChatroom.chatroomId,
                    chatroomJoinStatus = ChatroomJoinStatus.JOIN
                )
            }

        // history 기록
        userChatroomHistoryWriter.create(
            userId = userId,
            chatroomId = chatroomId,
            chatroomJoinStatus = ChatroomJoinStatus.JOIN
        )

        return userChatroom
    }

    @Transactional(readOnly = true)
    fun readChatByChatroomId(
        userId: Long,
        chatroomId: Long,
        page: Int,
        size: Int
    ): ReadChatListOuterResponse {
        val pageable = PageRequest.of(
            page,
            size,
            Sort.by(Sort.Direction.DESC, "createdAt")
        )

        val chatPage = chatReader.findByUserIdAndChatroomId(
            userId = userId,
            chatroomId = chatroomId,
            pageable = pageable
        )

        val userMap = userClient.findByIdIn(chatPage.content.map {it.userId}).associateBy {it.userId }

        val contents = chatPage.content.map { chat ->
            val user = userMap[chat.userId]
            ReadChatOuterResponse(
                chatId = chat.chatId,
                userName = user?.name ?: "-",
                content = chat.content,
                createdAt = chat.createdAt!!
            )
        }

        return ReadChatListOuterResponse(
            contents = contents,
            page = chatPage.number,
            size = chatPage.size,
            totalElements = chatPage.totalElements,
            totalPages = chatPage.totalPages,
            hasNext = chatPage.hasNext()
        )
    }

    fun readChatContainingByChatroomId(
        userId: Long,
        chatroomId: Long,
        readChatContainingContentOuterRequest: ReadChatContainingContentOuterRequest,
        page: Int,
        size: Int
    ): ReadChatListOuterResponse {
        val pageable = PageRequest.of(
            page,
            size,
            Sort.by(Sort.Direction.DESC, "createdAt")
        )

        val chatPage = chatReader.findByUserIdAndChatroomIdAnd(
            userId = userId,
            chatroomId = chatroomId,
            content = readChatContainingContentOuterRequest.content,
            pageable = pageable
        )

        val userMap = userClient.findByIdIn(chatPage.content.map {it.userId}).associateBy {it.userId }

        val contents = chatPage.content.map { chat ->
            val user = userMap[chat.userId]
            ReadChatOuterResponse(
                chatId = chat.chatId,
                userName = user?.name ?: "-",
                content = chat.content,
                createdAt = chat.createdAt!!
            )
        }

        return ReadChatListOuterResponse(
            contents = contents,
            page = chatPage.number,
            size = chatPage.size,
            totalElements = chatPage.totalElements,
            totalPages = chatPage.totalPages,
            hasNext = chatPage.hasNext()
        )
    }

    fun readChatroomListByUserIdAndTitleContaining(
        userId: Long,
        title: String
    ): ReadChatroomListOuterResponse {
        val chatroomList = chatroomReader.findByUserIdAndTitleContaining(userId, title)

        return ReadChatroomListOuterResponse(
            chatroomList.map {
                ReadChatroomOuterResponse(
                    chatroomId = it.chatroomId,
                    title = it.title,
                    description = it.description,
                )
            }
        )
    }

    fun countChatByUserId(userId: Long): ReadChatCountInnerResponse {
        val response = chatReader.countByUserId(userId)

        return ReadChatCountInnerResponse(response)
    }
    fun countChatroomByUserId(userId: Long): ReadChatroomCountInnerResponse {
        val response = chatroomReader.countByUserId(userId)

        return ReadChatroomCountInnerResponse(response)
    }
    fun countChatByUserIdAndRiskLevel(userId: Long, riskLevel: RiskLevel): ReadChatCountInnerResponse {
        val response = chatReader.countByUserIdAndRiskLevel(userId, riskLevel)

        return ReadChatCountInnerResponse(response)
    }
    fun findChatByUserIdAndRiskLevel(page: Int, size: Int, userId: Long, riskLevel: RiskLevel): ReadChatPageInnerResponse {
        val pageable = PageRequest.of(
            page,
            size,
            Sort.by(Sort.Direction.DESC, "createdAt")
        )

        val chatPage = chatReader.findByUserIdAndRiskLevel(userId, riskLevel, pageable)
        val chatContent = chatPage.content

        val response = chatContent.map { chat ->
            ReadChatInnerResponse(
                chatId = chat.chatId,
                content = chat.content,
                chatroomId = chat.chatroomId,
                userId = chat.userId,
                riskLevel = chat.riskLevel,
            )
        }

        return ReadChatPageInnerResponse(
            contents = response,
            page = chatPage.number,
            size = chatPage.size,
            totalElements = chatPage.totalElements,
            totalPages = chatPage.totalPages,
            hasNext = chatPage.hasNext()
        )
    }
}