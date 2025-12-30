package fanteract.connect.repo

import fanteract.connect.entity.Chatroom
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ChatroomRepo : JpaRepository<Chatroom, Long> {
    @Query(
        """
        SELECT c
        FROM Chatroom c
        WHERE c.userId = :userId
          AND c.status = 'ACTIVATED'
    """,
    )
    fun findByUserId(
        @Param("userId") userId: Long,
    ): List<Chatroom>

    @Query(
        """
        SELECT c
        FROM Chatroom c
        WHERE c.userId = :userId
          AND c.title LIKE %:title%
          AND c.status = 'ACTIVATED'
    """,
    )
    fun findByUserIdAndTitleContaining(
        @Param("userId") userId: Long,
        @Param("title") title: String,
    ): List<Chatroom>

    @Query(
        """
        SELECT COUNT(c)
        FROM Chatroom c
        WHERE c.userId = :userId
          AND c.status = 'ACTIVATED'
    """,
    )
    fun countByUserId(
        @Param("userId") userId: Long,
    ): Long

    @Modifying
    @Query(
        "update Chatroom c " +
            "set c.chatCount = c.chatCount + :delta " +
            "where c.id = :chatroomId",
    )
    fun incrementChatCount(
        chatroomId: Long,
        delta: Long,
    )
}
