package com.lowdragmc.ldlibdevplugin.annotation

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.*

abstract class AbstractAnnotationMethodInspection : AbstractBaseJavaLocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitField(field: PsiField) {
                super.visitField(field)

                if (!isAnnotatedField(field)) return

                val annotation = getAnnotation(field) ?: return
                val containingClass = field.containingClass ?: return

                // check all method attributes
                getMethodAttributes().forEach { methodAttribute ->
                    checkMethodAttribute(holder, field, annotation, containingClass, methodAttribute)
                }
            }
        }
    }

    private fun checkMethodAttribute(
        holder: ProblemsHolder,
        field: PsiField,
        annotation: PsiAnnotation,
        containingClass: PsiClass,
        methodAttribute: MethodAttribute
    ) {
        val methodNameAttr = getMethodNameAttribute(annotation, methodAttribute.attributeName) ?: return
        val methodName = getMethodName(field, methodAttribute.attributeName) ?: return

        // Skip empty method names
        if (methodName.isEmpty()) return
        
        // Check if the element is physical (not from DummyHolder)
        if (!methodNameAttr.isPhysical) return

        val candidateMethods = containingClass.findMethodsByName(methodName, true)
        val matchingMethod = candidateMethods.find { method ->
            isValidMethod(method, field.type, methodAttribute)
        }

        if (matchingMethod == null) {
            // check if static
            val staticMethod = candidateMethods.find { method ->
                method.hasModifierProperty(PsiModifier.STATIC) &&
                        hasCorrectParameterCount(method, field.type, methodAttribute)
            }

            val errorMessage = when {
                staticMethod != null -> "${getAnnotationName()} ${methodAttribute.displayName} '$methodName' cannot be static"
                candidateMethods.isNotEmpty() -> "${getAnnotationName()} ${methodAttribute.displayName} '$methodName' has incorrect signature"
                else -> "${getAnnotationName()} ${methodAttribute.displayName} '$methodName' not found"
            }

            val fixes = mutableListOf<LocalQuickFix>()

            if (staticMethod != null) {
                fixes.add(RemoveStaticModifierQuickFix(staticMethod))
            } else {
                fixes.add(createMethodQuickFix(methodName, field.type, field.name, methodAttribute))
            }

            holder.registerProblem(
                methodNameAttr,
                errorMessage,
                ProblemHighlightType.ERROR,
                *fixes.toTypedArray()
            )
        }
    }

    abstract fun isAnnotatedField(field: PsiField): Boolean
    abstract fun getAnnotation(field: PsiField): PsiAnnotation?
    abstract fun getMethodNameAttribute(annotation: PsiAnnotation, attributeName: String): PsiLiteralExpression?
    abstract fun getMethodName(field: PsiField, attributeName: String): String?
    abstract fun isValidMethod(method: PsiMethod, fieldType: PsiType, methodAttribute: MethodAttribute): Boolean
    abstract fun hasCorrectParameterCount(method: PsiMethod, fieldType: PsiType, methodAttribute: MethodAttribute): Boolean
    abstract fun getAnnotationName(): String
    abstract fun getMethodAttributes(): List<MethodAttribute>
    abstract fun createMethodQuickFix(methodName: String, fieldType: PsiType, fieldName: String, methodAttribute: MethodAttribute): LocalQuickFix

    data class MethodAttribute(
        val attributeName: String,
        val displayName: String,
        val methodType: String // 用于区分不同类型的方法
    )
}

class RemoveStaticModifierQuickFix(
    private val method: PsiMethod
) : LocalQuickFix {
    override fun getName(): String = "Remove static modifier from method '${method.name}'"
    override fun getFamilyName(): String = "Remove static modifier"
    override fun applyFix(
        project: Project,
        descriptor: ProblemDescriptor
    ) {
        val modifierList = method.modifierList
        modifierList.setModifierProperty(PsiModifier.STATIC, false)
    }
}