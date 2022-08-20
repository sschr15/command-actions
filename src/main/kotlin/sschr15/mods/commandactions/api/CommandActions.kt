@file:JvmName("CommandActions")

package sschr15.mods.commandactions.api

import net.minecraft.server.MinecraftServer
import sschr15.mods.commandactions.impl.functions
import sschr15.mods.commandactions.impl.runCommand
import kotlin.io.path.*

/**
 * Run config-defined functions. All functions will be run server-side.
 *
 * @param server the server instance.
 * @param functionName the name of the function to run.
 *        This will be resolved to a directory or `.mcfunction` file as appropriate for the server's configuration.
 *        If resolving to a directory, all files in the directory will be executed as mcfunctions, and all
 *        subdirectories will recurse.
 *        If no candidate is found, no action will be taken.
 * @param preprocessingReplacements a map of replacements to apply to the function before running it.
 *        All replacements will be applied before the function is run.
 */
fun runFunctions(server: MinecraftServer, functionName: String, preprocessingReplacements: Map<String, *>) =
    server.runCommand(functions / functionName, preprocessingReplacements)
