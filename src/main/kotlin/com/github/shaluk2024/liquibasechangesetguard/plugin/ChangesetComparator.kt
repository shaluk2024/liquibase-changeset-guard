package com.github.shaluk2024.liquibasechangesetguard.plugin

import com.intellij.psi.xml.XmlTag

/**
 * Defines how to compare a current tag with its committed version.
 */
interface ChangesetComparator {
    fun isModified(tag: XmlTag, committedFileContent: String): Boolean
}