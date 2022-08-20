@file:Suppress("PropertyName")

package sschr15.mods.commandactions.impl

import net.minecraft.server.network.ServerPlayerEntity

interface ServerPlayerFirstJoin {
    var `COMMANDS - firstJoin`: Boolean
}

var ServerPlayerEntity.firstJoin: Boolean
    get() = (this as ServerPlayerFirstJoin).`COMMANDS - firstJoin`
    set(value) {
        (this as ServerPlayerFirstJoin).`COMMANDS - firstJoin` = value
    }
