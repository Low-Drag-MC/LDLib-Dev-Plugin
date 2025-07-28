package com.lowdragmc.ldlibdevplugin.annotation.configselector

import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.lowdragmc.ldlibdevplugin.annotation.AnnotationMethodReferenceHandler

class ConfigSelectorReferenceHandler : AnnotationMethodReferenceHandler() {

    override fun canApply(
        value: String,
        annotation: PsiAnnotation,
        nameValuePair: PsiNameValuePair
    ): Boolean {
        return annotation.qualifiedName == ConfigSelectorUtils.CONFIG_SELECTOR_ANNOTATION &&
                nameValuePair.name == ConfigSelectorUtils.SUB_CONFIGURATOR_BUILDER
    }

    override fun resolve(
        element: PsiLiteralExpression,
        methodName: String,
        annotation: PsiAnnotation,
        parameterName: String
    ): PsiElement? {
        val containingClass = PsiTreeUtil.getParentOfType(element, PsiClass::class.java) ?: return null
        val annotatedField = getAnnotatedField(annotation) ?: return null

        return containingClass.findMethodsByName(methodName, true)
            .find { method ->
                when (parameterName) {
                    ConfigSelectorUtils.SUB_CONFIGURATOR_BUILDER -> 
                        ConfigSelectorUtils.isValidSubConfiguratorBuilderMethod(method, annotatedField.type)
                    else -> false
                }
            }
    }

    override fun getVariants(
        element: PsiLiteralExpression,
        methodName: String,
        annotation: PsiAnnotation,
        parameterName: String
    ): Array<Any> {
        val containingClass = PsiTreeUtil.getParentOfType(element, PsiClass::class.java) ?: return emptyArray()
        val annotatedField = getAnnotatedField(annotation) ?: return emptyArray()

        return containingClass.methods
            .filter { method ->
                when (parameterName) {
                    ConfigSelectorUtils.SUB_CONFIGURATOR_BUILDER -> 
                        ConfigSelectorUtils.isValidSubConfiguratorBuilderMethod(method, annotatedField.type)
                    else -> false
                }
            }
            .map { it.name }
            .toTypedArray()
    }

    private fun getAnnotatedField(annotation: PsiAnnotation): PsiField? {
        val modifierList = annotation.parent as? PsiModifierList ?: return null
        return modifierList.parent as? PsiField
    }
}