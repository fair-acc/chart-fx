package de.gsi.microservice.concepts.cmwlight;

import static java.util.Objects.requireNonNull;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.zeromq.*;

import de.gsi.dataset.utils.AssertUtils;
import de.gsi.serializer.*;
import de.gsi.serializer.spi.*;

/**
 * A lightweight implementation of the CMW RDA client protocol part.
 * Serializes CmwLightMessage to ZeroMQ messages and vice versa.
 */
@SuppressWarnings("PMD.UnusedLocalVariable") // Unused variables are taken from the protocol and should be available for reference
public class CmwLightProtocol {
    private static final int MAX_MSG_SIZE = 512;
    private static final IoBuffer outBuffer = new FastByteBuffer(MAX_MSG_SIZE);
    private static final CmwLightSerialiser serialiser = new CmwLightSerialiser(outBuffer);
    private static final IoClassSerialiser classSerialiser = new IoClassSerialiser(new FastByteBuffer(0));
    private static final String VERSION = "1.0.0"; // Protocol version used if msg.version is null or empty

    /**
     * The message specified by the byte contained in the first frame of a message defines what type of message is present
     */
    public enum MessageType {
        SERVER_CONNECT_ACK(0x01),
        SERVER_REP(0x02),
        SERVER_HB(0x03),
        CLIENT_CONNECT(0x20),
        CLIENT_REQ(0x21),
        CLIENT_HB(0x22);

        private final byte value;

        MessageType(int value) {
            this.value = (byte) value;
        }

        public byte value() {
            return value;
        }

        public static MessageType of(int value) {
            if (value < 0x4) {
                return values()[value - 1];
            } else {
                return values()[value - 0x20 + CLIENT_CONNECT.ordinal()];
            }
        }
    }

    /**
     * Frame Types in the descriptor (Last frame of a message containing the type of each sub message)
     */
    public enum FrameType {
        HEADER(0),
        BODY(1),
        BODY_DATA_CONTEXT(2),
        BODY_REQUEST_CONTEXT(3),
        BODY_EXCEPTION(4);

        private final byte value;

        FrameType(int value) {
            this.value = (byte) value;
        }

        public byte value() {
            return value;
        }
    }

    /**
     * Field names for the Request Header
     */
    public enum FieldName {
        EVENT_TYPE_TAG("eventType"),
        MESSAGE_TAG("message"),
        ID_TAG("0"),
        DEVICE_NAME_TAG("1"),
        REQ_TYPE_TAG("2"),
        OPTIONS_TAG("3"),
        CYCLE_NAME_TAG("4"),
        ACQ_STAMP_TAG("5"),
        CYCLE_STAMP_TAG("6"),
        UPDATE_TYPE_TAG("7"),
        SELECTOR_TAG("8"),
        CLIENT_INFO_TAG("9"),
        NOTIFICATION_ID_TAG("a"),
        SOURCE_ID_TAG("b"),
        FILTERS_TAG("c"),
        DATA_TAG("x"),
        SESSION_ID_TAG("d"),
        SESSION_BODY_TAG("e"),
        PROPERTY_NAME_TAG("f");

        private final String name;

        FieldName(String name) {
            this.name = name;
        }

        public String value() {
            return name;
        }
    }

    /**
     * request type used in request header REQ_TYPE_TAG
     */
    public enum RequestType {
        GET(0),
        SET(1),
        CONNECT(2),
        REPLY(3),
        EXCEPTION(4),
        SUBSCRIBE(5),
        UNSUBSCRIBE(6),
        NOTIFICATION_DATA(7),
        NOTIFICATION_EXC(8),
        SUBSCRIBE_EXCEPTION(9),
        EVENT(10),
        SESSION_CONFIRM(11);

        private final byte value;

        RequestType(int value) {
            this.value = (byte) value;
        }

        public static RequestType of(int value) {
            return values()[value];
        }

        public byte value() {
            return value;
        }
    }

    /**
     * UpdateType
     */
    public enum UpdateType {
        NORMAL(0),
        FIRST_UPDATE(1), // Initial update sent when the subscription is created.
        IMMEDIATE_UPDATE(2); // Update sent after the value has been modified by a set call.

        private final byte value;

        UpdateType(int value) {
            this.value = (byte) value;
        }

        public static UpdateType of(int value) {
            return values()[value];
        }

        public byte value() {
            return value;
        }
    }

