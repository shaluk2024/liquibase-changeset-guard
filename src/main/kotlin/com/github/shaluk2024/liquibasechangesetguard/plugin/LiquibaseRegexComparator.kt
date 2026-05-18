package com.github.shaluk2024.liquibasechangesetguard.plugin

import com.intellij.psi.xml.XmlTag
import java.util.regex.Pattern

class LiquibaseRegexComparator : ChangesetComparator {
    override fun isModified(tag: XmlTag, committedFileContent: String): Boolean {
        val id = tag.getAttributeValue("id") ?: return false
        val author = tag.getAttributeValue("author") ?: ""
        val originalContent = extractOriginalContent(committedFileContent, id, author) ?: return false
        return normalize(tag.value.text) != normalize(originalContent)
    }

    private fun extractOriginalContent(fullText: String, id: String, author: String): String? {
        val escapedId = Pattern.quote(id)
        val escapedAuthor = Pattern.quote(author)
        val regex = Regex(
            // ✅ quotes added around attribute values
            """<changeSet[^>]*id="$escapedId"[^>]*author="$escapedAuthor"[^>]*>(.*?)</changeSet>""",
            RegexOption.DOT_MATCHES_ALL
        )
        return regex.find(fullText)?.groupValues?.get(1)
    }

    //private fun normalize(text: String) = text.trim().replace("\\s+".toRegex(), " ")

    private fun normalize(text: String) = text
        .trim()
        .replace("\\s+".toRegex(), " ")         // collapse all whitespace to single space
        .replace("> <".toRegex(), "><")          // remove spaces between closing and opening tags
        .replace("> <".toRegex(), "><")          // handle self-closing tags too
        .replace("/> <".toRegex(), "/><")
        .replace("> </".toRegex(), "></")
}