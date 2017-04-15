/*
 * MIT License
 *
 * Copyright (c) 2017 Frederik Ar. Mikkelsen
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.frederikam.gensokyobot.audio;

import com.frederikam.gensokyobot.commandmeta.MessagingException;
import com.frederikam.gensokyobot.feature.I18n;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.audio.AudioSendHandler;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.managers.AudioManager;
import net.dv8tion.jda.core.utils.PermissionUtil;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GuildPlayer extends AudioEventAdapter implements AudioSendHandler {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(GuildPlayer.class);

    public final JDA jda;
    final String guildId;
    public final Map<String, VideoSelection> selections = new HashMap<>();
    private TextChannel currentTC;

    @SuppressWarnings("LeakingThisInConstructor")
    public GuildPlayer(JDA jda, Guild guild) {
        this.jda = jda;
        this.guildId = guild.getId();

        AudioManager manager = guild.getAudioManager();
        manager.setSendingHandler(this);
    }

    public void joinChannel(Member usr) throws MessagingException {
        VoiceChannel targetChannel = getUserCurrentVoiceChannel(usr);
        joinChannel(targetChannel);
    }

    public void joinChannel(VoiceChannel targetChannel) throws MessagingException {
        if (targetChannel == null) {
            throw new MessagingException(I18n.get(getGuild()).getString("playerUserNotInChannel"));
        }

        if (!PermissionUtil.checkPermission(targetChannel, targetChannel.getGuild().getSelfMember(), Permission.VOICE_CONNECT)
                && !targetChannel.getMembers().contains(getGuild().getSelfMember())) {
            throw new MessagingException(I18n.get(getGuild()).getString("playerJoinConnectDenied"));
        }

        if (!PermissionUtil.checkPermission(targetChannel, targetChannel.getGuild().getSelfMember(), Permission.VOICE_SPEAK)) {
            throw new MessagingException(I18n.get(getGuild()).getString("playerJoinSpeakDenied"));
        }

        AudioManager manager = getGuild().getAudioManager();

        manager.openAudioConnection(targetChannel);

        log.info("Connected to voice channel " + targetChannel);
    }

    public void leaveVoiceChannelRequest(TextChannel channel, boolean silent) {
        AudioManager manager = getGuild().getAudioManager();
        if (!silent) {
            if (manager.getConnectedChannel() == null) {
                channel.sendMessage(I18n.get(getGuild()).getString("playerNotInChannel")).queue();
            } else {
                channel.sendMessage(MessageFormat.format(I18n.get(getGuild()).getString("playerLeftChannel"), getChannel().getName())).queue();
            }
        }
        manager.closeAudioConnection();
    }

    /**
     * May return null if the member is currently not in a channel
     */
    public VoiceChannel getUserCurrentVoiceChannel(Member member) {
        return member.getVoiceState().getChannel();
    }

    public VoiceChannel getChannel() {
        return getUserCurrentVoiceChannel(getGuild().getSelfMember());
    }

    public TextChannel getActiveTextChannel() {
        if (currentTC != null) {
            return currentTC;
        } else {
            log.warn("No currentTC in " + getGuild() + "! Returning public channel...");
            return getGuild().getPublicChannel();
        }

    }

    /**
     * @return Users who are not bots
     */
    public List<Member> getHumanUsersInVC() {
        VoiceChannel vc = getChannel();
        if (vc == null) {
            return new ArrayList<>();
        }

        List<Member> members = vc.getMembers();
        ArrayList<Member> nonBots = new ArrayList<>();
        for (Member member : members) {
            if (!member.getUser().isBot()) {
                nonBots.add(member);
            }
        }
        return nonBots;
    }

    @Override
    public String toString() {
        return "[GP:" + getGuild().getId() + "]";
    }

    public Guild getGuild() {
        return jda.getGuildById(guildId);
    }

    public void setCurrentTC(TextChannel currentTC) {
        this.currentTC = currentTC;
    }

    public TextChannel getCurrentTC() {
        return currentTC;
    }

    @Override
    public boolean canProvide() {
        //TODO
        return false;
    }

    @Override
    public byte[] provide20MsAudio() {
        //TODO
        return new byte[0];
    }

    @Override
    public boolean isOpus() {
        return true;
    }
}
