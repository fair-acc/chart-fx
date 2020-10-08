package de.gsi.microservice.concepts.majordomo;

import static org.zeromq.ZMQ.Socket;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.zeromq.SocketType;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;
import org.zeromq.util.ZData;

import zmq.SocketBase;

/**
 * Majordomo Protocol (MDP) definitions and implementations according to https://rfc.zeromq.org/spec/7/
 *
 * @author rstein
 */
public class MajordomoProtocol { // NOPMD nomen-est-omen
    public static final byte[] EMPTY_FRAME = new byte[] {};
    private static final String HEX_CHAR = "0123456789ABCDEF";

    public static MdpMessage receiveMdpMessage(final Socket socket) {
        return receiveMdpMessage(socket, true);
    }

    public static MdpMessage receiveMdpMessage(final Socket socket, final boolean wait) {
        assert socket != null : "socket must not be null";
        final int flags = wait ? 0 : ZMQ.DONTWAIT;

        final ZMsg msg = ZMsg.recvMsg(socket, flags);
        if (msg == null) {
            return null;
        }
        assert msg.size() >= 3;

        final byte[] senderId;

        if (socket.getSocketType() == SocketType.ROUTER) {
            senderId = msg.pop().getData();
            assert senderId != null : "first sender frame is empty";
        } else {
            senderId = null;
        }

        final ZFrame emptyFrame = msg.pop();
        assert emptyFrame.hasData() && emptyFrame.getData().length == 0 : "nominally empty message has data: " + emptyFrame.getData().length + " - '" + emptyFrame.toString() + "'";

        final ZFrame protocolFrame = msg.pop();
        assert protocolFrame.hasData();
        final MdpSubProtocol protocol = MdpSubProtocol.getProtocol(protocolFrame);

        switch (protocol) {
        case C_CLIENT:
            assert msg.size() >= 2;
            final MdpClientCommand clientCommand = MdpClientCommand.getCommand(MdpClientCommand.C_UNKNOWN.newFrame());
            final ZFrame serviceName = msg.pop();
            assert serviceName.getData() != null : "empty serviceName";

            final byte[][] clientMessages = new byte[msg.size()][];
            for (int i = 0; i < clientMessages.length; i++) {
                final ZFrame dataFrame = msg.pop();
                clientMessages[i] = dataFrame.hasData() ? dataFrame.getData() : new byte[0];
                dataFrame.destroy();
            }
            return new MdpClientMessage(senderId, clientCommand, serviceName.getData(), clientMessages);
        case W_WORKER:
            final ZFrame commandFrame = msg.pop();
            assert protocolFrame.hasData();
            final MdpWorkerCommand workerCommand = MdpWorkerCommand.getCommand(commandFrame);
            switch (workerCommand) {
            case W_HEARTBEAT:
            case W_DISCONNECT:
                assert msg.isEmpty()
                    : "MDP V0.1 does not support further frames for W_HEARTBEAT or W_DISCONNECT";
                return new MdpWorkerMessage(senderId, workerCommand, null, null);
            case W_READY:
                // service-name is optional
                return new MdpWorkerMessage(senderId, workerCommand, msg.isEmpty() ? null : msg.pop().getData(), null);
            case W_REQUEST:
            case W_REPLY:
                final byte[] clientSourceId = msg.pop().getData();
                assert clientSourceId != null : "clientSourceID must not be null";
                final ZFrame emptyFrame2 = msg.pop();
                assert emptyFrame2.hasData() && emptyFrame2.getData().length == 0 : "nominally empty message has data: " + emptyFrame2.getData().length + " - '" + emptyFrame2.toString() + "'";

                final byte[][] workerMessages = new byte[msg.size()][];
                for (int i = 0; i < workerMessages.length; i++) {
                    final ZFrame dataFrame = msg.pop();
                    workerMessages[i] = dataFrame.hasData() ? dataFrame.getData() : new byte[0];
                    dataFrame.destroy();
                }
                return new MdpWorkerMessage(senderId, workerCommand, null, clientSourceId, workerMessages);

            case W_UNKNOWN:
            default:
                assert false : "should not reach here for production code";
                return null;
            }
        case UNKNOWN:
        default:
            assert false : "should not reach here for production code";
            return null;
        }
    }

