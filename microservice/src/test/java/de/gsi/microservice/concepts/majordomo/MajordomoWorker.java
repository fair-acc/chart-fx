package de.gsi.microservice.concepts.majordomo;

import static de.gsi.microservice.concepts.majordomo.MajordomoProtocol.MdpMessage;
import static de.gsi.microservice.concepts.majordomo.MajordomoProtocol.MdpWorkerCommand;
import static de.gsi.microservice.concepts.majordomo.MajordomoProtocol.MdpWorkerMessage;
import static de.gsi.microservice.concepts.majordomo.MajordomoProtocol.receiveMdpMessage;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import de.gsi.microservice.rbac.RbacRole;
import de.gsi.microservice.utils.SystemProperties;

/**
 * Majordomo Protocol Client API, Java version Implements the MajordomoProtocol/Worker spec at
 * http://rfc.zeromq.org/spec:7.
 *
 * default heart-beat time-out [ms] is set by system property: 'OpenCMW.heartBeat' // default: 2500 [ms]
 * default heart-beat liveness is set by system property: 'OpenCMW.heartBeatLiveness' // [counts] 3-5 is reasonable
 * N.B. heartbeat expires when last heartbeat message is more than HEARTBEAT_INTERVAL * HEARTBEAT_LIVENESS ms ago.
 * this implies also, that worker must either return their message within 'HEARTBEAT_INTERVAL * HEARTBEAT_LIVENESS ms' or decouple their secondary handler interface into another thread.
 *
 */