    public static CmwLightMessage recvMsg(final ZMQ.Socket socket, int tout) throws RdaLightException {
        return parseMsg(ZMsg.recvMsg(socket, tout));
    }

    public static CmwLightMessage parseMsg(final ZMsg data) throws RdaLightException { // NOPMD - NPath complexity acceptable (complex protocol)
        AssertUtils.notNull("data", data);
        final ZFrame firstFrame = requireNonNull(data.pollFirst());
        if (firstFrame != null && Arrays.equals(firstFrame.getData(), new byte[] { MessageType.SERVER_CONNECT_ACK.value() })) {
            final CmwLightMessage reply = new CmwLightMessage(MessageType.SERVER_CONNECT_ACK);
            final ZFrame versionData = requireNonNull(data.pollFirst());
            AssertUtils.notNull("version data in connection acknowledgement frame", versionData);
            reply.version = versionData.getString(Charset.defaultCharset());
            return reply;
        }
        if (firstFrame != null && Arrays.equals(firstFrame.getData(), new byte[] { MessageType.CLIENT_CONNECT.value() })) {
            final CmwLightMessage reply = new CmwLightMessage(MessageType.CLIENT_CONNECT);
            final ZFrame versionData = requireNonNull(data.pollFirst());
            AssertUtils.notNull("version data in connection acknowledgement frame", versionData);
            reply.version = versionData.getString(Charset.defaultCharset());
            return reply;
        }
        if (firstFrame != null && Arrays.equals(firstFrame.getData(), new byte[] { MessageType.SERVER_HB.value() })) {
            return CmwLightMessage.SERVER_HB;
        }
        if (firstFrame != null && Arrays.equals(firstFrame.getData(), new byte[] { MessageType.CLIENT_HB.value() })) {
            return CmwLightMessage.CLIENT_HB;
        }
        byte[] descriptor = checkDescriptor(data.pollLast(), firstFrame);
        final ZFrame headerMsg = data.poll();
        AssertUtils.notNull("message header", headerMsg);
        CmwLightMessage reply = getReplyFromHeader(firstFrame, headerMsg);
        switch (reply.requestType) {
        case REPLY:
            assertDescriptor(descriptor, FrameType.HEADER, FrameType.BODY, FrameType.BODY_DATA_CONTEXT);
            reply.bodyData = requireNonNull(data.pollFirst());
            reply.dataContext = parseContextData(requireNonNull(data.pollFirst()));
            return reply;
        case NOTIFICATION_DATA: // notification update
            assertDescriptor(descriptor, FrameType.HEADER, FrameType.BODY, FrameType.BODY_DATA_CONTEXT);
            reply.notificationId = (long) reply.options.get(FieldName.NOTIFICATION_ID_TAG.value());
            reply.bodyData = requireNonNull(data.pollFirst());
            reply.dataContext = parseContextData(requireNonNull(data.pollFirst()));
            return reply;
        case EXCEPTION: // exception on get/set request
        case NOTIFICATION_EXC: // exception on notification, e.g null pointer in server notify code
        case SUBSCRIBE_EXCEPTION: // exception on subscribe e.g. nonexistent property, wrong filters
            assertDescriptor(descriptor, FrameType.HEADER, FrameType.BODY_EXCEPTION);
            reply.exceptionMessage = parseExceptionMessage(requireNonNull(data.pollFirst()));
            return reply;
        case GET:
            assertDescriptor(descriptor, FrameType.HEADER, FrameType.BODY_REQUEST_CONTEXT);
            reply.requestContext = parseRequestContext(requireNonNull(data.pollFirst()));
            return reply;
        case SUBSCRIBE: // descriptor: [0] options: SOURCE_ID_TAG // seems to be sent after subscription is accepted
            if (reply.messageType == MessageType.SERVER_REP) {
                assertDescriptor(descriptor, FrameType.HEADER);
                reply.sourceId = (long) reply.options.get(FieldName.SOURCE_ID_TAG.value());
            } else {
                assertDescriptor(descriptor, FrameType.HEADER, FrameType.BODY_REQUEST_CONTEXT);
                reply.requestContext = parseRequestContext(requireNonNull(data.pollFirst()));
            }
            return reply;
        case SESSION_CONFIRM: // descriptor: [0] options: SESSION_BODY_TAG
            assertDescriptor(descriptor, FrameType.HEADER);
            reply.sessionBody = (Map<String, Object>) reply.options.get(FieldName.SESSION_BODY_TAG.value());
            return reply;
        case EVENT:
        case UNSUBSCRIBE:
        case CONNECT:
            assertDescriptor(descriptor, FrameType.HEADER);
            return reply;
        case SET:
            assertDescriptor(descriptor, FrameType.HEADER, FrameType.BODY, FrameType.BODY_REQUEST_CONTEXT);
            reply.bodyData = requireNonNull(data.pollFirst());
            reply.requestContext = parseRequestContext(requireNonNull(data.pollFirst()));
            return reply;
        default:
            throw new RdaLightException("received unknown or non-client request type: " + reply.requestType);
        }
    }

