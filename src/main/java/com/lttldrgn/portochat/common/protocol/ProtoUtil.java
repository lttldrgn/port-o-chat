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

import com.lttldrgn.portochat.proto.Portochat.ChannelJoin;
import com.lttldrgn.portochat.proto.Portochat.ChannelList;
import com.lttldrgn.portochat.proto.Portochat.ChannelPart;
import com.lttldrgn.portochat.proto.Portochat.Notification;
import com.lttldrgn.portochat.proto.Portochat.PortoChatMessage;
import com.lttldrgn.portochat.proto.Portochat.Request;
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
}