    /**
     * Send worker message according to the MDP 'client' sub-protocol
     *
     * @param socket ZeroMQ socket to send the message on
     * @param mdpClientCommand the MajordomoProtocol mdpWorkerCommand
     * @param sourceID the unique source ID of the peer client (usually 5 bytes, can be overwritten, BROKER sockets need this to be non-null)
     * @param serviceName the unique, original source ID the broker shall forward this message to
     * @param msg message(s) to be sent to MajordomoProtocol broker (if more than one, than the last is assumed to be a RBAC-token
     *
     * @return {@code true} if successful
     */
    public static boolean sendClientMessage(final Socket socket, final MdpClientCommand mdpClientCommand, final byte[] sourceID, final byte[] serviceName, final byte[]... msg) {
        assert socket != null : "socket must not be null";
        assert mdpClientCommand != null : "mdpClientCommand must not be null";
        assert serviceName != null : "serviceName must not be null";

        final SocketBase socketBase = socket.base();
        boolean status = true;
        if (socket.getSocketType() == SocketType.ROUTER) {
            assert sourceID != null : "sourceID must be non-null when using ROUTER sockets";
            status = socketBase.send(new zmq.Msg(sourceID), ZMQ.SNDMORE); // frame 0: source ID (optional, only needed for broker sockets)
        }
        status &= socketBase.send(new zmq.Msg(EMPTY_FRAME), ZMQ.SNDMORE); // frame 1: empty frame (0 bytes)
        status &= socketBase.send(new zmq.Msg(MdpSubProtocol.C_CLIENT.getFrameData()), ZMQ.SNDMORE); // frame 2: 'MDPCxx' client sub-protocol version
        status &= socketBase.send(new zmq.Msg(serviceName), ZMQ.SNDMORE); // frame 3: service name (UTF-8 string)
        // frame 3++: msg frames (last one being usually the RBAC token)
        for (int i = 0; i < msg.length; i++) {
            status &= socketBase.send(new zmq.Msg(msg[i]), i < msg.length - 1 ? ZMQ.SNDMORE : 0);
        }

        return status;
    }

    /**
     * Send worker message according to the MDP 'worker' sub-protocol
     *
     * @param socket ZeroMQ socket to send the message on
     * @param mdpWorkerCommand the MajordomoProtocol mdpWorkerCommand
     * @param sourceID the unique source ID of the peer client (usually 5 bytes, can be overwritten, BROKER sockets need this to be non-null)
     * @param clientID the unique, original source ID the broker shall forward this message to
     * @param msg message(s) to be sent to MajordomoProtocol broker (if more than one, than the last is assumed to be a RBAC-token
     *
     * @return {@code true} if successful
     */
    public static boolean sendWorkerMessage(final Socket socket, MdpWorkerCommand mdpWorkerCommand, final byte[] sourceID, final byte[] clientID, final byte[]... msg) {
        assert socket != null : "socket must not be null";
        assert mdpWorkerCommand != null : "mdpWorkerCommand must not be null";

        final SocketBase socketBase = socket.base();
        boolean status = true;
        if (socket.getSocketType() == SocketType.ROUTER) {
            assert sourceID != null : "sourceID must be non-null when using ROUTER sockets";
            status = socketBase.send(new zmq.Msg(sourceID), ZMQ.SNDMORE); // frame 0: source ID (optional, only needed for broker sockets)
        }
        socketBase.send(new zmq.Msg(EMPTY_FRAME), ZMQ.SNDMORE); // frame 1: empty frame (0 bytes)
        socketBase.send(new zmq.Msg(MdpSubProtocol.W_WORKER.getFrameData()), ZMQ.SNDMORE); // frame 2: 'MDPWxx' worker sub-protocol version

        switch (mdpWorkerCommand) {
        case W_HEARTBEAT:
        case W_DISCONNECT:
            assert msg.length == 0 : "byte[]... msg must be empty for W_HEARTBEAT and W_DISCONNECT commands";
            status &= socketBase.send(new zmq.Msg(mdpWorkerCommand.getFrameData()), 0); // frame 3: mdpWorkerCommand (1-byte:  W_HEARTBEAT, W_DISCONNECT)
            return status;
        case W_REQUEST:
        case W_REPLY:
        case W_READY:
            socketBase.send(new zmq.Msg(mdpWorkerCommand.getFrameData()), ZMQ.SNDMORE); // frame 3: mdpWorkerCommand (1-byte: W_READY, W_REQUEST, W_REPLY)
            assert clientID != null;
            if (msg.length == 0 && mdpWorkerCommand == MdpWorkerCommand.W_READY) {
                status &= socketBase.send(new zmq.Msg(clientID), 0); // frame 4: client ID (i.e. sourceID of the client that is known to the broker
            } else {
                assert msg.length != 0 : "byte[]... msg must not be empty";
                status &= socketBase.send(new zmq.Msg(clientID), ZMQ.SNDMORE); // frame 4: client ID (i.e. sourceID of the client that is known to the broker

                // optional additional payload after ready (e.g. service uniqueID, input/output property layout etc.)
                status &= socketBase.send(new zmq.Msg(EMPTY_FRAME), ZMQ.SNDMORE); // frame 5: empty frame (0 bytes)
                for (int i = 0; i < msg.length; i++) {
                    status &= socketBase.send(new zmq.Msg(msg[i]), i < msg.length - 1 ? ZMQ.SNDMORE : 0);
                }
            }
            return status;
        case W_UNKNOWN:
        default:
            throw new IllegalArgumentException("should not reach here/unknown command: " + mdpWorkerCommand);
        }
    }

