package com.github.shaluk2024.liquibasechangesetguard.plugin

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vcs.changes.ChangeListManager

object VcsService {
    fun getCommittedContent(project: Project, file: VirtualFile): String? {
        val change = ChangeListManager.getInstance(project).getChange(file) ?: return null
        // Skip new files (no beforeRevision)
        return change.beforeRevision?.content
    }
}