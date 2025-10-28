package com.lowdragmc.ldlibdevplugin.annotation.skippersistedvalue

import com.intellij.psi.*
import com.lowdragmc.ldlibdevplugin.annotation.AnnotationUtils
import com.lowdragmc.ldlibdevplugin.annotation.configsetter.ConfigSetterUtils

object SkipPersistedValueUtils {

    const val SKIP_PERSISTED_VALUE_ANNOTATION = "com.lowdragmc.lowdraglib2.syncdata.annotation.SkipPersistedValue"
    const val FIELD_ATTRIBUTE = "field"
    const val CONFIGURABLE_ANNOTATION = "com.lowdragmc.lowdraglib2.configurator.annotation.Configurable"
    const val PERSISTED_ANNOTATION = "com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted"

    fun isAnnotatedMethod(method: PsiMethod): Boolean {
        return method.hasAnnotation(SKIP_PERSISTED_VALUE_ANNOTATION)
    }

    fun getFieldName(method: PsiMethod): String? {
        val annotation = method.getAnnotation(SKIP_PERSISTED_VALUE_ANNOTATION) ?: return null
        return AnnotationUtils.getMethodName(annotation, ConfigSetterUtils.FIELD_ATTRIBUTE)
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

    /**
     * Validate skip persisted value method signature:
     * - Returns boolean
     * - Has exactly one parameter
     * - Parameter type matches the field type
     * - Cannot be static
     */
    fun isValidSkipPersistedValueMethod(method: PsiMethod, fieldType: PsiType): Boolean {
        // Method cannot be static
        if (method.hasModifierProperty(PsiModifier.STATIC)) return false

        // Must return boolean
        val returnType = method.returnType ?: return false
        if (!returnType.equalsToText("boolean") && returnType.canonicalText != "java.lang.Boolean") {
            return false
        }

        // Must have exactly one parameter
        val parameters = method.parameterList.parameters
        if (parameters.size != 1) return false

        // Parameter type must match field type
        val paramType = parameters[0].type
        return AnnotationUtils.isTypeCompatible(paramType, fieldType) ||
                AnnotationUtils.isTypeCompatible(fieldType, paramType)
    }

    /**
     * Find the target field with @Configurable or @Persisted annotation
     */
    fun findSkipPersistedValueField(method: PsiMethod, containingClass: PsiClass): PsiField? {
        if (!isAnnotatedMethod(method)) return null

        val fieldName = getFieldName(method) ?: return null

        val field = findFieldInClassHierarchy(containingClass, fieldName)

        return if (field != null && isValidField(field)) field else null
    }

    /**
     * Find the field without checking @Configurable or @Persisted annotation (for quick fix suggestions)
     */
    fun findFieldWithoutConfigurableCheck(method: PsiMethod, containingClass: PsiClass): PsiField? {
        if (!isAnnotatedMethod(method)) return null

        val fieldName = getFieldName(method) ?: return null

        return findFieldInClassHierarchy(containingClass, fieldName)
    }

    private fun findFieldInClassHierarchy(psiClass: PsiClass, fieldName: String): PsiField? {
        var currentClass: PsiClass? = psiClass
        while (currentClass != null) {
            val field = currentClass.findFieldByName(fieldName, false)
            if (field != null) return field
            currentClass = currentClass.superClass
        }
        return null
    }

    /**
     * Find all @SkipPersistedValue methods for a specific field
     */
    fun findSkipPersistedValueMethods(field: PsiField, containingClass: PsiClass): List<PsiMethod> {
        if (!isValidField(field)) return emptyList()

        val fieldName = field.name
        val result = mutableListOf<PsiMethod>()

        findMethodsInClassHierarchy(containingClass, fieldName, result)

        return result.filter { method ->
            isAnnotatedMethod(method) &&
                    getFieldName(method) == fieldName &&
                    isValidSkipPersistedValueMethod(method, field.type)
        }
    }

    private fun findMethodsInClassHierarchy(psiClass: PsiClass, fieldName: String, result: MutableList<PsiMethod>) {
        var currentClass: PsiClass? = psiClass
        while (currentClass != null) {
            currentClass.methods
                .filter { method ->
                    isAnnotatedMethod(method) && getFieldName(method) == fieldName
                }
                .forEach { result.add(it) }
            currentClass = currentClass.superClass
        }
    }
}
