package com.lowdragmc.ldlibdevplugin.annotation.configsearch

import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.lowdragmc.ldlibdevplugin.annotation.AnnotationMethodReferenceHandler

class ConfigSearchReferenceHandler : AnnotationMethodReferenceHandler() {

    override fun canApply(
        value: String,
        annotation: PsiAnnotation,
        nameValuePair: PsiNameValuePair
    ): Boolean {
        return annotation.qualifiedName == ConfigSearchUtils.CONFIG_SEARCH_ANNOTATION &&
                nameValuePair.name == ConfigSearchUtils.SEARCH_CONFIGURATOR_METHOD
    }

    override fun resolve(
        element: PsiLiteralExpression,
        methodName: String,
        annotation: PsiAnnotation,
        parameterName: String
    ): PsiElement? {
        val containingClass = PsiTreeUtil.getParentOfType(element, PsiClass::class.java) ?: return null

        return containingClass.findMethodsByName(methodName, true)
            .find { method ->
                when (parameterName) {
                    ConfigSearchUtils.SEARCH_CONFIGURATOR_METHOD -> 
                        ConfigSearchUtils.isValidSearchConfiguratorMethod(method)
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

        return containingClass.methods
            .filter { method ->
                when (parameterName) {
                    ConfigSearchUtils.SEARCH_CONFIGURATOR_METHOD -> 
                        ConfigSearchUtils.isValidSearchConfiguratorMethod(method)
                    else -> false
                }
            }
            .map { it.name }
            .toTypedArray()
    }
}