    public static String strhex(byte[] data) {
        if (data == null) {
            return "";
        }
        StringBuilder b = new StringBuilder();
        for (byte aData : data) {
            int b1 = aData >>> 4 & 0xf;
            int b2 = aData & 0xf;
            b.append(HEX_CHAR.charAt(b1));
            b.append(HEX_CHAR.charAt(b2));
        }
        return b.toString();
    }

    /**
     * MDP sub-protocol V0.1
     */
    public enum MdpSubProtocol {
        C_CLIENT("MDPC01"), // MajordomoProtocol/Client implementation version
        W_WORKER("MDPW01"), // MajordomoProtocol/Worker implementation version
        UNKNOWN("XXXXXX");

        private final byte[] data;
        MdpSubProtocol(final String value) {
            this.data = value.getBytes(StandardCharsets.UTF_8);
        }

        public boolean frameEquals(ZFrame frame) {
            return Arrays.equals(data, frame.getData());
        }

        public byte[] getFrameData() {
            return data;
        }

        public boolean isEquals(final byte[] other) {
            return Arrays.equals(this.data, other);
        }

        public ZFrame newFrame() {
            return new ZFrame(data);
        }

        public static MdpSubProtocol getProtocol(ZFrame frame) {
            for (MdpSubProtocol knownProtocol : MdpSubProtocol.values()) {
                if (knownProtocol.frameEquals(frame)) {
                    if (knownProtocol == UNKNOWN) {
                        continue;
                    }
                    return knownProtocol;
                }
            }
            return UNKNOWN;
        }
    }

    /**
     * MajordomoProtocol/Server commands, as byte values
     */
    public enum MdpWorkerCommand {
        W_READY(0x01),
        W_REQUEST(0x02),
        W_REPLY(0x03),
        W_HEARTBEAT(0x04),
        W_DISCONNECT(0x05),
        W_UNKNOWN(-1);

        private final byte[] data;
        MdpWorkerCommand(final int value) { //watch for ints>255, will be truncated
            this.data = new byte[] { (byte) (value & 0xFF) };
        }

        public boolean frameEquals(ZFrame frame) {
            return Arrays.equals(data, frame.getData());
        }

        public byte[] getFrameData() {
            return data;
        }

        public ZFrame newFrame() {
            return new ZFrame(data);
        }

        public static MdpWorkerCommand getCommand(ZFrame frame) {
            for (MdpWorkerCommand knownMdpCommand : MdpWorkerCommand.values()) {
                if (knownMdpCommand.frameEquals(frame)) {
                    if (knownMdpCommand == W_UNKNOWN) {
                        continue;
                    }
                    return knownMdpCommand;
                }
            }
            return W_UNKNOWN;
        }
    }

    /**
     * MajordomoProtocol/Client commands, as byte values
     */
    public enum MdpClientCommand {
        C_UNKNOWN(-1); // N.B. Majordomo V0.1 does not provide dedicated client commands

        private final byte[] data;
        MdpClientCommand(final int value) { //watch for ints>255, will be truncated
            this.data = new byte[] { (byte) (value & 0xFF) };
        }

