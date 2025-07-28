
package com.lowdragmc.ldlibdevplugin.annotation.configselector

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.lowdragmc.ldlibdevplugin.annotation.AbstractAnnotationMethodInspection

private val SUB_CONFIGURATOR_BUILDER_ATTR = AbstractAnnotationMethodInspection.MethodAttribute(
    ConfigSelectorUtils.SUB_CONFIGURATOR_BUILDER, 
    "sub configurator builder method", 
    "sub configurator builder"
)

class ConfigSelectorMethodMissingInspection : AbstractAnnotationMethodInspection() {

    override fun isAnnotatedField(field: PsiField): Boolean {
        return ConfigSelectorUtils.isAnnotatedField(field)
    }

    override fun getAnnotation(field: PsiField): PsiAnnotation? {
        return field.getAnnotation(ConfigSelectorUtils.CONFIG_SELECTOR_ANNOTATION)
    }

    override fun getMethodNameAttribute(annotation: PsiAnnotation, attributeName: String): PsiLiteralExpression? {
        return annotation.findAttributeValue(attributeName) as? PsiLiteralExpression
    }

    override fun getMethodName(field: PsiField, attributeName: String): String? {
        return when (attributeName) {
            ConfigSelectorUtils.SUB_CONFIGURATOR_BUILDER -> ConfigSelectorUtils.getSubConfiguratorBuilder(field)
            else -> null
        }
    }

    override fun isValidMethod(method: PsiMethod, fieldType: PsiType, methodAttribute: MethodAttribute): Boolean {
        return when (methodAttribute.attributeName) {
            ConfigSelectorUtils.SUB_CONFIGURATOR_BUILDER -> 
                ConfigSelectorUtils.isValidSubConfiguratorBuilderMethod(method, fieldType)
            else -> false
        }
    }

    override fun hasCorrectParameterCount(method: PsiMethod, fieldType: PsiType, methodAttribute: MethodAttribute): Boolean {
        return when (methodAttribute.attributeName) {
            ConfigSelectorUtils.SUB_CONFIGURATOR_BUILDER -> {
                val parameters = method.parameterList.parameters
                parameters.size == 2 && validateSubConfiguratorBuilderParameters(parameters, fieldType)
            }
            else -> false
        }
    }

    private fun validateSubConfiguratorBuilderParameters(parameters: Array<PsiParameter>, fieldType: PsiType): Boolean {
        if (parameters.size != 2) return false
        
        // Validate first parameter: fieldType or its parent class
        val firstParamValid = parameters[0].type.let { paramType ->
            paramType == fieldType || fieldType.isAssignableFrom(paramType)
        }
        
        // Validate second parameter: ConfiguratorGroup or its subclass
        val secondParamValid = parameters[1].type.let { paramType ->
            (paramType as? PsiClassType)?.let { classType ->
                val psiClass = classType.resolve() ?: return false
                psiClass.qualifiedName == "com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup" ||
                        com.intellij.psi.util.InheritanceUtil.isInheritor(
                            psiClass, 
                            "com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup"
                        )
            } == true
        }
        
        return firstParamValid && secondParamValid
    }

    override fun getAnnotationName(): String = "ConfigSelector"

    override fun getMethodAttributes(): List<MethodAttribute> {
        return listOf(SUB_CONFIGURATOR_BUILDER_ATTR)
    }

    override fun createMethodQuickFix(methodName: String, fieldType: PsiType, fieldName: String, methodAttribute: MethodAttribute): LocalQuickFix {
        return when (methodAttribute.attributeName) {
            ConfigSelectorUtils.SUB_CONFIGURATOR_BUILDER -> 
                CreateSubConfiguratorBuilderMethodQuickFix(methodName, fieldType, fieldName)
            else -> throw IllegalArgumentException("Unknown method attribute: ${methodAttribute.attributeName}")
        }
    }
}

class CreateSubConfiguratorBuilderMethodQuickFix(
    private val methodName: String,
    private val fieldType: PsiType,
    private val fieldName: String
) : LocalQuickFix {

    override fun getName(): String = "Create sub configurator builder method '$methodName'"

    override fun getFamilyName(): String = "Create ConfigSelector sub configurator builder method"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement
        val containingClass = PsiTreeUtil.getParentOfType(element, PsiClass::class.java) ?: return

        val elementFactory = JavaPsiFacade.getElementFactory(project)
        val fieldTypeName = fieldType.presentableText

        val methodText = """
            private void $methodName($fieldTypeName value, com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup group) {
                // TODO: Implement $fieldName sub configurator builder
            }
        """.trimIndent()

        val method = elementFactory.createMethodFromText(methodText, containingClass)
        containingClass.add(method)
    }
}