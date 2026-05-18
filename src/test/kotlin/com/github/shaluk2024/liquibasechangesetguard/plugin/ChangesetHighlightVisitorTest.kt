package com.github.shaluk2024.liquibasechangesetguard.plugin

import com.intellij.ide.highlighter.XmlFileType
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Unit tests for [ChangesetHighlightVisitor].
 *
 * Verifies that the visitor:
 * - Correctly identifies Liquibase changelog files as suitable for analysis
 * - Skips non-Liquibase XML files and non-XML files
 * - Handles edge cases like empty changelogs and non-changeSet tags
 * - Correctly implements the clone() contract required by HighlightVisitor
 */
class ChangesetHighlightVisitorTest : BasePlatformTestCase() {

    private lateinit var visitor: ChangesetHighlightVisitor

    override fun setUp() {
        super.setUp()
        // Create a fresh visitor instance before each test
        visitor = ChangesetHighlightVisitor()
    }

    /**
     * Happy path: regular XML file with no relation to Liquibase.
     * Visitor should skip it entirely.
     */
    fun testNonLiquibaseXmlFileIsNotSuitable() {
        val psiFile = myFixture.configureByText(XmlFileType.INSTANCE, """
            <foo>
                <bar>baz</bar>
            </foo>
        """.trimIndent())

        assertFalse("Non-Liquibase XML should not be suitable",
            visitor.suitableForFile(psiFile))
    }

    /**
     * Happy path: valid Liquibase changelog file with changesets.
     * Visitor should accept it for analysis.
     */
    fun testLiquibaseChangelogFileIsSuitable() {
        val psiFile = myFixture.configureByText(XmlFileType.INSTANCE, """
            <databaseChangeLog>
                <changeSet id="001" author="shalu">
                    <createTable tableName="users"/>
                </changeSet>
            </databaseChangeLog>
        """.trimIndent())

        assertTrue("Liquibase changelog file should be suitable",
            visitor.suitableForFile(psiFile))
    }

    /**
     * Edge case: XML file with a root tag other than databaseChangeLog.
     * Should be rejected — not a Liquibase file.
     */
    fun testXmlFileWithNonChangelogRootTagIsNotSuitable() {
        val psiFile = myFixture.configureByText(XmlFileType.INSTANCE, "<root/>")

        assertFalse("XML file with non-changelog root tag should not be suitable",
            visitor.suitableForFile(psiFile))
    }

    /**
     * Edge case: valid databaseChangeLog root but no changeSets inside.
     * File structure is valid — visitor should still accept it.
     * (No warnings will be produced since there are no changeSets to visit.)
     */
    fun testEmptyChangelogFileIsSuitable() {
        val psiFile = myFixture.configureByText(XmlFileType.INSTANCE, """
            <databaseChangeLog>
            </databaseChangeLog>
        """.trimIndent())

        assertTrue("Empty changelog file should still be suitable",
            visitor.suitableForFile(psiFile))
    }

    /**
     * Edge case: non-XML file (e.g. Java source file).
     * Visitor must reject it — suitableForFile checks for XmlFile type.
     */
    fun testNonXmlFileIsNotSuitable() {
        val psiFile = myFixture.configureByText("Test.java", """
            public class Test {}
        """.trimIndent())

        assertFalse("Non-XML file should not be suitable",
            visitor.suitableForFile(psiFile))
    }

    /**
     * Edge case: changelog contains non-changeSet tags like include and property.
     * Visitor should silently skip them without throwing any exception.
     */
    fun testVisitIgnoresNonChangeSetTags() {
        val psiFile = myFixture.configureByText(XmlFileType.INSTANCE, """
            <databaseChangeLog>
                <include file="other.xml"/>
                <property name="foo" value="bar"/>
            </databaseChangeLog>
        """.trimIndent())

        // Visiting non-changeSet tags should be a no-op — no exceptions thrown
        val xmlFile = psiFile as com.intellij.psi.xml.XmlFile
        xmlFile.rootTag?.subTags?.forEach { tag ->
            visitor.visit(tag)
        }
    }

    /**
     * Contract test: clone() must return a new separate instance of the visitor.
     * Required by the HighlightVisitor interface — IntelliJ calls clone()
     * to create per-file visitor instances.
     */
    fun testCloneReturnsNewInstance() {
        val clone = visitor.clone()

        // Must be a different object reference
        assertNotSame("clone() should return a new instance", visitor, clone)

        // Must be the same type
        assertInstanceOf(clone, ChangesetHighlightVisitor::class.java)
    }
}