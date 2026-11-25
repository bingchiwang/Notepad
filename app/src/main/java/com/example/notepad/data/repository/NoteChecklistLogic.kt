package com.example.notepad.data.repository

/**
 * 記事內嵌 Markdown 風格核取清單項目（`- [ ]` / `- [x]`）
 */
data class NoteChecklistItem(
    val lineIndex: Int,
    val isChecked: Boolean,
    val text: String
)

/**
 * 記事內嵌核取方塊（Checklist）與選取文字轉換純邏輯工具
 *
 * 支援解析與切換 `- [ ]`／`- [x]`（亦相容 `* [ ]` 與大寫 `[X]`）、
 * 於游標或選區快速插入核取項目、按 Enter 自動延續下一項、
 * 以及清理選取文字轉為待辦事項或時間規劃標題。
 */
object NoteChecklistLogic {

    private val CHECKLIST_LINE_REGEX = Regex("""^(\s*)([-*])\s+\[([ xX])](?:\s+(.*)|\s*)$""")
    private val MARKDOWN_PREFIX_REGEX = Regex("""^(?:\s*[-*+]\s+\[[ xX]]\s*|\s*[-*+]\s+|\s*#+\s*|\s*\d+\.\s+)""")

    /**
     * 解析記事內容中的所有核取清單項目
     */
    fun parseChecklistItems(content: String): List<NoteChecklistItem> {
        if (content.isEmpty()) return emptyList()
        val lines = content.split("\n")
        return lines.mapIndexedNotNull { index, rawLine ->
            val match = CHECKLIST_LINE_REGEX.matchEntire(rawLine) ?: return@mapIndexedNotNull null
            val checkedMarker = match.groupValues[3]
            val itemText = match.groupValues[4].trim()
            NoteChecklistItem(
                lineIndex = index,
                isChecked = checkedMarker.equals("x", ignoreCase = true),
                text = itemText
            )
        }
    }

    /**
     * 切換指定行號（0-based）之核取狀態（`- [ ]` ↔ `- [x]`），其餘內容保持不變
     */
    fun toggleChecklistLine(content: String, lineIndex: Int): String {
        val lines = content.split("\n").toMutableList()
        if (lineIndex !in lines.indices) return content

        val match = CHECKLIST_LINE_REGEX.matchEntire(lines[lineIndex]) ?: return content
        val indent = match.groupValues[1]
        val bullet = match.groupValues[2]
        val wasChecked = match.groupValues[3].equals("x", ignoreCase = true)
        val itemBody = match.groupValues[4]
        val nextMarker = if (wasChecked) " " else "x"

        lines[lineIndex] = if (itemBody.isNotEmpty()) {
            "$indent$bullet [$nextMarker] $itemBody"
        } else {
            "$indent$bullet [$nextMarker] "
        }
        return lines.joinToString("\n")
    }

    /**
     * 於目前游標或選取範圍快速插入／轉換 `- [ ] ` 核取項目
     * @return 更新後的 `(新內文, 新游標位置)`
     */
    fun insertChecklistItem(
        content: String,
        selectionStart: Int,
        selectionEnd: Int
    ): Pair<String, Int> {
        val safeStart = minOf(selectionStart, selectionEnd).coerceIn(0, content.length)
        val safeEnd = maxOf(selectionStart, selectionEnd).coerceIn(0, content.length)

        // 若有跨字元選取：將選區涵蓋的每一非空行轉成核取項目
        if (safeStart < safeEnd) {
            val blockStart = content.lastIndexOf('\n', safeStart - 1).let { if (it < 0) 0 else it + 1 }
            val blockEnd = content.indexOf('\n', safeEnd).let { if (it < 0) content.length else it }
            val selectedBlock = content.substring(blockStart, blockEnd)
            val lines = selectedBlock.split("\n")
            val allChecklist = lines.all { it.isBlank() || CHECKLIST_LINE_REGEX.matches(it) }
            val transformed = lines.joinToString("\n") { line ->
                when {
                    line.isBlank() -> line
                    allChecklist -> stripMarkdownPrefixes(line)
                    CHECKLIST_LINE_REGEX.matches(line) -> line
                    else -> "- [ ] ${stripMarkdownPrefixes(line)}"
                }
            }
            val updated = content.substring(0, blockStart) + transformed + content.substring(blockEnd)
            return updated to (blockStart + transformed.length)
        }

        // 無選取範圍：檢查游標所在行
        val lineStart = if (safeStart == 0) 0 else content.lastIndexOf('\n', safeStart - 1) + 1
        val lineEnd = content.indexOf('\n', safeStart).let { if (it < 0) content.length else it }
        val currentLine = content.substring(lineStart, lineEnd)

        return when {
            currentLine.isBlank() -> {
                val updated = content.substring(0, lineStart) + "- [ ] " + content.substring(lineEnd)
                updated to (lineStart + 6)
            }

            CHECKLIST_LINE_REGEX.matches(currentLine) -> {
                // 目前已在核取行內 -> 直接於本行尾換行並延續下一筆 `- [ ] `
                val prefixInsert = "\n- [ ] "
                val updated = content.substring(0, lineEnd) + prefixInsert + content.substring(lineEnd)
                updated to (lineEnd + prefixInsert.length)
            }

            else -> {
                val cleanBody = currentLine.trimStart()
                val leadingSpaces = currentLine.take(currentLine.length - cleanBody.length)
                val replacement = "$leadingSpaces- [ ] $cleanBody"
                val updated = content.substring(0, lineStart) + replacement + content.substring(lineEnd)
                updated to (lineStart + replacement.length)
            }
        }
    }

