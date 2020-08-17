/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNamedElement
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement

interface LightClassProvider {

    companion object {
        val EP_NAME: ExtensionPointName<LightClassProvider> = ExtensionPointName.create("org.jetbrains.kotlin.asJava.lightClassProvider")

        @JvmStatic
        fun getInstance(project: Project): LightClassProvider {
            return ServiceManager.getService(project, LightClassProvider::class.java)
        }
    }

    fun getLightClass(declaration: KtClassOrObject): PsiClass?
    fun getLightElements(element: KtElement): List<PsiNamedElement>
    fun getLightMethods(element: PsiElement): List<PsiMethod>
    fun getLightClassWithBuiltinMapping(element: KtClassOrObject): PsiClass?
}

internal fun KtClassOrObject.getLightClass(): PsiClass? = LightClassProvider.getInstance(project).getLightClass(this)
internal fun KtElement.getLightElements(): List<PsiNamedElement> = LightClassProvider.getInstance(project).getLightElements(this)
internal fun PsiElement.getLightMethods(): List<PsiMethod> = LightClassProvider.getInstance(project).getLightMethods(this)
internal fun KtClassOrObject.getLightClassWithBuiltinMapping(element: KtClassOrObject): PsiClass? = LightClassProvider.getInstance(project).getLightClassWithBuiltinMapping(this)
