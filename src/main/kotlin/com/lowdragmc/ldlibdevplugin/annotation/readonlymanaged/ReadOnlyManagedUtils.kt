package com.lowdragmc.ldlibdevplugin.annotation.readonlymanaged

import com.intellij.psi.*
import com.intellij.psi.util.InheritanceUtil
import com.lowdragmc.ldlibdevplugin.annotation.AnnotationUtils

object ReadOnlyManagedUtils {

    const val READ_ONLY_MANAGED_ANNOTATION = "com.lowdragmc.lowdraglib2.syncdata.annotation.ReadOnlyManaged"
    const val ON_DIRTY_METHOD = "onDirtyMethod"
    const val SERIALIZE_METHOD = "serializeMethod"
    const val DESERIALIZE_METHOD = "deserializeMethod"

    fun isAnnotatedField(field: PsiField): Boolean {
        return field.hasAnnotation(READ_ONLY_MANAGED_ANNOTATION)
    }

    fun getOnDirtyMethod(field: PsiField): String? {
        val annotation = field.getAnnotation(READ_ONLY_MANAGED_ANNOTATION) ?: return null
        return AnnotationUtils.getMethodName(annotation, ON_DIRTY_METHOD);
    }

    fun isValidOnDirtyMethod(method: PsiMethod): Boolean {
        // method cannot be static
        if (method.hasModifierProperty(PsiModifier.STATIC)) return false

        val parameters = method.parameterList.parameters
        val returnType = method.returnType ?: return false
        
        val isBooleanReturn = AnnotationUtils.isBooleanType(returnType)
        return parameters.size == 0 && isBooleanReturn
    }

    fun isOnDirtyMethod(method: PsiMethod, field: PsiField): Boolean {
        val expectedMethodName = getOnDirtyMethod(field) ?: return false

        if (method.name != expectedMethodName) return false

        return isValidOnDirtyMethod(method)
    }
    
    fun getSerializeMethod(field: PsiField): String? {
        val annotation = field.getAnnotation(READ_ONLY_MANAGED_ANNOTATION) ?: return null
        return AnnotationUtils.getMethodName(annotation, SERIALIZE_METHOD);
    }

    fun isValidSerializeMethod(method: PsiMethod, fieldType: PsiType): Boolean {
        // method cannot be static
        if (method.hasModifierProperty(PsiModifier.STATIC)) return false

        val parameters = method.parameterList.parameters
        if (parameters.size != 1 || !AnnotationUtils.isTypeCompatible(
                parameters[0].type, fieldType)) return false
        val returnType = method.returnType ?: return false

        // Check if return type is Tag or its subclass
        return when (returnType) {
            is PsiClassType -> {
                val psiClass = returnType.resolve() ?: return false
                InheritanceUtil.isInheritor(psiClass, "net.minecraft.nbt.Tag")
            }
            else -> false
        }

    }

    fun isSerializeMethod(method: PsiMethod, field: PsiField): Boolean {
        val expectedMethodName = getSerializeMethod(field) ?: return false

        if (method.name != expectedMethodName) return false

        return isValidSerializeMethod(method, field.type)
    } 

    fun getDeserializeMethod(field: PsiField): String? {
        val annotation = field.getAnnotation(READ_ONLY_MANAGED_ANNOTATION) ?: return null
        return AnnotationUtils.getMethodName(annotation, DESERIALIZE_METHOD);
    }

    fun isValidDeserializeMethod(method: PsiMethod, fieldType: PsiType): Boolean {
        // method cannot be static
        if (method.hasModifierProperty(PsiModifier.STATIC)) return false

        val parameters = method.parameterList.parameters
        if (parameters.size != 1) return false

        // Check if the parameter is Tag or its subclass
        val paramType = parameters[0].type
        val isTagParameter = when (paramType) {
            is PsiClassType -> {
                val psiClass = paramType.resolve() ?: return false
                InheritanceUtil.isInheritor(psiClass, "net.minecraft.nbt.Tag")
            }
            else -> false
        }

        if (!isTagParameter) return false
        
        val returnType = method.returnType ?: return false
        return AnnotationUtils.isTypeCompatible(returnType, fieldType)
    }

    fun isDeserializeMethod(method: PsiMethod, field: PsiField): Boolean {
        val expectedMethodName = getDeserializeMethod(field) ?: return false

        if (method.name != expectedMethodName) return false

        return isValidDeserializeMethod(method, field.type)
    }

    fun findReadOnlyManagedField(method: PsiMethod, containingClass: PsiClass): PsiField? {
        return containingClass.fields.find { field ->
            isAnnotatedField(field) && (
                    isOnDirtyMethod(method, field) ||
                    isSerializeMethod(method, field) ||
                            isDeserializeMethod(method, field)
                    )
        }
    }
}