package com.lowdragmc.ldlibdevplugin.annotation.configlist

import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.lowdragmc.ldlibdevplugin.annotation.AnnotationMethodReferenceHandler

class ConfigListReferenceHandler : AnnotationMethodReferenceHandler() {

    override fun canApply(
        value: String,
        annotation: PsiAnnotation,
        nameValuePair: PsiNameValuePair
    ): Boolean {
        return annotation.qualifiedName == ConfigListUtils.CONFIG_LIST_ANNOTATION && (
                nameValuePair.name == ConfigListUtils.CONFIGURATOR_METHOD ||
                nameValuePair.name == ConfigListUtils.ADD_DEFAULT_METHOD)
    }

    override fun resolve(
        element: PsiLiteralExpression,
        methodName: String,
        annotation: PsiAnnotation,
        parameterName: String
    ): PsiElement? {
        val containingClass = PsiTreeUtil.getParentOfType(element, PsiClass::class.java) ?: return null
        val annotatedField = getAnnotatedField(annotation) ?: return null
        
        if (!ConfigListUtils.isValidFieldType(annotatedField)) return null
        val elementType = ConfigListUtils.getElementType(annotatedField) ?: return null

        return containingClass.findMethodsByName(methodName, true)
            .find { method ->
                when (parameterName) {
                    ConfigListUtils.CONFIGURATOR_METHOD -> 
                        ConfigListUtils.isValidConfiguratorMethod(method, elementType)
                    ConfigListUtils.ADD_DEFAULT_METHOD -> 
                        ConfigListUtils.isValidAddDefaultMethod(method, elementType)
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
        
        if (!ConfigListUtils.isValidFieldType(annotatedField)) return emptyArray()
        val elementType = ConfigListUtils.getElementType(annotatedField) ?: return emptyArray()

        return containingClass.methods
            .filter { method ->
                when (parameterName) {
                    ConfigListUtils.CONFIGURATOR_METHOD -> 
                        ConfigListUtils.isValidConfiguratorMethod(method, elementType)
                    ConfigListUtils.ADD_DEFAULT_METHOD -> 
                        ConfigListUtils.isValidAddDefaultMethod(method, elementType)
                    else -> false
                }
            }
            .map { it.name }  // get method name
            .toTypedArray()
    }

    private fun getAnnotatedField(annotation: PsiAnnotation): PsiField? {
        val modifierList = annotation.parent as? PsiModifierList ?: return null
        return modifierList.parent as? PsiField
    }
}