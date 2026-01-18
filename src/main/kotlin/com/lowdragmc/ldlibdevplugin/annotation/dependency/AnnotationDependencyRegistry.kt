package com.lowdragmc.ldlibdevplugin.annotation.dependency

import com.lowdragmc.ldlibdevplugin.annotation.lang.LangUtils

/**
 * Central registry for annotation dependencies
 */
object AnnotationDependencyRegistry {
    private val dependencies = mutableListOf<AnnotationDependency>()
    private val annotationTemplates = mutableMapOf<String, AnnotationTemplate>()

    init {
        registerDependencies()
        registerAnnotationTemplates()
    }

    private fun registerDependencies() {
        registerConfigurableDependencies("com.lowdragmc.lowdraglib2.configurator.annotation.ConfigList")
        registerConfigurableDependencies("com.lowdragmc.lowdraglib2.configurator.annotation.ConfigColor")
        registerConfigurableDependencies("com.lowdragmc.lowdraglib2.configurator.annotation.ConfigHDR")
        registerConfigurableDependencies("com.lowdragmc.lowdraglib2.configurator.annotation.ConfigList")
        registerConfigurableDependencies("com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber")
        registerConfigurableDependencies("com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSelector")
        registerConfigurableDependencies("com.lowdragmc.lowdraglib2.configurator.annotation.DefaultValue")

        addDependency(
            "com.lowdragmc.lowdraglib2.syncdata.annotation.UpdateListener",
            listOf(
                "com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced"
            ),
            "Fields with @UpdateListener must also have @DescSynced",
            DependencySeverity.ERROR
        )

        addDependency(
            "com.lowdragmc.lowdraglib2.syncdata.annotation.ConditionalSynced",
            listOf(
                "com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced"
            ),
            "Fields with @ConditionalSynced must also have @DescSynced",
            DependencySeverity.ERROR
        )

        addDependency(
            "com.lowdragmc.lowdraglib2.syncdata.annotation.RequireRerender",
            listOf(
                "com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced"
            ),
            "Fields with @RequireRerender must also have @DescSynced",
            DependencySeverity.ERROR
        )
    }

    private fun registerConfigurableDependencies(annotation: String, severity: DependencySeverity = DependencySeverity.ERROR) {
        // If a field has @annotation, it should also have @Configurable
        val simpleName = annotation.substringAfterLast('.')
        addDependency(
            annotation,
            listOf(LangUtils.CONFIGURABLE_ANNOTATION),
            "Fields with @$simpleName must also be annotated with @Configurable",
            severity
        )
    }

    private fun registerAnnotationTemplates() {
        // Register templates for automatic annotation generation
        registerTemplate(
            LangUtils.CONFIGURABLE_ANNOTATION,
            mapOf("name" to "\"\""),
            AnnotationTarget.FIELD
        )

        registerTemplate(
            "com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced",
            emptyMap(),
            AnnotationTarget.FIELD
        )
    }

    fun addDependency(
        sourceAnnotation: String,
        requiredAnnotations: List<String>,
        description: String,
        severity: DependencySeverity = DependencySeverity.ERROR
    ) {
        dependencies.add(
            AnnotationDependency(
                sourceAnnotation,
                requiredAnnotations,
                description,
                severity
            )
        )
    }

    fun registerTemplate(
        qualifiedName: String,
        defaultValues: Map<String, Any>,
        target: AnnotationTarget
    ) {
        annotationTemplates[qualifiedName] = AnnotationTemplate(
            qualifiedName,
            defaultValues,
            target
        )
    }

    fun getDependencies(): List<AnnotationDependency> = dependencies.toList()

    fun getDependenciesFor(annotationName: String): List<AnnotationDependency> {
        return dependencies.filter { it.sourceAnnotation == annotationName }
    }

    fun getTemplate(qualifiedName: String): AnnotationTemplate? {
        return annotationTemplates[qualifiedName]
    }

    fun getAllTemplates(): Map<String, AnnotationTemplate> = annotationTemplates.toMap()
}
