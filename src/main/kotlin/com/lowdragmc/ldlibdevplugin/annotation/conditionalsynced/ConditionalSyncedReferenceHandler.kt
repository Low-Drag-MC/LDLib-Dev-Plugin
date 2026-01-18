package com.lowdragmc.ldlibdevplugin.annotation.conditionalsynced

import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.lowdragmc.ldlibdevplugin.annotation.AnnotationMethodReferenceHandler

class ConditionalSyncedReferenceHandler : AnnotationMethodReferenceHandler() {

    override fun canApply(value: String, annotation: PsiAnnotation, nameValuePair: PsiNameValuePair): Boolean {
        return annotation.qualifiedName == ConditionalSyncedUtils.CONDITIONAL_SYNCED_ANNOTATION &&
                nameValuePair.name == ConditionalSyncedUtils.METHOD_NAME
    }

    override fun resolve(element: PsiLiteralExpression, methodName: String, annotation: PsiAnnotation, parameterName: String): PsiElement? {
        val containingClass = PsiTreeUtil.getParentOfType(element, PsiClass::class.java) ?: return null
        val annotatedField = (annotation.parent as? PsiModifierList)?.parent as? PsiField ?: return null

        return containingClass.findMethodsByName(methodName, true)
            .find { method -> ConditionalSyncedUtils.isValidMethod(method, annotatedField.type) }
    }

    override fun getVariants(element: PsiLiteralExpression, methodName: String, annotation: PsiAnnotation, parameterName: String): Array<Any> {
        val containingClass = PsiTreeUtil.getParentOfType(element, PsiClass::class.java) ?: return emptyArray()
        val annotatedField = (annotation.parent as? PsiModifierList)?.parent as? PsiField ?: return emptyArray()

        return containingClass.methods
            .filter { method -> ConditionalSyncedUtils.isValidMethod(method, annotatedField.type) }
            .map { it.name }
            .toTypedArray()
    }
}
