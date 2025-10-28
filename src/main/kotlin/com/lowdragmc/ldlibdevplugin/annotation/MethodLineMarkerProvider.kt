package com.lowdragmc.ldlibdevplugin.annotation

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiField
import com.lowdragmc.ldlibdevplugin.annotation.configlist.ConfigListUtils
import com.lowdragmc.ldlibdevplugin.annotation.configsearch.ConfigSearchUtils
import com.lowdragmc.ldlibdevplugin.annotation.configselector.ConfigSelectorUtils
import com.lowdragmc.ldlibdevplugin.annotation.configsetter.ConfigSetterUtils
import com.lowdragmc.ldlibdevplugin.annotation.readonlymanaged.ReadOnlyManagedUtils
import com.lowdragmc.ldlibdevplugin.annotation.updatelistener.UpdateListenerUtils
import com.lowdragmc.ldlibdevplugin.annotation.skippersistedvalue.SkipPersistedValueUtils

class MethodLineMarkerProvider : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (element !is PsiIdentifier) return null

        when (val parent = element.parent) {
            is PsiMethod -> {
                val containingClass = parent.containingClass ?: return null

                // find the target field with @UpdateListener
                val updateListenerField = UpdateListenerUtils.findUpdateListenerField(parent, containingClass)
                if (updateListenerField != null) {
                    return NavigationGutterIconBuilder.create(AllIcons.Nodes.Field)
                        .setTarget(updateListenerField)
                        .setTooltipText("Navigate to UpdateListener field: ${updateListenerField.name}")
                        .createLineMarkerInfo(element)
                }

                // find the target field with @ReadOnlyManaged
                val readOnlyManagedField = ReadOnlyManagedUtils.findReadOnlyManagedField(parent, containingClass)
                if (readOnlyManagedField != null) {
                    return NavigationGutterIconBuilder.create(AllIcons.Nodes.Field)
                        .setTarget(readOnlyManagedField)
                        .setTooltipText("Navigate to ReadOnlyManaged field: ${readOnlyManagedField.name}")
                        .createLineMarkerInfo(element)
                }

                // find the target field with @ConfigList
                val configListField = ConfigListUtils.findConfigListField(parent, containingClass)
                if (configListField != null) {
                    return NavigationGutterIconBuilder.create(AllIcons.Nodes.Field)
                        .setTarget(configListField)
                        .setTooltipText("Navigate to ConfigList field: ${configListField.name}")
                        .createLineMarkerInfo(element)
                }

                // find the target field with @ConfigSelector
                val configSelectorField = ConfigSelectorUtils.findConfigSelectorField(parent, containingClass)
                if (configSelectorField != null) {
                    return NavigationGutterIconBuilder.create(AllIcons.Nodes.Field)
                        .setTarget(configSelectorField)
                        .setTooltipText("Navigate to ConfigSelector field: ${configSelectorField.name}")
                        .createLineMarkerInfo(element)
                }

                // find the target field with @ConfigSearch
                val configSearchField = ConfigSearchUtils.findConfigSearchField(parent, containingClass)
                if (configSearchField != null) {
                    return NavigationGutterIconBuilder.create(AllIcons.Nodes.Field)
                        .setTarget(configSearchField)
                        .setTooltipText("Navigate to ConfigSearch field: ${configSearchField.name}")
                        .createLineMarkerInfo(element)
                }

                // find the target field with @Configurable for @SkipPersistedValue
                val skipPersistedValueField = SkipPersistedValueUtils.findSkipPersistedValueField(parent, containingClass)
                if (skipPersistedValueField != null) {
                    return NavigationGutterIconBuilder.create(AllIcons.Nodes.Field)
                        .setTarget(skipPersistedValueField)
                        .setTooltipText("Navigate to field with @SkipPersistedValue: ${skipPersistedValueField.name}")
                        .createLineMarkerInfo(element)
                }
            }

            is PsiField -> {
                val containingClass = parent.containingClass ?: return null

                // find ConfigSetter methods for this field
                val configSetterMethods = ConfigSetterUtils.findConfigSetterMethods(parent, containingClass)
                if (configSetterMethods.isNotEmpty()) {
                    return NavigationGutterIconBuilder.create(AllIcons.Nodes.Method)
                        .setTargets(configSetterMethods)
                        .setTooltipText("Navigate to ConfigSetter methods for field: ${parent.name}")
                        .createLineMarkerInfo(element)
                }

                // find SkipPersistedValue methods for this field
                val skipPersistedValueMethods = SkipPersistedValueUtils.findSkipPersistedValueMethods(parent, containingClass)
                if (skipPersistedValueMethods.isNotEmpty()) {
                    return NavigationGutterIconBuilder.create(AllIcons.Nodes.Method)
                        .setTargets(skipPersistedValueMethods)
                        .setTooltipText("Navigate to SkipPersistedValue methods for field: ${parent.name}")
                        .createLineMarkerInfo(element)
                }
            }
        }
        
        return null
    }
}