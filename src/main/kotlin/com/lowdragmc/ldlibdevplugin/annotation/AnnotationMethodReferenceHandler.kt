package com.lowdragmc.ldlibdevplugin.annotation

import com.intellij.psi.*

abstract class AnnotationMethodReferenceHandler : AnnotationReferenceHandler() {

    override fun createPsiReference(
        element: PsiLiteralExpression,
        value: String,
        annotation: PsiAnnotation,
        nameValuePair: PsiNameValuePair
    ): PsiReference {
        return AnnotationMethodReference(this, element, value, annotation, nameValuePair.name ?: "");
    }

    abstract fun resolve(
        element: PsiLiteralExpression,
        methodName: String,
        annotation: PsiAnnotation,
        parameterName: String
    ): PsiElement?

    abstract fun getVariants(
        element: PsiLiteralExpression,
        methodName: String,
        annotation: PsiAnnotation,
        parameterName: String
    ): Array<Any>

}