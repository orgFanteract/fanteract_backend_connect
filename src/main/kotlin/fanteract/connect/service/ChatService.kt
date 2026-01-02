package fanteract.connect.service

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import fanteract.connect.adapter.ChatReader
import fanteract.connect.adapter.ChatWriter
import fanteract.connect.adapter.ChatroomReader
import fanteract.connect.adapter.ChatroomWriter
import fanteract.connect.adapter.MessageAdapter
import fanteract.connect.adapter.UserChatroomHistoryReader
import fanteract.connect.adapter.UserChatroomHistoryWriter
import fanteract.connect.adapter.UserChatroomReader
import fanteract.connect.adapter.UserChatroomWriter
import fanteract.connect.client.AccountClient
import fanteract.connect.dto.UpdateActivePointRequest
import fanteract.connect.dto.client.CreateChatRequest
import fanteract.connect.dto.client.MessageWrapper
import fanteract.connect.dto.client.WriteCommentForUserRequest
import fanteract.connect.dto.inner.*
import fanteract.connect.dto.outer.*
import fanteract.connect.entity.UserChatroom
import fanteract.connect.enumerate.ActivePoint
import fanteract.connect.enumerate.Balance
import fanteract.connect.enumerate.ChatroomJoinStatus
import fanteract.connect.enumerate.RiskLevel
import fanteract.connect.enumerate.TopicService
import fanteract.connect.enumerate.WriteStatus
import fanteract.connect.exception.ExceptionType
import fanteract.connect.exception.MessageType
import fanteract.connect.filter.ProfanityFilterService
import fanteract.connect.util.ChatCountAccumulator
import fanteract.connect.util.DeltaInMemoryStorage
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
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
    private val accountClient: AccountClient,
    private val outboxConnectService: OutboxConnectService,
    private val profanityFilterService: ProfanityFilterService,
    private val chatCountAccumulator: ChatCountAccumulator,
    private val messageAdapter: MessageAdapter,
    private val deltaInMemoryStorage: DeltaInMemoryStorage,
) {
    fun createChatroom(
        userId: Long,
        createChatroomOuterRequest: CreateChatroomOuterRequest,
    ): CreateChatroomOuterResponse {
        val chatroom =
            chatroomWriter.create(
                title = createChatroomOuterRequest.title,
                description = createChatroomOuterRequest.description,
                userId = userId,
            )

        // TODO: 해당 채팅방에 참여 상태로 변경

        val flag = false

        if (flag) {
            // 카프카 기반 쓰기 행위 메세지 전달
            messageAdapter.sendMessageUsingBroker(
                message =
                    WriteCommentForUserRequest(
                        userId = userId,
                        writeStatus = WriteStatus.CREATED,
                        riskLevel = RiskLevel.ALLOW,
                    ),
                topicService = TopicService.CONNECT_SERVICE,
                methodName = "createChatroomForUser",
            )
        } else {
            deltaInMemoryStorage.addDelta(userId, "chatroomCount", 1)
        }

        return CreateChatroomOuterResponse(
            chatroomId = chatroom.chatroomId,
        )
    }

    fun readChatroomListByUserId(userId: Long): ReadChatroomListOuterResponse {
        val chatroomList = chatroomReader.findByUserId(userId)

        return ReadChatroomListOuterResponse(
            chatroomList.map {
                ReadChatroomOuterResponse(
                    chatroomId = it.chatroomId,
                    title = it.title,
                    description = it.description,
                )
            },
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
        chatroomId: Long,
    ): JoinChatroomOuterResponse {
        // 채팅방 존재여부 확인
        chatroomReader.existsById(chatroomId)

        // 사용자 존재여부 확인
        accountClient.existsById(userId)

        // 채팅방 접속기록 확인
        val preUserChatroom = userChatroomReader.findByUserIdAndChatroomId(userId, chatroomId)

        // 접속 기록이 존재하는지 확인
        if (preUserChatroom != null && preUserChatroom.chatroomJoinStatus == ChatroomJoinStatus.JOIN) {
            throw ExceptionType.withType(MessageType.ALREADY_JOINED)
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
        chatroomId: Long,
    ): LeaveChatroomOuterResponse {
        // 채팅방 존재여부 확인
        chatroomReader.existsById(chatroomId)

        // 사용자 존재여부 확인
        accountClient.existsById(userId)

        // 채팅방 접속기록 확인
        val preUserChatroom = userChatroomReader.findByUserIdAndChatroomId(userId, chatroomId)

        // 접속 기록이 존재하는지 확인
        if (preUserChatroom == null || preUserChatroom.chatroomJoinStatus == ChatroomJoinStatus.LEAVE) {
            throw ExceptionType.withType(MessageType.ALREADY_LEFT)
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

    @Scheduled(fixedDelay = 10000)
    fun updateChatCount() {
        val chatCounterMap = chatCountAccumulator.drain()

        if (chatCounterMap.isEmpty()) {
            return
        }

        chatCounterMap.forEach { (chatroomId, delta) ->
            chatroomWriter.incrementChatCount(chatroomId, delta)
        }
    }

    // 아웃박스 패턴 제거 및 채팅 수 업데이트 개선
    fun sendChatNew(
        sendChatRequest: SendChatRequest,
        chatroomId: Long,
        userId: Long,
    ): SendChatResponse {
        // 비용 검증 및 차감
        val user = accountClient.findById(userId)

        if (user.balance < Balance.CHAT.cost) {
            throw ExceptionType.withType(MessageType.NOT_ENOUGH_BALANCE)
        }

        accountClient.updateBalance(userId, -Balance.CHAT.cost)

        // 게시글 필터링 진행
        val riskLevel =
            profanityFilterService
                .checkProfanityAndUpdateAbusePoint(
                    userId = userId,
                    text = sendChatRequest.content,
                )

        // 채팅 내역 비동기 전송
        messageAdapter.sendMessageUsingBroker(
            message =
                CreateChatRequest(
                    content = sendChatRequest.content,
                    chatroomId = chatroomId,
                    userId = userId,
                    riskLevel = riskLevel,
                ),
            topicService = TopicService.CONNECT_SERVICE,
            methodName = "createChat",
        )

        // 채팅방 메타데이터 갱신
        chatCountAccumulator.increase(chatroomId)

        // 활동 점수 변경을 비동기 방식으로 진행
        if (riskLevel != RiskLevel.BLOCK) {
            messageAdapter.sendMessageUsingBroker(
                message =
                    UpdateActivePointRequest(
                        userId = userId,
                        activePoint = ActivePoint.CHAT.point,
                    ),
                topicService = TopicService.ACCOUNT_SERVICE,
                methodName = "updateActivePoint",
            )
        }

        val flag = false

        if (flag) {
            // 카프카 기반 쓰기 행위 메세지 전달
            messageAdapter.sendMessageUsingBroker(
                message =
                    WriteCommentForUserRequest(
                        userId = userId,
                        writeStatus = WriteStatus.CREATED,
                        riskLevel = riskLevel,
                    ),
                topicService = TopicService.CONNECT_SERVICE,
                methodName = "createChatForUser",
            )
        } else {
            deltaInMemoryStorage.addDelta(userId, "chatCount", 1)

            if (riskLevel == RiskLevel.BLOCK) {
                deltaInMemoryStorage.addDelta(userId, "restrictedChatCount", 1)
            }
        }

        // 결과를 구독자에게 전송
        return SendChatResponse(
            chatId = null,
            userName = user.name,
            content = sendChatRequest.content,
            createdAt = LocalDateTime.now(),
            riskLevel = riskLevel,
            sentAt = sendChatRequest.sentAt,
        )
    }

    fun sendChat(
        sendChatRequest: SendChatRequest,
        chatroomId: Long,
        userId: Long,
    ): SendChatResponse {
        // 비용 검증 및 차감
        val user = accountClient.findById(userId)

        if (user.balance < Balance.CHAT.cost) {
            throw ExceptionType.withType(MessageType.NOT_ENOUGH_BALANCE)
        }

        accountClient.updateBalance(userId, -Balance.CHAT.cost)

        // 게시글 필터링 진행
        val riskLevel =
            profanityFilterService
                .checkProfanityAndUpdateAbusePoint(
                    userId = userId,
                    text = sendChatRequest.content,
                )

        // 채팅 수 증가(동기)와 채팅 내역 저장(비동기)을 아웃박스 패턴으로 연결
        outboxConnectService
            .increaseChatCountAndCreateChat(
                chatroomId = chatroomId,
                content = sendChatRequest.content,
                userId = userId,
                riskLevel = riskLevel,
            )

        // 활동 점수 변경을 비동기 방식으로 진행
        if (riskLevel != RiskLevel.BLOCK) {
            chatWriter.sendMessageUsingMessage(
                content =
                    MessageWrapper(
                        methodName = "updateActivePoint",
                        content =
                            UpdateActivePointRequest(
                                userId = userId,
                                activePoint = ActivePoint.CHAT.point,
                            ),
                    ),
                topicService = TopicService.ACCOUNT_SERVICE,
                methodName = "updateActivePoint",
            )
        }

        // 결과를 구독자에게 전송
        return SendChatResponse(
            chatId = null,
            userName = user.name,
            content = sendChatRequest.content,
            createdAt = LocalDateTime.now(),
            riskLevel = riskLevel,
            sentAt = sendChatRequest.sentAt,
        )
    }

    fun sendChatOrigin(
        sendChatRequest: SendChatRequest,
        chatroomId: Long,
        userId: Long,
    ): SendChatResponse {
        // 비용 검증 및 차감
        val user = accountClient.findById(userId)

        if (user.balance < Balance.CHAT.cost) {
            throw ExceptionType.withType(MessageType.NOT_ENOUGH_BALANCE)
        }

        accountClient.updateBalance(userId, -Balance.CHAT.cost)

        // 게시글 필터링 진행
        val riskLevel =
            profanityFilterService
                .checkProfanityAndUpdateAbusePoint(
                    userId = userId,
                    text = sendChatRequest.content,
                )

        // 채팅룸 정보 갱신
        chatroomWriter.increaseChatCount(chatroomId)

        // 채팅 내역 생성
        chatWriter.create(
            chatroomId = chatroomId,
            content = sendChatRequest.content,
            userId = userId,
            riskLevel = riskLevel,
        )

        // 활동 점수 변경
        if (riskLevel != RiskLevel.BLOCK) {
            accountClient.updateActivePoint(
                userId = userId,
                activePoint = ActivePoint.CHAT.point,
            )
        }

        // 결과를 구독자에게 전송
        return SendChatResponse(
            chatId = null,
            userName = user.name,
            content = sendChatRequest.content,
            createdAt = LocalDateTime.now(),
            riskLevel = riskLevel,
            sentAt = sendChatRequest.sentAt,
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
                chatroomJoinStatus = ChatroomJoinStatus.LEAVE,
            )

        userChatroomHistoryWriter.create(
            userId = userId,
            chatroomId = chatroomId,
            chatroomJoinStatus = ChatroomJoinStatus.LEAVE,
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
                    chatroomJoinStatus = ChatroomJoinStatus.JOIN,
                )
            } else {
                // 입장 후 떠난 상태인 경우
                userChatroomWriter.update(
                    userChatroomId = preUserChatroom.userChatroomId,
                    userId = preUserChatroom.userId,
                    chatroomId = preUserChatroom.chatroomId,
                    chatroomJoinStatus = ChatroomJoinStatus.JOIN,
                )
            }

        // history 기록
        userChatroomHistoryWriter.create(
            userId = userId,
            chatroomId = chatroomId,
            chatroomJoinStatus = ChatroomJoinStatus.JOIN,
        )

        return userChatroom
    }

    @Transactional(readOnly = true)
    fun readChatByChatroomId(
        userId: Long,
        chatroomId: Long,
        page: Int,
        size: Int,
    ): ReadChatListOuterResponse {
        val pageable =
            PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt"),
            )

        val chatPage =
            chatReader.findByUserIdAndChatroomId(
                userId = userId,
                chatroomId = chatroomId,
                pageable = pageable,
            )

        val userMap = accountClient.findByIdIn(chatPage.content.map { it.userId }).associateBy { it.userId }

        val contents =
            chatPage.content.map { chat ->
                val user = userMap[chat.userId]
                ReadChatOuterResponse(
                    chatId = chat.chatId,
                    userName = user?.name ?: "-",
                    content = chat.content,
                    createdAt = chat.createdAt!!,
                )
            }

        return ReadChatListOuterResponse(
            contents = contents,
            page = chatPage.number,
            size = chatPage.size,
            totalElements = chatPage.totalElements,
            totalPages = chatPage.totalPages,
            hasNext = chatPage.hasNext(),
        )
    }

    fun readChatContainingByChatroomId(
        userId: Long,
        chatroomId: Long,
        readChatContainingContentOuterRequest: ReadChatContainingContentOuterRequest,
        page: Int,
        size: Int,
    ): ReadChatListOuterResponse {
        val pageable =
            PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt"),
            )

        val chatPage =
            chatReader.findByUserIdAndChatroomIdAnd(
                userId = userId,
                chatroomId = chatroomId,
                content = readChatContainingContentOuterRequest.content,
                pageable = pageable,
            )

        val userMap = accountClient.findByIdIn(chatPage.content.map { it.userId }).associateBy { it.userId }

        val contents =
            chatPage.content.map { chat ->
                val user = userMap[chat.userId]
                ReadChatOuterResponse(
                    chatId = chat.chatId,
                    userName = user?.name ?: "-",
                    content = chat.content,
                    createdAt = chat.createdAt!!,
                )
            }

        return ReadChatListOuterResponse(
            contents = contents,
            page = chatPage.number,
            size = chatPage.size,
            totalElements = chatPage.totalElements,
            totalPages = chatPage.totalPages,
            hasNext = chatPage.hasNext(),
        )
    }

    fun readChatroomListByUserIdAndTitleContaining(
        userId: Long,
        title: String,
    ): ReadChatroomListOuterResponse {
        val chatroomList = chatroomReader.findByUserIdAndTitleContaining(userId, title)

        return ReadChatroomListOuterResponse(
            chatroomList.map {
                ReadChatroomOuterResponse(
                    chatroomId = it.chatroomId,
                    title = it.title,
                    description = it.description,
                )
            },
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

    fun countChatByUserIdAndRiskLevel(
        userId: Long,
        riskLevel: RiskLevel,
    ): ReadChatCountInnerResponse {
        val response = chatReader.countByUserIdAndRiskLevel(userId, riskLevel)

        return ReadChatCountInnerResponse(response)
    }

    fun findChatByUserIdAndRiskLevel(
        page: Int,
        size: Int,
        userId: Long,
        riskLevel: RiskLevel,
    ): ReadChatPageInnerResponse {
        val pageable =
            PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt"),
            )

        val chatPage = chatReader.findByUserIdAndRiskLevel(userId, riskLevel, pageable)
        val chatContent = chatPage.content

        val response =
            chatContent.map { chat ->
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
            hasNext = chatPage.hasNext(),
        )
    }
}
