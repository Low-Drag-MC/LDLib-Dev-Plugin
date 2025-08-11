package com.lowdragmc.ldlibdevplugin.annotation.lang

import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonProperty
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.lowdragmc.ldlibdevplugin.LangFileUtils
import com.lowdragmc.ldlibdevplugin.annotation.AnnotationReferenceHandler

class LangReferenceHandler : AnnotationReferenceHandler() {
    
    override fun canApply(value: String, annotation: PsiAnnotation, nameValuePair: PsiNameValuePair): Boolean {
        return when {
            annotation.hasQualifiedName(LangUtils.CONFIGURABLE_ANNOTATION) &&
                    nameValuePair.name in setOf(LangUtils.NAME, LangUtils.TIPS, null) -> true
            annotation.hasQualifiedName(LangUtils.CONFIG_HEADER_ANNOTATION) &&
                    nameValuePair.name in setOf(LangUtils.VALUE, null) -> true
            else -> false
        }
    }
    
    override fun createPsiReference(
        element: PsiLiteralExpression,
        value: String,
        annotation: PsiAnnotation,
        nameValuePair: PsiNameValuePair
    ): PsiReference {
        return ConfigurableLangReference(element, value)
    }
}

class ConfigurableLangReference(
    element: PsiLiteralExpression,
    private val key: String
) : PsiReferenceBase<PsiLiteralExpression>(element) {
    
    override fun resolve(): PsiElement? {
        val project = element.project
        val langFile = LangFileUtils.isKeyExists(project, key) ?: return null
        
        // Find the JsonProperty corresponding to the key
        val psiFile = PsiManager.getInstance(project).findFile(langFile.file) as? JsonFile
            ?: return null
        
        return psiFile.topLevelValue?.let { topLevel ->
            PsiTreeUtil.findChildrenOfType(topLevel, JsonProperty::class.java)
                .find { it.name == key }
                ?.nameElement
        }
    }
    
    override fun getVariants(): Array<Any> {
        val project = element.project
        val langFiles = LangFileUtils.findAllLangFiles(project)
        
        return langFiles
            .flatMap { it.keys }
            .toSet()
            .toTypedArray()
    }
    
    override fun getRangeInElement(): TextRange {
        // Exclude quotes from the range
        val text = element.text
        return if (text.length >= 2 && text.startsWith('"') && text.endsWith('"')) {
            TextRange(1, text.length - 1)
        } else {
            super.getRangeInElement()
        }
    }
}