@file:Suppress("FunctionName")

package sschr15.mods.commandactions.mixin

import net.minecraft.nbt.NbtCompound
import net.minecraft.network.ClientConnection
import net.minecraft.server.PlayerManager
import net.minecraft.server.network.ServerPlayerEntity
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Unique
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import sschr15.mods.commandactions.impl.CommandActionsInternal
import sschr15.mods.commandactions.impl.ServerPlayerFirstJoin
import sschr15.mods.commandactions.impl.firstJoin

@Mixin(PlayerManager::class)
class PlayerManagerMixin {
    @Inject(method = ["onPlayerConnect"], at = [At(value = "INVOKE", target = "net/minecraft/server/network/ServerPlayerEntity.onSpawn()V")])
    private fun `COMMANDS - Trigger player join command`(connection: ClientConnection, player: ServerPlayerEntity, ci: CallbackInfo) {
        CommandActionsInternal.onPlayerJoin(player)
    }

    @Inject(method = ["loadPlayerData"], at = [At("RETURN")])
    private fun `COMMANDS - Mark if player's first join`(player: ServerPlayerEntity, cir: CallbackInfoReturnable<NbtCompound?>) {
        player.firstJoin = cir.returnValue == null
    }
}

@Mixin(ServerPlayerEntity::class)
class ServerPlayerEntityMixin : ServerPlayerFirstJoin {
    @field:Unique
    override var `COMMANDS - firstJoin` = false
}
