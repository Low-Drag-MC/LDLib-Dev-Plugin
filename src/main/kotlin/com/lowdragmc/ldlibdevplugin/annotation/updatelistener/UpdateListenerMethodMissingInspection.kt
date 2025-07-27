package com.lowdragmc.ldlibdevplugin.annotation.updatelistener

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.lowdragmc.ldlibdevplugin.annotation.AbstractAnnotationMethodInspection
import com.lowdragmc.ldlibdevplugin.annotation.AnnotationUtils

val METHOD_NAME = AbstractAnnotationMethodInspection.MethodAttribute(UpdateListenerUtils.METHOD_NAME, "listener method", "listener")
class UpdateListenerMethodMissingInspection : AbstractAnnotationMethodInspection() {

    override fun isAnnotatedField(field: PsiField): Boolean {
        return UpdateListenerUtils.isAnnotatedField(field)
    }

    override fun getAnnotation(field: PsiField): PsiAnnotation? {
        return field.getAnnotation(UpdateListenerUtils.UPDATE_LISTENER_ANNOTATION)
    }

    override fun getMethodNameAttribute(annotation: PsiAnnotation, attributeName: String): PsiLiteralExpression? {
        return annotation.findAttributeValue(attributeName) as? PsiLiteralExpression
    }

    override fun getMethodName(field: PsiField, attributeName: String): String? {
        return if (attributeName == UpdateListenerUtils.METHOD_NAME) UpdateListenerUtils.getMethodName(field) else null
    }

    override fun isValidMethod(method: PsiMethod, fieldType: PsiType, methodAttribute: MethodAttribute): Boolean {
        return UpdateListenerUtils.isValidMethod(method, fieldType)
    }

    override fun hasCorrectParameterCount(method: PsiMethod, fieldType: PsiType, methodAttribute: MethodAttribute): Boolean {
        return method.parameterList.parameters.size == 2 &&
                method.parameterList.parameters.all { param ->
                    AnnotationUtils.isTypeCompatible(param.type, fieldType)
                }
    }

    override fun getAnnotationName(): String = "UpdateListener"

    override fun getMethodAttributes(): List<MethodAttribute> {
        return listOf(METHOD_NAME)
    }

    override fun createMethodQuickFix(methodName: String, fieldType: PsiType, fieldName: String, methodAttribute: MethodAttribute): LocalQuickFix {
        return CreateUpdateListenerMethodQuickFix(methodName, fieldType, fieldName)
    }
}

class CreateUpdateListenerMethodQuickFix(
    private val methodName: String,
    private val fieldType: PsiType,
    private val fieldName: String
) : LocalQuickFix {

    override fun getName(): String = "Create UpdateListener method '$methodName'"

    override fun getFamilyName(): String = "Create UpdateListener method"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement
        val containingClass = PsiTreeUtil.getParentOfType(element, PsiClass::class.java) ?: return

        val elementFactory = JavaPsiFacade.getElementFactory(project)
        val fieldTypeName = fieldType.presentableText

        val methodText = """
            private void $methodName($fieldTypeName oldValue, $fieldTypeName newValue) {
                // TODO: Implement $fieldName update listener
            }
        """.trimIndent()

        val method = elementFactory.createMethodFromText(methodText, containingClass)
        containingClass.add(method)
    }
}