    /**
     * 當使用者於記事編輯區按下換行（輸入單一 `\n`）時，自動延續 `- [ ] ` 或結束空白清單行
     * @return 若觸發清單換行輔助則回傳 `(調整後內文, 調整後游標)`，否則回傳 `null`
     */
    fun handleEnterAutoContinue(
        oldContent: String,
        newContent: String,
        newCursor: Int
    ): Pair<String, Int>? {
        if (newContent.length != oldContent.length + 1) return null
        val insertedIndex = (newCursor - 1).coerceIn(0, newContent.lastIndex.coerceAtLeast(0))
        if (newContent.getOrNull(insertedIndex) != '\n') return null

        val prevLineStart = if (insertedIndex == 0) 0 else newContent.lastIndexOf('\n', insertedIndex - 1) + 1
        val prevLine = newContent.substring(prevLineStart, insertedIndex)
        val match = CHECKLIST_LINE_REGEX.matchEntire(prevLine) ?: return null

        val indent = match.groupValues[1]
        val bullet = match.groupValues[2]
        val itemBody = match.groupValues[4].trim()

        return if (itemBody.isEmpty()) {
            // 空白 `- [ ] ` 行按 Enter -> 移除空核取標記並結束清單輸入
            val updated = newContent.substring(0, prevLineStart) + newContent.substring(insertedIndex + 1)
            updated to prevLineStart
        } else {
            val nextPrefix = "$indent$bullet [ ] "
            val updated = newContent.substring(0, insertedIndex + 1) +
                nextPrefix +
                newContent.substring(insertedIndex + 1)
            updated to (insertedIndex + 1 + nextPrefix.length)
        }
    }

    /**
     * 從選取文字或目前游標所在行萃取適合轉為待辦或行程之乾淨標題
     */
    fun extractActionableTitle(
        content: String,
        selectionStart: Int,
        selectionEnd: Int
    ): String {
        val safeStart = minOf(selectionStart, selectionEnd).coerceIn(0, content.length)
        val safeEnd = maxOf(selectionStart, selectionEnd).coerceIn(0, content.length)
        val rawSlice = if (safeStart < safeEnd) {
            content.substring(safeStart, safeEnd)
        } else {
            val lineStart = if (safeStart == 0) 0 else content.lastIndexOf('\n', safeStart - 1) + 1
            val lineEnd = content.indexOf('\n', safeStart).let { if (it < 0) content.length else it }
            content.substring(lineStart, lineEnd)
        }

        return rawSlice
            .lineSequence()
            .map { stripMarkdownPrefixes(it).trim() }
            .filter { it.isNotEmpty() }
            .joinToString(" ")
            .trim()
    }

    /**
     * 移除單行前端的 Markdown 核取標記、項目符號或標題符號
     */
    fun stripMarkdownPrefixes(line: String): String {
        return line.replaceFirst(MARKDOWN_PREFIX_REGEX, "").trimStart()
    }
}
