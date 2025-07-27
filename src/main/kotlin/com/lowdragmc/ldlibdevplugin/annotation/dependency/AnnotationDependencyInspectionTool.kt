package com.lowdragmc.ldlibdevplugin.annotation.dependency

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil

class AnnotationDependencyInspection : AbstractBaseJavaLocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitField(field: PsiField) {
                super.visitField(field)
                checkElementDependencies(field, holder)
            }

            override fun visitClass(clazz: PsiClass) {
                super.visitClass(clazz)
                checkElementDependencies(clazz, holder)
            }

            override fun visitMethod(method: PsiMethod) {
                super.visitMethod(method)
                checkElementDependencies(method, holder)
            }
        }
    }

    private fun checkElementDependencies(element: PsiElement, holder: ProblemsHolder) {
        // Skip if element is not physical
        if (!element.isPhysical) return

        val violations = AnnotationDependencyChecker.checkDependencies(element)
        
        violations.forEach { violation ->
            val highlightType = when (violation.dependency.severity) {
                DependencySeverity.ERROR -> ProblemHighlightType.ERROR
                DependencySeverity.WARNING -> ProblemHighlightType.WARNING
                DependencySeverity.INFO -> ProblemHighlightType.INFORMATION
            }

            val fixes = violation.missingAnnotations.mapNotNull { missingAnnotation ->
                val template = AnnotationDependencyRegistry.getTemplate(missingAnnotation)
                if (template != null) {
                    AddMissingAnnotationQuickFix(
                        element = element,
                        annotationTemplate = template,
                        context = violation.context
                    )
                } else null
            }

            val message = buildString {
                append(violation.dependency.description)
                if (violation.missingAnnotations.size == 1) {
                    append(" Missing: @${getSimpleName(violation.missingAnnotations.first())}")
                } else {
                    append(" Missing one of: ")
                    append(violation.missingAnnotations.joinToString(", ") { "@${getSimpleName(it)}" })
                }
            }

            holder.registerProblem(
                violation.sourceAnnotation,
                message,
                highlightType,
                *fixes.toTypedArray()
            )
        }
    }

    private fun getSimpleName(qualifiedName: String): String {
        return qualifiedName.substringAfterLast('.')
    }
}

class AddMissingAnnotationQuickFix(
    private val element: PsiElement,
    private val annotationTemplate: AnnotationTemplate,
    private val context: DependencyContext
) : LocalQuickFix {

    override fun getName(): String {
        val simpleName = annotationTemplate.qualifiedName.substringAfterLast('.')
        return "Add @$simpleName annotation"
    }

    override fun getFamilyName(): String = "Add missing annotation"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val elementFactory = JavaPsiFacade.getElementFactory(project)
        
        val targetElement = when (annotationTemplate.target) {
            AnnotationTarget.FIELD -> element as? PsiField
            AnnotationTarget.CLASS -> context.containingClass
            AnnotationTarget.METHOD -> element as? PsiMethod
        } ?: return

        // Build annotation text
        val annotationText = buildAnnotationText()
        
        try {
            val annotation = elementFactory.createAnnotationFromText(annotationText, targetElement)
            val modifierList = targetElement.modifierList
            
            if (modifierList != null) {
                modifierList.addBefore(annotation, modifierList.firstChild)
            } else {
                // If no modifier list exists, we need to create one
                when (targetElement) {
                    is PsiField -> {
                        val newModifierList = elementFactory.createFieldFromText("${annotationText} int dummy;", null).modifierList
                        if (newModifierList != null) {
                            targetElement.addBefore(newModifierList, targetElement.firstChild)
                        }
                    }
                    is PsiClass -> {
                        targetElement.addBefore(annotation, targetElement.firstChild)
                    }
                    is PsiMethod -> {
                        val newModifierList = elementFactory.createMethodFromText("${annotationText} void dummy() {}", null).modifierList
                        if (newModifierList != null) {
                            targetElement.addBefore(newModifierList, targetElement.firstChild)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback: just add the annotation name without parameters
            val simpleAnnotation = elementFactory.createAnnotationFromText("@${annotationTemplate.qualifiedName}", targetElement)
            targetElement.modifierList?.addBefore(simpleAnnotation, targetElement.modifierList?.firstChild)
        }
    }

    private fun buildAnnotationText(): String {
        val simpleName = annotationTemplate.qualifiedName.substringAfterLast('.')
        
        return if (annotationTemplate.defaultValues.isEmpty()) {
            "@$simpleName"
        } else {
            val params = annotationTemplate.defaultValues.entries.joinToString(", ") { (key, value) ->
                "$key = $value"
            }
            "@$simpleName($params)"
        }
    }
}
