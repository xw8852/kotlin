/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.search

import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.psi.KtSimpleNameExpression

class FirPsiBasedClassResolverFabricImpl : PsiBasedClassResolverFabric() {
    override fun getInstance(targetClassFqName: String): PsiBasedClassResolver {
        return FirPsiBasedClassResolverImpl()
    }

    override fun getInstance(target: PsiClass): PsiBasedClassResolver {
        return FirPsiBasedClassResolverImpl()
    }
}

//TODO: Provide FIR support for resolver
private class FirPsiBasedClassResolverImpl : PsiBasedClassResolver {
    override fun addConflict(fqName: String) {

    }

    override fun canBeTargetReference(ref: KtSimpleNameExpression): PsiBasedClassResolver.ResolveResult {
        return PsiBasedClassResolver.ResolveResult.UNSURE
    }

}