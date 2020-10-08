package de.gsi.microservice.concepts.majordomo;

import static de.gsi.microservice.concepts.majordomo.MajordomoProtocol.MdpClientCommand;
import static de.gsi.microservice.concepts.majordomo.MajordomoProtocol.MdpSubProtocol;

import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Formatter;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

/**
* Majordomo Protocol Client API, Java version Implements the MajordomoProtocol/Worker spec at
* http://rfc.zeromq.org/spec:7.
*
*/
public class MajordomoClientV1 {
    private static final Logger LOGGER = LoggerFactory.getLogger(MajordomoClientV1.class);
    private static final AtomicInteger CLIENT_V1_INSTANCE = new AtomicInteger();
    private final String uniqueID;
    private final byte[] uniqueIdBytes;
    private String broker;
    private ZContext ctx;
    private ZMQ.Socket clientSocket;
    private long timeout = 2500;
    private int retries = 3;
    private Formatter log = new Formatter(System.out);
    private ZMQ.Poller poller;

    public MajordomoClientV1(String broker, String clientName) {
        this.broker = broker;
        ctx = new ZContext();

        uniqueID = clientName + "PID=" + ManagementFactory.getRuntimeMXBean().getName() + "-InstanceID=" + CLIENT_V1_INSTANCE.getAndIncrement();
        uniqueIdBytes = uniqueID.getBytes(ZMQ.CHARSET);

        reconnectToBroker();
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
        clientSocket.setIdentity(uniqueIdBytes);
        clientSocket.connect(broker);

        if (poller != null) {
            poller.unregister(clientSocket);
            poller.close();
        }
        poller = ctx.createPoller(1);
        poller.register(clientSocket, ZMQ.Poller.POLLIN);
        LOGGER.atDebug().addArgument(broker).log("connecting to broker at: '{}'");
    }

    public void destroy() {
        ctx.destroy();
    }

    public int getRetries() {
        return retries;
    }

    public void setRetries(int retries) {
        this.retries = retries;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public String getUniqueID() {
        return uniqueID;
    }

    /**
     * Send request to broker and get reply by hook or crook takes ownership of
     * request message and destroys it when sent. Returns the reply message or
     * NULL if there was no reply.
     *
     * @param service service name
     * @param msgs message(s) to be sent to MajordomoProtocol broker (if more than one, than the last is assumed to be a RBAC-token
     * @return reply message or NULL if there was no reply
     */
    public ZMsg send(final String service, final byte[]... msgs) {
        return send(service.getBytes(StandardCharsets.UTF_8), msgs);
    }

    /**
     * Send request to broker and get reply by hook or crook takes ownership of
     * request message and destroys it when sent. Returns the reply message or
     * NULL if there was no reply.
     *
     * @param service UTF-8 encoded service name byte array
     * @param msgs message(s) to be sent to MajordomoProtocol broker (if more than one, than the last is assumed to be a RBAC-token
     * @return reply message or NULL if there was no reply
     */
    public ZMsg send(final byte[] service, final byte[]... msgs) {
        ZMsg reply = null;

        int retriesLeft = retries;
        while (retriesLeft > 0 && !Thread.currentThread().isInterrupted()) {
            if (!MajordomoProtocol.sendClientMessage(clientSocket, MdpClientCommand.C_UNKNOWN, null, service, msgs)) {
                throw new IllegalStateException("could not send request " + Arrays.toString(msgs));
            }

            // Poll socket for a reply, with timeout
            if (poller.poll(timeout) == -1)
                break; // Interrupted

            if (poller.pollin(0)) {
                ZMsg msg = ZMsg.recvMsg(clientSocket, false);
                LOGGER.atDebug().addArgument(msg).log("received reply: '{}'");

                if (msg == null) {
                    break;
                }
                // Don't try to handle errors, just assert noisily
                assert (msg.size() >= 3);

                ZFrame emptyFrame = msg.pop();
                assert emptyFrame.size() == 0;

                ZFrame header = msg.pop();
                assert (MdpSubProtocol.C_CLIENT.isEquals(header.getData()));
                header.destroy();

                ZFrame replyService = msg.pop();
                assert (Arrays.equals(service, replyService.getData()));
                replyService.destroy();

                reply = msg;
                break;
            } else {
                if (--retriesLeft == 0) {
                    log.format("W: permanent error, abandoning\n");
                    break;
                }
                log.format("W: no reply, reconnecting\n");
                reconnectToBroker();
            }
        }
        return reply;
    }
}