    public static void sendMsg(final ZMQ.Socket socket, final CmwLightMessage msg) throws RdaLightException {
        serialiseMsg(msg).send(socket);
    }

    public static ZMsg serialiseMsg(final CmwLightMessage msg) throws RdaLightException {
        final ZMsg result = new ZMsg();
        switch (msg.messageType) {
        case SERVER_CONNECT_ACK:
        case CLIENT_CONNECT:
            result.add(new ZFrame(new byte[] { msg.messageType.value() }));
            result.add(new ZFrame(msg.version == null || msg.version.isEmpty() ? VERSION : msg.version));
            return result;
        case CLIENT_HB:
        case SERVER_HB:
            result.add(new ZFrame(new byte[] { msg.messageType.value() }));
            return result;
        case SERVER_REP:
        case CLIENT_REQ:
            result.add(new byte[] { msg.messageType.value() });
            result.add(serialiseHeader(msg));
            switch (msg.requestType) {
            case CONNECT:
            case EVENT:
            case SESSION_CONFIRM:
            case UNSUBSCRIBE:
                addDescriptor(result, FrameType.HEADER);
                break;
            case GET:
            case SUBSCRIBE:
                AssertUtils.notNull("requestContext", msg.requestContext);
                result.add(serialiseRequestContext(msg.requestContext));
                addDescriptor(result, FrameType.HEADER, FrameType.BODY_REQUEST_CONTEXT);
                break;
            case SET:
                AssertUtils.notNull("bodyData", msg.bodyData);
                AssertUtils.notNull("requestContext", msg.requestContext);
                result.add(msg.bodyData);
                result.add(serialiseRequestContext(msg.requestContext));
                addDescriptor(result, FrameType.HEADER, FrameType.BODY, FrameType.BODY_REQUEST_CONTEXT);
                break;
            case REPLY:
            case NOTIFICATION_DATA:
                AssertUtils.notNull("bodyData", msg.bodyData);
                result.add(msg.bodyData);
                result.add(serialiseDataContext(msg.dataContext));
                addDescriptor(result, FrameType.HEADER, FrameType.BODY, FrameType.BODY_DATA_CONTEXT);
                break;
            case NOTIFICATION_EXC:
            case EXCEPTION:
            case SUBSCRIBE_EXCEPTION:
                AssertUtils.notNull("exceptionMessage", msg.exceptionMessage);
                result.add(serialiseExceptionMessage(msg.exceptionMessage));
                addDescriptor(result, FrameType.HEADER, FrameType.BODY_EXCEPTION);
                break;
            default:
            }
            return result;
        default:
        }

        throw new RdaLightException("Invalid cmwMessage: " + msg);
    }

    private static ZFrame serialiseExceptionMessage(final CmwLightMessage.ExceptionMessage exceptionMessage) {
        outBuffer.reset();
        serialiser.setBuffer(outBuffer);
        serialiser.putHeaderInfo();
        serialiser.put("ContextAcqStamp", exceptionMessage.contextAcqStamp);
        serialiser.put("ContextCycleStamp", exceptionMessage.contextCycleStamp);
        serialiser.put("Message", exceptionMessage.message);
        serialiser.put("Type", exceptionMessage.type);
        outBuffer.flip();
        return new ZFrame(Arrays.copyOfRange(outBuffer.elements(), 0, outBuffer.limit()));
    }

    private static void addDescriptor(final ZMsg result, final FrameType... frametypes) {
        byte[] descriptor = new byte[frametypes.length];
        for (int i = 0; i < descriptor.length; i++) {
            descriptor[i] = frametypes[i].value();
        }
        result.add(new ZFrame(descriptor));
    }

