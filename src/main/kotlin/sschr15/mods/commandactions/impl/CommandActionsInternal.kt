package sschr15.mods.commandactions.impl

import net.minecraft.server.MinecraftServer
import net.minecraft.server.function.CommandFunction
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier
import org.quiltmc.loader.api.ModContainer
import org.quiltmc.loader.api.QuiltLoader
import org.quiltmc.qkl.wrapper.qsl.lifecycle.onServerReady
import org.quiltmc.qkl.wrapper.qsl.registerEvents
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer
import org.slf4j.LoggerFactory
import sschr15.mods.commandactions.impl.processing.preprocess
import sschr15.mods.commandactions.impl.processing.tokenize
import java.nio.file.Path
import kotlin.io.path.*

private val tempId = Identifier("commandactions", "temp")
internal val functions = QuiltLoader.getConfigDir() / "command-actions" / "functions"

private val logger = LoggerFactory.getLogger("cmd-actions")

internal fun MinecraftServer.runCommand(location: Path, replacements: Map<String, *>) {
    if (location.isDirectory()) {
        location.forEach { runCommand(it, replacements) }
    } else if (location.notExists() && location.resolveSibling(location.nameWithoutExtension + ".mcfunction").exists()) {
        runCommand(location.resolveSibling(location.nameWithoutExtension + ".mcfunction"), replacements)
    } else if (location.exists()) {
        val content = location.readText()
        val tokenized = tokenize(content)
        val replaced = preprocess(tokenized, replacements.map { (k, v) -> "C_$k" to v.toString() }.toMap())
        val commandLines = replaced.joinToString("") { it.value }.trim().lines()
        val func = try {
            CommandFunction.create(tempId, commandManager.dispatcher, commandSource, commandLines)
        } catch (e: IllegalArgumentException) {
            logger.error("Error in $location", e)
            logger.error(commandLines.joinToString("\n"))
            return
        }
        commandFunctionManager.execute(func, commandSource)
    }
}

object CommandActionsInternal : ModInitializer {
    private val MinecraftServer.serverArgs get() = mapOf(
        "serverName" to name,
        "isSinglePlayer" to isSingleplayer,
        "online" to isOnlineMode,
        "motd" to serverMotd,
    )

    private val ServerPlayerEntity.playerArgs get() = server.serverArgs + listOf(
        "playerName" to name.string,
        "playerUUID" to uuid,
        "player" to name.string,
        "auth" to publicKey,
        "firstJoin" to firstJoin,
    )

    override fun onInitialize(mod: ModContainer) {
        functions.createDirectories()
//        version = mod.metadata().version()
        registerEvents {
            onServerReady {
                runCommand(functions / "server-ready", serverArgs)
            }
        }
    }

    fun onPlayerJoin(player: ServerPlayerEntity) {
        player.server.runCommand(functions / "player-join", player.playerArgs)
    }
}
