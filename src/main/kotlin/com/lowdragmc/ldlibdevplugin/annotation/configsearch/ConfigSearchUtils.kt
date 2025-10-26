package com.lowdragmc.ldlibdevplugin.annotation.configsearch

import com.intellij.psi.*
import com.lowdragmc.ldlibdevplugin.annotation.AnnotationUtils

object ConfigSearchUtils {

    const val CONFIG_SEARCH_ANNOTATION = "com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSearch"
    const val SEARCH_CONFIGURATOR_METHOD = "searchConfiguratorMethod"

    fun isAnnotatedField(field: PsiField): Boolean {
        return field.hasAnnotation(CONFIG_SEARCH_ANNOTATION)
    }

    fun getSearchConfiguratorMethod(field: PsiField): String? {
        val annotation = field.getAnnotation(CONFIG_SEARCH_ANNOTATION) ?: return null
        return AnnotationUtils.getMethodName(annotation, SEARCH_CONFIGURATOR_METHOD)
    }

    /**
     * Validate search configurator method signature:
     * ISearchConfigurator method()
     *
     * @param method the method to validate
     */
    fun isValidSearchConfiguratorMethod(method: PsiMethod): Boolean {
        // Method cannot be static
        if (method.hasModifierProperty(PsiModifier.STATIC)) return false

        // Method should have no parameters
        val parameters = method.parameterList.parameters
        if (parameters.isNotEmpty()) return false

        val returnType = method.returnType ?: return false

        // Check if return type is ISearchConfigurator
        val isSearchConfiguratorReturn = when (returnType) {
            is PsiClassType -> {
                val psiClass = returnType.resolve() ?: return false
                psiClass.qualifiedName == "com.lowdragmc.lowdraglib2.configurator.ui.SearchComponentConfigurator.ISearchConfigurator"
            }
            else -> false
        }

        return isSearchConfiguratorReturn
    }

    fun isSearchConfiguratorMethod(method: PsiMethod, field: PsiField): Boolean {
        val expectedMethodName = getSearchConfiguratorMethod(field) ?: return false
        if (method.name != expectedMethodName) return false

        return isValidSearchConfiguratorMethod(method)
    }

    fun findConfigSearchField(method: PsiMethod, containingClass: PsiClass): PsiField? {
        return containingClass.fields.find { field ->
            isAnnotatedField(field) && isSearchConfiguratorMethod(method, field)
        }
    }
}