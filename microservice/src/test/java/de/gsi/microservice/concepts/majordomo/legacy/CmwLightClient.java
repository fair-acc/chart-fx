package de.gsi.microservice.concepts.majordomo.legacy;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.zeromq.*;

import de.gsi.serializer.DataType;
import de.gsi.serializer.IoClassSerialiser;
import de.gsi.serializer.spi.CmwLightSerialiser;
import de.gsi.serializer.spi.FastByteBuffer;
import de.gsi.serializer.spi.WireDataFieldDescription;

/**
 * CmwLight Prototype Client.
 *
 * This is a slimmed-down implementation aimed at being backward-compatible with the proprietary, closed-source,
 * middle-ware protocol that is internally used at GSI. Please use the OpenCmwClient for new projects.
 *
 * @author Alexander Krimm
 * @author rstein
 *
 * @deprecated
 * N.B. This is intended as a fall-back implementation for existing users only. New users are strongly encouraged to
 * use one of the new faster OpenCmwClient (Majordomo/YaS) resp. REST/JSON-based transport and serialisation protocols.
 */
@Deprecated(since = "2020")
public class CmwLightClient {
    private final ZContext context = new ZContext();
    private final ZMQ.Socket controlChannel;
    private final AtomicInteger connectionState = new AtomicInteger(0);

    // Message Types in the descriptor (Last part of a message containing the type of each sub message)
    private static final byte MT_HEADER = 0;
    private static final byte MT_BODY = 1; //
    private static final byte MT_BODY_DATA_CONTEXT = 2;
    private static final byte MT_BODY_REQUEST_CONTEXT = 3;
    private static final byte MT_BODY_EXCEPTION = 4;

    // TRANSPORT frames are only used internally in the cmw dispatcher socket
    private final byte SERVER_CONNECT_ACK = (byte) 0x01;
    private final byte SERVER_REP = (byte) 0x02;
    private final byte SERVER_HB = (byte) 0x03;
    private final byte CLIENT_CONNECT = (byte) 0x20;
    private final byte CLIENT_REQ = (byte) 0x21;
    private final byte CLIENT_HB = (byte) 0x22;
    // private final byte SERVER_TRANSPORT_CLOSE_ALL = (byte) 0x11;
    // private final byte SERVER_TRANSPORT_REP = (byte) 0x12;
    // private final byte SERVER_TRANSPORT_MULTI_REP = (byte) 0x13;
    // private final byte CLIENT_TRANSPORT_CONNECT = (byte) 0x30;
    // private final byte CLIENT_TRANSPORT_CLOSE = (byte) 0x31;
    // private final byte CLIENT_TRANSPORT_CLOSE_ALL = (byte) 0x32;
    // private final byte CLIENT_TRANSPORT_REQ = (byte) 0x33;

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
    public static final byte UT_IMMEDIATE_UPDATE = (byte) 2; //Update sent after the value has been modified by a set call.

    public CmwLightClient() {
        System.out.println("create new context");
        controlChannel = context.createSocket(SocketType.DEALER);
        System.out.println("setup socket");
        controlChannel.setSndHWM(0);
        controlChannel.setRcvHWM(0);
        controlChannel.setIdentity(getIdentity().getBytes()); // hostname/process/id/channel
        controlChannel.setLinger(0);
    }