    private static ZFrame serialiseHeader(final CmwLightMessage msg) throws RdaLightException {
        outBuffer.reset();
        serialiser.setBuffer(outBuffer);
        serialiser.putHeaderInfo();
        serialiser.put(FieldName.REQ_TYPE_TAG.value(), msg.requestType.value());
        serialiser.put(FieldName.ID_TAG.value(), msg.id);
        serialiser.put(FieldName.DEVICE_NAME_TAG.value(), msg.deviceName);
        serialiser.put(FieldName.PROPERTY_NAME_TAG.value(), msg.propertyName);
        if (msg.updateType != null)
            serialiser.put(FieldName.UPDATE_TYPE_TAG.value(), msg.updateType.value());
        serialiser.put(FieldName.SESSION_ID_TAG.value(), msg.sessionId);
        // StartMarker marks start of Data Object
        putMap(serialiser, FieldName.OPTIONS_TAG.value(), msg.options);
        outBuffer.flip();
        return new ZFrame(Arrays.copyOfRange(outBuffer.elements(), 0, outBuffer.limit()));
    }

    private static ZFrame serialiseRequestContext(final CmwLightMessage.RequestContext requestContext) throws RdaLightException {
        outBuffer.reset();
        serialiser.putHeaderInfo();
        serialiser.put(FieldName.SELECTOR_TAG.value(), requestContext.selector);
        putMap(serialiser, FieldName.FILTERS_TAG.value(), requestContext.filters);
        putMap(serialiser, FieldName.DATA_TAG.value(), requestContext.data);
        outBuffer.flip();
        return new ZFrame(Arrays.copyOfRange(outBuffer.elements(), 0, outBuffer.limit()));
    }

    private static ZFrame serialiseDataContext(final CmwLightMessage.DataContext dataContext) throws RdaLightException {
        outBuffer.reset();
        serialiser.putHeaderInfo();
        serialiser.put(FieldName.CYCLE_NAME_TAG.value(), dataContext.cycleName);
        serialiser.put(FieldName.CYCLE_STAMP_TAG.value(), dataContext.cycleStamp);
        serialiser.put(FieldName.ACQ_STAMP_TAG.value(), dataContext.acqStamp);
        putMap(serialiser, FieldName.DATA_TAG.value(), dataContext.data);
        outBuffer.flip();
        return new ZFrame(Arrays.copyOfRange(outBuffer.elements(), 0, outBuffer.limit()));
    }

    private static void putMap(final CmwLightSerialiser serialiser, final String fieldName, final Map<String, Object> map) throws RdaLightException {
        if (map != null) {
            final WireDataFieldDescription dataFieldMarker = new WireDataFieldDescription(serialiser, serialiser.getParent(), -1,
                    fieldName, DataType.START_MARKER, -1, -1, -1);
            serialiser.putStartMarker(dataFieldMarker);
            for (final Map.Entry<String, Object> entry : map.entrySet()) {
                if (entry.getValue() instanceof String) {
                    serialiser.put(entry.getKey(), (String) entry.getValue());
                } else if (entry.getValue() instanceof Integer) {
                    serialiser.put(entry.getKey(), (Integer) entry.getValue());
                } else if (entry.getValue() instanceof Long) {
                    serialiser.put(entry.getKey(), (Long) entry.getValue());
                } else if (entry.getValue() instanceof Boolean) {
                    serialiser.put(entry.getKey(), (Boolean) entry.getValue());
                } else if (entry.getValue() instanceof Map) {
                    final Map<String, Object> subMap = (Map<String, Object>) entry.getValue();
                    putMap(serialiser, entry.getKey(), subMap);
                } else {
                    throw new RdaLightException("unsupported map entry type: " + entry.getValue().getClass().getCanonicalName());
                }
            }
            serialiser.putEndMarker(dataFieldMarker);
        }
    }

