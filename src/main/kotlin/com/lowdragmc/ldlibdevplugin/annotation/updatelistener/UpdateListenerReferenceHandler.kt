package com.lowdragmc.ldlibdevplugin.annotation.updatelistener

import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.lowdragmc.ldlibdevplugin.annotation.AnnotationMethodReferenceHandler

class UpdateListenerReferenceHandler : AnnotationMethodReferenceHandler() {

    override fun canApply(
        value: String,
        annotation: PsiAnnotation,
        nameValuePair: PsiNameValuePair
    ): Boolean {
        return annotation.qualifiedName == UpdateListenerUtils.UPDATE_LISTENER_ANNOTATION &&
                nameValuePair.name == UpdateListenerUtils.METHOD_NAME
    }

    override fun resolve(
        element: PsiLiteralExpression,
        methodName: String,
        annotation: PsiAnnotation,
        parameterName: String
    ): PsiElement? {
        val containingClass = PsiTreeUtil.getParentOfType(element, PsiClass::class.java) ?: return null
        val modifierList = annotation.parent as? PsiModifierList ?: return null
        val annotatedField = modifierList.parent as? PsiField ?: return null

        return containingClass.findMethodsByName(methodName, true)
            .find { method ->
                UpdateListenerUtils.isValidMethod(method, annotatedField.type)
            }
    }

    override fun getVariants(
        element: PsiLiteralExpression,
        methodName: String,
        annotation: PsiAnnotation,
        parameterName: String
    ): Array<Any> {
        val containingClass = PsiTreeUtil.getParentOfType(element, PsiClass::class.java) ?: return emptyArray()
        val modifierList = annotation.parent as? PsiModifierList ?: return emptyArray()
        val annotatedField = modifierList.parent as? PsiField ?: return emptyArray()

        return containingClass.methods
            .filter { method ->
                UpdateListenerUtils.isValidMethod(method, annotatedField.type)
            }
            .map { it.name }  // get method name
            .toTypedArray()
    }
}