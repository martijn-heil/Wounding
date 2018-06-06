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
import org.bukkit.Material.REDSTONE_WIRE
import org.bukkit.Material.REDSTONE_BLOCK
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority.MONITOR
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.plugin.java.JavaPlugin
import java.lang.Math.random

class Wounding : JavaPlugin() {
    override fun onEnable() {
        saveDefaultConfig()
        val causes = config.getStringList("causes").map { EntityDamageEvent.DamageCause.valueOf(it) }
        server.pluginManager.registerEvents(object : Listener {
            @EventHandler(priority = MONITOR)
            fun onPlayerHit(e: EntityDamageEvent) {
                val entity = e.entity
                val world = entity.world
                if (entity is Player && causes.contains(e.cause) &&
                        chance(config.getInt("chance"))) {
                    bleed(entity)
                }
            }
        }, this)
    }
}

fun chance(chance: Int) = (random() * 100) < chance

fun bleed(e: LivingEntity) {
    e.health -= 2
    e.world.playEffect(e.location, STEP_SOUND, REDSTONE_BLOCK)
    e.world.spawnFallingBlock(e.location, REDSTONE_WIRE, 0x00).dropItem = false
}