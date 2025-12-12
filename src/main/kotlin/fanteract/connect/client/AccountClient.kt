package fanteract.connect.client

import fanteract.connect.dto.client.ReadUserExistsInnerResponse
import fanteract.connect.dto.client.ReadUserInnerResponse
import fanteract.connect.dto.client.ReadUserListInnerResponse
import fanteract.connect.dto.client.UpdateAbusePointInnerRequest
import fanteract.connect.dto.client.UpdateActivePointInnerRequest
import fanteract.connect.dto.client.UpdateBalanceInnerRequest
import fanteract.connect.exception.ExceptionType
import fanteract.connect.exception.MessageType
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import kotlin.collections.toTypedArray
import kotlin.jvm.java

@Component
class AccountClient(
    @Value($$"${client.account-service.url}") userServiceUrl: String,
    private val restClient: RestClient = RestClient.builder()
        .baseUrl(userServiceUrl)
        .build(),
    private val circuitBreakerRegistry: CircuitBreakerRegistry
) {
    @CircuitBreaker(name = "accountClient", fallbackMethod = "existsByIdFallback")
    fun existsById(userId: Long): Boolean {
        val response = restClient.get()
            .uri("/internal/users/{userId}/exists", userId)
            .retrieve()
            .body(ReadUserExistsInnerResponse::class.java)

        return response?.exists ?: false
    }

    @CircuitBreaker(name = "accountClient", fallbackMethod = "findByIdFallback")
    fun findById(userId: Long): ReadUserInnerResponse {
        val response = restClient.get()
            .uri("/internal/users/{userId}", userId)
            .retrieve()
            .body(ReadUserInnerResponse::class.java)

        return requireNotNull(response) { "User not found for id=$userId" }
    }

    @CircuitBreaker(name = "accountClient", fallbackMethod = "updateBalanceFallback")
    fun updateBalance(userId: Long, balance: Int) {
        val request = UpdateBalanceInnerRequest(balance = balance)

        restClient.put()
            .uri("/internal/users/{userId}/balance", userId)
            .body(request)
            .retrieve()
            .toBodilessEntity()
    }

    @CircuitBreaker(name = "accountClient", fallbackMethod = "updateActivePointFallback")
    fun updateActivePoint(userId: Long, activePoint: Int) {
        val request = UpdateActivePointInnerRequest(activePoint = activePoint)

        restClient.put()
            .uri("/internal/users/{userId}/active-point", userId)
            .body(request)
            .retrieve()
            .toBodilessEntity()
    }

    @CircuitBreaker(name = "accountClient", fallbackMethod = "findByIdInFallback")
    fun findByIdIn(userIds: List<Long>): List<ReadUserInnerResponse> {
        val response = restClient.get()
            .uri { builder ->
                builder
                    .path("/internal/users/batch")
                    .queryParam("userIds", *userIds.toTypedArray())
                    .build()
            }
            .retrieve()
            .body(ReadUserListInnerResponse::class.java)

        return response?.users ?: emptyList()
    }

    @CircuitBreaker(name = "accountClient", fallbackMethod = "updateAbusePointFallback")
    fun updateAbusePoint(userId: Long, abusePoint: Int) {
        val request = UpdateAbusePointInnerRequest(abusePoint = abusePoint)

        restClient.put()
            .uri("/internal/users/{userId}/abuse-point", userId)
            .body(request)
            .retrieve()
            .toBodilessEntity()
    }

    // ===== fallback methods =====
    @Suppress("unused")
    private fun existsByIdFallback(userId: Long, ex: Throwable): Boolean {
        returnStatus("accountClient", ex)
        throw ExceptionType.withType(MessageType.INVALID_CONNECTED_SERVICE)
    }

    @Suppress("unused")
    private fun findByIdFallback(userId: Long, ex: Throwable): ReadUserInnerResponse {
        returnStatus("accountClient", ex)
        throw ExceptionType.withType(MessageType.INVALID_CONNECTED_SERVICE)
    }

    @Suppress("unused")
    private fun findByIdInFallback(userIds: List<Long>, ex: Throwable): List<ReadUserInnerResponse> {
        returnStatus("accountClient", ex)
        throw ExceptionType.withType(MessageType.INVALID_CONNECTED_SERVICE)
    }

    @Suppress("unused")
    private fun updateBalanceFallback(userId: Long, balance: Int, ex: Throwable) {
        returnStatus("accountClient", ex)
        throw ExceptionType.withType(MessageType.INVALID_CONNECTED_SERVICE)
    }

    @Suppress("unused")
    private fun updateActivePointFallback(userId: Long, activePoint: Int, ex: Throwable) {
        returnStatus("accountClient", ex)
        throw ExceptionType.withType(MessageType.INVALID_CONNECTED_SERVICE)
    }

    @Suppress("unused")
    private fun updateAbusePointFallback(userId: Long, abusePoint: Int, ex: Throwable) {
        returnStatus("accountClient", ex)
        throw ExceptionType.withType(MessageType.INVALID_CONNECTED_SERVICE)
    }

    fun returnStatus(client: String, ex: Throwable) {
        val cb = circuitBreakerRegistry.circuitBreaker(client)
        val state = cb.state
        println("fallback ex=${ex::class.qualifiedName}:${ex.message}, state=$state")
    }
}