/*
 *
 * Wounding
 * Copyright (C) 2018 martijn
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

import com.github.jengelman.gradle.plugins.shadow.ShadowExtension
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.JavaVersion.VERSION_1_8
import java.net.URI
import org.apache.tools.ant.filters.*
import org.gradle.plugins.ide.idea.model.IdeaLanguageLevel

plugins {
    `java-gradle-plugin`
    kotlin("jvm") version "1.2.41"
    id("com.github.johnrengelman.shadow") version "2.0.3"
    idea
}

group = "nl.luchtgames"
version = "1.0-SNAPSHOT"
description = "Wounding"

apply {
    plugin("java")
    plugin("kotlin")
    plugin("com.github.johnrengelman.shadow")
    plugin("idea")
}

java {
    sourceCompatibility = VERSION_1_8
    targetCompatibility = VERSION_1_8
}

tasks {
    withType<ProcessResources> {
        filter(mapOf(Pair("tokens", mapOf(Pair("version", version)))), ReplaceTokens::class.java)
    }
    withType<ShadowJar> {
        this.classifier = null
        this.configurations = listOf(project.configurations.shadow)
        relocate("com.comphenix.packetwrapper", "shadow.com.comphenix.packetwrapper")
        exclude("com.comphenix.protocol")
    }
}

defaultTasks = listOf("shadowJar")

repositories {
    maven { url = URI("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }

    mavenCentral()
    mavenLocal()
}

idea {
    project {
        languageLevel = IdeaLanguageLevel("1.8")
    }
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

dependencies {
    compileOnly("org.bukkit:bukkit:1.12.2-R0.1-SNAPSHOT") { isChanging = true }
    compileOnly(fileTree("lib") { include("*.jar") })
    shadow(kotlin("stdlib"))
}