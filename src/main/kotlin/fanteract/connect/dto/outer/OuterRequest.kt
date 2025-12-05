package fanteract.connect.dto.outer

data class CreateChatroomOuterRequest(
    val title: String,
    val description: String?,
)

data class SendChatRequest(
    val content: String,
)

data class ReadChatContainingContentOuterRequest(
    val content: String,
)