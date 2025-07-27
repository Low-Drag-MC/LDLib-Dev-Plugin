package com.lowdragmc.ldlibdevplugin.annotation

import com.intellij.psi.*

abstract class AnnotationReferenceHandler {

    abstract fun canApply(value: String, annotation: PsiAnnotation, nameValuePair: PsiNameValuePair): Boolean

    abstract fun createPsiReference(
        element: PsiLiteralExpression,
        value: String,
        annotation: PsiAnnotation,
        nameValuePair: PsiNameValuePair
    ): PsiReference;

}