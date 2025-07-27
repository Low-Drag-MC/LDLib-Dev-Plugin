package com.lowdragmc.ldlibdevplugin.annotation.type

import com.intellij.codeInspection.*
import com.intellij.psi.*

class AnnotationTypeInspection : AbstractBaseJavaLocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitField(field: PsiField) {
                super.visitField(field)
                checkFieldTypes(field, holder)
            }
        }
    }

    private fun checkFieldTypes(field: PsiField, holder: ProblemsHolder) {
        // Skip if field is not physical
        if (!field.isPhysical) return

        val violations = AnnotationTypeChecker.checkAllFieldTypes(field)
        
        violations.forEach { violation ->
            val annotationName = violation.annotation.qualifiedName?.substringAfterLast('.') ?: "annotation"
            val message = "@$annotationName requires ${violation.expectedType}, but field is ${violation.actualType}"

            holder.registerProblem(
                violation.annotation,
                message,
                ProblemHighlightType.WARNING
            )
        }
    }
}