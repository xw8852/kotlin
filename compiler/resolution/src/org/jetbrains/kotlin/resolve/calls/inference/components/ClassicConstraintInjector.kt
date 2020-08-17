/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference.components

import org.jetbrains.kotlin.types.AbstractTypeApproximator
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.refinement.TypeRefinement

class ClassicConstraintInjector(
    constraintIncorporator: ConstraintIncorporator,
    typeApproximator: AbstractTypeApproximator,
    val kotlinTypeRefiner: KotlinTypeRefiner
) : ConstraintInjector(constraintIncorporator, typeApproximator) {
    @OptIn(TypeRefinement::class)
    override fun refineType(type: KotlinTypeMarker): KotlinTypeMarker {
        return if (type is KotlinType) {
            kotlinTypeRefiner.refineType(type)
        } else {
            type
        }
    }
}
