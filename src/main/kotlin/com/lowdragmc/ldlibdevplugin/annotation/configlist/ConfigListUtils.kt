package com.lowdragmc.ldlibdevplugin.annotation.configlist

import com.intellij.psi.*
import com.intellij.psi.util.InheritanceUtil
import com.lowdragmc.ldlibdevplugin.annotation.AnnotationUtils

object ConfigListUtils {

    const val CONFIG_LIST_ANNOTATION = "com.lowdragmc.lowdraglib2.configurator.annotation.ConfigList"
    const val CONFIGURATOR_METHOD = "configuratorMethod"
    const val ADD_DEFAULT_METHOD = "addDefaultMethod"

    fun isAnnotatedField(field: PsiField): Boolean {
        return field.hasAnnotation(CONFIG_LIST_ANNOTATION)
    }

    /**
     * Check if field type is valid for @ConfigList (array or Collection)
     */
    fun isValidFieldType(field: PsiField): Boolean {
        return AnnotationUtils.isArrayOrCollection(field.type)
    }

    /**
     * Get element type from field (array component type or Collection generic type)
     */
    fun getElementType(field: PsiField): PsiType? {
        return AnnotationUtils.getElementType(field.type)
    }

    fun getConfiguratorMethod(field: PsiField): String? {
        val annotation = field.getAnnotation(CONFIG_LIST_ANNOTATION) ?: return null
        return AnnotationUtils.getMethodName(annotation, CONFIGURATOR_METHOD)
    }

    fun getAddDefaultMethod(field: PsiField): String? {
        val annotation = field.getAnnotation(CONFIG_LIST_ANNOTATION) ?: return null
        return AnnotationUtils.getMethodName(annotation, ADD_DEFAULT_METHOD)
    }

    /**
     * Validate configurator method signature:
     * Configurator method(Supplier<elementType> getter, Consumer<elementType> setter)
     */
    fun isValidConfiguratorMethod(method: PsiMethod, elementType: PsiType): Boolean {
        // Method cannot be static
        if (method.hasModifierProperty(PsiModifier.STATIC)) return false

        val parameters = method.parameterList.parameters
        if (parameters.size != 2) return false

        val returnType = method.returnType ?: return false

        // Check if return type is Configurator or its subclass
        val isConfiguratorReturn = when (returnType) {
            is PsiClassType -> {
                val psiClass = returnType.resolve() ?: return false
                InheritanceUtil.isInheritor(psiClass, "com.lowdragmc.lowdraglib2.configurator.ui.Configurator") ||
                        psiClass.qualifiedName == "com.lowdragmc.lowdraglib2.configurator.ui.Configurator"
            }
            else -> false
        }

        if (!isConfiguratorReturn) return false

        // Check first parameter: Supplier<elementType>
        val supplierParam = parameters[0].type
        val isValidSupplier = when (supplierParam) {
            is PsiClassType -> {
                val psiClass = supplierParam.resolve() ?: return false
                if (psiClass.qualifiedName != "java.util.function.Supplier") return false
                
                val typeArgs = supplierParam.parameters
                if (typeArgs.size != 1) return false
                
                AnnotationUtils.isTypeCompatible(typeArgs[0], elementType)
            }
            else -> false
        }

        if (!isValidSupplier) return false

        // Check second parameter: Consumer<elementType>
        val consumerParam = parameters[1].type
        val isValidConsumer = when (consumerParam) {
            is PsiClassType -> {
                val psiClass = consumerParam.resolve() ?: return false
                if (psiClass.qualifiedName != "java.util.function.Consumer") return false
                
                val typeArgs = consumerParam.parameters
                if (typeArgs.size != 1) return false
                
                AnnotationUtils.isTypeCompatible(typeArgs[0], elementType)
            }
            else -> false
        }

        return isValidConsumer
    }

    /**
     * Validate add default method signature:
     * elementType method()
     */
    fun isValidAddDefaultMethod(method: PsiMethod, elementType: PsiType): Boolean {
        // Method cannot be static
        if (method.hasModifierProperty(PsiModifier.STATIC)) return false

        val parameters = method.parameterList.parameters
        if (parameters.isNotEmpty()) return false

        val returnType = method.returnType ?: return false
        return AnnotationUtils.isTypeCompatible(returnType, elementType)
    }

    fun isConfiguratorMethod(method: PsiMethod, field: PsiField): Boolean {
        val expectedMethodName = getConfiguratorMethod(field) ?: return false
        if (method.name != expectedMethodName) return false
        
        val elementType = getElementType(field) ?: return false
        return isValidConfiguratorMethod(method, elementType)
    }

    fun isAddDefaultMethod(method: PsiMethod, field: PsiField): Boolean {
        val expectedMethodName = getAddDefaultMethod(field) ?: return false
        if (method.name != expectedMethodName) return false
        
        val elementType = getElementType(field) ?: return false
        return isValidAddDefaultMethod(method, elementType)
    }

    fun findConfigListField(method: PsiMethod, containingClass: PsiClass): PsiField? {
        return containingClass.fields.find { field ->
            isAnnotatedField(field) && isValidFieldType(field) && (
                    isConfiguratorMethod(method, field) ||
                    isAddDefaultMethod(method, field)
                    )
        }
    }
}