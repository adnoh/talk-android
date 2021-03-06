/*
 * Nextcloud Talk application
 *
 * @author Mario Danic
 * Copyright (C) 2017-2018 Mario Danic <mario@lovelyhq.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.nextcloud.talk.models.json.chat;

import android.text.TextUtils;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonIgnore;
import com.bluelinelabs.logansquare.annotation.JsonObject;
import com.nextcloud.talk.R;
import com.nextcloud.talk.application.NextcloudTalkApplication;
import com.nextcloud.talk.models.json.converters.EnumSystemMessageTypeConverter;
import com.nextcloud.talk.utils.ApiUtils;
import com.nextcloud.talk.utils.TextMatchers;
import com.stfalcon.chatkit.commons.models.IMessage;
import com.stfalcon.chatkit.commons.models.IUser;
import com.stfalcon.chatkit.commons.models.MessageContentType;

import org.parceler.Parcel;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import androidx.annotation.Nullable;
import lombok.Data;

@Parcel
@Data
@JsonObject
public class ChatMessage implements IMessage, MessageContentType, MessageContentType.Image {
    List<MessageType> messageTypesToIgnore = Arrays.asList(MessageType.REGULAR_TEXT_MESSAGE,
            MessageType.SYSTEM_MESSAGE, MessageType.SINGLE_LINK_VIDEO_MESSAGE,
            MessageType.SINGLE_LINK_AUDIO_MESSAGE, MessageType.SINGLE_LINK_MESSAGE);

    private boolean hasFileAttachment() {
        if (messageParameters != null && messageParameters.size() > 0) {
            for (String key : messageParameters.keySet()) {
                Map<String, String> individualHashMap = messageParameters.get(key);
                if (individualHashMap.get("type").equals("file")) {
                    return true;
                }
            }
        }

        return false;
    }

    @Nullable
    @Override
    public String getImageUrl() {
        if (messageParameters != null && messageParameters.size() > 0) {
            for (String key : messageParameters.keySet()) {
                Map<String, String> individualHashMap = messageParameters.get(key);
                if (individualHashMap.get("type").equals("file")) {
                    selectedIndividualHashMap = individualHashMap;
                    return String.format(Locale.getDefault(),
                            "%s/index.php/core/preview?fileId=%s&x=%d&y=%d&forceIcon=1",
                            baseUrl, individualHashMap.get("id"), 480, 480);
                }
            }
        }

        if (!messageTypesToIgnore.contains(getMessageType())) {
            return getMessage().trim();
        }

        return null;
    }

    public MessageType getMessageType() {
        if (!TextUtils.isEmpty(getSystemMessage())) {
            return MessageType.SYSTEM_MESSAGE;
        }

        if (hasFileAttachment()) {
            return MessageType.SINGLE_NC_ATTACHMENT_MESSAGE;
        }

        return TextMatchers.getMessageTypeFromString(getText());
    }

    public enum MessageType {
        REGULAR_TEXT_MESSAGE,
        SYSTEM_MESSAGE,
        SINGLE_LINK_GIPHY_MESSAGE,
        SINGLE_LINK_TENOR_MESSAGE,
        SINGLE_LINK_GIF_MESSAGE,
        SINGLE_LINK_MESSAGE,
        SINGLE_LINK_VIDEO_MESSAGE,
        SINGLE_LINK_IMAGE_MESSAGE,
        SINGLE_LINK_AUDIO_MESSAGE,
        SINGLE_NC_ATTACHMENT_MESSAGE,
    }

    public enum SystemMessageType {
        DUMMY,
        CONVERSATION_CREATED,
        CONVERSATION_RENAMED,
        CALL_STARTED,
        CALL_JOINED,
        CALL_LEFT,
        CALL_ENDED,
        GUESTS_ALLOWED,
        GUESTS_DISALLOWED,
        PASSWORD_SET,
        PASSWORD_REMOVED,
        USER_ADDED,
        USER_REMOVED,
        MODERATOR_PROMOTED,
        MODERATOR_DEMOTED,
        FILE_SHARED,
    }

    String baseUrl;
    @JsonField(name = "id")
    int jsonMessageId;
    @JsonField(name = "token")
    String token;
    // guests or users
    @JsonField(name = "actorType")
    String actorType;
    @JsonField(name = "actorId")
    String actorId;
    // send when crafting a message
    @JsonField(name = "actorDisplayName")
    String actorDisplayName;
    @JsonField(name = "timestamp")
    long timestamp;
    // send when crafting a message, max 1000 lines
    @JsonField(name = "message")
    String message;
    @JsonField(name = "messageParameters")
    HashMap<String, HashMap<String, String>> messageParameters;

    @JsonField(name = "systemMessage", typeConverter = EnumSystemMessageTypeConverter.class)
    SystemMessageType systemMessageType;

    @JsonIgnore
    public boolean isGrouped;
    @JsonIgnore
    public String activeUserId;
    @JsonIgnore
    public Map<String, String> selectedIndividualHashMap;


    public Map<String, String> getSelectedIndividualHashMap() {
        return selectedIndividualHashMap;
    }

    public void setSelectedIndividualHashMap(Map<String, String> selectedIndividualHashMap) {
        this.selectedIndividualHashMap = selectedIndividualHashMap;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public String getId() {
        return Integer.toString(jsonMessageId);
    }

    @Override
    public String getText() {
        return ChatUtils.getParsedMessage(getMessage(), getMessageParameters());
    }

    public String getLastMessageDisplayText() {
        if (getMessageType().equals(MessageType.REGULAR_TEXT_MESSAGE) || getMessageType().equals(MessageType.SYSTEM_MESSAGE)) {
            return getText();
        } else {
            if (getMessageType().equals(MessageType.SINGLE_LINK_GIPHY_MESSAGE)
                    || getMessageType().equals(MessageType.SINGLE_LINK_TENOR_MESSAGE)
                    || getMessageType().equals(MessageType.SINGLE_LINK_GIF_MESSAGE)) {
                return (NextcloudTalkApplication.getSharedApplication().getString(R.string.nc_sent_a_gif));
            } else if (getMessageType().equals(MessageType.SINGLE_NC_ATTACHMENT_MESSAGE)) {
                return (NextcloudTalkApplication.getSharedApplication().getString(R.string.nc_sent_an_attachment));
            } else if (getMessageType().equals(MessageType.SINGLE_LINK_MESSAGE)) {
                return (NextcloudTalkApplication.getSharedApplication().getString(R.string.nc_sent_a_link));
            } else if (getMessageType().equals(MessageType.SINGLE_LINK_AUDIO_MESSAGE)) {
                return (NextcloudTalkApplication.getSharedApplication().getString(R.string.nc_sent_an_audio));
            } else if (getMessageType().equals(MessageType.SINGLE_LINK_VIDEO_MESSAGE)) {
                return (NextcloudTalkApplication.getSharedApplication().getString(R.string.nc_sent_a_video));
            } else if (getMessageType().equals(MessageType.SINGLE_LINK_IMAGE_MESSAGE)) {
                return (NextcloudTalkApplication.getSharedApplication().getString(R.string.nc_sent_an_image));
            }
        }

        return "";
    }

    @Override
    public IUser getUser() {
        return new IUser() {
            @Override
            public String getId() {
                return actorId;
            }

            @Override
            public String getName() {
                return actorDisplayName;
            }

            @Override
            public String getAvatar() {
                if (getActorType().equals("users")) {
                    return ApiUtils.getUrlForAvatarWithName(getBaseUrl(), actorId, R.dimen.avatar_size);
                } else {
                    return null;
                }
            }
        };
    }

    @Override
    public Date getCreatedAt() {
        return new Date(timestamp * 1000L);
    }

    @Override
    public String getSystemMessage() {
        return new EnumSystemMessageTypeConverter().convertToString(getSystemMessageType());
    }
}
