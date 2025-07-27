package com.lowdragmc.ldlibdevplugin.annotation.dependency

import com.intellij.psi.*

/**
 * Utility class for checking annotation dependencies
 */
object AnnotationDependencyChecker {

    /**
     * Check all dependencies for a given element
     */
    fun checkDependencies(element: PsiElement): List<DependencyViolation> {
        val violations = mutableListOf<DependencyViolation>()
        
        when (element) {
            is PsiField -> violations.addAll(checkFieldDependencies(element))
            is PsiClass -> violations.addAll(checkClassDependencies(element))
            is PsiMethod -> violations.addAll(checkMethodDependencies(element))
        }
        
        return violations
    }

    private fun checkFieldDependencies(field: PsiField): List<DependencyViolation> {
        val violations = mutableListOf<DependencyViolation>()
        val context = DependencyContext(
            element = field,
            target = AnnotationTarget.FIELD,
            containingClass = field.containingClass
        )

        field.annotations.forEach { annotation ->
            val qualifiedName = annotation.qualifiedName ?: return@forEach
            val dependencies = AnnotationDependencyRegistry.getDependenciesFor(qualifiedName)
            
            dependencies.forEach { dependency ->
                val violation = checkDependency(field, annotation, dependency, context)
                if (violation != null) {
                    violations.add(violation)
                }
            }
        }

        return violations
    }

    private fun checkClassDependencies(clazz: PsiClass): List<DependencyViolation> {
        val violations = mutableListOf<DependencyViolation>()
        val context = DependencyContext(
            element = clazz,
            target = AnnotationTarget.CLASS,
            containingClass = clazz
        )

        clazz.annotations.forEach { annotation ->
            val qualifiedName = annotation.qualifiedName ?: return@forEach
            val dependencies = AnnotationDependencyRegistry.getDependenciesFor(qualifiedName)
            
            dependencies.forEach { dependency ->
                val violation = checkDependency(clazz, annotation, dependency, context)
                if (violation != null) {
                    violations.add(violation)
                }
            }
        }

        return violations
    }

    private fun checkMethodDependencies(method: PsiMethod): List<DependencyViolation> {
        val violations = mutableListOf<DependencyViolation>()
        val context = DependencyContext(
            element = method,
            target = AnnotationTarget.METHOD,
            containingClass = method.containingClass
        )

        method.annotations.forEach { annotation ->
            val qualifiedName = annotation.qualifiedName ?: return@forEach
            val dependencies = AnnotationDependencyRegistry.getDependenciesFor(qualifiedName)
            
            dependencies.forEach { dependency ->
                val violation = checkDependency(method, annotation, dependency, context)
                if (violation != null) {
                    violations.add(violation)
                }
            }
        }

        return violations
    }

    private fun checkDependency(
        element: PsiElement,
        sourceAnnotation: PsiAnnotation,
        dependency: AnnotationDependency,
        context: DependencyContext
    ): DependencyViolation? {
        val missingAnnotations = mutableListOf<String>()

        // Check if at least one of the required annotations is present
        val hasAnyRequired = dependency.requiredAnnotations.any { requiredAnnotation ->
            hasRequiredAnnotation(element, requiredAnnotation, context)
        }

        if (!hasAnyRequired) {
            missingAnnotations.addAll(dependency.requiredAnnotations)
        }

        return if (missingAnnotations.isNotEmpty()) {
            DependencyViolation(
                dependency = dependency,
                missingAnnotations = missingAnnotations,
                context = context,
                sourceAnnotation = sourceAnnotation
            )
        } else {
            null
        }
    }

    private fun hasRequiredAnnotation(
        element: PsiElement,
        requiredAnnotation: String,
        context: DependencyContext
    ): Boolean {
        return when (context.target) {
            AnnotationTarget.FIELD -> {
                (element as? PsiField)?.hasAnnotation(requiredAnnotation) == true ||
                        context.containingClass?.hasAnnotation(requiredAnnotation) == true
            }
            AnnotationTarget.CLASS -> {
                (element as? PsiClass)?.hasAnnotation(requiredAnnotation) == true
            }
            AnnotationTarget.METHOD -> {
                (element as? PsiMethod)?.hasAnnotation(requiredAnnotation) == true ||
                        context.containingClass?.hasAnnotation(requiredAnnotation) == true
            }
        }
    }
}
