package com.lowdragmc.ldlibdevplugin.annotation.rpc

import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.lowdragmc.ldlibdevplugin.annotation.AnnotationReferenceHandler

class RPCPacketReferenceHandler : AnnotationReferenceHandler() {
    override fun canApply(value: String, annotation: PsiAnnotation, nameValuePair: PsiNameValuePair): Boolean {
        return annotation.hasQualifiedName(RPCPacketUtils.RPC_PACKET_ANNOTATION)
    }

    override fun createPsiReference(element: PsiLiteralExpression, value: String, annotation: PsiAnnotation, nameValuePair: PsiNameValuePair): PsiReference {
        return RPCPacketMethodReference(element, value)
    }
}

class RPCPacketMethodReference(element: PsiLiteralExpression, private val packetId: String) : PsiReferenceBase<PsiLiteralExpression>(element) {
    override fun resolve(): PsiElement? {
        return RPCPacketUtils.findRPCPacketMethods(element.project, packetId).firstOrNull()
    }

    override fun getVariants(): Array<Any> = emptyArray()

    override fun getRangeInElement(): TextRange {
        val text = element.text
        return if (text.startsWith("\"") && text.endsWith("\"")) TextRange(1, text.length - 1) else TextRange.EMPTY_RANGE
    }
}
