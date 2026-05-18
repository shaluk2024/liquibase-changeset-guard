package com.github.shaluk2024.liquibasechangesetguard

import com.intellij.ide.highlighter.XmlFileType
import com.intellij.openapi.components.service
import com.intellij.psi.xml.XmlFile
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.PsiErrorElementUtil
import com.github.shaluk2024.liquibasechangesetguard.services.MyProjectService

@TestDataPath("\$CONTENT_ROOT/src/test/testData")
class MyPluginTest : BasePlatformTestCase() {

    fun testXMLFile() {
        val psiFile = myFixture.configureByText(XmlFileType.INSTANCE, "<foo>bar</foo>")
        val xmlFile = assertInstanceOf(psiFile, XmlFile::class.java)

        assertFalse(PsiErrorElementUtil.hasErrors(project, xmlFile.virtualFile))

        assertNotNull(xmlFile.rootTag)

        xmlFile.rootTag?.let {
            assertEquals("foo", it.name)
            assertEquals("bar", it.value.text)
        }
    }

    fun testRename() {
        myFixture.testRename("foo.xml", "foo_after.xml", "a2")
    }

//    fun testProjectService() {
//        val projectService = project.service<MyProjectService>()
//
//        assertNotSame(projectService.getRandomNumber(), projectService.getRandomNumber())
//    }

//    fun testProjectService() {
//        // Verify ChangelogGitCacheService is registered and wired correctly
//        val cacheService = project.service<com.github.shaluk2024.liquibasechangesetguard.plugin.ChangelogGitCacheService>()
//        assertNotNull("ChangelogGitCacheService must be registered in plugin.xml", cacheService)
//
//        // Verify cache returns null for an unknown file (cold cache — no Git in tests)
//        val psiFile = myFixture.configureByText(XmlFileType.INSTANCE, """
//        <?xml version="1.0" encoding="UTF-8"?>
//        <databaseChangeLog>
//            <changeSet id="test-001" author="shalu">
//                <createTable tableName="test_table">
//                    <column name="id" type="BIGINT"/>
//                </createTable>
//            </changeSet>
//        </databaseChangeLog>
//    """.trimIndent())
//
//        val virtualFile = psiFile.virtualFile
//        // Cold cache should return null (no Git available in test environment)
//        assertNull(
//            "Cache should be empty for a file not yet fetched from Git",
//            cacheService.getCommittedMap(virtualFile)
//        )
//    }

    override fun getTestDataPath() = "src/test/testData/rename"
}
