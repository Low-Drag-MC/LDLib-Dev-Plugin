package com.lowdragmc.ldlibdevplugin.annotation.rpc

import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil

object RPCPacketUtils {
    const val RPC_PACKET_ANNOTATION = "com.lowdragmc.lowdraglib2.networking.rpc.RPCPacket"
    const val DISTRIBUTOR_CLASS = "com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor"
    const val RPC_SENDER_TYPE = "com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender"

    fun findRPCPacketMethods(project: com.intellij.openapi.project.Project, packetId: String): List<PsiMethod> {
        val annotationClass = JavaPsiFacade.getInstance(project)
            .findClass(RPC_PACKET_ANNOTATION, GlobalSearchScope.allScope(project)) ?: return emptyList()
        
        return AnnotatedElementsSearch.searchPsiMethods(annotationClass, GlobalSearchScope.allScope(project))
            .filter { method ->
                val annotation = method.getAnnotation(RPC_PACKET_ANNOTATION)
                annotation?.findAttributeValue("value")?.let { 
                    (it as? PsiLiteralExpression)?.value == packetId 
                } ?: false
            }
    }

    fun isRPCPacketMethod(method: PsiMethod): Boolean {
        return method.hasAnnotation(RPC_PACKET_ANNOTATION) && method.hasModifierProperty(PsiModifier.STATIC)
    }

    fun findDistributorCalls(method: PsiMethod): List<PsiElement> {
        return ReferencesSearch.search(method).mapNotNull { reference ->
            if (reference is RPCPacketMethodReference) {
                reference.element
            } else {
                null
            }
        }
    }

    fun getEffectiveParameters(method: PsiMethod): List<PsiType> {
        val parameters = method.parameterList.parameters
        if (parameters.isEmpty()) return emptyList()
        
        val firstParamType = parameters[0].type as? PsiClassType
        return if (firstParamType?.resolve()?.qualifiedName == RPC_SENDER_TYPE) {
            parameters.drop(1).map { it.type }
        } else {
            parameters.map { it.type }
        }
    }
}