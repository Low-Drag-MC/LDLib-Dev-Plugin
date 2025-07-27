package com.lowdragmc.ldlibdevplugin.annotation

import com.intellij.codeInsight.daemon.ImplicitUsageProvider
import com.intellij.psi.*
import com.lowdragmc.ldlibdevplugin.annotation.configlist.ConfigListUtils
import com.lowdragmc.ldlibdevplugin.annotation.configselector.ConfigSelectorUtils
import com.lowdragmc.ldlibdevplugin.annotation.configsetter.ConfigSetterUtils
import com.lowdragmc.ldlibdevplugin.annotation.readonlymanaged.ReadOnlyManagedUtils
import com.lowdragmc.ldlibdevplugin.annotation.updatelistener.UpdateListenerUtils

class MethodImplicitUsageProvider : ImplicitUsageProvider {

    override fun isImplicitUsage(element: PsiElement): Boolean {
        if (element !is PsiMethod) return false

        val containingClass = element.containingClass ?: return false

        // Check UpdateListener methods
        if (UpdateListenerUtils.findUpdateListenerField(element, containingClass) != null) {
            return true
        }

        // Check ReadOnlyManaged methods
        if (ReadOnlyManagedUtils.findReadOnlyManagedField(element, containingClass) != null) {
            return true
        }

        // Check ConfigSetter methods
        if (ConfigSetterUtils.findConfigSetterField(element, containingClass) != null) {
            return true
        }

        // Check ConfigList methods
        if (ConfigListUtils.findConfigListField(element, containingClass) != null) {
            return true
        }

        // Check ConfigSelector methods
        if (ConfigSelectorUtils.findConfigSelectorField(element, containingClass) != null) {
            return true
        }

        return false
    }

    override fun isImplicitRead(element: PsiElement): Boolean = false
    override fun isImplicitWrite(element: PsiElement): Boolean = false
}