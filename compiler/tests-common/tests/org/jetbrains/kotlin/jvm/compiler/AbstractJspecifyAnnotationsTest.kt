/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.compiler

import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiCompiledElement
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.DeclarationDescriptorVisitorEmptyBodies
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.load.java.AnnotationTypeQualifierResolver
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor
import org.jetbrains.kotlin.load.java.lazy.JavaResolverSettings
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaClassDescriptor
import org.jetbrains.kotlin.load.java.typeEnhancement.JavaTypeEnhancement
import org.jetbrains.kotlin.load.java.typeEnhancement.SignatureEnhancement
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.MockLibraryUtil
import org.jetbrains.kotlin.types.isFlexible
import org.jetbrains.kotlin.types.isNullable
import org.jetbrains.kotlin.utils.JavaTypeEnhancementState
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.io.File

abstract class AbstractJspecifyAnnotationsTest : AbstractLoadJavaTest() {
    companion object {
        const val FOREIGN_ANNOTATIONS_SOURCES_PATH = "third-party/jdk8-annotations"

        val PREDICATE_REGEX = Regex("""P(SometimesNull|NonNull|UnknownNull)\((.*?)\)""")
        val PACKAGE_REGEX = Regex("""package (.*?);""")
    }

    override fun getExtraClasspath() = createJarWithForeignAnnotations()

    protected fun createJarWithForeignAnnotations(): List<File> = listOf(
        MockLibraryUtil.compileJavaFilesLibraryToJar(FOREIGN_ANNOTATIONS_SOURCES_PATH, "foreign-annotations"),
        ForTestCompileRuntime.jvmAnnotationsForTests()
    )

    override fun doTestSourceJava(javaFileName: String) {
        val javaFile = File(javaFileName)
        val packageName = PACKAGE_REGEX.find(javaFile.readText())?.groupValues?.last() ?: ""
        val testPackageDir = if (!packageName.isBlank()) File(tmpdir, packageName.replace(".", "/")).apply { mkdirs() } else tmpdir

        FileUtil.copy(javaFile, File(testPackageDir, javaFile.name))

        val javaPackageAndContext = LoadDescriptorUtil.loadTestPackageAndBindingContextFromJavaRoot(
            tmpdir, testRootDisposable, jdkKind, ConfigurationKind.JDK_ONLY, false,
            true, useJavacWrapper(), null, extraClasspath, { },
            FqName.topLevel(Name.identifier(packageName))
        )
        val storageManager = LockBasedStorageManager("RuntimeModuleData")
        val signatureEnhancement = SignatureEnhancement(
            AnnotationTypeQualifierResolver(storageManager, JavaTypeEnhancementState.DISABLED_JSR_305),
            JavaTypeEnhancementState.DISABLED_JSR_305,
            JavaTypeEnhancement(
                JavaResolverSettings.Default
            )
        )

        javaPackageAndContext.first.acceptVoid(object : DeclarationDescriptorVisitorEmptyBodies<Void?, Void?>() {
            override fun visitPackageViewDescriptor(descriptor: PackageViewDescriptor, data: Void?): Void? {
                return visitDeclarationRecursively(descriptor, descriptor.memberScope)
            }

            override fun visitClassDescriptor(descriptor: ClassDescriptor, data: Void?): Void? {
                return visitDeclarationRecursively(descriptor, descriptor.defaultType.memberScope)
            }

            override fun visitFunctionDescriptor(descriptor: FunctionDescriptor, data: Void?): Void? {
                return visitDeclaration(descriptor);
            }

            override fun visitPropertyDescriptor(descriptor: PropertyDescriptor, data: Void?): Void? {
                return visitDeclaration(descriptor);
            }

            private fun visitDeclaration(descriptor: DeclarationDescriptor): Void? {
                if (descriptor is JavaMethodDescriptor) {
                    val de = descriptor.containingDeclaration
                    if (de is LazyJavaClassDescriptor) {
                        val enh = signatureEnhancement.enhanceSignatures(de.outerContext, listOf(descriptor))
                        val enh1 = enh.first()
                        val enhanced = enh1.findPsi() ?: return null
                        if (enhanced is PsiCompiledElement)
                            return null
                        val predicate = enhanced.children.firstIsInstanceOrNull<PsiComment>()?.text ?: return null
                        val parsedPredicate = parsePredicate(predicate)

                        for (pr in parsedPredicate) {
                            when (val gg = pr.second) {
                                is ReturnValueType -> {
                                    val ex = when (pr.first) {
                                        PredicateType.NOT_NULL -> enh1.returnType?.isNullable() != true
                                        PredicateType.NULLABLE -> enh1.returnType?.isNullable() != false
                                        PredicateType.UNKNOWN -> enh1.returnType?.isFlexible() != false
                                    }
                                    assert(ex)
                                }
                                is ParameterType -> {
                                    val parameter = enh1.valueParameters.find { it.name.identifier == gg.parameterName }!!
                                    val ex = when (pr.first) {
                                        PredicateType.NOT_NULL -> !parameter.type.isNullable()
                                        PredicateType.NULLABLE -> parameter.type.isNullable()
                                        PredicateType.UNKNOWN -> parameter.type.isFlexible()
                                    }
                                    assert(ex)
                                }
                                is TypeArgumentTypeOfParameter -> {}
                                is TypeArgumentTypeOfReturnValue -> {}
                            }
                        }
                    }
                }
                return null
            }

            private fun visitDeclarationRecursively(descriptor: DeclarationDescriptor, memberScope: MemberScope): Void? {
                for (member in DescriptorUtils.getAllDescriptors(memberScope)) {
                    member.acceptVoid(this)
                }

                return visitDeclaration(descriptor)
            }
        })
    }

