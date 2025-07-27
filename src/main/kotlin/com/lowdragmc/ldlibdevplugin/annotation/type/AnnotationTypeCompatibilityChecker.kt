
package com.lowdragmc.ldlibdevplugin.annotation.type

import com.intellij.psi.*
import com.lowdragmc.ldlibdevplugin.annotation.AnnotationUtils

/**
 * Utility for checking annotation field type compatibility
 */
object AnnotationTypeChecker {

    private val typeRules = mapOf(
        "com.lowdragmc.lowdraglib2.configurator.annotation.ConfigColor" to AnnotationUtils::isIntType,
        "com.lowdragmc.lowdraglib2.configurator.annotation.ConfigHDR" to AnnotationUtils::isVector4fType,
        "com.lowdragmc.lowdraglib2.configurator.annotation.ConfigList" to AnnotationUtils::isArrayOrCollection,
        "com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSelector" to AnnotationUtils::isEnumOrStringType
    )

    /**
     * Check if the field type is compatible with the annotation
     */
    fun checkFieldType(annotation: PsiAnnotation, field: PsiField): TypeViolation? {
        val annotationName = annotation.qualifiedName ?: return null
        val typeChecker = typeRules[annotationName] ?: return null
        
        if (!typeChecker(field.type)) {
            return TypeViolation(
                annotation = annotation,
                field = field,
                expectedType = getExpectedTypeDescription(annotationName),
                actualType = field.type.presentableText
            )
        }
        
        return null
    }

    /**
     * Get all type violations for a field
     */
    fun checkAllFieldTypes(field: PsiField): List<TypeViolation> {
        val violations = mutableListOf<TypeViolation>()
        
        field.annotations.forEach { annotation ->
            checkFieldType(annotation, field)?.let { violation ->
                violations.add(violation)
            }
        }
        
        return violations
    }

    private fun getExpectedTypeDescription(annotationName: String): String {
        return when (annotationName) {
            "com.lowdragmc.lowdraglib2.configurator.annotation.ConfigColor" -> "int or Integer"
            "com.lowdragmc.lowdraglib2.configurator.annotation.ConfigHDR" -> "Vector4f"
            "com.lowdragmc.lowdraglib2.configurator.annotation.ConfigList" -> "array or Collection"
            "com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSelector" -> "enum or String"
            else -> "compatible type"
        }
    }
}

/**
 * Represents a type compatibility violation
 */
data class TypeViolation(
    val annotation: PsiAnnotation,
    val field: PsiField,
    val expectedType: String,
    val actualType: String
)
