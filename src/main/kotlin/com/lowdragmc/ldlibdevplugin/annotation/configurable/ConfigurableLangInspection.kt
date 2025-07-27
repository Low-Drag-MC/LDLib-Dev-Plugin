package com.lowdragmc.ldlibdevplugin.annotation.configurable

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.*
import com.lowdragmc.ldlibdevplugin.LangFileUtils
import java.io.File

class ConfigurableLangInspection : AbstractBaseJavaLocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitField(field: PsiField) {
                super.visitField(field)
                
                if (!field.hasAnnotation(ConfigurableUtils.CONFIGURABLE_ANNOTATION)) return
                
                val annotation = field.getAnnotation(ConfigurableUtils.CONFIGURABLE_ANNOTATION) ?: return
                val project = field.project
                
                // Check name attribute
                annotation.findAttributeValue(ConfigurableUtils.NAME)
                    ?.let { it as? PsiLiteralExpression }
                    ?.let { nameAttr ->
                        val nameValue = nameAttr.value as? String
                        if (!nameValue.isNullOrBlank()) {
                            checkLangKey(holder, nameAttr, nameValue, project)
                        }
                    }
                
                // Check tips attribute
                when (val tipsAttr = annotation.findAttributeValue(ConfigurableUtils.TIPS)) {
                    // Tips as array
                    is PsiArrayInitializerMemberValue -> {
                        tipsAttr.initializers
                            .filterIsInstance<PsiLiteralExpression>()
                            .forEach { tipExpr ->
                                val tipValue = tipExpr.value as? String
                                if (!tipValue.isNullOrBlank()) {
                                    checkLangKey(holder, tipExpr, tipValue, project)
                                }
                            }
                    }
                    // Tips as single string
                    is PsiLiteralExpression -> {
                        val tipValue = tipsAttr.value as? String
                        if (!tipValue.isNullOrBlank()) {
                            checkLangKey(holder, tipsAttr, tipValue, project)
                        }
                    }
                }
            }
        }
    }
    
    private fun checkLangKey(
        holder: ProblemsHolder,
        element: PsiLiteralExpression,
        key: String,
        project: Project
    ) {
        if (LangFileUtils.isKeyExists(project, key) != null) return
        
        // Key doesn't exist, report problem
        val langFiles = LangFileUtils.findAllLangFiles(project)
        val fixes = if (langFiles.isEmpty()) {
            // No language files exist, provide file creation fix
            listOf(CreateLangFileQuickFix(key, key))
        } else {
            // Language files exist, provide key addition fixes
            langFiles.map { langFile -> AddLangKeyQuickFix(langFile, key, key) }
        }
        
        holder.registerProblem(
            element,
            "Missing translation key '$key' in language files",
            ProblemHighlightType.WARNING,
            *fixes.toTypedArray()
        )
    }
}

/**
 * Quick fix to create a new language file with the missing key
 */
class CreateLangFileQuickFix(
    private val key: String,
    private val value: String
) : LocalQuickFix {
    
    override fun getName(): String = "Create en_us.json file and add key '$key'"
    
    override fun getFamilyName(): String = "Create language file"
    
    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        // TODO: In a real implementation, this should show a dialog to let user choose mod_id
        val modId = "examplemod" // Simplified version using default value
        
        val langFilePath = LangFileUtils.createLangFilePath(modId)
        val projectBasePath = project.basePath ?: return
        val fullPath = File(projectBasePath, langFilePath)
        
        // Create directory structure
        fullPath.parentFile.mkdirs()
        
        // Create file content
        val content = """
            {
              "$key": "$value"
            }
        """.trimIndent()
        
        try {
            fullPath.writeText(content, Charsets.UTF_8)
            // Refresh file system
            VfsUtil.markDirtyAndRefresh(false, true, true, fullPath.parentFile)
        } catch (e: Exception) {
            // Handle error silently - could be improved with proper error reporting
        }
    }
}

/**
 * Quick fix to add a key to an existing language file
 */
class AddLangKeyQuickFix(
    private val langFile: LangFileUtils.LangFile,
    private val key: String,
    private val value: String
) : LocalQuickFix {
    
    override fun getName(): String = "Add key '$key' to ${langFile.modId}/lang/en_us.json"
    
    override fun getFamilyName(): String = "Add translation key"
    
    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        LangFileUtils.addKeyToLangFile(langFile.file, key, value)
        // Refresh file
        VfsUtil.markDirtyAndRefresh(false, false, false, langFile.file)
    }
}