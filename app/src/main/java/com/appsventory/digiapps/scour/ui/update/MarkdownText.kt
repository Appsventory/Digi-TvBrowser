package com.appsventory.digiapps.scour.ui.update

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appsventory.digiapps.scour.ui.theme.DigiColors

private sealed interface MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Bullet(val text: String) : MdBlock
    data class Quote(val text: String) : MdBlock
    data class Row(val cells: List<String>) : MdBlock
    data class Paragraph(val text: String) : MdBlock
    data object Gap : MdBlock
}

private val IMAGE = Regex("!\\[[^\\]]*]\\([^)]*\\)")
private val INLINE = Regex("\\*\\*(.+?)\\*\\*|`([^`]+)`|\\[([^\\]]+)]\\([^)]*\\)")
private val TABLE_RULE = Regex("^\\|?[\\s:|-]+\\|?$")
private const val HEADING_BASE_SP = 20
private const val HEADING_STEP_SP = 2

private fun parse(markdown: String): List<MdBlock> {
    val out = ArrayList<MdBlock>()
    for (raw in markdown.replace("\r", "").lines()) {
        val line = IMAGE.replace(raw, "").trim()
        when {
            line.isEmpty() -> if (out.lastOrNull() != MdBlock.Gap) out += MdBlock.Gap
            line.startsWith("<!--") || line.startsWith("<") -> Unit
            line.startsWith("#") -> {
                val level = line.takeWhile { it == '#' }.length.coerceAtMost(3)
                out += MdBlock.Heading(level, line.trimStart('#').trim())
            }
            line.startsWith("- ") || line.startsWith("* ") -> out += MdBlock.Bullet(line.drop(2))
            line.startsWith(">") -> out += MdBlock.Quote(line.trimStart('>').trim())
            line.startsWith("|") -> {
                if (!TABLE_RULE.matches(line)) {
                    out += MdBlock.Row(line.trim('|').split('|').map { it.trim() }.filter { it.isNotEmpty() })
                }
            }
            else -> out += MdBlock.Paragraph(line)
        }
    }
    return out
}

private fun inline(text: String): AnnotatedString = buildAnnotatedString {
    var last = 0
    for (m in INLINE.findAll(text)) {
        append(text.substring(last, m.range.first))
        val (bold, code, link) = m.destructured
        when {
            bold.isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(bold) }
            code.isNotEmpty() -> withStyle(
                SpanStyle(fontFamily = FontFamily.Monospace, background = DigiColors.BgSelected)
            ) { append(code) }
            else -> withStyle(SpanStyle(color = DigiColors.Accent)) { append(link) }
        }
        last = m.range.last + 1
    }
    append(text.substring(last))
}

/** Small GitHub-flavoured-markdown subset for release notes (headings, bullets, tables, bold, code). */
@Composable
fun MarkdownText(markdown: String, modifier: Modifier = Modifier) {
    val blocks = remember(markdown) { parse(markdown) }
    Column(modifier) {
        blocks.forEach { b ->
            when (b) {
                is MdBlock.Heading -> Text(
                    inline(b.text), color = DigiColors.TextPrimary,
                    fontSize = (HEADING_BASE_SP - (b.level - 1) * HEADING_STEP_SP).sp,
                    fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
                )
                is MdBlock.Bullet -> Text(
                    inline("•  " + b.text), color = DigiColors.TextSecondary,
                    fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                )
                is MdBlock.Quote -> Text(
                    inline(b.text), color = DigiColors.TextMuted, fontSize = 13.sp,
                    fontStyle = FontStyle.Italic, modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                )
                is MdBlock.Row -> Text(
                    inline(b.cells.joinToString("  —  ")), color = DigiColors.TextSecondary,
                    fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                )
                is MdBlock.Paragraph -> Text(
                    inline(b.text), color = DigiColors.TextSecondary, fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                MdBlock.Gap -> Spacer(Modifier.height(6.dp))
            }
        }
    }
}
