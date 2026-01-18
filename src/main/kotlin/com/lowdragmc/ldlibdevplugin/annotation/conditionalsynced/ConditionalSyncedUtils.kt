package com.lowdragmc.ldlibdevplugin.annotation.conditionalsynced

import com.intellij.psi.*
import com.lowdragmc.ldlibdevplugin.annotation.AnnotationUtils

object ConditionalSyncedUtils {
    const val CONDITIONAL_SYNCED_ANNOTATION = "com.lowdragmc.lowdraglib2.syncdata.annotation.ConditionalSynced"
    const val METHOD_NAME = "methodName"

    fun isAnnotatedField(field: PsiField): Boolean {
        return field.hasAnnotation(CONDITIONAL_SYNCED_ANNOTATION)
    }

    fun getMethodName(field: PsiField): String? {
        val annotation = field.getAnnotation(CONDITIONAL_SYNCED_ANNOTATION) ?: return null
        return AnnotationUtils.getMethodName(annotation, METHOD_NAME)
    }

    fun isValidMethod(method: PsiMethod, fieldType: PsiType): Boolean {
        // Return type must be boolean
        if (!AnnotationUtils.isBooleanType(method.returnType ?: PsiTypes.booleanType())) return false

        // Must have exactly 1 parameter of field type
        val parameters = method.parameterList.parameters
        if (parameters.size != 1) return false

        return AnnotationUtils.isTypeCompatible(parameters[0].type, fieldType)
    }

    fun findConditionalSyncedField(method: PsiMethod, containingClass: PsiClass): PsiField? {
        return containingClass.fields.find { field ->
            isAnnotatedField(field) && isConditionalSyncedMethod(method, field)
        }
    }

    fun isConditionalSyncedMethod(method: PsiMethod, field: PsiField): Boolean {
        val expectedMethodName = getMethodName(field) ?: return false
        if (method.name != expectedMethodName) return false
        return isValidMethod(method, field.type)
    }
}