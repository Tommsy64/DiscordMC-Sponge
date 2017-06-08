/*
 * A Discord to Minecraft (and visa versa) gateway for Sponge.
 * Copyright (C) 2017  Thomas Pakh
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program.  If not, see LICENSE.md at the root of the project.
 */
package com.tommsy.discordmc;

import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameConstructionEvent;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.plugin.Plugin;

import com.google.inject.Inject;

import sx.blah.discord.Discord4J;
import sx.blah.discord.util.DiscordException;

@Plugin(id = "discordmc")
public class DiscordMC {
    @Inject
    private Logger logger;

    private DiscordInterface discord;

    @Listener
    public void onGameConstruction(GameConstructionEvent event) {
        Discord4J.audioDisabled.set(true);
    }

    @Listener
    public void onGamePreInitialization(GamePreInitializationEvent event) {
        // TODO: Do config.
    }

    @Listener
    public void onGameInitialization(GameInitializationEvent event) {
        String token = "";

        try {
            discord = new DiscordInterface(logger, token);
        } catch (DiscordException e) {
            e.printStackTrace();
            logger.error("Error creating Discord Interface!", e);
            return;
        }
        Sponge.getEventManager().registerListeners(this, discord);
    }

    @Listener
    public void onServerStart(GameStartedServerEvent event) {

    }

    @Listener
    public void reload(GameReloadEvent event) {
        // TODO: Reload
    }
}
