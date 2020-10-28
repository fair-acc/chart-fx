package de.gsi.microservice.concepts.cmwlight;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.*;

/**
 * A lightweight implementation of the CMW RDA client part.
 * Reads all sockets from a single Thread, which can also be embedded into other event loops.
 * Manages connection state and automatically reconnects broken connections.
 */
public class CmwLightClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(CmwLightClient.class);
    private static final AtomicLong connectionIdGenerator = new AtomicLong(0); // global counter incremented for each connection
    private static final AtomicInteger requestIdGenerator = new AtomicInteger(0);
    private final AtomicInteger channelId = new AtomicInteger(0); // connection local counter incremented for each channel
    private final ZMQ.Context context;
    private final ZMQ.Socket controlChannel;
    private final AtomicReference<ConnectionState> connectionState = new AtomicReference<>(ConnectionState.DISCONNECTED);
    private final String address;
    private String sessionId;
    private long connectionId;
    Map<Long, Subscription> subscriptions = Collections.synchronizedMap(new HashMap<>()); // all subscriptions added to the server
    // private final List<QueuedRequests> queuedRequests = new LimitedArrayList(); // all requests which are waiting for a reply/timeout from the server
    private long lastHbReceived = -1;
    private long lastHbSent = -1;
    private final int heartbeatInterval = 1000; // time between to heartbeats in ms
    private final int heartbeatAllowedMisses = 3; // number of heartbeats which can be missed before resetting the conection
    private final long subscriptionTimeout = 1000; // maximum time after which a connection should be reconnected
    private int backOff = 20;

    public void unsubscribe(final Subscription subscription) throws CmwLightProtocol.RdaLightException {
        subscriptions.remove(subscription);
        sendUnsubscribe(subscription);
    }

    public enum ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }

    public CmwLightClient(String address, ZMQ.Context context) {
        LOGGER.atTrace().addArgument(address).log("connecting to: {}");
        this.context = context;
        controlChannel = context.socket(SocketType.DEALER);
        controlChannel.setIdentity(getIdentity().getBytes()); // hostname/process/id/channel
        controlChannel.setSndHWM(0);
        controlChannel.setRcvHWM(0);
        controlChannel.setLinger(0);
        this.address = address;
        this.sessionId = getSessionId();
    }

    public String getAddress() {
        return address;
    }

    public ConnectionState getConnectionState() {
        return connectionState.get();
    }

    public ZMQ.Context getContext() {
        return context;
    }

    public ZMQ.Socket getSocket() {
        return controlChannel;
    }

    private String getIdentity() {
        String hostname;
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            hostname = "localhost";
        }
        final long processId = ProcessHandle.current().pid();
        connectionId = connectionIdGenerator.incrementAndGet();
        final int channelId = this.channelId.incrementAndGet();
        return hostname + '/' + processId + '/' + connectionId + '/' + channelId;
    }

    private String getSessionId() {
        return "asdf";
    }

    public void connect() throws CmwLightProtocol.RdaLightException {
        if (connectionState.getAndSet(ConnectionState.CONNECTING) != ConnectionState.DISCONNECTED) {
            return;
        }
        controlChannel.connect(address);
        CmwLightProtocol.serialiseMsg(CmwLightMessage.connect("1.0.0")).send(controlChannel);
        connectionState.set(ConnectionState.CONNECTING);
        lastHbReceived = System.currentTimeMillis();
    }

    private void resetConnection() throws CmwLightProtocol.RdaLightException {
        disconnect();
        connect();
    }

    private void disconnect() {
        connectionState.set(ConnectionState.DISCONNECTED);
        controlChannel.disconnect(address);
        // disconnect/reset subscriptions
    }

    public CmwLightMessage receiveData() throws CmwLightProtocol.RdaLightException {
        final long currentTime = System.currentTimeMillis();
        if (housekeeping(currentTime))
            return null; // heartbeats and subscription timeouts/connects
        // receive data
        final ZMsg data = ZMsg.recvMsg(controlChannel, ZMQ.DONTWAIT);
        if (data == null)
            return null;
        final CmwLightMessage reply = CmwLightProtocol.parseMsg(data);
        if (connectionState.get().equals(ConnectionState.CONNECTING) && reply.messageType == CmwLightProtocol.MessageType.SERVER_CONNECT_ACK) {
            connectionState.set(ConnectionState.CONNECTED);
            backOff = 20; // reset back-off time
            return reply;
        }
        if (connectionState.get() != ConnectionState.CONNECTED) {
            LOGGER.atWarn().addArgument(reply).log("received data before connection established: {}");
        }
        if (reply.requestType == CmwLightProtocol.RequestType.SUBSCRIBE_EXCEPTION) {
            final long id = reply.id;
            final Subscription sub = subscriptions.get(id);
            sub.subscriptionState = SubscriptionState.UNSUBSCRIBED;
            sub.timeoutValue = currentTime + sub.backOff;
            sub.backOff *= 2;
        }
        if (reply.requestType == CmwLightProtocol.RequestType.SUBSCRIBE) {
            final long id = reply.id;
            final Subscription sub = subscriptions.get(id);
            sub.updateId = (long) reply.options.get(CmwLightProtocol.FieldName.SOURCE_ID_TAG.value());
            sub.subscriptionState = SubscriptionState.SUBSCRIBED;
            sub.backOff = 20;
        }
        if (reply != null) {
            lastHbReceived = currentTime;
        }
        return reply;
    }

    public boolean housekeeping(final long currentTime) throws CmwLightProtocol.RdaLightException {
        switch (connectionState.get()) {
        case DISCONNECTED: // reconnect after adequate back off
            if (currentTime > lastHbSent + backOff) {
                connect();
            }
            break;
        case CONNECTING:
            if (currentTime > lastHbReceived + heartbeatInterval * heartbeatAllowedMisses) { // connect timed out -> increase back of and retry
                backOff = backOff * 2;
                resetConnection();
                return true;
            }
            break;
        case CONNECTED:
            if (currentTime > lastHbSent + heartbeatInterval) { // check for heartbeat interval
                // send Heartbeats
                sendHeartBeat();
                lastHbSent = currentTime;
                // check if heartbeat was received
                if (lastHbReceived + heartbeatInterval * heartbeatAllowedMisses < currentTime) {
                    LOGGER.atInfo().log("Connection timed out, reconnecting");
                    resetConnection();
                    return true;
                }
                // check timeouts of connection/subscription requests
                for (Subscription sub : subscriptions.values()) {
                    switch (sub.subscriptionState) {
                    case SUBSCRIBING:
                        // check timeout
                        if (currentTime > sub.timeoutValue) {
                            sub.subscriptionState = SubscriptionState.UNSUBSCRIBED;
                            sub.timeoutValue = currentTime + backOff;
                            backOff = backOff * 2; // exponential back of
                            LOGGER.atInfo().addArgument(sub.toString()).log("Subscription did not connect, retrying in {}ms: {}");
                        }
                        break;
                    case UNSUBSCRIBED:
                        if (currentTime > sub.timeoutValue) {
                            sendSubscribe(sub);
                        }
                        break;
                    case SUBSCRIBED:
                        // do nothing
                        break;
                    }
                }
            }
            break;
        }
        return false;
    }

    public void subscribe(final String device, final String property, final String selector) {
        subscribe(device, property, selector, null);
    }

    public void subscribe(final String device, final String property, final String selector, final Map<String, Object> filters) {
        subscribe(new Subscription(device, property, selector, filters));
    }

    public void subscribe(final Subscription sub) {
        subscriptions.put(sub.id, sub);
    }

    public void sendHeartBeat() throws CmwLightProtocol.RdaLightException {
        CmwLightProtocol.sendMsg(controlChannel, CmwLightMessage.CLIENT_HB);
    }

    private void sendSubscribe(final Subscription sub) throws CmwLightProtocol.RdaLightException {
        if (!sub.subscriptionState.equals(SubscriptionState.UNSUBSCRIBED)) {
            return; // already subscribed/subscription in progress
        }
        CmwLightProtocol.sendMsg(controlChannel, CmwLightMessage.subscribeRequest(
                                                         sessionId, sub.id, sub.device, sub.property,
                                                         Map.of(CmwLightProtocol.FieldName.SESSION_BODY_TAG.value(), Collections.<String, Object>emptyMap()),
                                                         new CmwLightMessage.RequestContext(sub.selector, sub.filters, null),
                                                         CmwLightProtocol.UpdateType.IMMEDIATE_UPDATE));
        sub.subscriptionState = SubscriptionState.SUBSCRIBING;
        sub.timeoutValue = System.currentTimeMillis() + subscriptionTimeout;
    }

    private void sendUnsubscribe(final Subscription sub) throws CmwLightProtocol.RdaLightException {
        if (sub.subscriptionState.equals(SubscriptionState.UNSUBSCRIBED)) {
            return; // not currently subscribed to this property
        }
        CmwLightProtocol.sendMsg(controlChannel, CmwLightMessage.unsubscribeRequest(
                                                         sessionId, sub.updateId, sub.device, sub.property,
                                                         Map.of(CmwLightProtocol.FieldName.SESSION_BODY_TAG.value(), Collections.<String, Object>emptyMap()),
                                                         CmwLightProtocol.UpdateType.IMMEDIATE_UPDATE));
    }

    public static class Subscription {
        public final String property;
        public final String device;
        public final String selector;
        public final Map<String, Object> filters;
        private SubscriptionState subscriptionState = SubscriptionState.UNSUBSCRIBED;
        private int backOff = 20;
        private final long id = requestIdGenerator.incrementAndGet();
        private long updateId = -1;
        private long timeoutValue = -1;

        public Subscription(final String device, final String property, final String selector, final Map<String, Object> filters) {
            this.property = property;
            this.device = device;
            this.selector = selector;
            this.filters = filters;
        }

        @Override
        public String toString() {
            return "Subscription{"
                    + "property='" + property + '\'' + ", device='" + device + '\'' + ", selector='" + selector + '\'' + ", filters=" + filters + ", subscriptionState=" + subscriptionState + ", backOff=" + backOff + ", id=" + id + ", updateId=" + updateId + ", timeoutValue=" + timeoutValue + '}';
        }
    }

    public enum SubscriptionState {
        UNSUBSCRIBED,
        SUBSCRIBING,
        SUBSCRIBED
    }
}
