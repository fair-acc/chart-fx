package de.gsi.microservice.concepts.majordomo;

import static de.gsi.microservice.concepts.majordomo.MajordomoProtocol.MdpClientCommand;
import static de.gsi.microservice.concepts.majordomo.MajordomoProtocol.MdpSubProtocol;
import static de.gsi.microservice.concepts.majordomo.MajordomoProtocol.sendClientMessage;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

/**
 * Majordomo Protocol Client API, asynchronous Java version. Implements the
 * MajordomoProtocol/Worker spec at http://rfc.zeromq.org/spec:7.
 */
public class MajordomoClientV2 {
    private static final Logger LOGGER = LoggerFactory.getLogger(MajordomoClientV2.class);
    private final String broker;
    private final ZContext ctx;
    private ZMQ.Socket clientSocket;
    private long timeout = 2500;
    private ZMQ.Poller poller;

    public MajordomoClientV2(final String broker) {
        this.broker = broker;
        ctx = new ZContext();
        reconnectToBroker();
    }

    public void destroy() {
        ctx.destroy();
    }

    public long getTimeout() {
        return timeout;
    }

    /**
     * Returns the reply message or NULL if there was no reply. Does not attempt
     * to recover from a broker failure, this is not possible without storing
     * all unanswered requests and resending them all…
     */
    public ZMsg recv() {
        ZMsg reply = null;

        // Poll socket for a reply, with timeout
        if (poller.poll(timeout * 1000) == -1) {
            return null; // Interrupted
        }

        if (poller.pollin(0)) {
            ZMsg msg = ZMsg.recvMsg(clientSocket);
            LOGGER.atDebug().addArgument(msg).log("received reply: '{}'");

            // Don't try to handle errors, just assert noisily
            assert (msg.size() >= 4);

            ZFrame empty = msg.pop();
            assert (empty.getData().length == 0);
            empty.destroy();

            ZFrame header = msg.pop();
            assert (MdpSubProtocol.C_CLIENT.isEquals(header.getData()));
            header.destroy();

            ZFrame replyService = msg.pop();
            replyService.destroy();

            reply = msg;
        }
        return reply;
    }

    /**
     * Send request to broker and get reply by hook or crook Takes ownership of request message and destroys it when sent.
     *
     * @param service UTF-8 encoded service name byte array
     * @param msgs message(s) to be sent to MajordomoProtocol broker (if more than one, than the last is assumed to be a RBAC-token
     */
    public boolean send(final byte[] service, final byte[]... msgs) {
        return sendClientMessage(clientSocket, MdpClientCommand.C_UNKNOWN, null, service, msgs);
    }

    /**
     * Send request to broker and get reply by hook or crook Takes ownership of request message and destroys it when sent.
     *
     * @param service UTF-8 encoded service name byte array
     * @param msgs message(s) to be sent to MajordomoProtocol broker (if more than one, than the last is assumed to be a RBAC-token
     */
    public boolean send(final String service, final byte[]... msgs) {
        return send(service.getBytes(StandardCharsets.UTF_8), msgs);
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    /**
     * Connect or reconnect to broker
     */
    void reconnectToBroker() {
        if (clientSocket != null) {
            clientSocket.close();
        }
        clientSocket = ctx.createSocket(SocketType.DEALER);
        clientSocket.setHWM(0);
        clientSocket.setIdentity("clientV2".getBytes(StandardCharsets.UTF_8));
        clientSocket.connect(broker);
        if (poller != null) {
            poller.unregister(clientSocket);
            poller.close();
        }
        poller = ctx.createPoller(1);
        poller.register(clientSocket, ZMQ.Poller.POLLIN);
        LOGGER.atDebug().addArgument(broker).log("connecting to broker at: '{}'");
    }
}
