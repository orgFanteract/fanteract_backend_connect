package fanteract.connect.scheduler

import fanteract.connect.client.OutboxKafkaPublisher
import fanteract.connect.enumerate.OutboxStatus
import fanteract.connect.repo.OutboxConnectRepo
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import kotlin.random.Random

// 스케줄러 2
@Component
class OutboxPublishScheduler(
    private val outboxConnectRepo: OutboxConnectRepo,
    private val publisher: OutboxKafkaPublisher,
) {
    private val workerId = "outbox-worker-${Random.nextInt(100000)}"
    private val topic = "account.mypage.delta" // 원하는 토픽명

    @Scheduled(fixedDelay = 10000)
    @Transactional
    fun publishOutbox() {
        val targets = outboxConnectRepo.findTop500ByOutboxStatusOrderByCreatedAtAsc(OutboxStatus.NEW)

        if (targets.isEmpty()) {
            return
        }

        for (target in targets) {
            val updated = outboxConnectRepo.bulkUpdateStatus(OutboxStatus.PROCESSING, listOf(target.outboxId))

            if (updated == 0) {
                continue
            }

            try {
                publisher.publish(
                    topic = topic,
                    data = target.content,
                )

                target.outboxStatus = OutboxStatus.SENT
                outboxConnectRepo.save(target)
            } catch (ex: Exception) {
                // TODO : 재시도 로직 추가 필요
                target.outboxStatus = OutboxStatus.FAILED
                outboxConnectRepo.save(target)
            }
        }
    }
}