    private static CmwLightMessage getReplyFromHeader(final ZFrame firstFrame, final ZFrame header) throws RdaLightException {
        CmwLightMessage reply = new CmwLightMessage(MessageType.of(firstFrame.getData()[0]));
        classSerialiser.setDataBuffer(FastByteBuffer.wrap(header.getData()));
        final FieldDescription headerMap;
        try {
            headerMap = classSerialiser.parseWireFormat().getChildren().get(0);
            for (FieldDescription field : headerMap.getChildren()) {
                if (field.getFieldName().equals(FieldName.REQ_TYPE_TAG.value()) && field.getType() == byte.class) {
                    reply.requestType = RequestType.of((byte) (((WireDataFieldDescription) field).data()));
                } else if (field.getFieldName().equals(FieldName.ID_TAG.value()) && field.getType() == long.class) {
                    reply.id = (long) ((WireDataFieldDescription) field).data();
                } else if (field.getFieldName().equals(FieldName.DEVICE_NAME_TAG.value()) && field.getType() == String.class) {
                    reply.deviceName = (String) ((WireDataFieldDescription) field).data();
                } else if (field.getFieldName().equals(FieldName.OPTIONS_TAG.value())) {
                    reply.options = readMap(field);
                } else if (field.getFieldName().equals(FieldName.UPDATE_TYPE_TAG.value()) && field.getType() == byte.class) {
                    reply.updateType = UpdateType.of((byte) ((WireDataFieldDescription) field).data());
                } else if (field.getFieldName().equals(FieldName.SESSION_ID_TAG.value()) && field.getType() == String.class) {
                    reply.sessionId = (String) ((WireDataFieldDescription) field).data();
                } else if (field.getFieldName().equals(FieldName.PROPERTY_NAME_TAG.value()) && field.getType() == String.class) {
                    reply.propertyName = (String) ((WireDataFieldDescription) field).data();
                } else {
                    throw new RdaLightException("Unknown CMW header field: " + field.getFieldName());
                }
            }
        } catch (IllegalStateException e) {
            throw new RdaLightException("unparsable header: " + Arrays.toString(header.getData()) + "(" + header.toString() + ")", e);
        }
        if (reply.requestType == null) {
            throw new RdaLightException("Header does not contain request type field");
        }
        return reply;
    }

    private static Map<String, Object> readMap(final FieldDescription field) {
        Map<String, Object> result = null;
        for (FieldDescription dataField : field.getChildren()) {
            if (result == null) {
                result = new HashMap<>();
            }
            //if ( 'condition' ) {
            // find out how to see if the field is itself a map
            // result.put(dataField.getFieldName(), readMap(dataField))
            // } else {
            result.put(dataField.getFieldName(), ((WireDataFieldDescription) dataField).data());
            //}
        }
        return result;
    }

    private static CmwLightMessage.ExceptionMessage parseExceptionMessage(final ZFrame exceptionBody) throws RdaLightException {
        if (exceptionBody == null) {
            throw new RdaLightException("malformed subscription exception");
        }
        final CmwLightMessage.ExceptionMessage exceptionMessage = new CmwLightMessage.ExceptionMessage();
        classSerialiser.setDataBuffer(FastByteBuffer.wrap(exceptionBody.getData()));
        final FieldDescription exceptionFields = classSerialiser.parseWireFormat().getChildren().get(0);
        for (FieldDescription field : exceptionFields.getChildren()) {
            if (field.getFieldName().equals("ContextAcqStamp") && field.getType() == long.class) {
                exceptionMessage.contextAcqStamp = (long) ((WireDataFieldDescription) field).data();
            } else if (field.getFieldName().equals("ContextCycleStamp") && field.getType() == long.class) {
                exceptionMessage.contextCycleStamp = (long) ((WireDataFieldDescription) field).data();
            } else if (field.getFieldName().equals("Message") && field.getType() == String.class) {
                exceptionMessage.message = (String) ((WireDataFieldDescription) field).data();
            } else if (field.getFieldName().equals("Type") && field.getType() == byte.class) {
                exceptionMessage.type = (byte) ((WireDataFieldDescription) field).data();
            } else {
                throw new RdaLightException("Unsupported field in exception body: " + field.getFieldName());
            }
        }
        return exceptionMessage;
    }

