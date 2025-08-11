package com.lowdragmc.ldlibdevplugin.annotation

import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.lowdragmc.ldlibdevplugin.annotation.configlist.ConfigListReferenceHandler
import com.lowdragmc.ldlibdevplugin.annotation.configselector.ConfigSelectorReferenceHandler
import com.lowdragmc.ldlibdevplugin.annotation.configsetter.ConfigSetterReferenceHandler
import com.lowdragmc.ldlibdevplugin.annotation.lang.LangReferenceHandler
import com.lowdragmc.ldlibdevplugin.annotation.readonlymanaged.ReadOnlyManagedReferenceHandler
import com.lowdragmc.ldlibdevplugin.annotation.updatelistener.UpdateListenerReferenceHandler

class AnnotationReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(PsiLiteralExpression::class.java),
            AnnotationReferenceProvider()
        )
    }
}

class AnnotationReferenceProvider : PsiReferenceProvider() {
    private val handlers : List<AnnotationReferenceHandler> = listOf(
        UpdateListenerReferenceHandler(),
        ReadOnlyManagedReferenceHandler(),
        ConfigSetterReferenceHandler(),
        LangReferenceHandler(),
        ConfigListReferenceHandler(),
        ConfigSelectorReferenceHandler(),
    )

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        if (element !is PsiLiteralExpression) return PsiReference.EMPTY_ARRAY

        val value = element.value as? String ?: return PsiReference.EMPTY_ARRAY

        val annotation = PsiTreeUtil.getParentOfType(element, PsiAnnotation::class.java) ?: return PsiReference.EMPTY_ARRAY
        
        val nameValuePair = findNameValuePair(element) ?: return PsiReference.EMPTY_ARRAY

        // Find handler
        val handler = handlers.find { handler ->
            handler.canApply(value, annotation, nameValuePair)
        } ?: return PsiReference.EMPTY_ARRAY
        
        return arrayOf(handler.createPsiReference(element, value, annotation, nameValuePair))
    }
    
    private fun findNameValuePair(element: PsiLiteralExpression): PsiNameValuePair? {
        // First try to find nameValuePair directly
        var nameValuePair = PsiTreeUtil.getParentOfType(element, PsiNameValuePair::class.java)
        
        if (nameValuePair != null) {
            return nameValuePair
        }
        
        // If not found, it might be in an array initializer
        val arrayInitializer = PsiTreeUtil.getParentOfType(element, PsiArrayInitializerMemberValue::class.java)
        if (arrayInitializer != null) {
            nameValuePair = PsiTreeUtil.getParentOfType(arrayInitializer, PsiNameValuePair::class.java)
            if (nameValuePair != null) {
                return nameValuePair
            }
        }
        
        // If still not found, check if it's a direct parameter of annotation (might be default attribute)
        val annotation = PsiTreeUtil.getParentOfType(element, PsiAnnotation::class.java)
        if (annotation != null) {
            val parameterList = annotation.parameterList
            
            // Iterate through all nameValuePairs in parameter list
            for (attribute in parameterList.attributes) {
                if (PsiTreeUtil.isAncestor(attribute, element, false)) {
                    return attribute
                }
            }
        }
        
        return null
    }
}