    private fun parsePredicate(predicates: String) =
        PREDICATE_REGEX.findAll(predicates).map { aa ->
            val (predicate, reference) = aa.destructured
            val pp = PredicateType.getPredicateTypeByText(predicate)
            val refType = ReferenceType.computeType(reference)

            pp to refType
        }.toList()
}

private enum class PredicateType(val text: String) {
    NULLABLE("SometimesNull"),
    NOT_NULL("NonNull"),
    UNKNOWN("UnknownNull");

    companion object {
        fun getPredicateTypeByText(text: String) = values().find { it.text == text }!!
    }
}

private sealed class ReferenceType {
    companion object {
        const val RETURN_VALUE_MARKER = "_"
        const val JAVA_IDENTIFIER_PATTERN = """[A-Za-z][A-Za-z0-9_]*"""
        const val TYPE_ARGUMENT_PATH_PATTERN = """\{((?:[0-9]\d*)(?:,[0-9]\d*)*)\}"""
        val simpleParameter = Regex("""^$JAVA_IDENTIFIER_PATTERN$""")
        val parameterWithPathToTypeArgument = Regex("""^($JAVA_IDENTIFIER_PATTERN)$TYPE_ARGUMENT_PATH_PATTERN$""")
        val returnValueWithPathToTypeArgument = Regex("""^$RETURN_VALUE_MARKER$TYPE_ARGUMENT_PATH_PATTERN$""")

        fun computeType(referenceText: String): ReferenceType {
            if (referenceText == RETURN_VALUE_MARKER)
                return ReturnValueType

            if (simpleParameter.matches(referenceText))
                return ParameterType(referenceText)

            val parameterWithPathToTypeArgument = parameterWithPathToTypeArgument.find(referenceText)

            if (parameterWithPathToTypeArgument != null) {
                val (reference, typeArgumentPath) = parameterWithPathToTypeArgument.destructured

                return TypeArgumentTypeOfParameter(
                    reference,
                    typeArgumentPath.split(",").map { it.toInt() }.toIntArray()
                )
            }

            val returnValueWithPathToTypeArgument = returnValueWithPathToTypeArgument.find(referenceText)

            if (returnValueWithPathToTypeArgument != null) {
                val (typeArgumentPath) = returnValueWithPathToTypeArgument.destructured

                return TypeArgumentTypeOfReturnValue(
                    typeArgumentPath.split(",").map { it.toInt() }.toIntArray()
                )
            }

            throw IllegalStateException("")
        }
    }
}

private object ReturnValueType : ReferenceType()
private class ParameterType(val parameterName: String) : ReferenceType()
private class TypeArgumentTypeOfParameter(val parameterName: String, val argumentPath: IntArray) : ReferenceType()
private class TypeArgumentTypeOfReturnValue(val argumentPath: IntArray) : ReferenceType()
