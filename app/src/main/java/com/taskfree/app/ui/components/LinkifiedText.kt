import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink

// recognise   https://x, http://x,  www.x
private val urlRegex =
    "(https?://\\S+)|(www\\.\\S+)".toRegex(RegexOption.IGNORE_CASE)

fun linkified(raw: String, linkStyle: SpanStyle): AnnotatedString =
    buildAnnotatedString {
        var last = 0
        for (m in urlRegex.findAll(raw)) {
            append(raw.substring(last, m.range.first))
            last = m.range.last + 1

            val shown = m.value
            val link  = if (shown.startsWith("www.", true))
                "https://$shown"          // add scheme so the OS can open it
            else shown

            withLink(LinkAnnotation.Url(link, TextLinkStyles(linkStyle))) {
                append(shown)
            }
        }
        append(raw.substring(last))
    }

@Composable
fun AutoLinkedText(
    raw: String,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = LocalContentColor.current,
    linkStyle: SpanStyle = SpanStyle(
        color = MaterialTheme.colorScheme.primary,
        textDecoration = TextDecoration.Underline
    )
) {
    val text = remember(raw) { linkified(raw, linkStyle) }
    Text(text, style = style, color = color)
}
