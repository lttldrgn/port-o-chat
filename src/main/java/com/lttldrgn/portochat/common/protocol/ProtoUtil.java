/*
 *  This file is a part of port-o-chat.
 * 
 *  port-o-chat is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.lttldrgn.portochat.common.protocol;

import com.google.protobuf.ByteString;
import com.lttldrgn.portochat.common.User;
import com.lttldrgn.portochat.proto.Portochat.ChannelJoin;
import com.lttldrgn.portochat.proto.Portochat.ChannelList;
import com.lttldrgn.portochat.proto.Portochat.ChannelPart;
import com.lttldrgn.portochat.proto.Portochat.ChatMessage;
import com.lttldrgn.portochat.proto.Portochat.Notification;
import com.lttldrgn.portochat.proto.Portochat.Ping;
import com.lttldrgn.portochat.proto.Portochat.Pong;
import com.lttldrgn.portochat.proto.Portochat.PortoChatMessage;
import com.lttldrgn.portochat.proto.Portochat.Request;
import com.lttldrgn.portochat.proto.Portochat.Response;
import com.lttldrgn.portochat.proto.Portochat.UserConnectionStatus;
import com.lttldrgn.portochat.proto.Portochat.UserData;
import com.lttldrgn.portochat.proto.Portochat.UserList;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Class to assist in making protobuf messages
 * @author Brandon
 */
public class ProtoUtil {
    public static PortoChatMessage createChannelListRequest() {
        PortoChatMessage.Builder appMessage = PortoChatMessage.newBuilder();
        Request.Builder request = Request.newBuilder();
        request.setRequestType(Request.RequestType.ChannelList);
        appMessage.setRequest(request);
        return appMessage.build();
    }

    public static PortoChatMessage createChannelList(List<String> channels) {
        PortoChatMessage.Builder appMessage = PortoChatMessage.newBuilder();
        ChannelList.Builder channelBuilder = ChannelList.newBuilder();
        channelBuilder.getChannelsBuilder().addAllValues(channels);
        appMessage.setChannelList(channelBuilder);
        return appMessage.build();
    }

    public static PortoChatMessage createChannelUserListRequest(String channelName) {
        PortoChatMessage.Builder appMessage = PortoChatMessage.newBuilder();
        Request.Builder request = Request.newBuilder();
        request.setRequestType(Request.RequestType.ChannelUserList);
        request.getStringRequestDataBuilder().setValue(channelName);
        appMessage.setRequest(request);
        return appMessage.build();
    }

    public static PortoChatMessage createUserListRequest() {
        PortoChatMessage.Builder appMessage = PortoChatMessage.newBuilder();
        Request.Builder request = Request.newBuilder();
        request.setRequestType(Request.RequestType.UserList);
        appMessage.setRequest(request);
        return appMessage.build();
    }

    public static PortoChatMessage createSetUserNameRequest(String username) {
        PortoChatMessage.Builder appMessage = PortoChatMessage.newBuilder();
        Request.Builder request = Request.newBuilder();
        request.setRequestId(UUID.randomUUID().toString());
        request.setRequestType(Request.RequestType.SetUserName);
        request.getStringRequestDataBuilder().setValue(username);
        appMessage.setRequest(request);
        return appMessage.build();
    }

    public static PortoChatMessage createChannelJoinRequest(String channel) {
        PortoChatMessage.Builder appMessage = PortoChatMessage.newBuilder();
        Request.Builder request = Request.newBuilder();
        request.setRequestId(UUID.randomUUID().toString());
        request.setRequestType(Request.RequestType.ChannelJoin);
        request.getStringRequestDataBuilder().setValue(channel);
        appMessage.setRequest(request);
        return appMessage.build();
    }

    public static PortoChatMessage createChannelJoinNotification(String channel, String userId) {
        PortoChatMessage.Builder appMessage = PortoChatMessage.newBuilder();
        Notification.Builder notification = Notification.newBuilder();
        ChannelJoin.Builder channelJoin = notification.getChannelJoinBuilder();
        channelJoin.setChannel(channel);
        channelJoin.setUserId(userId);
        appMessage.setNotification(notification);
        return appMessage.build();
    }

    public static PortoChatMessage createChannelPartNotification(String channel, String userId) {
        PortoChatMessage.Builder appMessage = PortoChatMessage.newBuilder();
        Notification.Builder notification = Notification.newBuilder();
        ChannelPart.Builder channelJoinPart = notification.getChannelPartBuilder();
        channelJoinPart.setChannel(channel);
        channelJoinPart.setUserId(userId);
        appMessage.setNotification(notification);
        return appMessage.build();
    }

