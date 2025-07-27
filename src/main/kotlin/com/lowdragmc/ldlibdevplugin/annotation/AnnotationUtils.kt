package com.lowdragmc.ldlibdevplugin.annotation

import com.intellij.psi.*
import com.intellij.psi.util.InheritanceUtil

object AnnotationUtils {
    fun getMethodName(annotation: PsiAnnotation, fieldName: String): String? {
        val methodNameAttr = annotation.findAttributeValue(fieldName) as? PsiLiteralExpression
        return methodNameAttr?.value as? String
    }

    fun isTypeCompatible(paramType: PsiType, fieldType: PsiType): Boolean {
        return paramType == fieldType || paramType.isAssignableFrom(fieldType)
    }

    fun isBooleanType(type: PsiType): Boolean {
        return type == PsiTypes.booleanType() ||
                (type is PsiClassType &&
                        type.resolve()?.qualifiedName == "java.lang.Boolean")
    }

    fun isIntType(type: PsiType): Boolean {
        return type == PsiTypes.intType() ||
                (type is PsiClassType &&
                        type.resolve()?.qualifiedName == "java.lang.Integer")
    }

    fun isStringType(type: PsiType): Boolean {
        return type is PsiClassType &&
                type.resolve()?.qualifiedName == "java.lang.String"
    }

    fun isEnumType(type: PsiType): Boolean {
        return type is PsiClassType &&
                type.resolve()?.isEnum == true
    }

    fun isVector4fType(type: PsiType): Boolean {
        return type is PsiClassType &&
                type.resolve()?.qualifiedName == "org.joml.Vector4f"
    }

    fun isEnumOrStringType(type: PsiType): Boolean {
        return when (type) {
            is PsiClassType -> {
                val psiClass = type.resolve() ?: return false
                psiClass.isEnum ||
                        psiClass.qualifiedName == "java.lang.String"
            }
            else -> false
        }
    }

    /**
     * Extract element type from array or Collection type
     * For arrays: T[] -> T
     * For Collection<T>: Collection<T> -> T
     * Returns null if not an array or Collection
     */
    fun getElementType(type: PsiType): PsiType? {
        return when (type) {
            is PsiArrayType -> type.componentType
            is PsiClassType -> {
                val psiClass = type.resolve() ?: return null
                // Check if it's a Collection or its subclass
                if (InheritanceUtil.isInheritor(psiClass, "java.util.Collection")) {
                    val typeParameters = type.parameters
                    if (typeParameters.isNotEmpty()) {
                        typeParameters[0]
                    } else null
                } else null
            }
            else -> null
        }
    }

    /**
     * Check if the type is an array or Collection
     */
    fun isArrayOrCollection(type: PsiType): Boolean {
        return when (type) {
            is PsiArrayType -> true
            is PsiClassType -> {
                val psiClass = type.resolve() ?: return false
                InheritanceUtil.isInheritor(psiClass, "java.util.Collection")
            }
            else -> false
        }
    }

    /**
     * Get a human-readable description of the type
     */
    fun getTypeDescription(type: PsiType): String {
        return when (type) {
            is PsiArrayType -> "${type.componentType.presentableText}[]"
            is PsiClassType -> {
                val className = type.resolve()?.name ?: type.presentableText
                when {
                    type.parameters.isNotEmpty() -> {
                        val params = type.parameters.joinToString(", ") { it.presentableText }
                        "$className<$params>"
                    }
                    else -> className
                }
            }
            else -> type.presentableText
        }
    }
}