public class MajordomoWorker extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(MajordomoWorker.class);
    private static final int HEARTBEAT_LIVENESS = SystemProperties.getValueIgnoreCase("OpenCMW.heartBeatLiveness", 3); // [counts] 3-5 is reasonable
    private static final int HEARTBEAT_INTERVAL = SystemProperties.getValueIgnoreCase("OpenCMW.heartBeat", 2500); // [ms]
    private static final AtomicInteger WORKER_COUNTER = new AtomicInteger();

    // ---------------------------------------------------------------------
    protected final String uniqueID;
    protected final ZContext ctx;
    private final String brokerAddress;
    private final String serviceName;
    private final byte[] serviceBytes;

    private final AtomicBoolean run = new AtomicBoolean(true);
    private final SortedSet<RbacRole<?>> rbacRoles;
    private ZMQ.Socket workerSocket; // Socket to broker
    private long heartbeatAt; // When to send HEARTBEAT
    private int liveness; // How many attempts left
    private int reconnect = 2500; // Reconnect delay, msecs
    private RequestHandler requestHandler;
    private ZMQ.Poller poller;

    public MajordomoWorker(String brokerAddress, String serviceName, final RbacRole<?>... rbacRoles) {
        this(null, brokerAddress, serviceName, rbacRoles);
    }

    public MajordomoWorker(ZContext ctx, String serviceName, final RbacRole<?>... rbacRoles) {
        this(ctx, "inproc://broker", serviceName, rbacRoles);
    }

    protected MajordomoWorker(ZContext ctx, String brokerAddress, String serviceName, final RbacRole<?>... rbacRoles) {
        assert (brokerAddress != null);
        assert (serviceName != null);
        this.brokerAddress = brokerAddress;
        this.serviceName = serviceName;
        this.serviceBytes = serviceName.getBytes(StandardCharsets.UTF_8);

        // initialise RBAC role-based priority queues
        this.rbacRoles = Collections.unmodifiableSortedSet(new TreeSet<>(Set.of(rbacRoles)));

        this.ctx = Objects.requireNonNullElseGet(ctx, ZContext::new);
        if (ctx != null) {
            this.setDaemon(true);
        }
        this.setName(MajordomoWorker.class.getSimpleName() + "#" + WORKER_COUNTER.getAndIncrement());
        this.uniqueID = this.serviceName + "-PID=" + ManagementFactory.getRuntimeMXBean().getName() + "-TID=" + this.getId();

        this.setName(this.getClass().getSimpleName() + "(\"" + this.serviceName + "\")-" + uniqueID);

        LOGGER.atDebug().addArgument(serviceName).addArgument(uniqueID).log("created new service '{}' worker - uniqueID: {}");
    }

    public void destroy() {
        ctx.destroy();
    }

    public int getHeartbeat() {
        return HEARTBEAT_INTERVAL;
    }

    public SortedSet<RbacRole<?>> getRbacRoles() {
        return rbacRoles;
    }

    public int getReconnect() {
        return reconnect;
    }

    public RequestHandler getRequestHandler() {
        return requestHandler;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getUniqueID() {
        return uniqueID;
    }

    public MdpMessage handleRequestsFromBoker(final MdpWorkerMessage request) {
        if (request == null) {
            return null;
        }

        switch (request.command) {
        case W_REQUEST:
            return processRequest(request, request.clientSourceID);
        case W_HEARTBEAT:
            // Do nothing for heartbeats
            break;
        case W_DISCONNECT:
            reconnectToBroker();
            break;
        case W_UNKNOWN:
        default:
            // N.B. not too verbose logging since we do not want that sloppy clients can bring down the broker through warning or info messages
            if (LOGGER.isDebugEnabled()) {
                LOGGER.atDebug().addArgument(uniqueID).addArgument(request).log("worer '{}' received invalid message: '{}'");
            }
            break;
        }
        return null;
    }

    public MdpMessage processRequest(final MdpMessage request, final byte[] clientSourceID) {
        if (requestHandler != null) {
            // de-serialise
            // byte[] -> PropertyMap() (+ getObject(Class<?>))
            try {
                final byte[][] payload = requestHandler.handle(request.payload);
                // serialise
                return new MdpWorkerMessage(request.senderID, MdpWorkerCommand.W_REPLY, serviceBytes, clientSourceID, payload);
            } catch (Throwable e) { // NOPMD on purpose since we want to catch exceptions and courteously return this to the user
                final StringWriter sw = new StringWriter();
                final PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);

                final String exceptionMsg = // NOPMD NOSONAR -- easier to read !?!?
                        MajordomoWorker.this.getClass().getName() + " caught exception in user-provided call-back function for service '" + getServiceName() + "'\nrequest msg: " + request + "\nexception: " + sw.toString();
                return new MdpWorkerMessage(request.senderID, MdpWorkerCommand.W_REPLY, serviceBytes, clientSourceID, exceptionMsg.getBytes(StandardCharsets.UTF_8));
            }
        }
        return null;
    }

    public void registerHandler(final RequestHandler handler) {
        this.requestHandler = handler;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            handleReceive();
        }
        destroy();
    }

    public void setReconnect(int reconnect) {
        this.reconnect = reconnect;
    }

    @Override
    public synchronized void start() {
        run.set(true);
        reconnectToBroker();
        super.start();
    }

    public void stopWorker() {
        run.set(false);
    }

    /**
     * Send reply, if any, to broker and wait for next request.
     */
    protected void handleReceive() { // NOPMD -- single serial function .. easier to read
        while (run.get() && !Thread.currentThread().isInterrupted()) {
            // Poll socket for a reply, with timeout
            if (poller.poll(HEARTBEAT_INTERVAL) == -1) {
                break; // Interrupted
            }

            if (poller.pollin(0)) {
                final MdpMessage msg = receiveMdpMessage(workerSocket);
                if (msg == null) {
                    continue;
                    // break; // Interrupted
                }
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.atDebug().addArgument(uniqueID).addArgument(msg).log("worker '{}' received new message from broker: '{}'");
                }
                liveness = HEARTBEAT_LIVENESS;
                // Don't try to handle errors, just assert noisily
                assert msg.payload.length > 0 : "MdpWorkerMessage payload is null";
                if (!(msg instanceof MdpWorkerMessage)) {
                    assert false : "msg is not instance of MdpWorkerMessage";
                    continue;
                }
                final MdpWorkerMessage workerMessage = (MdpWorkerMessage) msg;

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.atDebug().addArgument(uniqueID).addArgument(workerMessage).log("worker '{}' received request: '{}'");
                }

                final MdpMessage reply = handleRequestsFromBoker(workerMessage);

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.atDebug().addArgument(uniqueID).addArgument(reply).log("worker '{}' received reply: '{}'");
                }

                if (reply != null) {
                    MajordomoProtocol.sendWorkerMessage(workerSocket, MdpWorkerCommand.W_REPLY, reply.senderID, workerMessage.clientSourceID, reply.payload);
                }

            } else if (--liveness == 0) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.atDebug().addArgument(uniqueID).log("worker '{}' disconnected from broker - retrying");
                }
                try {
                    Thread.sleep(reconnect); // NOSONAR
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Restore the interrupted status
                    break;
                }
                reconnectToBroker();
            }

            // Send HEARTBEAT if it's time
            if (System.currentTimeMillis() > heartbeatAt) {
                MajordomoProtocol.sendWorkerMessage(workerSocket, MdpWorkerCommand.W_HEARTBEAT, null, null);
                heartbeatAt = System.currentTimeMillis() + HEARTBEAT_INTERVAL;
            }
        }
        if (Thread.currentThread().isInterrupted()) {
            LOGGER.atInfo().addArgument(uniqueID).log("worker '{}' interrupt received, killing worker");
        }
    }

    /**
     * Connect or reconnect to broker
     */
    protected void reconnectToBroker() {
        if (workerSocket != null) {
            workerSocket.close();
        }
        workerSocket = ctx.createSocket(SocketType.DEALER);
        workerSocket.setHWM(0);
        workerSocket.connect(brokerAddress);
        LOGGER.atDebug().addArgument(uniqueID).addArgument(brokerAddress).log("worker '{}' connecting to broker at '{}'");

        // Register service with broker
        MajordomoProtocol.sendWorkerMessage(workerSocket, MdpWorkerCommand.W_READY, null, serviceBytes, getUniqueID().getBytes(StandardCharsets.UTF_8));

        if (poller != null) {
            poller.unregister(workerSocket);
            poller.close();
        }
        poller = ctx.createPoller(1);
        poller.register(workerSocket, ZMQ.Poller.POLLIN);

        // If liveness hits zero, queue is considered disconnected
        liveness = HEARTBEAT_LIVENESS;
        heartbeatAt = System.currentTimeMillis() + HEARTBEAT_INTERVAL;
    }

    public interface RequestHandler {
        byte[][] handle(byte[][] payload) throws Throwable;
    }
}