    private String getIdentity() {
        String hostname;
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            hostname = "localhost";
        }
        final long processId = ProcessHandle.current().pid();
        final long connectionId = 1L;
        final long channelId = 1L;
        return hostname + '/' + processId + '/' + connectionId + '/' + channelId;
    }

    private void receiveData() { // NOPMD -- masking complexity this is a demo/test
        System.out.println();
        final ZMsg data = ZMsg.recvMsg(controlChannel);
        final ZFrame firstFrame = data.pollFirst();
        if (firstFrame == null) {
            System.out.println("firstFrame is null");
            return;
        }
        if (Arrays.equals(firstFrame.getData(), new byte[] { SERVER_HB })) {
            System.out.println("Heartbeat received");
            // TODO: reset Heartbeat
            return;
        }
        if (!Arrays.equals(firstFrame.getData(), new byte[] { SERVER_REP })) {
            System.out.println("expected reply message, but got: " + data);
        }
        final byte[] descriptor = data.pollLast().getData();
        if (Arrays.equals(data.getLast().getData(), new byte[] { MT_HEADER })) {
            System.out.println("received header: " + data);
        } else if (Arrays.equals(data.getLast().getData(), new byte[] { MT_HEADER, MT_BODY_EXCEPTION })) {
            System.out.println("received exception: " + data);
        } else if (!Arrays.equals(data.getLast().getData(), new byte[] { MT_HEADER, MT_BODY, MT_BODY_DATA_CONTEXT })) {
            System.out.println("expected reply message, but got: " + Arrays.toString(data.getLast().getData()));
            return;
        } else {
            System.out.println("received reply: " + data);
        }

        final ZFrame bodyFrame = data.pollFirst();
        if (bodyFrame == null) {
            System.out.println("bodyFrame is null");
            return;
        }
        for (final byte desc : descriptor) {
            switch (desc) {
            case MT_HEADER:
                System.out.println("header: " + new String(bodyFrame.getData()));
                break;
            case MT_BODY:
                final byte[] bytes = bodyFrame.getData();
                final IoClassSerialiser classSerialiser = new IoClassSerialiser(FastByteBuffer.wrap(bytes), CmwLightSerialiser.class);
                final SnoopAcquisition snoopAcq = classSerialiser.deserialiseObject(SnoopAcquisition.class);
                System.out.println("body: " + snoopAcq);
                break;
            case MT_BODY_DATA_CONTEXT:
                System.out.println("body data context: " + new String(bodyFrame.getData()));
                break;
            case MT_BODY_REQUEST_CONTEXT:
                System.out.println("body request context: " + new String(bodyFrame.getData()));
                break;
            case MT_BODY_EXCEPTION:
                System.out.println("body exception: " + new String(bodyFrame.getData()));
                break;
            default:
                System.out.println("invalid message type (" + desc + "): " + new String(bodyFrame.getData()));
                break;
            }
        }
    }

    public void subscribeSnoopFromDigitizer() {
        System.out.println("connect control socket");
        controlChannel.connect("tcp://dal005.acc.gsi.de:13810");

        connectClient();

        subscribe("GSCD001", "SnoopTriggerEvents", "FAIR.SELECTOR.ALL");

        while (!Thread.currentThread().isInterrupted()) {
            receiveData();
        }
    }

    public void getSnoopFromDigitizer() {
        System.out.println("connect control socket");
        controlChannel.connect("tcp://dal005.acc.gsi.de:13810");

        connectClient();

        sendHeartBeat();

        get("GSCD001", "SnoopTriggerEvents", "FAIR.SELECTOR.S=4");

        while (!Thread.currentThread().isInterrupted()) {
            receiveData();
        }
    }

    public void getLocal() {
        System.out.println("connect control socket");
        controlChannel.connect("tcp://SYSPC004:7777");

        connectClient();

        sendHeartBeat();

        get("testdevice", "unknownProp", "FAIR.SELECTOR.ALL");

        // return reply to data
        while (true) {
            final byte[] data = controlChannel.recv();
            if (data != null && data.length != 0 && Arrays.equals(data, new byte[] { MT_HEADER, MT_BODY_EXCEPTION })) { // header, exception
                break;
            }
        }
        System.out.println("Received GET Exception");

        get("testdevice", "testproperty", "FAIR.SELECTOR.ALL");

        receiveData();
        receiveData();

        subscribe("testdevice", "testproperty", "FAIR.SELECTOR.ALL");
        // just return all data
        while (!Thread.currentThread().isInterrupted()) {
            receiveData();
        }
    }

    private void sendHeartBeat() {
        System.out.println("Sending Heartbeat");
        controlChannel.send(new byte[] { CLIENT_HB });
    }

    private void connectClient() {
        System.out.println("connect client");
        controlChannel.send(new byte[] { CLIENT_CONNECT }, ZMQ.SNDMORE);
        controlChannel.send("1.0.0".getBytes());
        System.out.println(ZMsg.recvMsg(controlChannel));
        System.out.println("Client connected");
    }

    private void subscribe(final String testdevice, final String testprop, final String selector) {
        System.out.println("Sending SUB request");
        controlChannel.send(new byte[] { CLIENT_REQ }, ZMQ.SNDMORE);
        final CmwLightSerialiser serialiser = new CmwLightSerialiser(new FastByteBuffer(1024));
        serialiser.putHeaderInfo();
        serialiser.put(REQ_TYPE_TAG, RT_SUBSCRIBE); // GET
        serialiser.put(ID_TAG, 1L);
        serialiser.put(DEVICE_NAME_TAG, testdevice);
        serialiser.put(PROPERTY_NAME_TAG, testprop);
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
        serialiser.put(SELECTOR_TAG, selector); // 8: Context c : filters, x: data
        serialiser.getBuffer().flip();
        controlChannel.send(serialiser.getBuffer().elements(), 0, serialiser.getBuffer().limit(), ZMQ.SNDMORE);
        controlChannel.send(new byte[] { MT_HEADER, MT_BODY_REQUEST_CONTEXT });
    }

    private void get(final String deviceName, final String property, final String ctxSelector) {
        System.out.println("Sending get request");
        controlChannel.send(new byte[] { CLIENT_REQ }, ZMQ.SNDMORE);
        final CmwLightSerialiser serialiser = new CmwLightSerialiser(new FastByteBuffer(1024));
        serialiser.putHeaderInfo();
        serialiser.put(REQ_TYPE_TAG, RT_GET); // GET
        serialiser.put(ID_TAG, 1L);
        serialiser.put(DEVICE_NAME_TAG, deviceName);
        serialiser.put(PROPERTY_NAME_TAG, property);
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
        serialiser.put("8", ctxSelector); // 8: Context c : filters, x: data
        serialiser.getBuffer().flip();
        controlChannel.send(serialiser.getBuffer().elements(), 0, serialiser.getBuffer().limit(), ZMQ.SNDMORE);
        controlChannel.send(new byte[] { MT_HEADER, MT_BODY_REQUEST_CONTEXT });
    }

    public static void main(String[] args) {
        //new TestClient().getLocal();
        new CmwLightClient().subscribeSnoopFromDigitizer();
    }

    private static class SnoopAcquisition {
        public String TriggerEventName;
        public long acquisitionStamp;
        public int chainIndex;
        public long chainStartStamp;
        public int eventNumber;
        public long eventStamp;
        public int processIndex;
        public long processStartStamp;
        public int sequenceIndex;
        public long sequenceStartStamp;
        public int timingGroupID;

        @Override
        public String toString() {
            return "SnoopAcquisition{"
                    + "TriggerEventName='" + TriggerEventName + '\'' + ", acquisitionStamp=" + acquisitionStamp + ", chainIndex=" + chainIndex + ", chainStartStamp=" + chainStartStamp + ", eventNumber=" + eventNumber + ", eventStamp=" + eventStamp + ", processIndex=" + processIndex + ", processStartStamp=" + processStartStamp + ", sequenceIndex=" + sequenceIndex + ", sequenceStartStamp=" + sequenceStartStamp + ", timingGroupID=" + timingGroupID + '}';
        }
    }
}
