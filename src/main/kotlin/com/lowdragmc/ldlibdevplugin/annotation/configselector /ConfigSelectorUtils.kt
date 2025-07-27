package com.lowdragmc.ldlibdevplugin.annotation.configselector

import com.intellij.psi.*
import com.intellij.psi.util.InheritanceUtil
import com.lowdragmc.ldlibdevplugin.annotation.AnnotationUtils

object ConfigSelectorUtils {

    const val CONFIG_SELECTOR_ANNOTATION = "com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSelector"
    const val SUB_CONFIGURATOR_BUILDER = "subConfiguratorBuilder"

    fun isAnnotatedField(field: PsiField): Boolean {
        return field.hasAnnotation(CONFIG_SELECTOR_ANNOTATION)
    }

    fun getSubConfiguratorBuilder(field: PsiField): String? {
        val annotation = field.getAnnotation(CONFIG_SELECTOR_ANNOTATION) ?: return null
        return AnnotationUtils.getMethodName(annotation, SUB_CONFIGURATOR_BUILDER)
    }

    /**
     * Validate sub configurator builder method signature:
     * void method(fieldType value, ConfiguratorGroup group)
     * 
     * @param method the method to validate
     * @param fieldType the type of the annotated field
     */
    fun isValidSubConfiguratorBuilderMethod(method: PsiMethod, fieldType: PsiType): Boolean {
        // Method cannot be static
        if (method.hasModifierProperty(PsiModifier.STATIC)) return false

        val parameters = method.parameterList.parameters
        if (parameters.size != 2) return false

        // Check first parameter: must be compatible with field type (field type or its parent class)
        val firstParamType = parameters[0].type
        val isFirstParamValid = AnnotationUtils.isTypeCompatible(firstParamType, fieldType) ||
                fieldType.isAssignableFrom(firstParamType)

        if (!isFirstParamValid) return false

        // Check second parameter: must be ConfiguratorGroup or its subclass
        val secondParamType = parameters[1].type
        val isSecondParamValid = when (secondParamType) {
            is PsiClassType -> {
                val psiClass = secondParamType.resolve() ?: return false
                InheritanceUtil.isInheritor(psiClass, "com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup") ||
                        psiClass.qualifiedName == "com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup"
            }
            else -> false
        }

        return isSecondParamValid
    }

    fun isSubConfiguratorBuilderMethod(method: PsiMethod, field: PsiField): Boolean {
        val expectedMethodName = getSubConfiguratorBuilder(field) ?: return false
        if (method.name != expectedMethodName) return false
        
        return isValidSubConfiguratorBuilderMethod(method, field.type)
    }

    fun findConfigSelectorField(method: PsiMethod, containingClass: PsiClass): PsiField? {
        return containingClass.fields.find { field ->
            isAnnotatedField(field) && isSubConfiguratorBuilderMethod(method, field)
        }
    }
}