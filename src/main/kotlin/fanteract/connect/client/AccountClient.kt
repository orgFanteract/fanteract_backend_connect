package fanteract.connect.client

import fanteract.connect.dto.client.ReadUserExistsInnerResponse
import fanteract.connect.dto.client.ReadUserInnerResponse
import fanteract.connect.dto.client.ReadUserListInnerResponse
import fanteract.connect.dto.client.UpdateAbusePointInnerRequest
import fanteract.connect.dto.client.UpdateActivePointInnerRequest
import fanteract.connect.dto.client.UpdateBalanceInnerRequest
import fanteract.connect.exception.ExceptionType
import fanteract.connect.exception.MessageType
import fanteract.connect.util.CircuitBreakerManager
import fanteract.connect.util.CircuitBreakerUtil
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import kotlin.collections.toTypedArray
import kotlin.jvm.java
import kotlin.text.get

@Component
class AccountClient(
    @Value($$"${client.account-service.url}") userServiceUrl: String,
    private val restClient: RestClient =
        RestClient
            .builder()
            .baseUrl(userServiceUrl)
            .build(),
    private val circuitBreakerRegistry: CircuitBreakerRegistry,
    private val circuitBreakerManager: CircuitBreakerManager,
    private val circuitBreakerUtil: CircuitBreakerUtil,
) {
    fun existsById(userId: Long): Boolean {
        val response =
            circuitBreakerUtil
                .circuitBreaker(
                    profile = circuitBreakerManager.accountConfig,
                ) {
                    restClient
                        .get()
                        .uri("/internal/users/{userId}/exists", userId)
                        .retrieve()
                        .body(ReadUserExistsInnerResponse::class.java)
                }.fallbackIfOpen {
                    throw ExceptionType.withType(MessageType.CALL_NOT_PERMITTED)
                }.fallback {
                    throw ExceptionType.withType(MessageType.INVALID_ACCESS_RESOURCE)
                }.get()

        return response?.exists ?: false
    }

    fun findById(userId: Long): ReadUserInnerResponse {
        val response =
            circuitBreakerUtil
                .circuitBreaker(
                    profile = circuitBreakerManager.accountConfig,
                ) {
                    restClient
                        .get()
                        .uri("/internal/users/{userId}", userId)
                        .retrieve()
                        .body(ReadUserInnerResponse::class.java)
                }.fallbackIfOpen {
                    throw ExceptionType.withType(MessageType.CALL_NOT_PERMITTED)
                }.fallback {
                    throw ExceptionType.withType(MessageType.INVALID_ACCESS_RESOURCE)
                }.get()

        return requireNotNull(response) { "User not found for id=$userId" }
    }

    fun updateBalance(
        userId: Long,
        balance: Int,
    ) {
        val request = UpdateBalanceInnerRequest(balance = balance)

        circuitBreakerUtil
            .circuitBreaker(
                profile = circuitBreakerManager.accountConfig,
            ) {
                restClient
                    .put()
                    .uri("/internal/users/{userId}/balance", userId)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity()
            }.fallbackIfOpen {
                throw ExceptionType.withType(MessageType.CALL_NOT_PERMITTED)
            }.fallback {
                throw ExceptionType.withType(MessageType.INVALID_ACCESS_RESOURCE)
            }.get()
    }

    fun updateActivePoint(
        userId: Long,
        activePoint: Int,
    ) {
        val request = UpdateActivePointInnerRequest(activePoint = activePoint)

        circuitBreakerUtil
            .circuitBreaker(
                profile = circuitBreakerManager.accountConfig,
            ) {
                restClient
                    .put()
                    .uri("/internal/users/{userId}/active-point", userId)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity()
            }.fallbackIfOpen {
                throw ExceptionType.withType(MessageType.CALL_NOT_PERMITTED)
            }.fallback {
                throw ExceptionType.withType(MessageType.INVALID_ACCESS_RESOURCE)
            }.get()
    }

    fun findByIdIn(userIds: List<Long>): List<ReadUserInnerResponse> {
        val response =
            circuitBreakerUtil
                .circuitBreaker(
                    profile = circuitBreakerManager.accountConfig,
                ) {
                    restClient
                        .get()
                        .uri { builder ->
                            builder
                                .path("/internal/users/batch")
                                .queryParam("userIds", *userIds.toTypedArray())
                                .build()
                        }.retrieve()
                        .body(ReadUserListInnerResponse::class.java)
                }.fallbackIfOpen {
                    throw ExceptionType.withType(MessageType.CALL_NOT_PERMITTED)
                }.fallback {
                    throw ExceptionType.withType(MessageType.INVALID_ACCESS_RESOURCE)
                }.get()

        return response?.users ?: emptyList()
    }

    fun updateAbusePoint(
        userId: Long,
        abusePoint: Int,
    ) {
        val request = UpdateAbusePointInnerRequest(abusePoint = abusePoint)

        circuitBreakerUtil
            .circuitBreaker(
                profile = circuitBreakerManager.accountConfig,
            ) {
                restClient
                    .put()
                    .uri("/internal/users/{userId}/abuse-point", userId)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity()
            }.fallbackIfOpen {
                throw ExceptionType.withType(MessageType.CALL_NOT_PERMITTED)
            }.fallback {
                throw ExceptionType.withType(MessageType.INVALID_ACCESS_RESOURCE)
            }.get()
    }
}
