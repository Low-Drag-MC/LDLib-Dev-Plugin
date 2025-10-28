package com.lowdragmc.ldlibdevplugin.annotation.skippersistedvalue

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.lowdragmc.ldlibdevplugin.annotation.AnnotationUtils

class SkipPersistedValueMethodInspection : AbstractBaseJavaLocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitMethod(method: PsiMethod) {
                super.visitMethod(method)

                if (!SkipPersistedValueUtils.isAnnotatedMethod(method)) return

                val annotation = method.getAnnotation(SkipPersistedValueUtils.SKIP_PERSISTED_VALUE_ANNOTATION) ?: return
                val fieldNameAttr = annotation.findAttributeValue(SkipPersistedValueUtils.FIELD_ATTRIBUTE) as? PsiLiteralExpression ?: return
                val fieldName = SkipPersistedValueUtils.getFieldName(method) ?: return

                val containingClass = method.containingClass ?: return

                // Find fields
                val fieldWithoutConfigurableCheck = SkipPersistedValueUtils.findFieldWithoutConfigurableCheck(method, containingClass)
                val targetField = SkipPersistedValueUtils.findSkipPersistedValueField(method, containingClass)

                when {
                    targetField == null && fieldWithoutConfigurableCheck != null -> {
                        // Field exists but doesn't have @Configurable
                        holder.registerProblem(
                            fieldNameAttr,
                            "SkipPersistedValue field '$fieldName' is not annotated with @Configurable",
                            ProblemHighlightType.ERROR,
                            AddConfigurableAnnotationQuickFix(fieldWithoutConfigurableCheck)
                        )
                    }
                    fieldWithoutConfigurableCheck == null -> {
                        // Field doesn't exist
                        holder.registerProblem(
                            fieldNameAttr,
                            "SkipPersistedValue field '$fieldName' not found",
                            ProblemHighlightType.ERROR
                        )
                    }
                    targetField != null && !SkipPersistedValueUtils.isValidSkipPersistedValueMethod(method, targetField.type) -> {
                        // Field exists with @Configurable, but method signature is incorrect
                        val errorMessage = when {
                            method.hasModifierProperty(PsiModifier.STATIC) -> 
                                "SkipPersistedValue method '${method.name}' cannot be static"
                            method.returnType?.let { 
                                !it.equalsToText("boolean") && it.canonicalText != "java.lang.Boolean" 
                            } == true -> 
                                "SkipPersistedValue method '${method.name}' must return boolean"
                            method.parameterList.parameters.size != 1 -> 
                                "SkipPersistedValue method '${method.name}' must have exactly one parameter"
                            else -> 
                                "SkipPersistedValue method '${method.name}' parameter type is incompatible with field type"
                        }

                        val fixes = mutableListOf<LocalQuickFix>()
                        if (method.hasModifierProperty(PsiModifier.STATIC)) {
                            fixes.add(RemoveStaticModifierQuickFix(method))
                        }

                        holder.registerProblem(
                            method.nameIdentifier ?: method,
                            errorMessage,
                            ProblemHighlightType.ERROR,
                            *fixes.toTypedArray()
                        )
                    }
                }
            }
        }
    }
}

class AddConfigurableAnnotationQuickFix(private val field: PsiField) : LocalQuickFix {
    override fun getName(): String = "Add @Configurable annotation to '${field.name}'"

    override fun getFamilyName(): String = "Add @Configurable annotation"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val elementFactory = JavaPsiFacade.getElementFactory(project)
        val annotation = elementFactory.createAnnotationFromText(
            "@${SkipPersistedValueUtils.CONFIGURABLE_ANNOTATION}",
            field
        )
        field.modifierList?.addBefore(annotation, field.modifierList?.firstChild)
    }
}

class RemoveStaticModifierQuickFix(private val method: PsiMethod) : LocalQuickFix {
    override fun getName(): String = "Remove static modifier from '${method.name}'"

    override fun getFamilyName(): String = "Remove static modifier"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        method.modifierList.setModifierProperty(PsiModifier.STATIC, false)
    }
}
