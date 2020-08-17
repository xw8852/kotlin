/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.search

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.usages.impl.FileStructureGroupRuleProvider
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.psi.*
import java.util.concurrent.atomic.AtomicInteger

abstract class PsiBasedClassResolverFabric{
    @TestOnly abstract fun getInstance(targetClassFqName: String): PsiBasedClassResolver

    abstract fun getInstance(target: PsiClass): PsiBasedClassResolver

    companion object {
        var EP_NAME = ExtensionPointName<FileStructureGroupRuleProvider>("org.jetbrains.kotlin.idea.search.psiBasedClassResolverFabric")

        @JvmStatic
        fun getInstance(project: Project): PsiBasedClassResolverFabric {
            return ServiceManager.getService(project, PsiBasedClassResolverFabric::class.java)
        }
    }
}

/**
 * Can quickly check whether a short name reference in a given file can resolve to the class/interface/type alias
 * with the given qualified name.
 */
interface PsiBasedClassResolver {

    enum class ResolveResult {
        MATCH,
        NO_MATCH,
        UNSURE;
    }

    companion object {
        @get:TestOnly
        val attempts = AtomicInteger()

        @get:TestOnly
        val trueHits = AtomicInteger()

        @get:TestOnly
        val falseHits = AtomicInteger()
    }

    @TestOnly
    fun addConflict(fqName: String)

    /**
     * Checks if a reference with the short name of [targetClassFqName] in the given file will resolve
     * to the target class.
     *
     * @return true if it will definitely resolve to that class, false if it will definitely resolve to something else,
     * null if full resolve is required to answer that question.
     */
    fun canBeTargetReference(ref: KtSimpleNameExpression): ResolveResult
}