package com.lowdragmc.ldlibdevplugin.annotation.rpc

import com.intellij.codeInspection.*
import com.intellij.psi.*
import com.lowdragmc.ldlibdevplugin.annotation.AnnotationUtils

class RPCPacketParametersInspection : AbstractBaseJavaLocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                val method = expression.resolveMethod() ?: return
                if (method.containingClass?.qualifiedName != RPCPacketUtils.DISTRIBUTOR_CLASS) return

                val methodName = method.name
                if (!methodName.startsWith("rpcTo")) return

                val args = expression.argumentList.expressions
                val packetIdIndex = if (methodName == "rpcToPlayer" || methodName == "rpcToTracking") 1 else 0
                if (args.size <= packetIdIndex) return

                val packetIdLiteral = args[packetIdIndex] as? PsiLiteralExpression ?: return
                val packetId = packetIdLiteral.value as? String ?: return

                val targetMethods = RPCPacketUtils.findRPCPacketMethods(expression.project, packetId)
                if (targetMethods.isEmpty()) return

                val targetMethod = targetMethods[0]
                val expectedParams = RPCPacketUtils.getEffectiveParameters(targetMethod)
                val actualArgs = args.drop(packetIdIndex + 1)

                if (expectedParams.size != actualArgs.size) {
                    holder.registerProblem(expression.argumentList, "Arguments count mismatch. Expected ${expectedParams.size}, but got ${actualArgs.size}.")
                    return
                }

                for (i in expectedParams.indices) {
                    val expected = expectedParams[i]
                    val actual = actualArgs[i].type ?: continue
                    if (!AnnotationUtils.isTypeCompatible(expected, actual)) {
                        holder.registerProblem(actualArgs[i], "Argument type mismatch. Expected '${expected.presentableText}', but got '${actual.presentableText}'.")
                    }
                }
            }
        }
    }
}