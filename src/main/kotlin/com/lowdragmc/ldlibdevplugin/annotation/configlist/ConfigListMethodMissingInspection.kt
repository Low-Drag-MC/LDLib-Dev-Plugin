package com.lowdragmc.ldlibdevplugin.annotation.configlist

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.lowdragmc.ldlibdevplugin.annotation.AbstractAnnotationMethodInspection
import com.lowdragmc.ldlibdevplugin.annotation.AnnotationUtils

private val CONFIGURATOR_METHOD_ATTR = AbstractAnnotationMethodInspection.MethodAttribute(
    ConfigListUtils.CONFIGURATOR_METHOD, 
    "configurator method", 
    "configurator"
)

private val ADD_DEFAULT_METHOD_ATTR = AbstractAnnotationMethodInspection.MethodAttribute(
    ConfigListUtils.ADD_DEFAULT_METHOD, 
    "add default method", 
    "add default"
)

class ConfigListMethodMissingInspection : AbstractAnnotationMethodInspection() {

    override fun isAnnotatedField(field: PsiField): Boolean {
        return ConfigListUtils.isAnnotatedField(field) && ConfigListUtils.isValidFieldType(field)
    }

    override fun getAnnotation(field: PsiField): PsiAnnotation? {
        return field.getAnnotation(ConfigListUtils.CONFIG_LIST_ANNOTATION)
    }

    override fun getMethodNameAttribute(annotation: PsiAnnotation, attributeName: String): PsiLiteralExpression? {
        return annotation.findAttributeValue(attributeName) as? PsiLiteralExpression
    }

    override fun getMethodName(field: PsiField, attributeName: String): String? {
        return when (attributeName) {
            ConfigListUtils.CONFIGURATOR_METHOD -> ConfigListUtils.getConfiguratorMethod(field)
            ConfigListUtils.ADD_DEFAULT_METHOD -> ConfigListUtils.getAddDefaultMethod(field)
            else -> null
        }
    }

    override fun isValidMethod(method: PsiMethod, fieldType: PsiType, methodAttribute: MethodAttribute): Boolean {
        val elementType = AnnotationUtils.getElementType(fieldType) ?: return false
        
        return when (methodAttribute.attributeName) {
            ConfigListUtils.CONFIGURATOR_METHOD -> ConfigListUtils.isValidConfiguratorMethod(method, elementType)
            ConfigListUtils.ADD_DEFAULT_METHOD -> ConfigListUtils.isValidAddDefaultMethod(method, elementType)
            else -> false
        }
    }

    override fun hasCorrectParameterCount(method: PsiMethod, fieldType: PsiType, methodAttribute: MethodAttribute): Boolean {
        val elementType = AnnotationUtils.getElementType(fieldType) ?: return false
        
        return when (methodAttribute.attributeName) {
            ConfigListUtils.CONFIGURATOR_METHOD -> {
                val parameters = method.parameterList.parameters
                parameters.size == 2 && validateConfiguratorParameters(parameters, elementType)
            }
            ConfigListUtils.ADD_DEFAULT_METHOD -> {
                method.parameterList.parameters.isEmpty() && 
                        method.returnType?.let { AnnotationUtils.isTypeCompatible(it, elementType) } == true
            }
            else -> false
        }
    }

    private fun validateConfiguratorParameters(parameters: Array<PsiParameter>, elementType: PsiType): Boolean {
        if (parameters.size != 2) return false
        
        // Validate Supplier<elementType> parameter
        val supplierValid = parameters[0].type.let { paramType ->
            (paramType as? PsiClassType)?.let { classType ->
                classType.resolve()?.qualifiedName == "java.util.function.Supplier" &&
                        classType.parameters.size == 1 &&
                        AnnotationUtils.isTypeCompatible(classType.parameters[0], elementType)
            } == true
        }
        
        // Validate Consumer<elementType> parameter
        val consumerValid = parameters[1].type.let { paramType ->
            (paramType as? PsiClassType)?.let { classType ->
                classType.resolve()?.qualifiedName == "java.util.function.Consumer" &&
                        classType.parameters.size == 1 &&
                        AnnotationUtils.isTypeCompatible(classType.parameters[0], elementType)
            } == true
        }
        
        return supplierValid && consumerValid
    }

    override fun getAnnotationName(): String = "ConfigList"

    override fun getMethodAttributes(): List<MethodAttribute> {
        return listOf(CONFIGURATOR_METHOD_ATTR, ADD_DEFAULT_METHOD_ATTR)
    }

    override fun createMethodQuickFix(methodName: String, fieldType: PsiType, fieldName: String, methodAttribute: MethodAttribute): LocalQuickFix {
        val elementType = AnnotationUtils.getElementType(fieldType) ?: return object : LocalQuickFix {
            override fun getName(): String = "Invalid field type - not array or Collection"
            override fun getFamilyName(): String = "Invalid field type"
            override fun applyFix(project: Project, descriptor: ProblemDescriptor) {}
        }
        
        return when (methodAttribute.attributeName) {
            ConfigListUtils.CONFIGURATOR_METHOD -> CreateConfiguratorMethodQuickFix(methodName, elementType, fieldName)
            ConfigListUtils.ADD_DEFAULT_METHOD -> CreateAddDefaultMethodQuickFix(methodName, elementType, fieldName)
            else -> throw IllegalArgumentException("Unknown method attribute: ${methodAttribute.attributeName}")
        }
    }
}

class CreateConfiguratorMethodQuickFix(
    private val methodName: String,
    private val elementType: PsiType,
    private val fieldName: String
) : LocalQuickFix {

    override fun getName(): String = "Create configurator method '$methodName'"

    override fun getFamilyName(): String = "Create ConfigList configurator method"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement
        val containingClass = PsiTreeUtil.getParentOfType(element, PsiClass::class.java) ?: return

        val elementFactory = JavaPsiFacade.getElementFactory(project)
        val elementTypeName = elementType.presentableText

        val methodText = """
            private com.lowdragmc.lowdraglib2.configurator.ui.Configurator $methodName(java.util.function.Supplier<$elementTypeName> getter, java.util.function.Consumer<$elementTypeName> setter) {
                // TODO: Implement $fieldName configurator method
                return null;
            }
        """.trimIndent()

        val method = elementFactory.createMethodFromText(methodText, containingClass)
        containingClass.add(method)
    }
}

class CreateAddDefaultMethodQuickFix(
    private val methodName: String,
    private val elementType: PsiType,
    private val fieldName: String
) : LocalQuickFix {

    override fun getName(): String = "Create add default method '$methodName'"

    override fun getFamilyName(): String = "Create ConfigList add default method"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement
        val containingClass = PsiTreeUtil.getParentOfType(element, PsiClass::class.java) ?: return

        val elementFactory = JavaPsiFacade.getElementFactory(project)
        val elementTypeName = elementType.presentableText

        val methodText = """
            private $elementTypeName $methodName() {
                // TODO: Implement $fieldName add default method
                return null;
            }
        """.trimIndent()

        val method = elementFactory.createMethodFromText(methodText, containingClass)
        containingClass.add(method)
    }
}