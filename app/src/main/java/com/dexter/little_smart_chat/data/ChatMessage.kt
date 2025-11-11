import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
sealed class ChatMessage(open val left: Boolean) : Parcelable {
    @Parcelize
    data class Text(var content: String, override val left: Boolean) : ChatMessage(left)
    @Parcelize
    data class Image(val url: String, override val left: Boolean) : ChatMessage(left)
    @Parcelize
    data class Video(val url: String, override val left: Boolean) : ChatMessage(left)
    @Parcelize
    data class Web(val url: String, override val left: Boolean) : ChatMessage(left)

    @Parcelize
    data class Markdown(var markdownContent: String, override val left: Boolean) : ChatMessage(left)
}