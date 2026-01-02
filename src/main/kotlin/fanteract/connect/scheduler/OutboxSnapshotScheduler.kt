package fanteract.connect.scheduler

import com.fasterxml.jackson.databind.ObjectMapper
import fanteract.connect.dto.client.MyPageDeltaEvent
import fanteract.connect.entity.OutboxConnect
import fanteract.connect.enumerate.OutboxStatus
import fanteract.connect.repo.OutboxConnectRepo
import fanteract.connect.util.DeltaInMemoryStorage
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class OutboxSnapshotScheduler(
    private val deltaStorage: DeltaInMemoryStorage,
    private val outboxConnectRepo: OutboxConnectRepo,
    private val objectMapper: ObjectMapper,
) {
    @Scheduled(fixedDelay = 10000)
    fun flushToOutbox() {
        val snapshot = deltaStorage.snapshot()

        snapshot.forEach { (userId, fields) ->
            val payload =
                objectMapper.writeValueAsString(
                    MyPageDeltaEvent(userId = userId, deltas = fields),
                )

            // 아웃 박스 저장
            outboxConnectRepo.save(
                OutboxConnect(
                    content = payload,
                    topic = "account.mypagtarget.delta",
                    outboxStatus = OutboxStatus.NEW,
                ),
            )

            // 저장 성공 시 차감
            fields.forEach { (field, delta) ->
                deltaStorage.subtract(userId, field, delta)
            }
        }
    }
}