    public static PortoChatMessage createChannelAddedNotification(String channel) {
        PortoChatMessage.Builder appMessage = PortoChatMessage.newBuilder();
        Notification.Builder notification = appMessage.getNotificationBuilder();
        notification.getChannelAddedBuilder().setChannel(channel);
        return appMessage.build();
    }

    public static PortoChatMessage createChannelRemovedNotification(String channel) {
        PortoChatMessage.Builder appMessage = PortoChatMessage.newBuilder();
        Notification.Builder notification = appMessage.getNotificationBuilder();
        notification.getChannelRemovedBuilder().setChannel(channel);
        return appMessage.build();
    }

    public static PortoChatMessage createUserList(List<User> users, String channel) {
        PortoChatMessage.Builder appMessage = PortoChatMessage.newBuilder();
        UserList.Builder userList = appMessage.getUserListBuilder();
        for (User user : users) {
            userList.addUsers(convertToUserData(user));
        }
        if (channel != null) {
            userList.setChannel(channel);
        }
        return appMessage.build();
    }

    private static UserData convertToUserData(User user) {
        UserData.Builder userData = UserData.newBuilder();
        userData.setId(user.getId());
        userData.setName(user.getName());
        userData.setHost(user.getHost());
        return userData.build();
    }

    public static User convertToUser(UserData userData) {
        User user = new User();
        user.setId(userData.getId());
        user.setName(userData.getName());
        user.setHost(userData.getHost());
        return user;
    }

    public static List<User> getUserList(UserList userList) {
        ArrayList<User> users = new ArrayList<>();
        for (UserData data : userList.getUsersList()) {
            User user = convertToUser(data);
            users.add(user);
        }
        return users;
    }

    public static PortoChatMessage createUserConnectionStatus(User user, boolean connected) {
        PortoChatMessage.Builder appMessage = PortoChatMessage.newBuilder();
        UserConnectionStatus.Builder status = appMessage.getNotificationBuilder().getUserConnectionStatusBuilder();
        if (user != null) {
            status.setUser(convertToUserData(user));
        }
        status.setConnected(connected);
        return appMessage.build();
    }

    public static PortoChatMessage createPing(long timestamp) {
        PortoChatMessage.Builder appMessage = PortoChatMessage.newBuilder();
        Ping.Builder ping = appMessage.getPingBuilder();
        ping.setTimestamp(timestamp);
        return appMessage.build();
    }

    public static PortoChatMessage createPong(long timestamp) {
        PortoChatMessage.Builder appMessage = PortoChatMessage.newBuilder();
        Pong.Builder pong = appMessage.getPongBuilder();
        pong.setTimestamp(timestamp);
        return appMessage.build();
    }

    /**
     * Create a Chat message
     * @param senderId Id of the sender
     * @param destinationId ID of the destination (user or channel)
     * @param isChannel Set true if destined for a channel
     * @param message Message to send
     * @param action Set true if the message is an action
     * @return PortoChatMessage containing the chat message
     */
    public static PortoChatMessage createChatMessage(String senderId, String destinationId, boolean isChannel, String message, boolean action) {
        PortoChatMessage.Builder appMessage = PortoChatMessage.newBuilder();
        ChatMessage.Builder chatMessage = appMessage.getChatMessageBuilder();
        if (senderId != null) {
            chatMessage.setSenderId(senderId);
        }
        chatMessage.setDestinationId(destinationId);
        chatMessage.setIsChannel(isChannel);
        chatMessage.setMessage(message);
        chatMessage.setIsAction(action);
        return appMessage.build();
    }

    public static PortoChatMessage createSetPublicKey(byte[] encodedKey) {
        PortoChatMessage.Builder appMessage = PortoChatMessage.newBuilder();
        Request.Builder request = appMessage.getRequestBuilder();
        request.setRequestType(Request.RequestType.SetUserPublicKey);
        request.setByteData(ByteString.copyFrom(encodedKey));
        return appMessage.build();
    }

    public static PortoChatMessage createSetServerSharedKey(byte[] encodedKey) {
        PortoChatMessage.Builder appMessage = PortoChatMessage.newBuilder();
        Request.Builder request = appMessage.getRequestBuilder();
        request.setRequestId(UUID.randomUUID().toString());
        request.setRequestType(Request.RequestType.SetServerSharedKey);
        request.setByteData(ByteString.copyFrom(encodedKey));
        return appMessage.build();
    }

    public static PortoChatMessage createServerKeyAccepted(String requestId) {
        PortoChatMessage.Builder appMessage = PortoChatMessage.newBuilder();
        Response.Builder response = appMessage.getResponseBuilder();
        if (requestId != null && !requestId.isEmpty()) {
            response.setRequestId(requestId);
        }
        response.setResponseType(Response.ResponseType.ServerKeyAccepted);
        return appMessage.build();
    }
}
