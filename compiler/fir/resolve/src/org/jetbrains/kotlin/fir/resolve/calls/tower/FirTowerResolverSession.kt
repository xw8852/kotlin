/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.tower

import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*

class FirTowerResolverSession internal constructor(
    internal val components: BodyResolveComponents,
    internal val manager: TowerResolveManager,
    internal val candidateFactoriesAndCollectors: CandidateFactoriesAndCollectors,
    private val mainCallInfo: CallInfo,
) {

    private val invokeResolver = FirTowerInvokeResolver(this)

    fun runResolution(info: CallInfo) {
        val mainTask = createTask()
        when (val receiver = info.explicitReceiver) {
            is FirResolvedQualifier -> {
                manager.enqueueResolverTask { mainTask.runResolverForQualifierReceiver(info, receiver) }
                invokeResolver.enqueueResolveTasksForQualifier(info, receiver)
            }
            null -> enqueueResolveForNoReceiver(info, mainTask)
            else -> run {
                if (receiver is FirQualifiedAccessExpression) {
                    val calleeReference = receiver.calleeReference
                    if (calleeReference is FirSuperReference) {
                        return@run manager.enqueueResolverTask { mainTask.runResolverForSuperReceiver(info, receiver.typeRef) }
                    }
                }

                manager.enqueueResolverTask { mainTask.runResolverForExpressionReceiver(info, receiver) }
                invokeResolver.enqueueResolveTasksForExpressionReceiver(info, receiver)
            }
        }
    }

    private fun enqueueResolveForNoReceiver(info: CallInfo, mainTask: FirTowerResolveTask) {
        manager.enqueueResolverTask { mainTask.runResolverForNoReceiver(info) }
        invokeResolver.enqueueResolveTasksForNoReceiver(info)
    }

    private fun createTask() =
        FirTowerResolveTask(
            this,
            TowerDataElementsForName(mainCallInfo.name, components.towerDataContext),
            candidateFactoriesAndCollectors.resultCollector,
            candidateFactoriesAndCollectors.candidateFactory,
            candidateFactoriesAndCollectors.stubReceiverCandidateFactory
        )

}
