package fanteract.connect.util

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class ChatCountAccumulator {
    private val pendingCountMap: MutableMap<Long, Long> = ConcurrentHashMap()

    fun increase(chatroomId: Long){
        pendingCountMap.merge(chatroomId, 1L) { old, one -> old + one }
    }

    fun drain(): Map<Long, Long> {
        val snapshot = HashMap(pendingCountMap)
        pendingCountMap.clear()
        return snapshot
    }
}