        public boolean frameEquals(ZFrame frame) {
            return Arrays.equals(data, frame.getData());
        }

        public byte[] getFrameData() {
            return data;
        }

        public ZFrame newFrame() {
            return new ZFrame(data);
        }

        public static MdpClientCommand getCommand(ZFrame frame) {
            for (MdpClientCommand knownMdpCommand : MdpClientCommand.values()) {
                if (knownMdpCommand.frameEquals(frame)) {
                    if (knownMdpCommand == C_UNKNOWN) {
                        continue;
                    }
                    return knownMdpCommand;
                }
            }
            return C_UNKNOWN;
        }
    }

    public static class MdpMessage {
        public final MdpSubProtocol protocol;
        public final boolean isClient;
        public final byte[] senderID;
        public final String senderIdHex;
        public final String senderName;
        public final byte[][] payload;

        public MdpMessage(final byte[] senderID, final MdpSubProtocol protocol, final byte[]... payload) {
            this.isClient = protocol == MdpSubProtocol.C_CLIENT;
            this.senderID = senderID;
            this.senderIdHex = strhex(senderID);
            this.senderName = senderID == null ? null : new String(senderID, StandardCharsets.UTF_8);
            this.protocol = protocol;
            this.payload = payload;
        }

        public byte[] getRbacFrame() {
            if (hasRbackToken()) {
                return payload[payload.length - 1];
            }
            return null;
        }

        public boolean hasRbackToken() {
            return payload.length >= 2;
        }

        @Override
        public String toString() {
            return "MdpMessage{isClient=" + isClient + ", senderID=" + ZData.toString(senderID) + ", payload=" + toString(payload) + '}';
        }

        protected static String toString(byte[][] byteValue) {
            if (byteValue == null) {
                return "(null)";
            }
            if (byteValue.length == 0) {
                return "[]";
            }
            if (byteValue.length == 1) {
                return "[" + ZData.toString(byteValue[0]) + "]";
            }
            StringBuilder b = new StringBuilder();
            b.append('[').append(ZData.toString(byteValue[0]));

            for (int i = 1; i < byteValue.length; i++) {
                b.append(", ").append(ZData.toString(byteValue[i]));
            }

            b.append(']');
            return b.toString();
        }
    }

    public static class MdpClientMessage extends MdpMessage {
        public final MdpClientCommand command;
        public final byte[] serviceNameBytes; // UTF-8 encoded service name
        public final String serviceName;
        public MdpClientMessage(final byte[] senderID, final MdpClientCommand clientCommand, final byte[] serviceNameBytes, final byte[]... clientMessages) {
            super(senderID, MdpSubProtocol.C_CLIENT, clientMessages);
            this.command = clientCommand;
            this.serviceNameBytes = serviceNameBytes;
            this.serviceName = new String(serviceNameBytes, StandardCharsets.UTF_8);
        }

        @Override
        public String toString() {
            return "MdpClientMessage{senderID=" + ZData.toString(senderID) + ", serviceName='" + serviceName + "', payload=" + toString(payload) + '}';
        }
    }

    public static class MdpWorkerMessage extends MdpMessage {
        public final MdpWorkerCommand command;
        public final byte[] serviceNameBytes; // UTF-8 encoded service name (optional - only for W_READY)
        public final String serviceName;
        public final byte[] clientSourceID;
        public final String clientSourceName;
        public MdpWorkerMessage(final byte[] senderID, final MdpWorkerCommand workerCommand, final byte[] serviceName, final byte[] clientSourceID, final byte[]... workerMessages) {
            super(senderID, MdpSubProtocol.W_WORKER, workerMessages);
            this.command = workerCommand;
            this.serviceNameBytes = serviceName;
            this.serviceName = serviceName == null ? null : new String(serviceName, StandardCharsets.UTF_8);
            this.clientSourceID = clientSourceID;
            this.clientSourceName = clientSourceID == null ? null : new String(clientSourceID, StandardCharsets.UTF_8);
        }

        @Override
        public String toString() {
            return "MdpWorkerMessage{senderID=" + ZData.toString(senderID) + ", command=" + command + ", serviceName='" + serviceName + "', clientSourceID='" + ZData.toString(clientSourceID) + "', payload=" + toString(payload) + '}';
        }
    }
}
