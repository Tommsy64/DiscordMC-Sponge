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

import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.slf4j.Logger;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextTemplate;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.text.channel.MessageReceiver;
import org.spongepowered.api.text.format.TextColors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import static org.spongepowered.api.text.TextTemplate.arg;

import lombok.Getter;
import lombok.Setter;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.RateLimitException;
import sx.blah.discord.util.RequestBuffer;

public class DiscordInterface implements MessageReceiver {

    private static final TextTemplate DEFAULT_TEMPLATE = TextTemplate.of(
            TextColors.AQUA, "[D]", TextColors.WHITE, "<", arg("user").color(TextColors.WHITE),
            ">", arg("message"));
    private static final String PLAYING_TEXT = "Minecraft";

    private static final List<String> RECEIVE_CHANNELS_NAMES = ImmutableList.of("minecraft");
    private static final List<String> TRANSMIT_CHANNELS_NAMES = RECEIVE_CHANNELS_NAMES;

    private final IDiscordClient client;
    private final Logger log;

    private List<IChannel> receiveChannels = new LinkedList<>();
    private List<IChannel> transmitChannels = new LinkedList<>();

    DiscordInterface(Logger logger, String token) {
        this.log = logger;
        client = new ClientBuilder().withToken(token).build();
        client.getDispatcher().registerListener(this);
    }

    @EventSubscriber
    public void onReady(ReadyEvent event) {
        log.trace("Discord interface ready to interact with API.");
        IDiscordClient client = event.getClient();
        try {
            client.login();
        } catch (DiscordException e) {
            log.error("Error logging in.", e);
            return;
        } catch (RateLimitException e) {
            log.error("DiscordMC has exceeded the Discord api rate limit.", e);
            return;
        }
        client.online(PLAYING_TEXT);
        List<IChannel> channels = client.getChannels(false);
        for (IChannel channel : channels) {
            if (RECEIVE_CHANNELS_NAMES.contains(channel.getName()))
                receiveChannels.add(channel);
            if (TRANSMIT_CHANNELS_NAMES.contains(channel.getName()))
                transmitChannels.add(channel);
        }
        log.info("Successfully logged in with '{}'", client.getOurUser().getName());
    }

    @EventSubscriber
    public void onMessageReceived(MessageReceivedEvent event) {
        final IUser author = event.getAuthor();
        final Text hoverText = Text.builder(author.getName() + "#" + author.getDiscriminator()).toText();
        messageChannel.send(DEFAULT_TEMPLATE.apply(ImmutableMap.of(
                "user", Text.builder(author.getName()).onHover(TextActions.showText(hoverText)),
                "player", Text.of(event.getMessage()))).build());
    }

    @Getter
    @Setter
    private MessageChannel messageChannel = MessageChannel.TO_ALL;

    private final Queue<String> queuedMessages = new ArrayDeque<>();

    @Override
    public void sendMessage(Text message) {
        if (!client.isReady() || !client.isLoggedIn())
            queuedMessages.add(message.toPlain());
        else {
            for (IChannel channel : transmitChannels)
                RequestBuffer.request(() -> {
                    try {
                        channel.sendMessage(message.toPlain());
                    } catch (DiscordException e) {
                        log.warn("Could not send message to Discord.", e);
                    }
                });
        }
    }

    @Listener
    public void onServerStop(GameStoppingServerEvent event) {
        try {
            client.logout();
        } catch (DiscordException e) {
            log.warn("Could not logout.");
        }
    }
}
