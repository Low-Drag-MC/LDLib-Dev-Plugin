package com.lowdragmc.ldlibdevplugin.annotation.updatelistener

import com.intellij.psi.*
import com.lowdragmc.ldlibdevplugin.annotation.AnnotationUtils

object UpdateListenerUtils {

    const val UPDATE_LISTENER_ANNOTATION = "com.lowdragmc.lowdraglib2.syncdata.annotation.UpdateListener"
    const val METHOD_NAME = "methodName"

    fun isAnnotatedField(field: PsiField): Boolean {
        return field.hasAnnotation(UPDATE_LISTENER_ANNOTATION)
    }

    fun getMethodName(field: PsiField): String? {
        val annotation = field.getAnnotation(UPDATE_LISTENER_ANNOTATION) ?: return null
        return AnnotationUtils.getMethodName(annotation, METHOD_NAME);
    }

    fun isValidMethod(method: PsiMethod, fieldType: PsiType): Boolean {
        // method cannot be static
        if (method.hasModifierProperty(PsiModifier.STATIC)) return false
        
        val parameters = method.parameterList.parameters
        if (parameters.size != 2) return false

        return parameters.all { param ->
            AnnotationUtils.isTypeCompatible(param.type, fieldType)
        }
    }

    fun isUpdateListenerMethod(method: PsiMethod, field: PsiField): Boolean {
        val expectedMethodName = getMethodName(field) ?: return false

        if (method.name != expectedMethodName) return false

        return isValidMethod(method, field.type)
    }

    fun findUpdateListenerField(method: PsiMethod, containingClass: PsiClass): PsiField? {
        return containingClass.fields.find { field ->
            isAnnotatedField(field) && isUpdateListenerMethod(method, field)
        }
    }
}