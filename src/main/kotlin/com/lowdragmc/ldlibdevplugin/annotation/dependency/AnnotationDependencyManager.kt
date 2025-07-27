package com.lowdragmc.ldlibdevplugin.annotation.dependency

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField

/**
 * Represents a dependency relationship between annotations
 */
data class AnnotationDependency(
    val sourceAnnotation: String,
    val requiredAnnotations: List<String>,
    val description: String,
    val severity: DependencySeverity = DependencySeverity.ERROR
)

enum class DependencySeverity {
    ERROR, WARNING, INFO
}

/**
 * Represents the target where an annotation can be applied
 */
enum class AnnotationTarget {
    FIELD, CLASS, METHOD
}

/**
 * Context information for dependency checking
 */
data class DependencyContext(
    val element: PsiElement,
    val target: AnnotationTarget,
    val containingClass: PsiClass?
)

/**
 * Result of dependency validation
 */
data class DependencyViolation(
    val dependency: AnnotationDependency,
    val missingAnnotations: List<String>,
    val context: DependencyContext,
    val sourceAnnotation: PsiAnnotation
)

/**
 * Configuration for adding missing annotations
 */
data class AnnotationTemplate(
    val qualifiedName: String,
    val defaultValues: Map<String, Any> = emptyMap(),
    val target: AnnotationTarget
)
