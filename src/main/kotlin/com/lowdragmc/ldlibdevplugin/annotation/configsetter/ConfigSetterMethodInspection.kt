package com.lowdragmc.ldlibdevplugin.annotation.configsetter

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.*

class ConfigSetterMethodInspection : AbstractBaseJavaLocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitMethod(method: PsiMethod) {
                super.visitMethod(method)

                if (!ConfigSetterUtils.isAnnotatedMethod(method)) return

                val annotation = method.getAnnotation(ConfigSetterUtils.CONFIG_SETTER_ANNOTATION) ?: return
                val fieldNameAttr = annotation.findAttributeValue(ConfigSetterUtils.FIELD_ATTRIBUTE) as? PsiLiteralExpression ?: return
                val fieldName = ConfigSetterUtils.getFieldName(method) ?: return

                val containingClass = method.containingClass ?: return

                // find fields（won't check @Configurable）
                val fieldWithoutConfigurableCheck = ConfigSetterUtils.findFieldWithoutConfigurableCheck(method, containingClass)
                val targetField = ConfigSetterUtils.findConfigSetterField(method, containingClass)

                when {
                    targetField == null && fieldWithoutConfigurableCheck != null -> {
                        // field exists without @Configurable
                        holder.registerProblem(
                            fieldNameAttr,
                            "ConfigSetter field '$fieldName' is not annotated with @Configurable",
                            ProblemHighlightType.ERROR,
                            AddConfigurableAnnotationQuickFix(fieldWithoutConfigurableCheck)
                        )
                    }
                    fieldWithoutConfigurableCheck == null -> {
                        // field non exists
                        holder.registerProblem(
                            fieldNameAttr,
                            "ConfigSetter field '$fieldName' not found",
                            ProblemHighlightType.ERROR
                        )
                    }
                    targetField != null && !ConfigSetterUtils.isValidConfigSetterMethod(method, targetField.type) -> {
                        // field exists and with @Configurable，but parameters are incorrect
                        val errorMessage = when {
                            method.hasModifierProperty(PsiModifier.STATIC) -> "ConfigSetter method '${method.name}' cannot be static"
                            method.parameterList.parameters.size != 1 -> "ConfigSetter method '${method.name}' must have exactly one parameter"
                            else -> "ConfigSetter method '${method.name}' parameter type is incompatible with field type"
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

class RemoveStaticModifierQuickFix(
    private val method: PsiMethod
) : LocalQuickFix {

    override fun getName(): String = "Remove static modifier from method '${method.name}'"

    override fun getFamilyName(): String = "Remove static modifier"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val modifierList = method.modifierList
        modifierList.setModifierProperty(PsiModifier.STATIC, false)
    }
}

class AddConfigurableAnnotationQuickFix(
    private val field: PsiField
) : LocalQuickFix {

    override fun getName(): String = "Add @Configurable annotation to field '${field.name}'"

    override fun getFamilyName(): String = "Add @Configurable annotation"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val elementFactory = JavaPsiFacade.getElementFactory(project)
        val annotation = elementFactory.createAnnotationFromText(
            "@${ConfigSetterUtils.CONFIGURABLE_ANNOTATION}",
            field
        )

        val modifierList = field.modifierList ?: return
        modifierList.addBefore(annotation, modifierList.firstChild)
    }
}