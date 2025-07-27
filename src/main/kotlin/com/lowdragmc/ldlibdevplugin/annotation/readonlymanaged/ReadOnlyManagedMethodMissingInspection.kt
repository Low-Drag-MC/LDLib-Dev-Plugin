package com.lowdragmc.ldlibdevplugin.annotation.readonlymanaged

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.lowdragmc.ldlibdevplugin.annotation.AbstractAnnotationMethodInspection
import com.lowdragmc.ldlibdevplugin.annotation.AbstractAnnotationMethodInspection.MethodAttribute

val ON_DIRTY_METHOD = MethodAttribute(ReadOnlyManagedUtils.ON_DIRTY_METHOD, "onDirty method", "onDirty")
val SERIALIZE_METHOD = MethodAttribute(ReadOnlyManagedUtils.SERIALIZE_METHOD, "serialize method", "serialize")
val DESERIALIZE_METHOD = MethodAttribute(ReadOnlyManagedUtils.DESERIALIZE_METHOD, "deserialize method", "deserialize")
class ReadOnlyManagedMethodMissingInspection : AbstractAnnotationMethodInspection() {

    override fun isAnnotatedField(field: PsiField): Boolean {
        return ReadOnlyManagedUtils.isAnnotatedField(field)
    }

    override fun getAnnotation(field: PsiField): PsiAnnotation? {
        return field.getAnnotation(ReadOnlyManagedUtils.READ_ONLY_MANAGED_ANNOTATION)
    }

    override fun getMethodNameAttribute(annotation: PsiAnnotation, attributeName: String): PsiLiteralExpression? {
        return annotation.findAttributeValue(attributeName) as? PsiLiteralExpression
    }

    override fun getMethodName(field: PsiField, attributeName: String): String? {
        return when (attributeName) {
            ReadOnlyManagedUtils.ON_DIRTY_METHOD -> ReadOnlyManagedUtils.getOnDirtyMethod(field)
            ReadOnlyManagedUtils.SERIALIZE_METHOD -> ReadOnlyManagedUtils.getSerializeMethod(field)
            ReadOnlyManagedUtils.DESERIALIZE_METHOD -> ReadOnlyManagedUtils.getDeserializeMethod(field)
            else -> null
        }
    }

    override fun isValidMethod(method: PsiMethod, fieldType: PsiType, methodAttribute: MethodAttribute): Boolean {
        return when (methodAttribute.methodType) {
            "onDirty" -> ReadOnlyManagedUtils.isValidOnDirtyMethod(method)
            "serialize" -> ReadOnlyManagedUtils.isValidSerializeMethod(method, fieldType)
            "deserialize" -> ReadOnlyManagedUtils.isValidDeserializeMethod(method, fieldType)
            else -> false
        }
    }

    override fun hasCorrectParameterCount(method: PsiMethod, fieldType: PsiType, methodAttribute: MethodAttribute): Boolean {
        return when (methodAttribute.methodType) {
            "onDirty" -> method.parameterList.parameters.isEmpty()
            "serialize" -> method.parameterList.parameters.size == 1
            "deserialize" -> method.parameterList.parameters.size == 1
            else -> false
        }
    }

    override fun getAnnotationName(): String = "ReadOnlyManaged"

    override fun getMethodAttributes(): List<MethodAttribute> {
        return listOf(ON_DIRTY_METHOD, SERIALIZE_METHOD, DESERIALIZE_METHOD)
    }

    override fun createMethodQuickFix(methodName: String, fieldType: PsiType, fieldName: String, methodAttribute: MethodAttribute): LocalQuickFix {
        return when (methodAttribute.methodType) {
            "onDirty" -> CreateOnDirtyMethodQuickFix(methodName, fieldName)
            "serialize" -> CreateSerializeMethodQuickFix(methodName, fieldType, fieldName)
            "deserialize" -> CreateDeserializeMethodQuickFix(methodName, fieldType, fieldName)
            else -> throw IllegalArgumentException("Unknown method type: ${methodAttribute.methodType}")
        }
    }
}

// OnDirty 方法的 QuickFix
class CreateOnDirtyMethodQuickFix(
    private val methodName: String,
    private val fieldName: String
) : LocalQuickFix {

    override fun getName(): String = "Create ReadOnlyManaged onDirty method '$methodName'"

    override fun getFamilyName(): String = "Create ReadOnlyManaged onDirty method"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement
        val containingClass = PsiTreeUtil.getParentOfType(element, PsiClass::class.java) ?: return

        val elementFactory = JavaPsiFacade.getElementFactory(project)

        val methodText = """
            private boolean $methodName() {
                // TODO: Implement $fieldName dirty state check
                return false;
            }
        """.trimIndent()

        val method = elementFactory.createMethodFromText(methodText, containingClass)
        containingClass.add(method)
    }
}

// Serialize 方法的 QuickFix
class CreateSerializeMethodQuickFix(
    private val methodName: String,
    private val fieldType: PsiType,
    private val fieldName: String
) : LocalQuickFix {

    override fun getName(): String = "Create ReadOnlyManaged serialize method '$methodName'"

    override fun getFamilyName(): String = "Create ReadOnlyManaged serialize method"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement
        val containingClass = PsiTreeUtil.getParentOfType(element, PsiClass::class.java) ?: return

        val elementFactory = JavaPsiFacade.getElementFactory(project)
        val fieldTypeName = fieldType.presentableText

        val methodText = """
            private net.minecraft.nbt.Tag $methodName($fieldTypeName value) {
                // TODO: Implement $fieldName serialization
                return null;
            }
        """.trimIndent()

        val method = elementFactory.createMethodFromText(methodText, containingClass)
        containingClass.add(method)
    }
}

// Deserialize 方法的 QuickFix
class CreateDeserializeMethodQuickFix(
    private val methodName: String,
    private val fieldType: PsiType,
    private val fieldName: String
) : LocalQuickFix {

    override fun getName(): String = "Create ReadOnlyManaged deserialize method '$methodName'"

    override fun getFamilyName(): String = "Create ReadOnlyManaged deserialize method"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement
        val containingClass = PsiTreeUtil.getParentOfType(element, PsiClass::class.java) ?: return

        val elementFactory = JavaPsiFacade.getElementFactory(project)
        val fieldTypeName = fieldType.presentableText

        val methodText = """
            private $fieldTypeName $methodName(net.minecraft.nbt.Tag tag) {
                // TODO: Implement $fieldName deserialization
                return null;
            }
        """.trimIndent()

        val method = elementFactory.createMethodFromText(methodText, containingClass)
        containingClass.add(method)
    }
}
