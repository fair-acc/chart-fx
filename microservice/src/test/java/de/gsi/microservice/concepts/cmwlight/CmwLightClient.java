package de.gsi.microservice.concepts.cmwlight;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.*;

import de.gsi.serializer.DataType;
import de.gsi.serializer.FieldDescription;
import de.gsi.serializer.IoClassSerialiser;
import de.gsi.serializer.spi.CmwLightSerialiser;
import de.gsi.serializer.spi.FastByteBuffer;
import de.gsi.serializer.spi.WireDataFieldDescription;

@SuppressWarnings("PMD.UnusedLocalVariable") // Unused variables are taken from the protocol and should be available for reference
public class CmwLightClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(CmwLightClient.class);
    private static final AtomicInteger connectionId = new AtomicInteger(0); // global counter incremented for each connection
    private final AtomicInteger channelId = new AtomicInteger(0); // connection local counter incremented for each channel
    private final ZContext context = new ZContext();
    private final ZMQ.Socket controlChannel;
    private final AtomicReference<ConnectionState> connectionState = new AtomicReference<>(ConnectionState.DISCONNECTED);
    private final String address;
    private final IoClassSerialiser serialiser = new IoClassSerialiser(new FastByteBuffer(0), CmwLightSerialiser.class);

    // Contents of the first frame determine the type of the message
    private static final byte SERVER_CONNECT_ACK = (byte) 0x01;
    private static final byte SERVER_REP = (byte) 0x02;
    private static final byte SERVER_HB = (byte) 0x03;
    private static final byte CLIENT_CONNECT = (byte) 0x20;
    private static final byte CLIENT_REQ = (byte) 0x21;
    private static final byte CLIENT_HB = (byte) 0x22;

    // Message Types in the descriptor (Last part of a message containing the type of each sub message)
    private static final byte MT_HEADER = 0;
    private static final byte MT_BODY = 1; //
    private static final byte MT_BODY_DATA_CONTEXT = 2;
    private static final byte MT_BODY_REQUEST_CONTEXT = 3;
    private static final byte MT_BODY_EXCEPTION = 4;

    // Field names for the Request Header
    public static final String EVENT_TYPE_TAG = "eventType";
    public static final String MESSAGE_TAG = "message";
    public static final String ID_TAG = "0";
    public static final String DEVICE_NAME_TAG = "1";
    public static final String REQ_TYPE_TAG = "2";
    public static final String OPTIONS_TAG = "3";
    public static final String CYCLE_NAME_TAG = "4";
    public static final String ACQ_STAMP_TAG = "5";
    public static final String CYCLE_STAMP_TAG = "6";
    public static final String UPDATE_TYPE_TAG = "7";
    public static final String SELECTOR_TAG = "8";
    public static final String CLIENT_INFO_TAG = "9";
    public static final String NOTIFICATION_ID_TAG = "a";
    public static final String SOURCE_ID_TAG = "b";
    public static final String FILTERS_TAG = "c";
    public static final String DATA_TAG = "x";
    public static final String SESSION_ID_TAG = "d";
    public static final String SESSION_BODY_TAG = "e";
    public static final String PROPERTY_NAME_TAG = "f";

    // request type used in request header REQ_TYPE_TAG
    public static final byte RT_GET = 0;
    public static final byte RT_SET = 1;
    public static final byte RT_CONNECT = 2;
    public static final byte RT_REPLY = 3;
    public static final byte RT_EXCEPTION = 4;
    public static final byte RT_SUBSCRIBE = 5;
    public static final byte RT_UNSUBSCRIBE = 6;
    public static final byte RT_NOTIFICATION_DATA = 7;
    public static final byte RT_NOTIFICATION_EXC = 8;
    public static final byte RT_SUBSCRIBE_EXCEPTION = 9;
    public static final byte RT_EVENT = 10; // Also used as close
    public static final byte RT_SESSION_CONFIRM = 11;

    // UpdateType
    public static final byte UT_NORMAL = (byte) 0;
    public static final byte UT_FIRST_UPDATE = (byte) 1; // Initial update sent when the subscription is created.
    public static final byte UT_IMMEDIATE_UPDATE = (byte) 2; // Update sent after the value has been modified by a set call.

    public enum ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }

    public CmwLightClient(String address) {
        controlChannel = context.createSocket(SocketType.DEALER);
        controlChannel.setIdentity(getIdentity().getBytes()); // hostname/process/id/channel
        controlChannel.setSndHWM(0);
        controlChannel.setRcvHWM(0);
        controlChannel.setLinger(0);
        this.address = address;
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
        final int connectionId = CmwLightClient.connectionId.incrementAndGet();
        final int channelId = this.channelId.incrementAndGet();
        return hostname + '/' + processId + '/' + connectionId + '/' + channelId;
    }

    public void connect() throws RdaLightException {
        controlChannel.connect(address);
        if (connectionState.getAndSet(ConnectionState.CONNECTING) != ConnectionState.DISCONNECTED) {
            return;
        }
        controlChannel.send(new byte[] { CLIENT_CONNECT }, ZMQ.SNDMORE);
        controlChannel.send("1.0.0".getBytes());
        final ZMsg conAck = ZMsg.recvMsg(controlChannel);
        if (!Arrays.equals(conAck.pollFirst().getData(), new byte[] { SERVER_CONNECT_ACK })) {
            throw new RdaLightException(conAck.toString());
        }
    }

    public Reply receiveData() throws RdaLightException {
        final ZMsg data = ZMsg.recvMsg(controlChannel);
        final ZFrame firstFrame = data.pollFirst();
        if (firstFrame != null && Arrays.equals(firstFrame.getData(), new byte[] { SERVER_HB })) {
            return null;
        }
        if (firstFrame == null || !Arrays.equals(firstFrame.getData(), new byte[] { SERVER_REP })) {
            throw new RdaLightException("Expecting only messages of type Heartbeat or Reply but got: " + data);
        }
        final ZFrame descriptorMsg = data.pollLast();
        if (descriptorMsg == null) {
            throw new RdaLightException("Message does not contain descriptor");
        }
        final byte[] descriptor = descriptorMsg.getData();
        if (descriptor[0] != MT_HEADER) {
            throw new RdaLightException("First message of SERVER_REP has to be of type MT_HEADER but is: " + descriptor[0]);
        }
        final ZFrame headerMsg = data.pollFirst();
        if (headerMsg == null) {
            throw new RdaLightException("Message does not contain header");
        }
        final byte[] header = headerMsg.getData();
        serialiser.setDataBuffer(FastByteBuffer.wrap(header));
        final FieldDescription headerMap;
        byte reqType = -1;
        long id = -1;
        String deviceName = "";
        WireDataFieldDescription options = null;
        byte updateType = -1;
        String sessionId = "";
        String propName = "";

        try {
            headerMap = serialiser.parseWireFormat().getChildren().get(0);
            for (FieldDescription field : headerMap.getChildren()) {
                if (field.getFieldName().equals(REQ_TYPE_TAG) && field.getType() == byte.class) {
                    reqType = (byte) ((WireDataFieldDescription) field).data();
                } else if (field.getFieldName().equals(ID_TAG) && field.getType() == long.class) {
                    id = (long) ((WireDataFieldDescription) field).data();
                } else if (field.getFieldName().equals(DEVICE_NAME_TAG) && field.getType() == String.class) {
                    deviceName = (String) ((WireDataFieldDescription) field).data();
                    assert deviceName.equals(""); // this field is not used
                } else if (field.getFieldName().equals(OPTIONS_TAG)) {
                    options = (WireDataFieldDescription) field;
                } else if (field.getFieldName().equals(UPDATE_TYPE_TAG) && field.getType() == byte.class) {
                    updateType = (byte) ((WireDataFieldDescription) field).data();
                } else if (field.getFieldName().equals(SESSION_ID_TAG) && field.getType() == String.class) {
                    sessionId = (String) ((WireDataFieldDescription) field).data();
                    assert sessionId.equals(""); // this field is not used
                } else if (field.getFieldName().equals(PROPERTY_NAME_TAG) && field.getType() == String.class) {
                    propName = (String) ((WireDataFieldDescription) field).data();
                    assert propName.equals(""); // this field is not used
                } else {
                    throw new RdaLightException("Unknown CMW header field: " + field.getFieldName());
                }
            }
        } catch (IllegalStateException e) {
            throw new RdaLightException("unparsable header: " + Arrays.toString(header) + "(" + new String(header) + ")", e);
        }
        switch (reqType) {
            case RT_SESSION_CONFIRM:
                LOGGER.atDebug().log("received session confirm");
                // update session state
                return null;
            case RT_EVENT:
                LOGGER.atDebug().log("received event");
                return null;
            case RT_REPLY:
                LOGGER.atDebug().log("received reply");
                if (descriptor.length != 3 || descriptor[1] != MT_BODY || descriptor[2] != MT_BODY_DATA_CONTEXT) {
                    throw new RdaLightException("Notification update does not contain the proper data");
                }
                // todo return correct type
                return createDataReply(id, updateType, data.pollFirst(), data.pollFirst());
            case RT_EXCEPTION:
                LOGGER.atDebug().log("received exc");
                if (descriptor.length != 2 || descriptor[1] != MT_BODY_EXCEPTION) {
                    throw new RdaLightException("Exception does not contain the proper data");
                }
                return createSubscriptionExceptionReply(id, data.pollFirst()); // todo return correct type
            case RT_SUBSCRIBE:
                // seems to be sent after subscription is accepted
                LOGGER.atDebug().log("received subscription reply: " + Arrays.toString(header) + "(" + new String(header) + ")");
                return null;
            case RT_SUBSCRIBE_EXCEPTION:
                LOGGER.atDebug().log("received subscription exception");
                if (descriptor.length != 2 || descriptor[1] != MT_BODY_EXCEPTION) {
                    throw new RdaLightException("Notification update does not contain the proper data");
                }
                return createSubscriptionExceptionReply(id, data.pollFirst());
            case RT_NOTIFICATION_EXC:
                LOGGER.atDebug().log("received notification exc");
                if (descriptor.length != 2 || descriptor[1] != MT_BODY_EXCEPTION) {
                    throw new RdaLightException("Notification update does not contain the proper data");
                }
                return createSubscriptionExceptionReply(id, data.pollFirst()); // todo return correct type
            case RT_NOTIFICATION_DATA:
                if (descriptor.length != 3 || descriptor[1] != MT_BODY || descriptor[2] != MT_BODY_DATA_CONTEXT) {
                    throw new RdaLightException("Notification update does not contain the proper data");
                }
                long notificationId = -1;
                if (options != null) {
                    final FieldDescription notificationIdField = options.findChildField(NOTIFICATION_ID_TAG); //long
                    notificationId = (long) ((WireDataFieldDescription) notificationIdField).data();
                }
                return createSubscriptionUpdateReply(id, updateType, notificationId, data.pollFirst(), data.pollFirst());
            case RT_CONNECT:
            case RT_SET:
            default:
                throw new RdaLightException("received unknown request type: " + reqType);
        }
    }

    private Reply createDataReply(final long id, final byte updateType, final ZFrame bodyData, final ZFrame contextData) throws RdaLightException {
        final GetReply reply = new GetReply();
        reply.id = id;
        reply.updateType = updateType;
        reply.bodyData = bodyData;

        serialiser.setDataBuffer(FastByteBuffer.wrap(contextData.getData()));
        final FieldDescription contextMap;
        try {
            contextMap = serialiser.parseWireFormat().getChildren().get(0);
        } catch (IllegalStateException e) {
            final byte[] contextBytes = serialiser.getDataBuffer().elements();
            throw new RdaLightException("unparsable header: " + Arrays.toString(contextBytes) + "(" + new String(contextBytes) + ")");
        }
        for (FieldDescription field : contextMap.getChildren()) {
            if (field.getFieldName().equals(CYCLE_NAME_TAG) && field.getType() == String.class) {
                reply.cycleName = (String) ((WireDataFieldDescription) field).data();
            } else if (field.getFieldName().equals(ACQ_STAMP_TAG) && field.getType() == long.class) {
                reply.acqStamp = (long) ((WireDataFieldDescription) field).data();
            } else if (field.getFieldName().equals(CYCLE_STAMP_TAG) && field.getType() == long.class) {
                reply.cycleStamp = (long) ((WireDataFieldDescription) field).data();
            } else if (field.getFieldName().equals(DATA_TAG)) {
                for (FieldDescription dataField : field.getChildren()) {
                    if (dataField.getFieldName().equals("acqStamp") && dataField.getType() == long.class) {
                        reply.acqStamp2 = (long) ((WireDataFieldDescription) dataField).data();
                    } else if (dataField.getFieldName().equals("cycleName") && dataField.getType() == String.class) {
                        reply.cycleName2 = (String) ((WireDataFieldDescription) dataField).data();
                    } else if (dataField.getFieldName().equals("cycleStamp") && dataField.getType() == long.class) {
                        reply.cycleStamp2 = (long) ((WireDataFieldDescription) dataField).data();
                    } else if (dataField.getFieldName().equals("type") && dataField.getType() == int.class) {
                        reply.type = (int) ((WireDataFieldDescription) dataField).data();
                    } else if (dataField.getFieldName().equals("version") && dataField.getType() == int.class) {
                        reply.version = (int) ((WireDataFieldDescription) dataField).data();
                    } else {
                        throw new UnsupportedOperationException("Unknown data field: " + field.getFieldName());
                    }
                }
            } else {
                throw new UnsupportedOperationException("Unknown field: " + field.getFieldName());
            }
        }
        assert reply.acqStamp == reply.acqStamp2;
        assert reply.cycleName.equals(reply.cycleName2);
        assert reply.cycleStamp == reply.cycleStamp2;
        return reply;
    }

    private Reply createSubscriptionExceptionReply(final long id, final ZFrame pollFirst) throws RdaLightException {
        final SubscriptionExceptionReply reply = new SubscriptionExceptionReply();
        serialiser.setDataBuffer(FastByteBuffer.wrap(pollFirst.getData()));
        final FieldDescription exceptionFields = serialiser.parseWireFormat().getChildren().get(0);
        for (FieldDescription field : exceptionFields.getChildren()) {
            if (field.getFieldName().equals("ContextAcqStamp") && field.getType() == long.class) {
                reply.contextAcqStamp = (long) ((WireDataFieldDescription) field).data();
            } else if (field.getFieldName().equals("ContextCycleStamp") && field.getType() == long.class) {
                reply.contextCycleStamp = (long) ((WireDataFieldDescription) field).data();
            } else if (field.getFieldName().equals("Message") && field.getType() == String.class) {
                reply.message = (String) ((WireDataFieldDescription) field).data();
            } else if (field.getFieldName().equals("Type") && field.getType() == byte.class) {
                reply.type = (byte) ((WireDataFieldDescription) field).data();
            } else {
                throw new RdaLightException("Unsupported field in exception body: " + field.getFieldName());
            }
        }
        return reply;
    }

    private Reply createSubscriptionUpdateReply(final long id, final byte updateType, final long notificationId, final ZFrame bodyData, final ZFrame contextData) throws RdaLightException {
        final SubscriptionUpdate reply = new SubscriptionUpdate();
        reply.id = id;
        reply.updateType = updateType;
        reply.notificationId = notificationId;
        reply.bodyData = bodyData;

        serialiser.setDataBuffer(FastByteBuffer.wrap(contextData.getData()));
        final FieldDescription contextMap;
        try {
            contextMap = serialiser.parseWireFormat().getChildren().get(0);
        } catch (IllegalStateException e) {
            final byte[] contextBytes = serialiser.getDataBuffer().elements();
            throw new RdaLightException("unparsable header: " + Arrays.toString(contextBytes) + "(" + new String(contextBytes) + ")");
        }
        for (FieldDescription field : contextMap.getChildren()) {
            if (field.getFieldName().equals(CYCLE_NAME_TAG) && field.getType() == String.class) {
                reply.cycleName = (String) ((WireDataFieldDescription) field).data();
            } else if (field.getFieldName().equals(ACQ_STAMP_TAG) && field.getType() == long.class) {
                reply.acqStamp = (long) ((WireDataFieldDescription) field).data();
            } else if (field.getFieldName().equals(CYCLE_STAMP_TAG) && field.getType() == long.class) {
                reply.cycleStamp = (long) ((WireDataFieldDescription) field).data();
            } else if (field.getFieldName().equals(DATA_TAG)) {
                for (FieldDescription dataField : field.getChildren()) {
                    if (dataField.getFieldName().equals("acqStamp") && dataField.getType() == long.class) {
                        reply.acqStamp2 = (long) ((WireDataFieldDescription) dataField).data();
                    } else if (dataField.getFieldName().equals("cycleName") && dataField.getType() == String.class) {
                        reply.cycleName2 = (String) ((WireDataFieldDescription) dataField).data();
                    } else if (dataField.getFieldName().equals("cycleStamp") && dataField.getType() == long.class) {
                        reply.cycleStamp2 = (long) ((WireDataFieldDescription) dataField).data();
                    } else if (dataField.getFieldName().equals("type") && dataField.getType() == int.class) {
                        reply.type = (int) ((WireDataFieldDescription) dataField).data();
                    } else if (dataField.getFieldName().equals("version") && dataField.getType() == int.class) {
                        reply.version = (int) ((WireDataFieldDescription) dataField).data();
                    } else {
                        throw new UnsupportedOperationException("Unknown data field: " + field.getFieldName());
                    }
                }
            } else {
                throw new UnsupportedOperationException("Unknown field: " + field.getFieldName());
            }
        }
        assert reply.acqStamp == reply.acqStamp2;
        assert reply.cycleName.equals(reply.cycleName2);
        assert reply.cycleStamp == reply.cycleStamp2;
        return reply;
    }

    public void sendHeartBeat() {
        controlChannel.send(new byte[] { CLIENT_HB });
    }

    public void subscribe(final String device, final String property, final String selector) throws RdaLightException {
        subscribe(device, property, selector, null);
    }

    public void subscribe(final String device, final String property, final String selector, final Map<String, Object> filters) throws RdaLightException {
        controlChannel.send(new byte[] { CLIENT_REQ }, ZMQ.SNDMORE);
        final CmwLightSerialiser serialiser = new CmwLightSerialiser(new FastByteBuffer(1024));
        serialiser.putHeaderInfo();
        serialiser.put(REQ_TYPE_TAG, RT_SUBSCRIBE);
        serialiser.put(ID_TAG, 1L); // todo: id
        serialiser.put(DEVICE_NAME_TAG, device);
        serialiser.put(PROPERTY_NAME_TAG, property);
        serialiser.put(UPDATE_TYPE_TAG, UT_NORMAL);
        serialiser.put(SESSION_ID_TAG, "asdf"); // todo session id
        // StartMarker marks start of Data Object
        serialiser.putStartMarker(new WireDataFieldDescription(serialiser, serialiser.getParent(), -1,
                OPTIONS_TAG, DataType.START_MARKER, -1, -1, -1));
        serialiser.putStartMarker(new WireDataFieldDescription(serialiser, serialiser.getParent(), -1,
                "e", DataType.START_MARKER, -1, -1, -1));
        serialiser.getBuffer().flip();
        controlChannel.send(serialiser.getBuffer().elements(), 0, serialiser.getBuffer().limit(), ZMQ.SNDMORE);
        serialiser.getBuffer().reset();
        serialiser.putHeaderInfo();
        serialiser.put(SELECTOR_TAG, selector);
        if (filters != null && !filters.isEmpty()) {
            final WireDataFieldDescription filterFieldMarker = new WireDataFieldDescription(serialiser, serialiser.getParent(), -1,
                    FILTERS_TAG, DataType.START_MARKER, -1, -1, -1);
            serialiser.putStartMarker(filterFieldMarker);
            for (final Map.Entry<String, Object> entry : filters.entrySet()) {
                if (entry.getValue() instanceof String) {
                    serialiser.put(entry.getKey(), (String) entry.getValue());
                } else if (entry.getValue() instanceof Integer) {
                    serialiser.put(entry.getKey(), (Integer) entry.getValue());
                } else if (entry.getValue() instanceof Long) {
                    serialiser.put(entry.getKey(), (Long) entry.getValue());
                } else if (entry.getValue() instanceof Boolean) {
                    serialiser.put(entry.getKey(), (Boolean) entry.getValue());
                } else {
                    throw new RdaLightException("unsupported filter type: " + entry.getValue().getClass().getCanonicalName());
                }
            }
            serialiser.putEndMarker(filterFieldMarker);
        }
        // x: data
        serialiser.getBuffer().flip();
        controlChannel.send(serialiser.getBuffer().elements(), 0, serialiser.getBuffer().limit(), ZMQ.SNDMORE);
        controlChannel.send(new byte[] { MT_HEADER, MT_BODY_REQUEST_CONTEXT });
    }

    public void unsubscribe(final String device, final String property, final String selector) {
        controlChannel.send(new byte[] { CLIENT_REQ }, ZMQ.SNDMORE);
        final CmwLightSerialiser serialiser = new CmwLightSerialiser(new FastByteBuffer(1024));
        serialiser.putHeaderInfo();
        serialiser.put(REQ_TYPE_TAG, RT_UNSUBSCRIBE);
        serialiser.put(ID_TAG, 1L); // todo: id
        serialiser.put(DEVICE_NAME_TAG, device);
        serialiser.put(PROPERTY_NAME_TAG, property);
        serialiser.put(UPDATE_TYPE_TAG, UT_NORMAL);
        serialiser.put(SESSION_ID_TAG, "asdf"); // todo session id
        // StartMarker marks start of Data Object
        serialiser.putStartMarker(new WireDataFieldDescription(serialiser, serialiser.getParent(), -1,
                OPTIONS_TAG, DataType.START_MARKER, -1, -1, -1));
        serialiser.putStartMarker(new WireDataFieldDescription(serialiser, serialiser.getParent(), -1,
                "e", DataType.START_MARKER, -1, -1, -1));
        serialiser.getBuffer().flip();
        controlChannel.send(serialiser.getBuffer().elements(), 0, serialiser.getBuffer().limit(), ZMQ.SNDMORE);
        serialiser.getBuffer().reset();
        serialiser.putHeaderInfo();
        serialiser.put(SELECTOR_TAG, selector); // 8: Context c : filters, x: data
        serialiser.getBuffer().flip();
        controlChannel.send(serialiser.getBuffer().elements(), 0, serialiser.getBuffer().limit(), ZMQ.SNDMORE);
        controlChannel.send(new byte[] { MT_HEADER, MT_BODY_REQUEST_CONTEXT });
    }

    public void get(final String devName, final String prop, final String selector) {
        controlChannel.send(new byte[] { CLIENT_REQ }, ZMQ.SNDMORE);
        final CmwLightSerialiser serialiser = new CmwLightSerialiser(new FastByteBuffer(1024));
        serialiser.putHeaderInfo();
        serialiser.put(REQ_TYPE_TAG, RT_GET); // GET
        serialiser.put(ID_TAG, 1L);
        serialiser.put(DEVICE_NAME_TAG, devName);
        serialiser.put(PROPERTY_NAME_TAG, prop);
        serialiser.put(UPDATE_TYPE_TAG, UT_NORMAL);
        serialiser.put(SESSION_ID_TAG, "asdf");
        // StartMarker marks start of Data Object
        serialiser.putStartMarker(new WireDataFieldDescription(serialiser, serialiser.getParent(), -1,
                OPTIONS_TAG, DataType.START_MARKER, -1, -1, -1));
        serialiser.putStartMarker(new WireDataFieldDescription(serialiser, serialiser.getParent(), -1,
                "e", DataType.START_MARKER, -1, -1, -1));
        serialiser.getBuffer().flip();
        controlChannel.send(serialiser.getBuffer().elements(), 0, serialiser.getBuffer().limit(), ZMQ.SNDMORE);
        serialiser.getBuffer().reset();
        serialiser.putHeaderInfo();
        serialiser.put("8", selector); // 8: Context c : filters, x: data
        serialiser.getBuffer().flip();
        controlChannel.send(serialiser.getBuffer().elements(), 0, serialiser.getBuffer().limit(), ZMQ.SNDMORE);
        controlChannel.send(new byte[] { MT_HEADER, MT_BODY_REQUEST_CONTEXT });
    }

    public abstract static class Reply {
    }

    public static class SubscriptionExceptionReply extends Reply {
        public long contextAcqStamp;
        public long contextCycleStamp;
        public String selector;
        public String message;
        public byte type;
    }

    public static class HeaderReply extends Reply {
    }

    public static class GetExceptionReply extends Reply {
    }

    public static class GetReply extends Reply {
        public String cycleName;
        public String cycleName2;
        public long cycleStamp;
        public long cycleStamp2;
        public long acqStamp;
        public long acqStamp2;
        public int type;
        public int version;
        public long id;
        public byte updateType;
        public ZFrame bodyData;
    }

    public static class SubscriptionUpdate extends Reply {
        public String cycleName;
        public String cycleName2;
        public long cycleStamp;
        public long cycleStamp2;
        public long acqStamp;
        public long acqStamp2;
        public int type;
        public int version;
        public long id;
        public byte updateType;
        public long notificationId;
        public ZFrame bodyData;
    }

    public static class RdaLightException extends Exception {
        public RdaLightException(final String msg) {
            super(msg);
        }

        public RdaLightException(final String msg, final Throwable e) {
            super(msg, e);
        }
    }
}
