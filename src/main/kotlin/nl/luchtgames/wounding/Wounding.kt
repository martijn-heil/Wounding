/*
 *
 * Wounding
 * Copyright (C) 2018 Martijn Heil
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package nl.luchtgames.wounding

import org.bukkit.Effect.STEP_SOUND
import org.bukkit.Material
import org.bukkit.Material.*
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.EventPriority.MONITOR
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.lang.Math.random

class Wounding : JavaPlugin() {
    private val wounded = HashMap<LivingEntity, Int>()
    var LivingEntity.isWounded
        get() = wounded.contains(this)
        set(value) { if(value) wounded[this] = 0 else wounded.remove(this) }

    var LivingEntity.bandageAttempts
        get() = wounded[this] ?: 0
        set(value) { if (this.isWounded) wounded[this] = value }

    override fun onEnable() {
        saveDefaultConfig()
        val causes = config.getStringList("wounding.causes").map { EntityDamageEvent.DamageCause.valueOf(it) }

        server.pluginManager.registerEvents(object : Listener {
            @EventHandler(priority = MONITOR)
            fun onPlayerHit(e: EntityDamageEvent) {
                val entity = e.entity
                if (entity is LivingEntity && causes.contains(e.cause) &&
                        chance(config.getInt("wounding.chance"))) {
                    entity.isWounded = true
                }
            }

            @EventHandler(priority = MONITOR)
            fun onLivingEntityDeath(e: EntityDeathEvent) {
                if (e is LivingEntity) e.isWounded = false
            }

            @EventHandler(priority = MONITOR)
            fun onPlayerAttemptBandage(e: PlayerInteractAtEntityEvent) {
                val target = e.rightClicked
                if (target is LivingEntity) {
                    val usedItem: ItemStack? = when (e.hand) {
                        EquipmentSlot.OFF_HAND -> e.player.inventory.itemInOffHand
                        EquipmentSlot.HAND -> e.player.inventory.itemInMainHand
                        else -> null
                    }

                    val bandageLore = config.getString("bandage.lore")
                    if (usedItem != null &&
                            usedItem.type == Material.valueOf(config.getString("bandage.material")) &&
                            usedItem.hasItemMeta() && usedItem.itemMeta.hasLore() &&
                            bandageLore != null &&
                            usedItem.itemMeta.lore.contains(config.getString("bandage.lore"))) {
                        if (attemptToBandage(target)) usedItem.amount--
                    }
                }
            }

            @EventHandler(priority = EventPriority.MONITOR)
            fun onPlayerMove(e: PlayerMoveEvent) {
                var bandageAttempts = e.player.bandageAttempts - 1
                if(bandageAttempts < 0) bandageAttempts = 0
                e.player.bandageAttempts = bandageAttempts
            }
        }, this)

        server.scheduler.scheduleSyncRepeatingTask(this, {
            server.worlds.forEach { it.entities.forEach { if (it is LivingEntity && it.isWounded) bleed(it) } }
        }, 0, config.getLong("bleedInterval"))
    }

    override fun onDisable() {
        wounded.clear()
    }

    private fun attemptToBandage(target: LivingEntity): Boolean {
        val attempts = wounded[target]?.plus(1) ?: return false
        if (attempts >= config.getInt("bandageClicksNeeded")) { target.isWounded = false; return true }
        return false
    }
}

private fun chance(chance: Int) = (random() * 100) < chance

private fun bleed(e: LivingEntity) {
    e.health -= 2
    e.world.playEffect(e.location, STEP_SOUND, REDSTONE_BLOCK)
    e.world.spawnFallingBlock(e.location, REDSTONE_WIRE, 0x00).dropItem = false
}