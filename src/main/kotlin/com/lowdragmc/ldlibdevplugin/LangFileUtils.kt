package com.lowdragmc.ldlibdevplugin

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.json.JsonFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import java.io.InputStreamReader

object LangFileUtils {
    const val LANG_FILE_NAME = "en_us.json"
    
    // Regex to match language file paths: .../src/main/resources/assets/mod_id/lang/en_us.json
    private val LANG_PATH_REGEX = Regex(""".*/src/main/resources/assets/([^/]+)/lang/$LANG_FILE_NAME$""")
    
    // Gson instance for pretty printing JSON
    private val GSON: Gson = GsonBuilder().setPrettyPrinting().create()

    data class LangFile(
        val file: VirtualFile,
        val modId: String,
        val keys: Set<String>
    )

    /**
     * Find all en_us.json files in the project
     */
    fun findAllLangFiles(project: Project): List<LangFile> {
        val jsonFiles = FileTypeIndex.getFiles(
            JsonFileType.INSTANCE,
            GlobalSearchScope.projectScope(project)
        )

        return jsonFiles
            .filter { it.name == LANG_FILE_NAME }
            .mapNotNull { virtualFile ->
                LANG_PATH_REGEX.find(virtualFile.path)?.let { matchResult ->
                    val modId = matchResult.groupValues[1]
                    val keys = extractKeysFromLangFile(virtualFile)
                    LangFile(virtualFile, modId, keys)
                }
            }
    }

    /**
     * Extract all keys from the language file
     */
    private fun extractKeysFromLangFile(file: VirtualFile): Set<String> {
        return try {
            file.inputStream.use { inputStream ->
                InputStreamReader(inputStream, Charsets.UTF_8).use { reader ->
                    val jsonElement = JsonParser.parseReader(reader)
                    if (jsonElement.isJsonObject) {
                        jsonElement.asJsonObject.keySet()
                    } else {
                        emptySet()
                    }
                }
            }
        } catch (e: Exception) {
            emptySet()
        }
    }

    /**
     * Check if the key exists in any language file
     */
    fun isKeyExists(project: Project, key: String): LangFile? {
        return findAllLangFiles(project).find { it.keys.contains(key) }
    }

    /**
     * Get all possible mod_ids in the project
     */
    fun getAllModIds(project: Project): List<String> {
        return findAllLangFiles(project).map { it.modId }.distinct()
    }

    /**
     * Create language file path
     */
    fun createLangFilePath(modId: String): String {
        return "src/main/resources/assets/$modId/lang/$LANG_FILE_NAME"
    }

    /**
     * Add key-value pair to language file
     */
    fun addKeyToLangFile(file: VirtualFile, key: String, value: String): Boolean {
        return try {
            val content = file.inputStream.use { inputStream ->
                InputStreamReader(inputStream, Charsets.UTF_8).use { it.readText() }
            }

            val jsonObject = if (content.isBlank()) {
                JsonObject()
            } else {
                JsonParser.parseString(content).asJsonObject
            }

            jsonObject.addProperty(key, value)
            val newContent = GSON.toJson(jsonObject)

            file.setBinaryContent(newContent.toByteArray(Charsets.UTF_8))
            true
        } catch (e: Exception) {
            false
        }
    }
}