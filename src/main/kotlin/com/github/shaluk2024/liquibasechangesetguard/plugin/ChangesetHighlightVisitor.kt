package com.github.shaluk2024.liquibasechangesetguard.plugin

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.codeInsight.daemon.impl.HighlightVisitor
import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag

class ChangesetHighlightVisitor : HighlightVisitor {
    private var myHolder: HighlightInfoHolder? = null
    // Inversion of Control: We use a specific comparator strategy
    private val comparator: ChangesetComparator = LiquibaseRegexComparator()

    override fun suitableForFile(file: PsiFile): Boolean =
        file is XmlFile && file.rootTag?.name == "databaseChangeLog"

    override fun visit(element: PsiElement) {
        if (element !is XmlTag || element.name != "changeSet") return

        val vFile = element.containingFile.virtualFile ?: return
        val committedContent = VcsService.getCommittedContent(element.project, vFile) ?: return

        if (comparator.isModified(element, committedContent)) {
            createError(element)
        }
    }

    private fun createError(tag: XmlTag) {
        val id = tag.getAttributeValue("id")
        val info = HighlightInfo.newHighlightInfo(HighlightInfoType.ERROR)
            .range(tag)
            .descriptionAndTooltip("Changeset '$id' is already committed. Modifying it is prohibited.")
            .create()

        info?.let { myHolder?.add(it) }
    }

    override fun analyze(file: PsiFile, updateWholeFile: Boolean, holder: HighlightInfoHolder, action: Runnable): Boolean {
        myHolder = holder
        try {
            action.run()
        } finally {
            myHolder = null
        }
        return true
    }

    override fun clone(): HighlightVisitor = ChangesetHighlightVisitor()
}
