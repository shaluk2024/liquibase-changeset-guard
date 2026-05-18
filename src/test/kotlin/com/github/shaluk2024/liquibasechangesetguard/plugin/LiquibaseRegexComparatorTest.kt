package com.github.shaluk2024.liquibasechangesetguard.plugin

import com.intellij.ide.highlighter.XmlFileType
import com.intellij.psi.xml.XmlFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Unit tests for [LiquibaseRegexComparator].
 *
 * Verifies that the comparator correctly identifies whether a Liquibase
 * changeset has been modified compared to its committed version.
 *
 * Test environment: IntelliJ Platform test framework (BasePlatformTestCase)
 * No real Git or database required — all comparisons are done on in-memory PSI.
 */
class LiquibaseRegexComparatorTest : BasePlatformTestCase() {

    private lateinit var comparator: LiquibaseRegexComparator

    override fun setUp() {
        super.setUp()
        // Create a fresh comparator instance before each test
        comparator = LiquibaseRegexComparator()
    }

    /**
     * Happy path: changeset content in the editor matches the committed version.
     * No warning should be shown.
     */
    fun testUnmodifiedChangesetIsNotFlagged() {
        val content = """
            <databaseChangeLog>
                <changeSet id="001" author="shalu">
                    <createTable tableName="users">
                        <column name="id" type="BIGINT"/>
                    </createTable>
                </changeSet>
            </databaseChangeLog>
        """.trimIndent()

        val tag = getChangesetTag(content, 0)
        assertFalse("Unmodified changeset should not be flagged",
            comparator.isModified(tag, content))
    }

    /**
     * Core use case: developer changes column type inside a committed changeset.
     * Warning should be shown.
     */
    fun testModifiedChangesetIsFlagged() {
        val committed = """
            <databaseChangeLog>
                <changeSet id="001" author="shalu">
                    <createTable tableName="users">
                        <column name="id" type="BIGINT"/>
                    </createTable>
                </changeSet>
            </databaseChangeLog>
        """.trimIndent()

        // Column type changed from BIGINT to VARCHAR
        val current = """
            <databaseChangeLog>
                <changeSet id="001" author="shalu">
                    <createTable tableName="users">
                        <column name="id" type="VARCHAR"/>
                    </createTable>
                </changeSet>
            </databaseChangeLog>
        """.trimIndent()

        val tag = getChangesetTag(current, 0)
        assertTrue("Modified changeset should be flagged",
            comparator.isModified(tag, committed))
    }

    /**
     * Edge case: developer adds a brand new changeset to an existing file.
     * The new changeset (002) has no committed version — should NOT be flagged.
     */
    fun testNewChangesetNotInCommittedIsNotFlagged() {
        val committed = """
            <databaseChangeLog>
                <changeSet id="001" author="shalu">
                    <createTable tableName="users"/>
                </changeSet>
            </databaseChangeLog>
        """.trimIndent()

        // 002 is newly added — not present in committed content
        val current = """
            <databaseChangeLog>
                <changeSet id="001" author="shalu">
                    <createTable tableName="users"/>
                </changeSet>
                <changeSet id="002" author="shalu">
                    <createTable tableName="orders"/>
                </changeSet>
            </databaseChangeLog>
        """.trimIndent()

        // Only check the new changeset (index 1)
        val newTag = getChangesetTag(current, 1)
        assertFalse("New changeset not in committed content should not be flagged",
            comparator.isModified(newTag, committed))
    }

    /**
     * Edge case: developer reformats the XML (indentation, newlines) without
     * changing any actual content. Should NOT be flagged — whitespace is normalized.
     */
    fun testWhitespaceDifferencesAreIgnored() {
        // Committed version has inline elements
        val committed = """
            <databaseChangeLog>
                <changeSet id="001" author="shalu">
                    <createTable tableName="users"><column name="id" type="BIGINT"/></createTable>
                </changeSet>
            </databaseChangeLog>
        """.trimIndent()

        // Current version has expanded/indented elements — same logical content
        val current = """
            <databaseChangeLog>
                <changeSet id="001" author="shalu">
                    <createTable tableName="users">
                        <column name="id" type="BIGINT"/>
                    </createTable>
                </changeSet>
            </databaseChangeLog>
        """.trimIndent()

        val tag = getChangesetTag(current, 0)

        println(">>> tag.value.text: '${tag.value.text}'")


        assertFalse("Whitespace-only differences should not be flagged",
            comparator.isModified(tag, committed))
    }

    /**
     * Edge case: file has multiple changesets, only one is modified.
     * Verifies that the comparator correctly isolates which changeset was changed
     * and does not produce false positives on unmodified ones.
     */
    fun testOnlyModifiedChangesetIsFlaggedAmongMultiple() {
        val committed = """
            <databaseChangeLog>
                <changeSet id="001" author="shalu">
                    <createTable tableName="users"/>
                </changeSet>
                <changeSet id="002" author="shalu">
                    <createTable tableName="orders"/>
                </changeSet>
            </databaseChangeLog>
        """.trimIndent()

        // Only changeset 002 is modified — tableName changed
        val current = """
            <databaseChangeLog>
                <changeSet id="001" author="shalu">
                    <createTable tableName="users"/>
                </changeSet>
                <changeSet id="002" author="shalu">
                    <createTable tableName="orders_modified"/>
                </changeSet>
            </databaseChangeLog>
        """.trimIndent()

        val tags = getChangesetTags(current)
        assertFalse("Changeset 001 is unmodified — should not be flagged",
            comparator.isModified(tags[0], committed))
        assertTrue("Changeset 002 is modified — should be flagged",
            comparator.isModified(tags[1], committed))
    }