    private static CmwLightMessage.RequestContext parseRequestContext(final ZFrame contextData) throws RdaLightException {
        AssertUtils.notNull("contextData", contextData);
        CmwLightMessage.RequestContext requestContext = new CmwLightMessage.RequestContext();
        classSerialiser.setDataBuffer(FastByteBuffer.wrap(contextData.getData()));
        final FieldDescription contextMap;
        try {
            contextMap = classSerialiser.parseWireFormat().getChildren().get(0);
            for (FieldDescription field : contextMap.getChildren()) {
                if (field.getFieldName().equals(FieldName.SELECTOR_TAG.value()) && field.getType() == String.class) {
                    requestContext.selector = (String) ((WireDataFieldDescription) field).data();
                } else if (field.getFieldName().equals(FieldName.FILTERS_TAG.value())) {
                    for (FieldDescription dataField : field.getChildren()) {
                        if (requestContext.filters == null) {
                            requestContext.filters = new HashMap<>();
                        }
                        requestContext.filters.put(dataField.getFieldName(), ((WireDataFieldDescription) dataField).data());
                    }
                } else if (field.getFieldName().equals(FieldName.DATA_TAG.value())) {
                    for (FieldDescription dataField : field.getChildren()) {
                        if (requestContext.data == null) {
                            requestContext.data = new HashMap<>();
                        }
                        requestContext.data.put(dataField.getFieldName(), ((WireDataFieldDescription) dataField).data());
                    }
                } else {
                    throw new UnsupportedOperationException("Unknown field: " + field.getFieldName());
                }
            }
        } catch (IllegalStateException e) {
            throw new RdaLightException("unparsable context data: " + Arrays.toString(contextData.getData()) + "(" + new String(contextData.getData()) + ")");
        }
        return requestContext;
    }

    private static CmwLightMessage.DataContext parseContextData(final ZFrame contextData) throws RdaLightException {
        AssertUtils.notNull("contextData", contextData);
        CmwLightMessage.DataContext dataContext = new CmwLightMessage.DataContext();
        classSerialiser.setDataBuffer(FastByteBuffer.wrap(contextData.getData()));
        final FieldDescription contextMap;
        try {
            contextMap = classSerialiser.parseWireFormat().getChildren().get(0);
            for (FieldDescription field : contextMap.getChildren()) {
                if (field.getFieldName().equals(FieldName.CYCLE_NAME_TAG.value()) && field.getType() == String.class) {
                    dataContext.cycleName = (String) ((WireDataFieldDescription) field).data();
                } else if (field.getFieldName().equals(FieldName.ACQ_STAMP_TAG.value()) && field.getType() == long.class) {
                    dataContext.acqStamp = (long) ((WireDataFieldDescription) field).data();
                } else if (field.getFieldName().equals(FieldName.CYCLE_STAMP_TAG.value()) && field.getType() == long.class) {
                    dataContext.cycleStamp = (long) ((WireDataFieldDescription) field).data();
                } else if (field.getFieldName().equals(FieldName.DATA_TAG.value())) {
                    for (FieldDescription dataField : field.getChildren()) {
                        if (dataContext.data == null) {
                            dataContext.data = new HashMap<>();
                        }
                        dataContext.data.put(dataField.getFieldName(), ((WireDataFieldDescription) dataField).data());
                    }
                } else {
                    throw new UnsupportedOperationException("Unknown field: " + field.getFieldName());
                }
            }
        } catch (IllegalStateException e) {
            throw new RdaLightException("unparsable context data: " + Arrays.toString(contextData.getData()) + "(" + new String(contextData.getData()) + ")");
        }
        return dataContext;
    }

    private static void assertDescriptor(final byte[] descriptor, final FrameType... frameTypes) throws RdaLightException {
        if (descriptor.length != frameTypes.length) {
            throw new RdaLightException("descriptor does not match message type: \n  " + Arrays.toString(descriptor) + "\n  " + Arrays.toString(frameTypes));
        }
        for (int i = 1; i < descriptor.length; i++) {
            if (descriptor[i] != frameTypes[i].value()) {
                throw new RdaLightException("descriptor does not match message type: \n  " + Arrays.toString(descriptor) + "\n  " + Arrays.toString(frameTypes));
            }
        }
    }

    private static byte[] checkDescriptor(final ZFrame descriptorMsg, final ZFrame firstFrame) throws RdaLightException {
        if (firstFrame == null || !(Arrays.equals(firstFrame.getData(), new byte[] { MessageType.SERVER_REP.value() }) || Arrays.equals(firstFrame.getData(), new byte[] { MessageType.CLIENT_REQ.value() }))) {
            throw new RdaLightException("Expecting only messages of type Heartbeat or Reply but got: " + firstFrame);
        }
        if (descriptorMsg == null) {
            throw new RdaLightException("Message does not contain descriptor");
        }
        final byte[] descriptor = descriptorMsg.getData();
        if (descriptor[0] != FrameType.HEADER.value()) {
            throw new RdaLightException("First message of SERVER_REP has to be of type MT_HEADER but is: " + descriptor[0]);
        }
        return descriptor;
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
