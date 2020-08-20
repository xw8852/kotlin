/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol

class ExtendedSymbolRemapper(
    typeParametersInitial: Map<IrTypeParameter, IrTypeParameter>,
    valuesInitial: Map<IrValueDeclaration, IrValueDeclaration>
) : DeepCopySymbolRemapper(DescriptorsRemapper.Default) {

    private val extendedTypeParameters = typeParametersInitial.mapKeys { it.key.symbol }
    private val extendedValues = valuesInitial.mapKeys { it.key.symbol }

    override fun getReferencedValue(symbol: IrValueSymbol): IrValueSymbol =
        extendedValues[symbol]?.symbol ?: super.getReferencedValue(symbol)

    override fun getReferencedClassifier(symbol: IrClassifierSymbol): IrClassifierSymbol =
        if (symbol is IrTypeParameterSymbol)
            extendedTypeParameters[symbol]?.symbol ?: super.getReferencedClassifier(symbol)
        else
            super.getReferencedClassifier(symbol)
}
