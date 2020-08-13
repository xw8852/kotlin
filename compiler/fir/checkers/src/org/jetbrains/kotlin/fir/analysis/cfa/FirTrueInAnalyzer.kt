/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa

import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.contracts.FirContractDescription
import org.jetbrains.kotlin.fir.contracts.description.ConeCallsEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.description.ConeTrueInEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.effects
import org.jetbrains.kotlin.fir.declarations.FirContractDescriptionOwner
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccess
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedArgumentList
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraphVisitorVoid
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.FunctionCallNode
import org.jetbrains.kotlin.fir.resolve.isInvoke
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneType

object FirTrueInAnalyzer : AbstractFirPropertyInitializationChecker() {

    override fun analyze(graph: ControlFlowGraph, reporter: DiagnosticReporter) {
        val function = graph.declaration as? FirFunction<*> ?: return
        val effects = function.contractDescription?.effects ?: return
        if (effects.none { it is ConeTrueInEffectDeclaration }) return

        val requiredEffects = mutableMapOf<AbstractFirBasedSymbol<*>, MutableList<ConeTrueInEffectDeclaration>>()
        for (effect in effects) {
            if (effect is ConeTrueInEffectDeclaration) {
                val symbol = function.getParameterSymbol(effect.target.parameterIndex)
                requiredEffects.getOrPut(symbol) { mutableListOf() } += effect
            }
        }
        if (requiredEffects.isEmpty()) return

        graph.traverse(TraverseDirection.Forward, LambdaInvocationFinder(requiredEffects))
    }

    private class LambdaInvocationFinder(
        val requiredEffects: Map<AbstractFirBasedSymbol<*>, List<ConeTrueInEffectDeclaration>>
    ) : ControlFlowGraphVisitorVoid() {
        override fun visitNode(node: CFGNode<*>) {}

        override fun visitFunctionCallNode(node: FunctionCallNode) {
            val functionSymbol = node.fir.toResolvedCallableSymbol() as? FirFunctionSymbol<*>?
            val contractDescription = functionSymbol?.fir?.contractDescription

            checkReference(node.fir.explicitReceiver.toQualifiedReference(), node) {
                functionSymbol?.callableId?.isInvoke() == true || contractDescription.getParameterCallsEffect(-1) != null
            }

            for (arg in node.fir.argumentList.arguments) {
                checkReference(arg.toQualifiedReference(), node) {
                    node.fir.getArgumentCallsEffect(arg) != null
                }
            }
        }

        private inline fun checkReference(reference: FirReference?, node: CFGNode<*>, checkCondition: () -> Boolean = { true }) {
            val symbol = referenceToSymbol(reference)
            val effects = requiredEffects[symbol]
            if (effects != null && checkCondition()) {
                for (effect in effects) {

                }
            }
        }
    }

    private fun FirContractDescription?.getParameterCallsEffectDeclaration(index: Int): ConeCallsEffectDeclaration? {
        val effects = this?.effects
        val callsEffect = effects?.find { it is ConeCallsEffectDeclaration && it.valueParameterReference.parameterIndex == index }
        return callsEffect as? ConeCallsEffectDeclaration?
    }

    private fun FirFunctionCall.getArgumentCallsEffect(arg: FirExpression): EventOccurrencesRange? {
        val function = (this.toResolvedCallableSymbol() as? FirFunctionSymbol<*>?)?.fir
        val contractDescription = function?.contractDescription
        val resolvedArguments = argumentList as? FirResolvedArgumentList

        return if (function != null && resolvedArguments != null) {
            val parameter = resolvedArguments.mapping[arg]
            contractDescription.getParameterCallsEffect(function.valueParameters.indexOf(parameter))
        } else null
    }

    private fun FirContractDescription?.getParameterCallsEffect(index: Int): EventOccurrencesRange? {
        return getParameterCallsEffectDeclaration(index)?.kind
    }

    private fun FirFunction<*>.getParameterType(symbol: AbstractFirBasedSymbol<*>): ConeKotlinType? {
        return (if (this.symbol == symbol) receiverTypeRef else valueParameters.find { it.symbol == symbol }?.returnTypeRef)?.coneType
    }

    private fun FirFunction<*>.getParameterSymbol(index: Int): AbstractFirBasedSymbol<*> {
        return if (index == -1) this.symbol else this.valueParameters[index].symbol
    }

    private val FirFunction<*>.contractDescription: FirContractDescription?
        get() = (this as? FirContractDescriptionOwner)?.contractDescription

    private fun FirExpression?.toQualifiedReference(): FirReference? = (this as? FirQualifiedAccess)?.calleeReference

    private fun referenceToSymbol(reference: FirReference?): AbstractFirBasedSymbol<*>? = when (reference) {
        is FirResolvedNamedReference -> reference.resolvedSymbol
        is FirThisReference -> reference.boundSymbol
        else -> null
    }
}