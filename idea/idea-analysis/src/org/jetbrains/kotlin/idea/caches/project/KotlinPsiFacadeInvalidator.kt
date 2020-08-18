/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.project

import com.intellij.ProjectTopics
import com.intellij.openapi.fileTypes.FileTypeEvent
import com.intellij.openapi.fileTypes.FileTypeListener
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileCopyEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent
import org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade

class KotlinPsiFacadeInvalidatorActivity : StartupActivity {
    override fun runActivity(project: Project) {
        val connection = project.messageBus.connect(project)
        connection.subscribe(ProjectTopics.PROJECT_ROOTS, object : ModuleRootListener {
            override fun rootsChanged(event: ModuleRootEvent) {
                project.clearPackageCaches()
            }
        })

        connection.subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: List<VFileEvent>) {
                events.filter(::isRelevantEvent).let { createEvents ->
                    if (createEvents.isNotEmpty()) {
                        project.clearPackageCaches()
                    }
                }
            }
        })

        connection.subscribe(FileTypeManager.TOPIC, object : FileTypeListener {
            override fun fileTypesChanged(event: FileTypeEvent) {
                project.clearPackageCaches()
            }
        })
    }

    private fun Project.clearPackageCaches() = KotlinJavaPsiFacade.getInstance(this).clearPackageCaches()

    private fun isRelevantEvent(vFileEvent: VFileEvent) =
        vFileEvent is VFileCreateEvent || vFileEvent is VFileMoveEvent || vFileEvent is VFileCopyEvent

}