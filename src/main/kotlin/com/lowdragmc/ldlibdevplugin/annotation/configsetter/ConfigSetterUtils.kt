package com.lowdragmc.ldlibdevplugin.annotation.configsetter

import com.intellij.psi.*
import com.lowdragmc.ldlibdevplugin.annotation.AnnotationUtils

object ConfigSetterUtils {

    const val CONFIG_SETTER_ANNOTATION = "com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSetter"
    const val CONFIGURABLE_ANNOTATION = "com.lowdragmc.lowdraglib2.configurator.annotation.Configurable"
    const val PERSISTED_ANNOTATION = "com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted"
    const val FIELD_ATTRIBUTE = "field"

    fun isAnnotatedMethod(method: PsiMethod): Boolean {
        return method.hasAnnotation(CONFIG_SETTER_ANNOTATION)
    }

    fun isConfigurableField(field: PsiField): Boolean {
        return field.hasAnnotation(CONFIGURABLE_ANNOTATION)
    }

    fun isPersistedField(field: PsiField): Boolean {
        return field.hasAnnotation(PERSISTED_ANNOTATION)
    }

    fun isValidField(field: PsiField): Boolean {
        return isConfigurableField(field) || isPersistedField(field)
    }

    fun getFieldName(method: PsiMethod): String? {
        val annotation = method.getAnnotation(CONFIG_SETTER_ANNOTATION) ?: return null
        return AnnotationUtils.getMethodName(annotation, FIELD_ATTRIBUTE)
    }

    fun isValidConfigSetterMethod(method: PsiMethod, fieldType: PsiType): Boolean {
        // method cannot be static
        if (method.hasModifierProperty(PsiModifier.STATIC)) return false

        val parameters = method.parameterList.parameters
        if (parameters.size != 1) return false

        val paramType = parameters[0].type
        return AnnotationUtils.isTypeCompatible(paramType, fieldType) ||
                AnnotationUtils.isTypeCompatible(fieldType, paramType)
    }

    fun findConfigSetterField(method: PsiMethod, containingClass: PsiClass): PsiField? {
        if (!isAnnotatedMethod(method)) return null

        val fieldName = getFieldName(method) ?: return null

        val field = findFieldInClassHierarchy(containingClass, fieldName)

        return if (field != null && isValidField(field)) field else null
    }

    private fun findFieldInClassHierarchy(clazz: PsiClass, fieldName: String): PsiField? {
        clazz.fields.find { it.name == fieldName }?.let { return it }

        clazz.superClass?.let { superClass ->
            findFieldInClassHierarchy(superClass, fieldName)?.let { return it }
        }

        return null
    }

    fun findFieldWithoutConfigurableCheck(method: PsiMethod, containingClass: PsiClass): PsiField? {
        if (!isAnnotatedMethod(method)) return null

        val fieldName = getFieldName(method) ?: return null
        return findFieldInClassHierarchy(containingClass, fieldName)
    }

    fun findConfigSetterMethods(field: PsiField, containingClass: PsiClass): List<PsiMethod> {
        if (!isValidField(field)) return emptyList()

        val fieldName = field.name
        val result = mutableListOf<PsiMethod>()

        findMethodsInClassHierarchy(containingClass, fieldName, result)

        return result.filter { method ->
            isAnnotatedMethod(method) &&
                    getFieldName(method) == fieldName &&
                    isValidConfigSetterMethod(method, field.type)
        }
    }

    private fun findMethodsInClassHierarchy(clazz: PsiClass, fieldName: String, result: MutableList<PsiMethod>) {
        clazz.methods.forEach { method ->
            if (isAnnotatedMethod(method) && getFieldName(method) == fieldName) {
                result.add(method)
            }
        }

        clazz.superClass?.let { superClass ->
            findMethodsInClassHierarchy(superClass, fieldName, result)
        }
    }
}