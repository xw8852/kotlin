/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNamedElement
import org.jetbrains.kotlin.asJava.*
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement

class LightClassProviderImpl : LightClassProvider {
    override fun getLightClass(declaration: KtClassOrObject): PsiClass? = declaration.toLightClass()
    override fun getLightElements(element: KtElement): List<PsiNamedElement> = element.toLightElements()
    override fun getLightMethods(element: PsiElement): List<PsiMethod> = element.toLightMethods()
    override fun getLightClassWithBuiltinMapping(element: KtClassOrObject): PsiClass? = element.toLightClassWithBuiltinMapping()
}