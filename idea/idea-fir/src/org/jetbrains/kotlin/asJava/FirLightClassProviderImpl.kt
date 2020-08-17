/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNamedElement
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement

class FirLightClassProviderImpl : LightClassProvider {
    override fun getLightClass(declaration: KtClassOrObject): PsiClass? = null
    override fun getLightElements(element: KtElement): List<PsiNamedElement> = emptyList()
    override fun getLightMethods(element: PsiElement): List<PsiMethod> = emptyList()
    override fun getLightClassWithBuiltinMapping(element: KtClassOrObject): PsiClass? = null
}