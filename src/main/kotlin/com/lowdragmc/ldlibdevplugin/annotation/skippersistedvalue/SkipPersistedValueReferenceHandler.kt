package com.lowdragmc.ldlibdevplugin.annotation.skippersistedvalue

import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.lowdragmc.ldlibdevplugin.annotation.AnnotationReferenceHandler

class SkipPersistedValueReferenceHandler : AnnotationReferenceHandler() {

    override fun canApply(value: String, annotation: PsiAnnotation, nameValuePair: PsiNameValuePair): Boolean {
        return annotation.hasQualifiedName(SkipPersistedValueUtils.SKIP_PERSISTED_VALUE_ANNOTATION) &&
                (nameValuePair.name == SkipPersistedValueUtils.FIELD_ATTRIBUTE || nameValuePair.name == null)
    }

    override fun createPsiReference(
        element: PsiLiteralExpression,
        value: String,
        annotation: PsiAnnotation,
        nameValuePair: PsiNameValuePair
    ): PsiReference {
        return SkipPersistedValueFieldReference(element, value)
    }
}

class SkipPersistedValueFieldReference(
    element: PsiLiteralExpression,
    private val fieldName: String
) : PsiReferenceBase<PsiLiteralExpression>(element) {

    override fun resolve(): PsiElement? {
        val method = PsiTreeUtil.getParentOfType(element, PsiMethod::class.java) ?: return null
        val containingClass = method.containingClass ?: return null

        return SkipPersistedValueUtils.findSkipPersistedValueField(method, containingClass)
    }

    override fun getVariants(): Array<Any> {
        val method = PsiTreeUtil.getParentOfType(element, PsiMethod::class.java) ?: return emptyArray()
        val containingClass = method.containingClass ?: return emptyArray()

        val methodParamType = method.parameterList.parameters.firstOrNull()?.type

        val result = mutableListOf<PsiField>()
        collectCompatibleConfigurableFields(containingClass, methodParamType, result)

        return result
            .map { field -> field.name }
            .toTypedArray()
    }

    private fun collectCompatibleConfigurableFields(
        clazz: PsiClass,
        methodParamType: PsiType?,
        result: MutableList<PsiField>
    ) {
        var currentClass: PsiClass? = clazz
        while (currentClass != null) {
            currentClass.fields
                .filter { field ->
                    SkipPersistedValueUtils.isConfigurableField(field) &&
                            (methodParamType == null ||
                                    SkipPersistedValueUtils.isValidSkipPersistedValueMethod(
                                        PsiTreeUtil.getParentOfType(element, PsiMethod::class.java)!!,
                                        field.type
                                    ))
                }
                .forEach { result.add(it) }
            currentClass = currentClass.superClass
        }
    }
}
