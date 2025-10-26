package com.lowdragmc.ldlibdevplugin.annotation.configsearch

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.lowdragmc.ldlibdevplugin.annotation.AbstractAnnotationMethodInspection

private val SEARCH_CONFIGURATOR_METHOD_ATTR = AbstractAnnotationMethodInspection.MethodAttribute(
    ConfigSearchUtils.SEARCH_CONFIGURATOR_METHOD, 
    "search configurator method", 
    "search configurator"
)

class ConfigSearchMethodMissingInspection : AbstractAnnotationMethodInspection() {

    override fun isAnnotatedField(field: PsiField): Boolean {
        return ConfigSearchUtils.isAnnotatedField(field)
    }

    override fun getAnnotation(field: PsiField): PsiAnnotation? {
        return field.getAnnotation(ConfigSearchUtils.CONFIG_SEARCH_ANNOTATION)
    }

    override fun getMethodNameAttribute(annotation: PsiAnnotation, attributeName: String): PsiLiteralExpression? {
        return annotation.findAttributeValue(attributeName) as? PsiLiteralExpression
    }

    override fun getMethodName(field: PsiField, attributeName: String): String? {
        return when (attributeName) {
            ConfigSearchUtils.SEARCH_CONFIGURATOR_METHOD -> ConfigSearchUtils.getSearchConfiguratorMethod(field)
            else -> null
        }
    }

    override fun isValidMethod(method: PsiMethod, fieldType: PsiType, methodAttribute: MethodAttribute): Boolean {
        return when (methodAttribute.attributeName) {
            ConfigSearchUtils.SEARCH_CONFIGURATOR_METHOD -> 
                ConfigSearchUtils.isValidSearchConfiguratorMethod(method)
            else -> false
        }
    }

    override fun hasCorrectParameterCount(method: PsiMethod, fieldType: PsiType, methodAttribute: MethodAttribute): Boolean {
        return when (methodAttribute.attributeName) {
            ConfigSearchUtils.SEARCH_CONFIGURATOR_METHOD -> {
                // Search configurator method should have no parameters
                method.parameterList.parameters.isEmpty()
            }
            else -> false
        }
    }

    override fun getAnnotationName(): String = "ConfigSearch"

    override fun getMethodAttributes(): List<MethodAttribute> {
        return listOf(SEARCH_CONFIGURATOR_METHOD_ATTR)
    }

    override fun createMethodQuickFix(methodName: String, fieldType: PsiType, fieldName: String, methodAttribute: MethodAttribute): LocalQuickFix {
        return when (methodAttribute.attributeName) {
            ConfigSearchUtils.SEARCH_CONFIGURATOR_METHOD -> 
                CreateSearchConfiguratorMethodQuickFix(methodName, fieldName)
            else -> throw IllegalArgumentException("Unknown method attribute: ${methodAttribute.attributeName}")
        }
    }
}

class CreateSearchConfiguratorMethodQuickFix(
    private val methodName: String,
    private val fieldName: String
) : LocalQuickFix {

    override fun getName(): String = "Create search configurator method '$methodName'"

    override fun getFamilyName(): String = "Create ConfigSearch search configurator method"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement
        val containingClass = PsiTreeUtil.getParentOfType(element, PsiClass::class.java) ?: return

        val elementFactory = JavaPsiFacade.getElementFactory(project)

        val methodText = """
            private com.lowdragmc.lowdraglib2.configurator.ui.SearchComponentConfigurator.ISearchConfigurator $methodName() {
                // TODO: Implement $fieldName search configurator
                return null;
            }
        """.trimIndent()

        val method = elementFactory.createMethodFromText(methodText, containingClass)
        containingClass.add(method)
    }
}
