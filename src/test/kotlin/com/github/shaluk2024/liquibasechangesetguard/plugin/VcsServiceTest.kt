package com.github.shaluk2024.liquibasechangesetguard.plugin

import com.intellij.ide.highlighter.XmlFileType
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Unit tests for [VcsService].
 *
 * Verifies that VcsService correctly retrieves committed file content
 * from IntelliJ's internal VCS change list manager.
 *
 * Note: In the test environment there is no real Git repository,
 * so ChangeListManager returns null for all files — this is the
 * expected behaviour for untracked/new files.
 */
class VcsServiceTest : BasePlatformTestCase() {

    /**
     * Happy path: file exists in the test fixture but has no VCS history.
     * ChangeListManager.getChange() returns null — VcsService should return null safely.
     */
    fun testReturnsNullForFileWithNoVcsHistory() {
        val psiFile = myFixture.configureByText(XmlFileType.INSTANCE, """
            <databaseChangeLog>
                <changeSet id="001" author="shalu">
                    <createTable tableName="users"/>
                </changeSet>
            </databaseChangeLog>
        """.trimIndent())

        val result = VcsService.getCommittedContent(project, psiFile.virtualFile)
        assertNull("File with no VCS history should return null", result)
    }

    /**
     * Edge case: file is new and untracked by Git.
     * ChangeListManager.getChange() returns null for untracked files.
     * VcsService should return null — no warning should be shown for new files.
     */
    fun testReturnsNullForUntrackedFile() {
        val psiFile = myFixture.configureByText(XmlFileType.INSTANCE, """
            <databaseChangeLog>
                <changeSet id="001" author="shalu">
                    <createTable tableName="users"/>
                </changeSet>
            </databaseChangeLog>
        """.trimIndent())

        // Untracked files have no change entry in ChangeListManager
        val result = VcsService.getCommittedContent(project, psiFile.virtualFile)
        assertNull("Untracked file should return null", result)
    }
}