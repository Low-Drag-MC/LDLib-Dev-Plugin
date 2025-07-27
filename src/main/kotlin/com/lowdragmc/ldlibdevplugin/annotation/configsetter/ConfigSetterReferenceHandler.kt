package com.lowdragmc.ldlibdevplugin.annotation.configsetter

import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.lowdragmc.ldlibdevplugin.annotation.AnnotationReferenceHandler
import com.lowdragmc.ldlibdevplugin.annotation.AnnotationUtils

class ConfigSetterReferenceHandler : AnnotationReferenceHandler() {

    override fun canApply(value: String, annotation: PsiAnnotation, nameValuePair: PsiNameValuePair): Boolean {
        return annotation.hasQualifiedName(ConfigSetterUtils.CONFIG_SETTER_ANNOTATION) &&
                (nameValuePair.name == ConfigSetterUtils.FIELD_ATTRIBUTE || nameValuePair.name == null)
    }

    override fun createPsiReference(
        element: PsiLiteralExpression,
        value: String,
        annotation: PsiAnnotation,
        nameValuePair: PsiNameValuePair
    ): PsiReference {
        return ConfigSetterFieldReference(element, value)
    }
}

class ConfigSetterFieldReference(
    element: PsiLiteralExpression,
    private val fieldName: String
) : PsiReferenceBase<PsiLiteralExpression>(element) {

    override fun resolve(): PsiElement? {
        val method = PsiTreeUtil.getParentOfType(element, PsiMethod::class.java) ?: return null
        val containingClass = method.containingClass ?: return null

        return ConfigSetterUtils.findConfigSetterField(method, containingClass)
            ?: ConfigSetterUtils.findFieldWithoutConfigurableCheck(method, containingClass)
    }

    override fun getVariants(): Array<Any> {
        val method = PsiTreeUtil.getParentOfType(element, PsiMethod::class.java) ?: return emptyArray()
        val containingClass = method.containingClass ?: return emptyArray()
        
        // check type
        val methodParamType = if (method.parameterList.parameters.size == 1) {
            method.parameterList.parameters[0].type
        } else {
            null
        }

        val compatibleFields = mutableListOf<PsiField>()
        collectCompatibleConfigurableFields(containingClass, methodParamType, compatibleFields)

        return compatibleFields.map { it.name }.toTypedArray()
    }

    private fun collectCompatibleConfigurableFields(
        clazz: PsiClass, 
        methodParamType: PsiType?, 
        result: MutableList<PsiField>
    ) {
        // collect @Configurable fields
        val configurableFields = clazz.fields.filter { ConfigSetterUtils.isConfigurableField(it) }
        
        for (field in configurableFields) {
            if (methodParamType == null) {
                result.add(field)
            } else {
                val fieldType = field.type
                if (AnnotationUtils.isTypeCompatible(methodParamType, fieldType) || 
                    AnnotationUtils.isTypeCompatible(fieldType, methodParamType)) {
                    result.add(field)
                }
            }
        }

        clazz.superClass?.let { superClass ->
            collectCompatibleConfigurableFields(superClass, methodParamType, result)
        }
    }
}