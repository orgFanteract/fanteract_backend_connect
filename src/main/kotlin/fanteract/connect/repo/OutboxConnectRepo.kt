package fanteract.connect.repo

import fanteract.connect.entity.OutboxConnect
import fanteract.connect.enumerate.OutboxStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface OutboxConnectRepo : JpaRepository<OutboxConnect, Long> {
    fun findTop500ByOutboxStatusOrderByCreatedAtAsc(status: OutboxStatus): List<OutboxConnect>

    fun findAllByOutboxStatusOrderByCreatedAtDesc(outboxStatus: OutboxStatus): List<OutboxConnect>

    @Modifying
    @Query(
        "update OutboxConnect o " +
            "set o.outboxStatus = :status " +
            "where o.outboxId in :ids",
    )
    fun bulkUpdateStatus(
        @Param("status") status: OutboxStatus,
        @Param("ids") ids: List<Long>,
    ): Int
}