    /**
     * Edge case: changeset tag is missing the id attribute entirely.
     * Comparator should return false safely without throwing an exception.
     */
    fun testChangesetWithMissingIdIsNotFlagged() {
        val committed = """
            <databaseChangeLog>
                <changeSet id="001" author="shalu">
                    <createTable tableName="users"/>
                </changeSet>
            </databaseChangeLog>
        """.trimIndent()

        // id attribute is missing — comparator should handle gracefully
        val current = """
            <databaseChangeLog>
                <changeSet author="shalu">
                    <createTable tableName="users"/>
                </changeSet>
            </databaseChangeLog>
        """.trimIndent()

        val tag = getChangesetTag(current, 0)
        assertFalse("Changeset with missing id should not be flagged",
            comparator.isModified(tag, committed))
    }

    /**
     * Edge case: two changesets have the same id but different authors.
     * They are treated as different changesets in Liquibase — should NOT be flagged.
     */
    fun testChangesetWithSameIdDifferentAuthorIsNotFlagged() {
        val committed = """
            <databaseChangeLog>
                <changeSet id="001" author="shalu">
                    <createTable tableName="users"/>
                </changeSet>
            </databaseChangeLog>
        """.trimIndent()

        // Same id, different author — different changeset identity
        val current = """
            <databaseChangeLog>
                <changeSet id="001" author="gemini">
                    <createTable tableName="users"/>
                </changeSet>
            </databaseChangeLog>
        """.trimIndent()

        val tag = getChangesetTag(current, 0)
        assertFalse("Changeset with same id but different author should not be flagged",
            comparator.isModified(tag, committed))
    }

    /**
     * Edge case: changeset id contains special characters like hyphens and dots
     * (common in date-based ids like "2026-04-19-create-users").
     * Regex must escape these correctly — should still detect modifications.
     */
    fun testChangesetWithSpecialCharactersInIdIsFlagged() {
        val committed = """
            <databaseChangeLog>
                <changeSet id="2026-04-19-create-users-table" author="shalu">
                    <createTable tableName="users">
                        <column name="id" type="BIGINT"/>
                    </createTable>
                </changeSet>
            </databaseChangeLog>
        """.trimIndent()

        // Column type changed
        val current = """
            <databaseChangeLog>
                <changeSet id="2026-04-19-create-users-table" author="shalu">
                    <createTable tableName="users">
                        <column name="id" type="VARCHAR"/>
                    </createTable>
                </changeSet>
            </databaseChangeLog>
        """.trimIndent()

        val tag = getChangesetTag(current, 0)
        assertTrue("Changeset with special characters in id should still be flagged when modified",
            comparator.isModified(tag, committed))
    }

    /**
     * Edge case: committed content is empty (e.g. VCS returned blank string).
     * Should not flag anything — nothing to compare against.
     */
    fun testEmptyCommittedContentDoesNotFlag() {
        val current = """
            <databaseChangeLog>
                <changeSet id="001" author="shalu">
                    <createTable tableName="users"/>
                </changeSet>
            </databaseChangeLog>
        """.trimIndent()

        val tag = getChangesetTag(current, 0)
        assertFalse("Empty committed content should not flag any changeset",
            comparator.isModified(tag, ""))
    }

    /**
     * Edge case: only the precondition block inside the changeset was modified.
     * This is still a meaningful change — Liquibase behaviour changes — should be flagged.
     */
    fun testChangesetWithModifiedPreconditionIsFlagged() {
        val committed = """
            <databaseChangeLog>
                <changeSet id="001" author="shalu">
                    <preConditions onFail="MARK_RAN">
                        <sqlCheck expectedResult="0">SELECT COUNT(*) FROM users</sqlCheck>
                    </preConditions>
                    <createTable tableName="users"/>
                </changeSet>
            </databaseChangeLog>
        """.trimIndent()

        // onFail changed from MARK_RAN to HALT — affects Liquibase behaviour
        val current = """
            <databaseChangeLog>
                <changeSet id="001" author="shalu">
                    <preConditions onFail="HALT">
                        <sqlCheck expectedResult="0">SELECT COUNT(*) FROM users</sqlCheck>
                    </preConditions>
                    <createTable tableName="users"/>
                </changeSet>
            </databaseChangeLog>
        """.trimIndent()

        val tag = getChangesetTag(current, 0)
        assertTrue("Changeset with modified precondition should be flagged",
            comparator.isModified(tag, committed))
    }

    /**
     * Edge case: id and author attributes are swapped in order in the XML tag.
     * e.g. committed has id first, current has author first.
     * Content is logically identical — should NOT be flagged.
     */
    fun testChangesetWithSwappedAttributeOrderIsNotFlagged() {
        val committed = """
            <databaseChangeLog>
                <changeSet id="001" author="shalu">
                    <createTable tableName="users"/>
                </changeSet>
            </databaseChangeLog>
        """.trimIndent()

        // Attribute order swapped — same logical content
        val current = """
            <databaseChangeLog>
                <changeSet author="shalu" id="001">
                    <createTable tableName="users"/>
                </changeSet>
            </databaseChangeLog>
        """.trimIndent()

        val tag = getChangesetTag(current, 0)
        assertFalse("Changeset with swapped attribute order should not be flagged",
            comparator.isModified(tag, committed))
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    /** Parses XML content and returns the changeSet tag at the given index. */
    private fun getChangesetTag(content: String, index: Int) =
        (myFixture.configureByText(XmlFileType.INSTANCE, content) as XmlFile)
            .rootTag!!
            .findSubTags("changeSet")[index]

    /** Parses XML content and returns all changeSet tags. */
    private fun getChangesetTags(content: String) =
        (myFixture.configureByText(XmlFileType.INSTANCE, content) as XmlFile)
            .rootTag!!
            .findSubTags("changeSet")
}