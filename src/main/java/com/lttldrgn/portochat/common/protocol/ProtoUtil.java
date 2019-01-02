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

import com.lttldrgn.portochat.proto.Portochat;
import com.lttldrgn.portochat.proto.Portochat.ChannelJoin;
import com.lttldrgn.portochat.proto.Portochat.ChannelPart;
import java.util.UUID;

/**
 * Class to assist in making protobuf messages
 * @author Brandon
 */
public class ProtoUtil {
    public static Portochat.PortoChatMessage createChannelListRequest() {
        Portochat.PortoChatMessage.Builder appMessage = Portochat.PortoChatMessage.newBuilder();
        Portochat.Request.Builder request = Portochat.Request.newBuilder();
        request.setRequestType(Portochat.Request.RequestType.ChannelList);
        appMessage.setRequest(request);
        return appMessage.build();
    }

    public static Portochat.PortoChatMessage createChannelUserListRequest(String channelName) {
        Portochat.PortoChatMessage.Builder appMessage = Portochat.PortoChatMessage.newBuilder();
        Portochat.Request.Builder request = Portochat.Request.newBuilder();
        request.setRequestType(Portochat.Request.RequestType.ChannelUserList);
        request.getStringRequestDataBuilder().setValue(channelName);
        appMessage.setRequest(request);
        return appMessage.build();
    }

    public static Portochat.PortoChatMessage createUserListRequest() {
        Portochat.PortoChatMessage.Builder appMessage = Portochat.PortoChatMessage.newBuilder();
        Portochat.Request.Builder request = Portochat.Request.newBuilder();
        request.setRequestType(Portochat.Request.RequestType.UserList);
        appMessage.setRequest(request);
        return appMessage.build();
    }

    public static Portochat.PortoChatMessage createSetUserNameRequest(String username) {
        Portochat.PortoChatMessage.Builder appMessage = Portochat.PortoChatMessage.newBuilder();
        Portochat.Request.Builder request = Portochat.Request.newBuilder();
        request.setRequestId(UUID.randomUUID().toString());
        request.setRequestType(Portochat.Request.RequestType.SetUserName);
        request.getStringRequestDataBuilder().setValue(username);
        appMessage.setRequest(request);
        return appMessage.build();
    }

    public static Portochat.PortoChatMessage createChannelJoinRequest(String channel) {
        Portochat.PortoChatMessage.Builder appMessage = Portochat.PortoChatMessage.newBuilder();
        Portochat.Request.Builder request = Portochat.Request.newBuilder();
        request.setRequestId(UUID.randomUUID().toString());
        request.setRequestType(Portochat.Request.RequestType.ChannelJoin);
        request.getStringRequestDataBuilder().setValue(channel);
        appMessage.setRequest(request);
        return appMessage.build();
    }

    public static Portochat.PortoChatMessage createChannelJoinNotification(String channel, String userId) {
        Portochat.PortoChatMessage.Builder appMessage = Portochat.PortoChatMessage.newBuilder();
        Portochat.Notification.Builder notification = Portochat.Notification.newBuilder();
        ChannelJoin.Builder channelJoin = notification.getChannelJoinBuilder();
        channelJoin.setChannel(channel);
        channelJoin.setUserId(userId);
        appMessage.setNotification(notification);
        return appMessage.build();
    }

    public static Portochat.PortoChatMessage createChannelPartNotification(String channel, String userId) {
        Portochat.PortoChatMessage.Builder appMessage = Portochat.PortoChatMessage.newBuilder();
        Portochat.Notification.Builder notification = Portochat.Notification.newBuilder();
        ChannelPart.Builder channelJoinPart = notification.getChannelPartBuilder();
        channelJoinPart.setChannel(channel);
        channelJoinPart.setUserId(userId);
        appMessage.setNotification(notification);
        return appMessage.build();
    }
}
