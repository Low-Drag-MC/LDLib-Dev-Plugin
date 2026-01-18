package com.lowdragmc.ldlibdevplugin.annotation.conditionalsynced

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.lowdragmc.ldlibdevplugin.annotation.AbstractAnnotationMethodInspection
import com.lowdragmc.ldlibdevplugin.annotation.AnnotationUtils

val METHOD_NAME_ATTR = AbstractAnnotationMethodInspection.MethodAttribute(ConditionalSyncedUtils.METHOD_NAME, "conditional sync method", "sync evaluator")

class ConditionalSyncedMethodInspection : AbstractAnnotationMethodInspection() {

    override fun isAnnotatedField(field: PsiField): Boolean = ConditionalSyncedUtils.isAnnotatedField(field)

    override fun getAnnotation(field: PsiField): PsiAnnotation? = field.getAnnotation(ConditionalSyncedUtils.CONDITIONAL_SYNCED_ANNOTATION)

    override fun getMethodNameAttribute(annotation: PsiAnnotation, attributeName: String): PsiLiteralExpression? {
        return annotation.findAttributeValue(attributeName) as? PsiLiteralExpression
    }

    override fun getMethodName(field: PsiField, attributeName: String): String? {
        return if (attributeName == ConditionalSyncedUtils.METHOD_NAME) ConditionalSyncedUtils.getMethodName(field) else null
    }

    override fun isValidMethod(method: PsiMethod, fieldType: PsiType, methodAttribute: MethodAttribute): Boolean {
        return ConditionalSyncedUtils.isValidMethod(method, fieldType)
    }

    override fun hasCorrectParameterCount(method: PsiMethod, fieldType: PsiType, methodAttribute: MethodAttribute): Boolean {
        return method.parameterList.parameters.size == 1 &&
                AnnotationUtils.isTypeCompatible(method.parameterList.parameters[0].type, fieldType)
    }

    override fun getAnnotationName(): String = "ConditionalSynced"

    override fun getMethodAttributes(): List<MethodAttribute> = listOf(METHOD_NAME_ATTR)

    override fun createMethodQuickFix(methodName: String, fieldType: PsiType, fieldName: String, methodAttribute: MethodAttribute): LocalQuickFix {
        return CreateConditionalSyncedMethodQuickFix(methodName, fieldType, fieldName)
    }
}

class CreateConditionalSyncedMethodQuickFix(
    private val methodName: String,
    private val fieldType: PsiType,
    private val fieldName: String
) : LocalQuickFix {
    override fun getName(): String = "Create ConditionalSynced method '$methodName'"
    override fun getFamilyName(): String = "Create ConditionalSynced method"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement
        val containingClass = PsiTreeUtil.getParentOfType(element, PsiClass::class.java) ?: return
        val elementFactory = JavaPsiFacade.getElementFactory(project)
        val fieldTypeName = fieldType.presentableText

        val methodText = """
            public boolean $methodName($fieldTypeName value) {
                // TODO: Implement $fieldName sync condition
                return true;
            }
        """.trimIndent()

        val method = elementFactory.createMethodFromText(methodText, containingClass)
        containingClass.add(method)
    }
}
