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
import io.github.resilience4j.retry.annotation.Retry
import io.github.resilience4j.timelimiter.annotation.TimeLimiter
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import kotlin.collections.toTypedArray
import kotlin.jvm.java

@Component
class UserClient(
    @Value($$"${client.account-service.url}") userServiceUrl: String,
    private val restClient: RestClient = RestClient.builder()
        .baseUrl(userServiceUrl)
        .build(),
    private val circuitBreakerRegistry: CircuitBreakerRegistry
) {
    @CircuitBreaker(name = "user-service", fallbackMethod = "getExistsByIdFallback")
    @Retry(name = "user-service")
    fun existsById(userId: Long): Boolean {
        val response = restClient.get()
            .uri("/internal/users/{userId}/exists", userId)
            .retrieve()
            .body(ReadUserExistsInnerResponse::class.java)

        return response?.exists ?: false
    }

    fun getExistsByIdFallback(userId: Long, ex: Throwable): Boolean {
        val cb = circuitBreakerRegistry.circuitBreaker("user-service")
        val state = cb.state

        println("existsByIdFallback userId=$userId, ex=$ex, state=$state")
        throw ExceptionType.withType(MessageType.INVALID_ACTION)
    }
    
    @Retry(name = "user-service")
    fun findById(userId: Long): ReadUserInnerResponse {
        val response = restClient.get()
            .uri("/internal/users/{userId}", userId)
            .retrieve()
            .body(ReadUserInnerResponse::class.java)

        return requireNotNull(response) { "User not found for id=$userId" }
    }

    @CircuitBreaker(name = "user-service", fallbackMethod = "getUpdateBalanceFallback")
    @Retry(name = "user-service")
    fun updateBalance(userId: Long, balance: Int) {
        val request = UpdateBalanceInnerRequest(balance = balance)

        restClient.put()
            .uri("/internal/users/{userId}/balance", userId)
            .body(request)
            .retrieve()
            .toBodilessEntity()
    }

    fun getUpdateBalanceFallback(userId: Long, balance: Int, ex: Throwable) {
        val cb = circuitBreakerRegistry.circuitBreaker("user-service")
        val state = cb.state

        println("existsByIdFallback userId=$userId, ex=$ex, state=$state")
        throw ExceptionType.withType(MessageType.INVALID_ACTION)
    }

    @CircuitBreaker(name = "user-service", fallbackMethod = "getUpdateActivePointFallback")
    @Retry(name = "user-service")
    fun updateActivePoint(userId: Long, activePoint: Int) {
        val request = UpdateActivePointInnerRequest(activePoint = activePoint)

        restClient.put()
            .uri("/internal/users/{userId}/active-point", userId)
            .body(request)
            .retrieve()
            .toBodilessEntity()
    }

    fun getUpdateActivePointFallback(userId: Long, balance: Int, ex: Throwable) {
        val cb = circuitBreakerRegistry.circuitBreaker("user-service")
        val state = cb.state

        println("existsByIdFallback userId=$userId, ex=$ex, state=$state")
        throw ExceptionType.withType(MessageType.INVALID_ACTION)
    }

    @Retry(name = "user-service")
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

    @Retry(name = "user-service")
    fun updateAbusePoint(userId: Long, abusePoint: Int) {
        val request = UpdateAbusePointInnerRequest(abusePoint = abusePoint)

        restClient.put()
            .uri("/internal/users/{userId}/abuse-point", userId)
            .body(request)
            .retrieve()
            .toBodilessEntity()
    }
}