package com.example.data.epub

import android.content.Context
import android.net.Uri
import com.example.data.model.Chapter
import com.example.data.model.Novel
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class EpubManager(private val context: Context) {

    fun parseEpub(uri: Uri): Pair<Novel, List<Chapter>> {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Cannot open input stream for $uri")

        val files = mutableMapOf<String, ByteArray>()
        ZipInputStream(inputStream).use { zip ->
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    files[entry.name] = zip.readBytes()
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        // 1. Locate container.xml
        val containerBytes = files["META-INF/container.xml"]
            ?: throw IOException("Invalid EPUB: META-INF/container.xml not found")
        val containerXml = String(containerBytes, StandardCharsets.UTF_8)
        val containerDoc = Jsoup.parse(containerXml)
        val rootPath = containerDoc.select("rootfile").firstOrNull()?.attr("full-path")?.ifBlank { "OEBPS/content.opf" } ?: "OEBPS/content.opf"

        val opfBytes = files[rootPath] ?: throw IOException("Invalid EPUB: $rootPath not found")
        val opfXml = String(opfBytes, StandardCharsets.UTF_8)
        val opfDoc = Jsoup.parse(opfXml)

        val title = opfDoc.select("dc\\:title, title").firstOrNull()?.text()?.ifBlank { "Imported EPUB" } ?: "Imported EPUB"
        val author = opfDoc.select("dc\\:creator, creator").firstOrNull()?.text()?.ifBlank { "Unknown Author" } ?: "Unknown Author"
        val description = opfDoc.select("dc\\:description, description").firstOrNull()?.text()?.ifBlank { "Locally imported EPUB document." } ?: "Locally imported EPUB document."

        val baseDir = if (rootPath.contains("/")) rootPath.substringBeforeLast("/") + "/" else ""

        val manifest = mutableMapOf<String, String>()
        val manifestItems = opfDoc.select("manifest item")
        for (i in 0 until manifestItems.size) {
            val item: Element = manifestItems[i]
            manifest[item.attr("id")] = item.attr("href")
        }

        val spineElements = opfDoc.select("spine itemref")
        val spineItemRefs = mutableListOf<String>()
        for (i in 0 until spineElements.size) {
            spineItemRefs.add(spineElements[i].attr("idref"))
        }

        val novelId = "epub_${UUID.randomUUID().toString().take(8)}"
        val chapters = mutableListOf<Chapter>()
        var chapterIndex = 1f

        for (idref in spineItemRefs) {
            val href = manifest[idref] ?: continue
            val fullFilePath = baseDir + href
            val chapterBytes = files[fullFilePath] ?: files[href] ?: continue
            val rawHtml = String(chapterBytes, StandardCharsets.UTF_8)
            val chapterDoc = Jsoup.parse(rawHtml)

            val chTitle = chapterDoc.select("h1, h2, h3, title").firstOrNull()?.text()?.trim()
                ?.ifBlank { "Section ${chapterIndex.toInt()}" } ?: "Section ${chapterIndex.toInt()}"

            val pElements = chapterDoc.select("p")
            val paragraphs = mutableListOf<String>()
            for (i in 0 until pElements.size) {
                val pText = pElements[i].text().trim()
                if (pText.isNotBlank()) {
                    paragraphs.add(pText)
                }
            }

            val cleanContent = if (paragraphs.isNotEmpty()) {
                paragraphs.joinToString("\n\n")
            } else {
                chapterDoc.body()?.text()?.trim() ?: ""
            }

            if (cleanContent.isNotBlank()) {
                val words = cleanContent.split("\\s+".toRegex()).size
                chapters.add(
                    Chapter(
                        id = "${novelId}_ch_${chapterIndex.toInt()}",
                        novelId = novelId,
                        chapterNumber = chapterIndex,
                        title = chTitle,
                        url = "local://$href",
                        content = cleanContent,
                        wordCount = words,
                        isDownloaded = true
                    )
                )
                chapterIndex += 1f
            }
        }

        val totalWords = chapters.sumOf { it.wordCount.toLong() }
        val novel = Novel(
            id = novelId,
            title = title,
            author = author,
            coverUrl = "local_epub_cover",
            description = description,
            genres = "Local EPUB",
            status = "Completed",
            sourceId = "local_epub",
            sourceUrl = uri.toString(),
            inLibrary = true,
            totalChapters = chapters.size,
            totalWords = totalWords
        )

        return Pair(novel, chapters)
    }

    fun parseTxtFile(uri: Uri, fileName: String): Pair<Novel, List<Chapter>> {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Cannot open $uri")
        val text = inputStream.bufferedReader().use { it.readText() }

        val novelTitle = fileName.removeSuffix(".txt").replace("_", " ")
        val novelId = "txt_${UUID.randomUUID().toString().take(8)}"

        val regex = Regex("(?m)^(?:Chapter|Section|CHAPTER|第[0-9一二三四五六七八九十百千]+章)\\s*.*$")
        val matches = regex.findAll(text).toList()

        val chapters = mutableListOf<Chapter>()
        if (matches.size > 1) {
            for (i in matches.indices) {
                val match = matches[i]
                val chTitle = match.value.trim()
                val start = match.range.last + 1
                val end = if (i + 1 < matches.size) matches[i + 1].range.first else text.length
                val content = text.substring(start, end).trim()
                val words = content.split("\\s+".toRegex()).size
                chapters.add(
                    Chapter(
                        id = "${novelId}_ch_${i + 1}",
                        novelId = novelId,
                        chapterNumber = (i + 1).toFloat(),
                        title = chTitle,
                        url = "local://txt/${i + 1}",
                        content = content,
                        wordCount = words,
                        isDownloaded = true
                    )
                )
            }
        } else {
            val chunks = text.chunked(6000)
            chunks.forEachIndexed { i, chunk ->
                val words = chunk.split("\\s+".toRegex()).size
                chapters.add(
                    Chapter(
                        id = "${novelId}_ch_${i + 1}",
                        novelId = novelId,
                        chapterNumber = (i + 1).toFloat(),
                        title = "Part ${i + 1}",
                        url = "local://txt/${i + 1}",
                        content = chunk.trim(),
                        wordCount = words,
                        isDownloaded = true
                    )
                )
            }
        }

        val totalWords = chapters.sumOf { it.wordCount.toLong() }
        val novel = Novel(
            id = novelId,
            title = novelTitle,
            author = "Unknown Author",
            coverUrl = "local_txt_cover",
            description = "Imported plain text novel ($fileName)",
            genres = "Local File",
            status = "Completed",
            sourceId = "local_txt",
            sourceUrl = uri.toString(),
            inLibrary = true,
            totalChapters = chapters.size,
            totalWords = totalWords
        )

        return Pair(novel, chapters)
    }

    fun exportToEpub(novel: Novel, chapters: List<Chapter>, outputFile: File) {
        ZipOutputStream(FileOutputStream(outputFile)).use { zip ->
            val mimeEntry = ZipEntry("mimetype")
            mimeEntry.method = ZipEntry.STORED
            val mimeBytes = "application/epub+zip".toByteArray(StandardCharsets.US_ASCII)
            mimeEntry.size = mimeBytes.size.toLong()
            mimeEntry.compressedSize = mimeBytes.size.toLong()
            val crc = java.util.zip.CRC32()
            crc.update(mimeBytes)
            mimeEntry.crc = crc.value
            zip.putNextEntry(mimeEntry)
            zip.write(mimeBytes)
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("META-INF/container.xml"))
            val containerXml = """<?xml version="1.0" encoding="UTF-8"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
    <rootfiles>
        <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
    </rootfiles>
</container>""".trimIndent()
            zip.write(containerXml.toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()

            val manifestItems = StringBuilder()
            val spineItems = StringBuilder()

            chapters.forEachIndexed { index, ch ->
                val chFile = "chapter_${index + 1}.xhtml"
                manifestItems.append("""        <item id="ch${index + 1}" href="$chFile" media-type="application/xhtml+xml"/>${"\n"}""")
                spineItems.append("""        <itemref idref="ch${index + 1}"/>${"\n"}""")

                zip.putNextEntry(ZipEntry("OEBPS/$chFile"))
                val paras = ch.content.split("\n\n").joinToString("\n") { p -> "<p>${p.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")}</p>" }
                val xhtml = """<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <title>${ch.title}</title>
    <style>body { font-family: serif; margin: 5%; line-height: 1.6; } p { text-indent: 1.5em; margin: 0.5em 0; }</style>
</head>
<body>
    <h2>${ch.title}</h2>
    $paras
</body>
</html>""".trimIndent()
                zip.write(xhtml.toByteArray(StandardCharsets.UTF_8))
                zip.closeEntry()
            }

            zip.putNextEntry(ZipEntry("OEBPS/content.opf"))
            val opf = """<?xml version="1.0" encoding="UTF-8"?>
<package xmlns="http://www.idpf.org/2007/opf" unique-identifier="BookId" version="3.0">
    <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
        <dc:title>${novel.title}</dc:title>
        <dc:creator>${novel.author}</dc:creator>
        <dc:language>en</dc:language>
        <dc:identifier id="BookId">urn:uuid:${novel.id}</dc:identifier>
        <dc:description>${novel.description}</dc:description>
    </metadata>
    <manifest>
$manifestItems    </manifest>
    <spine>
$spineItems    </spine>
</package>""".trimIndent()
            zip.write(opf.toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()
        }
    }

    fun exportBackupJson(novels: List<Novel>, chapters: List<Chapter>): String {
        val root = JSONObject()
        val novelsArray = JSONArray()
        for (n in novels) {
            val obj = JSONObject().apply {
                put("id", n.id)
                put("title", n.title)
                put("author", n.author)
                put("coverUrl", n.coverUrl)
                put("description", n.description)
                put("genres", n.genres)
                put("status", n.status)
                put("sourceId", n.sourceId)
                put("sourceUrl", n.sourceUrl)
                put("totalChapters", n.totalChapters)
            }
            novelsArray.put(obj)
        }
        root.put("novels", novelsArray)

        val chArray = JSONArray()
        for (c in chapters) {
            val obj = JSONObject().apply {
                put("id", c.id)
                put("novelId", c.novelId)
                put("chapterNumber", c.chapterNumber)
                put("title", c.title)
                put("url", c.url)
                put("isRead", c.isRead)
                put("isBookmarked", c.isBookmarked)
            }
            chArray.put(obj)
        }
        root.put("chapters", chArray)
        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())
        return root.toString(2)
    }
}
