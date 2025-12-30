package fanteract.connect.repo

import fanteract.connect.entity.Chat
import fanteract.connect.enumerate.RiskLevel
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ChatRepo : JpaRepository<Chat, Long> {
    @Query(
        """
        SELECT c FROM Chat c 
        WHERE c.userId = :userId
        AND c.chatroomId = :chatroomId
        AND c.riskLevel <> 'BLOCK'
    """,
    )
    fun findByUserIdAndChatroomId(
        @Param("userId") userId: Long,
        @Param("chatroomId") chatroomId: Long,
        pageable: Pageable,
    ): Page<Chat>

    @Query(
        """
        SELECT c FROM Chat c 
        WHERE c.userId = :userId
        AND c.chatroomId = :chatroomId
        AND c.content LIKE %:content%
        AND c.riskLevel <> 'BLOCK'
    """,
    )
    fun findByUserIdAndChatroomIdAndContentContaining(
        @Param("userId") userId: Long,
        @Param("chatroomId") chatroomId: Long,
        @Param("content") content: String,
        pageable: Pageable,
    ): Page<Chat>

    @Query(
        """
        SELECT COUNT(c)
        FROM Chat c
        WHERE c.userId = :userId
          AND c.status = 'ACTIVATED'
    """,
    )
    fun countByUserId(
        @Param("userId") userId: Long,
    ): Long

    @Query(
        """
    SELECT COUNT(c)
    FROM Chat c
    WHERE c.userId = :userId
      AND c.riskLevel = :riskLevel
      AND c.status = 'ACTIVATED'
""",
    )
    fun countByUserIdAndRiskLevel(
        @Param("userId") userId: Long,
        @Param("riskLevel") riskLevel: RiskLevel,
    ): Long

    @Query(
        """
        SELECT c
        FROM Chat c
        WHERE c.userId = :userId
          AND c.riskLevel = :riskLevel
          AND c.status = 'ACTIVATED'
    """,
    )
    fun findByUserIdAndRiskLevel(
        @Param("userId") userId: Long,
        @Param("riskLevel") riskLevel: RiskLevel,
        pageable: Pageable,
    ): Page<Chat>
}
