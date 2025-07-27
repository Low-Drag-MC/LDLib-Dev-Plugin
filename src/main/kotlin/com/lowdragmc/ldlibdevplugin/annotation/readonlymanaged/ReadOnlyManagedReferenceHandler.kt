package com.lowdragmc.ldlibdevplugin.annotation.readonlymanaged

import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.lowdragmc.ldlibdevplugin.annotation.AnnotationMethodReferenceHandler

class ReadOnlyManagedReferenceHandler : AnnotationMethodReferenceHandler() {

    override fun canApply(
        value: String,
        annotation: PsiAnnotation,
        nameValuePair: PsiNameValuePair
    ): Boolean {
        return annotation.qualifiedName == ReadOnlyManagedUtils.READ_ONLY_MANAGED_ANNOTATION && (
                nameValuePair.name == ReadOnlyManagedUtils.ON_DIRTY_METHOD ||
                nameValuePair.name == ReadOnlyManagedUtils.SERIALIZE_METHOD ||
                nameValuePair.name == ReadOnlyManagedUtils.DESERIALIZE_METHOD)

    }

    override fun resolve(
        element: PsiLiteralExpression,
        methodName: String,
        annotation: PsiAnnotation,
        parameterName: String
    ): PsiElement? {
        val containingClass = PsiTreeUtil.getParentOfType(element, PsiClass::class.java) ?: return null
        val annotatedField = getAnnotatedField(annotation) ?: return null

        return containingClass.findMethodsByName(methodName, true)
            .find { method ->
                when (parameterName) {
                    ReadOnlyManagedUtils.ON_DIRTY_METHOD -> 
                        ReadOnlyManagedUtils.isValidOnDirtyMethod(method)
                    ReadOnlyManagedUtils.SERIALIZE_METHOD -> 
                        ReadOnlyManagedUtils.isValidSerializeMethod(method, annotatedField.type)
                    ReadOnlyManagedUtils.DESERIALIZE_METHOD -> 
                        ReadOnlyManagedUtils.isValidDeserializeMethod(method, annotatedField.type)
                    else -> false
                }
            }
    }

    override fun getVariants(
        element: PsiLiteralExpression,
        methodName: String,
        annotation: PsiAnnotation,
        parameterName: String
    ): Array<Any> {
        val containingClass = PsiTreeUtil.getParentOfType(element, PsiClass::class.java) ?: return emptyArray()
        val annotatedField = getAnnotatedField(annotation) ?: return emptyArray()

        return containingClass.methods
            .filter { method ->
                when (parameterName) {
                    ReadOnlyManagedUtils.ON_DIRTY_METHOD -> 
                        ReadOnlyManagedUtils.isValidOnDirtyMethod(method)
                    ReadOnlyManagedUtils.SERIALIZE_METHOD -> 
                        ReadOnlyManagedUtils.isValidSerializeMethod(method, annotatedField.type)
                    ReadOnlyManagedUtils.DESERIALIZE_METHOD -> 
                        ReadOnlyManagedUtils.isValidDeserializeMethod(method, annotatedField.type)
                    else -> false
                }
            }
            .map { it.name }  // get method name
            .toTypedArray()
    }

    private fun getAnnotatedField(annotation: PsiAnnotation): PsiField? {
        val modifierList = annotation.parent as? PsiModifierList ?: return null
        return modifierList.parent as? PsiField
    }

}