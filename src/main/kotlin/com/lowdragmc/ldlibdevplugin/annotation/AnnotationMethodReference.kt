package com.lowdragmc.ldlibdevplugin.annotation

import com.intellij.openapi.util.TextRange
import com.intellij.psi.*

class AnnotationMethodReference(
    private val handler: AnnotationMethodReferenceHandler,
    private val element: PsiLiteralExpression,
    private val methodName: String,
    private val annotation: PsiAnnotation,
    private val parameterName: String
) : PsiReferenceBase<PsiLiteralExpression>(element, getTextRange(element)) {

    override fun resolve(): PsiElement? {
        return handler.resolve(element, methodName, annotation, parameterName)
    }

    override fun getVariants(): Array<Any> {
        return handler.getVariants(element, methodName, annotation, parameterName);
    }

    companion object {
        private fun getTextRange(element: PsiLiteralExpression): TextRange {
            val text = element.text
            return when {
                text == "\"\"" -> TextRange(1, 1)
                text == "''" -> TextRange(1, 1)
                text.startsWith("\"") && text.endsWith("\"") && text.length >= 2 ->
                    TextRange(1, text.length - 1)
                text.startsWith("'") && text.endsWith("'") && text.length >= 2 ->
                    TextRange(1, text.length - 1)
                else ->
                    TextRange(0, text.length)
            }

        }
    }
}