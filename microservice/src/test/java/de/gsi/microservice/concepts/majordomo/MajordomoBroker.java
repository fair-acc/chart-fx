package de.gsi.microservice.concepts.majordomo;

import static java.util.Objects.requireNonNull;
import static org.zeromq.ZMQ.Socket;

import static de.gsi.microservice.concepts.majordomo.MajordomoProtocol.MdpClientCommand;
import static de.gsi.microservice.concepts.majordomo.MajordomoProtocol.MdpClientMessage;
import static de.gsi.microservice.concepts.majordomo.MajordomoProtocol.MdpMessage;
import static de.gsi.microservice.concepts.majordomo.MajordomoProtocol.MdpSubProtocol;
import static de.gsi.microservice.concepts.majordomo.MajordomoProtocol.MdpWorkerCommand;
import static de.gsi.microservice.concepts.majordomo.MajordomoProtocol.MdpWorkerMessage;
import static de.gsi.microservice.concepts.majordomo.MajordomoProtocol.receiveMdpMessage;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
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
import org.zeromq.ZMQException;

import de.gsi.microservice.rbac.BasicRbacRole;
import de.gsi.microservice.rbac.RbacRole;
import de.gsi.microservice.rbac.RbacToken;
import de.gsi.microservice.utils.SystemProperties;

/**
 * Majordomo Protocol broker -- a minimal implementation of http://rfc.zeromq.org/spec:7 and spec:8
 *
 *  default heart-beat time-out [ms] is set by system property: 'OpenCMW.heartBeat' // default: 2500 [ms]
 *  default heart-beat liveness is set by system property: 'OpenCMW.heartBeatLiveness' // [counts] 3-5 is reasonable
 *  N.B. heartbeat expires when last heartbeat message is more than HEARTBEAT_INTERVAL * HEARTBEAT_LIVENESS ms ago.
 *  this implies also, that worker must either return their message within 'HEARTBEAT_INTERVAL * HEARTBEAT_LIVENESS ms' or decouple their secondary handler interface into another thread.
 *
 *  default client time-out [s] is set by system property: 'OpenCMW.clientTimeOut' // default: 3600 [s] -- after which unanswered client messages and info are being deleted
 *
*/
public class MajordomoBroker extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(MajordomoBroker.class);
    private static final byte[] INTERNAL_SENDER_ID = null;
    private static final String INTERNAL_SERVICE_PREFIX = "mmi.";
    private static final byte[] INTERNAL_SERVICE_PREFIX_BYTES = INTERNAL_SERVICE_PREFIX.getBytes(StandardCharsets.UTF_8);
    private static final int HEARTBEAT_LIVENESS = SystemProperties.getValueIgnoreCase("OpenCMW.heartBeatLiveness", 3); // [counts] 3-5 is reasonable
    private static final int HEARTBEAT_INTERVAL = SystemProperties.getValueIgnoreCase("OpenCMW.heartBeat", 2500); // [ms]
    private static final int HEARTBEAT_EXPIRY = HEARTBEAT_INTERVAL * HEARTBEAT_LIVENESS;
    private static final int CLIENT_TIMEOUT = SystemProperties.getValueIgnoreCase("OpenCMW.clientTimeOut", 0); // [s]
    private static final AtomicInteger BROKER_COUNTER = new AtomicInteger();
    private static final AtomicInteger WORKER_COUNTER = new AtomicInteger();

    // ---------------------------------------------------------------------
    private final ZContext ctx;
    private final Socket internalRouterSocket;
    private final Socket internalServiceSocket;
    private final List<Socket> routerSockets = new ArrayList<>(); // Sockets for clients & public external workers
    private final AtomicBoolean run = new AtomicBoolean(false);
    private final SortedSet<RbacRole<?>> rbacRoles;
    private final Map<String, Service> services = new HashMap<>(); // known services Map<'service name', Service>
    private final Map<String, Worker> workers = new HashMap<>(); // known workers Map<addressHex, Worker>
    private final Map<String, Client> clients = new HashMap<>();

    private final Deque<Worker> waiting = new ArrayDeque<>(); // idle workers
    private long heartbeatAt = System.currentTimeMillis() + HEARTBEAT_INTERVAL; // When to send HEARTBEAT

    /**
     * Initialize broker state.
     * @param ioThreads number of threads dedicated to network IO (recommendation 1 thread per 1 GBit/s)
     * @param rbacRoles RBAC-based roles (used for IO prioritisation and service access control
     */
    public MajordomoBroker(final int ioThreads, final RbacRole<?>... rbacRoles) {
        this.setName(MajordomoBroker.class.getSimpleName() + "#" + BROKER_COUNTER.getAndIncrement());

        ctx = new ZContext(ioThreads);

        // initialise RBAC role-based priority queues
        this.rbacRoles = Collections.unmodifiableSortedSet(new TreeSet<>(Set.of(rbacRoles)));

        // generate and register internal default inproc socket
        this.internalRouterSocket = bind("inproc://broker"); // NOPMD
        this.internalServiceSocket = bind("inproc://intService"); // NOPMD
        //this.internalServiceSocket.setRouterMandatory(true);

        registerDefaultServices(rbacRoles); // NOPMD
    }

    public void addInternalService(final MajordomoWorker worker, final int nServiceThreads) {
        assert worker != null : "worker must not be null";
        final Service oldWorker = services.put(worker.getServiceName(), new Service(worker.getServiceName(), worker.getServiceName().getBytes(StandardCharsets.UTF_8), worker, nServiceThreads));
        if (oldWorker != null) {
            LOGGER.atWarn().addArgument(worker.getServiceName()).log("overwriting existing internal service definition for '{}'");
        }
    }

    /**
     * Bind broker to endpoint, can call this multiple times. We use a single
     * socket for both clients and workers.
     */
    public Socket bind(String endpoint) {
        final Socket routerSocket = ctx.createSocket(SocketType.ROUTER);
        routerSocket.setHWM(0);
        routerSocket.bind(endpoint);
        routerSockets.add(routerSocket);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().addArgument(endpoint).log("Majordomo broker/0.1 is active at '{}'");
        }
        return routerSocket;
    }

    public ZContext getContext() {
        return ctx;
    }

    public Socket getInternalRouterSocket() {
        return internalRouterSocket;
    }

    /**
     * @return unmodifiable list of registered external sockets
     */
    public List<Socket> getRouterSockets() {
        return Collections.unmodifiableList(routerSockets);
    }

    public Collection<Service> getServices() {
        return services.values();
    }

    public boolean isRunning() {
        return run.get();
    }

    public void removeService(final String serviceName) {
        services.remove(serviceName);
    }

    /**
     * main broker work happens here
     */
    public void run() {
        final ZMQ.Poller items = ctx.createPoller(routerSockets.size());
        for (Socket routerSocket : routerSockets) {
            items.register(routerSocket, ZMQ.Poller.POLLIN);
        }
        while (run.get() && !Thread.currentThread().isInterrupted()) {
            if (items.poll(HEARTBEAT_INTERVAL) == -1) {
                break; // interrupted
            }

            int loopCount = 0;
            while (run.get()) {
                boolean processData = false;
                for (Socket routerSocket : routerSockets) {
                    final MdpMessage msg = receiveMdpMessage(routerSocket, false);
                    if (msg != null) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.atDebug().addArgument(msg).log("Majordomo broker received new message: '{}'");
                        }
                        processData |= handleReceivedMessage(routerSocket, msg);
                    }
                }

                processClients();
                if (loopCount % 10 == 0) {
                    // perform maintenance tasks during the first and every tenth iteration
                    purgeWorkers();
                    purgeClients();
                    sendHeartbeats();
                }
                loopCount++;
                if (!processData) {
                    break;
                }
            }
        }
        items.close();
        destroy(); // interrupted
    }

    @Override
    public synchronized void start() {
        run.set(true);
        services.forEach((serviceName, service) -> service.internalWorkers.forEach(Thread::start));
        super.start();
    }

    public void stopBroker() {
        run.set(false);
    }

    /**
     * Deletes worker from all data structures, and destroys worker.
     */
    protected void deleteWorker(Worker worker, boolean disconnect) {
        assert (worker != null);
        if (disconnect) {
            MajordomoProtocol.sendWorkerMessage(worker.socket, MdpWorkerCommand.W_DISCONNECT, worker.address, null);
        }
        if (worker.service != null) {
            worker.service.waiting.remove(worker);
        }
        workers.remove(worker.addressHex);
    }

    /**
     * Disconnect all workers, destroy context.
     */
    protected void destroy() {
        Worker[] deleteList = workers.values().toArray(new Worker[0]);
        for (Worker worker : deleteList) {
            deleteWorker(worker, true);
        }
        ctx.destroy();
    }

    /**
     * Dispatch requests to waiting workers as possible
     */
    protected void dispatch(Service service) {
        assert (service != null);
        purgeWorkers();
        while (!service.waiting.isEmpty() && service.requestsPending()) {
            final MdpMessage msg = service.getNextPrioritisedMessage();
            if (msg == null) {
                // should be thrown only with VM '-ea' enabled -- assert noisily since this a (rare|design) library error
                assert false : "getNextPrioritisedMessage should not be null";
                continue;
            }
            Worker worker = service.waiting.pop();
            waiting.remove(worker);
            MajordomoProtocol.sendWorkerMessage(worker.socket, MdpWorkerCommand.W_REQUEST, worker.address, msg.senderID, msg.payload);
        }
    }

    protected boolean handleReceivedMessage(final Socket receiveSocket, final MdpMessage msg) {
        switch (msg.protocol) {
        case C_CLIENT:
            // Set reply return address to client sender
            final Client client = clients.computeIfAbsent(msg.senderName, s -> new Client(receiveSocket, msg.protocol, msg.senderName, msg.senderID));
            client.offerToQueue((MdpClientMessage) msg);
            return true;
        case W_WORKER:
            processWorker(receiveSocket, (MdpWorkerMessage) msg);
            return true;
        default:
            // N.B. not too verbose logging since we do not want that sloppy clients can bring down the broker through warning or info messages
            if (LOGGER.isDebugEnabled()) {
                LOGGER.atDebug().addArgument(msg).log("Majordomo broker invalid message: '{}'");
            }
            return false;
        }
    }

    /**
     * Process a request coming from a client.
     */
    protected void processClients() {
        // round-robbin
        clients.forEach((name, client) -> {
            final MdpClientMessage clientMessage = client.pop();
            if (clientMessage == null) {
                return;
            }
            // dispatch client message to worker queue
            final Service service = services.get(clientMessage.serviceName);
            if (service == null) {
                // not implemented -- according to Majordomo Management Interface (MMI) as defined in http://rfc.zeromq.org/spec:8
                MajordomoProtocol.sendClientMessage(client.socket, MdpClientCommand.C_UNKNOWN, clientMessage.senderID, clientMessage.serviceNameBytes, "501".getBytes(StandardCharsets.UTF_8));
                return;
            }
            // queue new client message RBAC-priority-based
            service.putPrioritisedMessage(clientMessage);

            // dispatch service
            if (service.isInternal) {
                final MdpClientMessage msg = service.getNextPrioritisedMessage();
                if (msg == null) {
                    // should be thrown only with VM '-ea' enabled -- assert noisily since this a (rare|design) library error
                    assert false : "getNextPrioritisedMessage should not be null";
                    return;
                }
                MajordomoProtocol.sendWorkerMessage(service.internalDispatchSocket, MdpWorkerCommand.W_REQUEST, null, msg.senderID, msg.payload);
            } else {
                //dispatch(requireService(clientMessage.serviceName, clientMessage.serviceNameBytes));
                dispatch(service);
            }
        });
    }

    /**
     * Process message sent to us by a worker.
     */
    protected void processWorker(final Socket receiveSocket, final MdpWorkerMessage msg) {
        final boolean isInternal = internalServiceSocket.equals(receiveSocket);
        final boolean workerReady = isInternal || workers.containsKey(msg.senderIdHex);
        final Worker worker; // = requireWorker(receiveSocket, msg.senderID, msg.senderIdHex);
        switch (msg.command) {
        case W_READY:
            worker = requireWorker(receiveSocket, msg.senderID, msg.senderIdHex);
            // Not first mdpWorkerCommand in session || Reserved service name
            if (workerReady || Arrays.equals(INTERNAL_SERVICE_PREFIX_BYTES, 0, 3, msg.senderID, 0, 3))
                deleteWorker(worker, true);
            else {
                // Attach worker to service and mark as idle
                worker.service = requireService(msg.serviceName, msg.serviceNameBytes);
                workerWaiting(worker);
            }
            break;
        case W_REPLY:
            if (workerReady) {
                worker = isInternal ? null : requireWorker(receiveSocket, msg.senderID, msg.senderIdHex);
                final byte[] serviceName = isInternal ? msg.senderID : worker.service.nameBytes;
                final Client client = clients.get(msg.clientSourceName);
                if (client == null || client.socket == null) {
                    break;
                }

                if (client.protocol == MdpSubProtocol.C_CLIENT) { // OpenCMW
                    MajordomoProtocol.sendClientMessage(client.socket, MdpClientCommand.C_UNKNOWN, msg.clientSourceID, serviceName, msg.payload);
                } else {
                    // TODO: add other branches for:
                    // * CmwLight
                    // * REST/JSON
                    // * REST/HTML
                    throw new IllegalStateException("Unexpected value: " + client.protocol);
                }
                if (!isInternal) {
                    workerWaiting(worker);
                }
            } else {
                worker = requireWorker(receiveSocket, msg.senderID, msg.senderIdHex);
                deleteWorker(worker, true);
            }
            break;
        case W_HEARTBEAT:
            worker = requireWorker(receiveSocket, msg.senderID, msg.senderIdHex);
            if (workerReady) {
                worker.expiry = System.currentTimeMillis() + HEARTBEAT_EXPIRY;
            } else {
                deleteWorker(worker, true);
            }
            break;
        case W_DISCONNECT:
            worker = requireWorker(receiveSocket, msg.senderID, msg.senderIdHex);
            deleteWorker(worker, false);
            break;
        default:
            // N.B. not too verbose logging since we do not want that sloppy clients can bring down the broker through warning or info messages
            if (LOGGER.isDebugEnabled()) {
                LOGGER.atDebug().addArgument(msg).log("Majordomo broker invalid message: '{}'");
            }
            break;
        }
    }

    /**
     * Look for &amp; kill expired clients.
     */
    protected /*synchronized*/ void purgeClients() {
        if (CLIENT_TIMEOUT <= 0) {
            return;
        }
        for (String clientName : clients.keySet().toArray(String[] ::new)) { // copy because we are going to remove keys
            Client client = clients.get(clientName);
            if (client == null || client.expiry < System.currentTimeMillis()) {
                clients.remove(clientName);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.atDebug().addArgument(client).log("Majordomo broker deleting expired client: '{}'");
                }
            }
        }
    }

    /**
     * Look for &amp; kill expired workers. Workers are oldest to most recent, so we stop at the first alive worker.
     */
    protected /*synchronized*/ void purgeWorkers() {
        for (Worker w = waiting.peekFirst(); w != null && w.expiry < System.currentTimeMillis(); w = waiting.peekFirst()) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.atInfo().addArgument(w.addressHex).addArgument(w.service == null ? "(unknown)" : w.service.name).log("Majordomo broker deleting expired worker: '{}' - service: '{}'");
            }
            deleteWorker(waiting.pollFirst(), false);
        }
    }

    protected void registerDefaultServices(final RbacRole<?>[] rbacRoles) {
        // add simple internal Majordomo worker

        // Majordomo Management Interface (MMI) as defined in http://rfc.zeromq.org/spec:8
        MajordomoWorker mmiService = new MajordomoWorker(ctx, "mmi.service", rbacRoles);
        mmiService.registerHandler(payload -> {
            final String serviceName = new String(payload[0], StandardCharsets.UTF_8);
            final String returnCode = services.containsKey(serviceName) ? "200" : "400";
            return new byte[][] { returnCode.getBytes(StandardCharsets.UTF_8) };
        });
        addInternalService(mmiService, 1);

        // echo service
        MajordomoWorker echoService = new MajordomoWorker(ctx, "mmi.echo", rbacRoles);
        echoService.registerHandler(input -> input); //  output = input : echo service is complex :-)
        addInternalService(echoService, 1);
    }

    /**
     * Locates the service (creates if necessary).
     *
     * @param serviceName      service name
     * @param serviceNameBytes UTF-8 encoded service name
     */
    protected Service requireService(final String serviceName, final byte[] serviceNameBytes) {
        assert (serviceNameBytes != null);
        return services.computeIfAbsent(serviceName, s -> new Service(serviceName, serviceNameBytes, null, 0));
    }

    /**
     * Finds the worker (creates if necessary).
     */
    protected Worker requireWorker(final Socket socket, final byte[] address, final String addressHex) {
        assert (addressHex != null);
        return workers.computeIfAbsent(addressHex, identity -> {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.atInfo().addArgument(addressHex).log("Majordomo broker registering new worker: '{}'");
            }
            return new Worker(socket, address, addressHex);
        });
    }

    /**
     * Send heartbeats to idle workers if it's time
     */
    protected /*synchronized*/ void sendHeartbeats() {
        // Send heartbeats to idle workers if it's time
        if (System.currentTimeMillis() >= heartbeatAt) {
            for (Worker worker : waiting) {
                MajordomoProtocol.sendWorkerMessage(worker.socket, MdpWorkerCommand.W_HEARTBEAT, worker.address, null);
            }
            heartbeatAt = System.currentTimeMillis() + HEARTBEAT_INTERVAL;
        }
    }

    /**
     * This worker is now waiting for work.
     */
    protected /*synchronized*/ void workerWaiting(Worker worker) {
        // Queue to broker and service waiting lists
        waiting.addLast(worker);
        //TODO: evaluate addLast vs. push (addFirst) - latter should be more beneficial w.r.t. CPU context switches (reuses the same thready/context frequently
        // do not know why original implementation wanted to spread across different workers (load balancing across different machines perhaps?!=)
        //worker.service.waiting.addLast(worker);
        worker.service.waiting.push(worker);
        worker.expiry = System.currentTimeMillis() + HEARTBEAT_EXPIRY;
        dispatch(worker.service);
    }

    /**
     * Main method - create and start new broker.
     *
     * @param args use '-v' for putting worker in verbose mode
     */
    public static void main(String[] args) {
        MajordomoBroker broker = new MajordomoBroker(1, BasicRbacRole.values());
        // broker.setDaemon(true); // use this if running in another app that controls threads
        // Can be called multiple times with different endpoints
        broker.bind("tcp://*:5555");
        broker.bind("tcp://*:5556");

        for (int i = 0; i < 10; i++) {
            // simple internalSock echo
            MajordomoWorker workerSession = new MajordomoWorker(broker.getContext(), "inproc.echo", BasicRbacRole.ADMIN);
            workerSession.registerHandler(input -> input); //  output = input : echo service is complex :-)
            workerSession.start();
        }

        broker.start();
    }

    /**
     * This defines a client service.
     */
    protected static class Client {
        protected final Socket socket; // Socket worker is connected to
        protected final MdpSubProtocol protocol;
        protected final String name; // Service name
        protected final byte[] nameBytes; // Service name as byte array
        protected final String nameHex; // Service name as hex String
        private final Deque<MdpClientMessage> requests = new ArrayDeque<>(); // List of client requests
        protected long expiry = System.currentTimeMillis() + CLIENT_TIMEOUT * 1000L; // Expires at unless heartbeat

        public Client(final Socket socket, final MdpSubProtocol protocol, final String name, final byte[] nameBytes) {
            this.socket = socket;
            this.protocol = protocol;
            this.name = name;
            this.nameBytes = nameBytes == null ? name.getBytes(StandardCharsets.UTF_8) : nameBytes;
            this.nameHex = MajordomoProtocol.strhex(nameBytes);
        }

        public void offerToQueue(final MdpClientMessage msg) {
            expiry = System.currentTimeMillis() + CLIENT_TIMEOUT * 1000L;
            requests.offer(msg);
        }

        public MdpClientMessage pop() {
            return requests.isEmpty() ? null : requests.poll();
        }
    }

    /**
     * This defines a single service.
     */
    protected class Service {
        protected final String name; // Service name
        protected final byte[] nameBytes; // Service name as byte array
        protected final MajordomoWorker mdpWorker;
        protected final boolean isInternal;
        protected final Map<RbacRole<?>, Queue<MdpClientMessage>> requests = new HashMap<>(); // RBAC-based queuing
        protected final Deque<Worker> waiting = new ArrayDeque<>(); // List of waiting workers
        protected final List<Thread> internalWorkers = new ArrayList<>();
        protected final Socket internalDispatchSocket;

        public Service(final String name, final byte[] nameBytes, final MajordomoWorker mdpWorker, final int nInternalThreads) {
            this.name = name;
            this.nameBytes = nameBytes == null ? name.getBytes(StandardCharsets.UTF_8) : nameBytes;
            this.mdpWorker = mdpWorker;
            this.isInternal = mdpWorker != null;
            if (isInternal) {
                this.internalDispatchSocket = ctx.createSocket(SocketType.PUSH);
                this.internalDispatchSocket.setHWM(0);
                this.internalDispatchSocket.bind("inproc://" + mdpWorker.getServiceName() + "push");
                for (int i = 0; i < nInternalThreads; i++) {
                    internalWorkers.add(new InternalWorkerThread(this));
                }
            } else {
                this.internalDispatchSocket = null;
            }
            rbacRoles.forEach(role -> requests.put(role, new ArrayDeque<>()));
            requests.put(BasicRbacRole.NULL, new ArrayDeque<>()); // add default queue
        }

        public boolean requestsPending() {
            return requests.entrySet().stream().anyMatch(map -> !map.getValue().isEmpty());
        }

        /**
         * @return next RBAC prioritised message or 'null' if there aren't any
         */
        protected final MdpClientMessage getNextPrioritisedMessage() {
            for (RbacRole<?> role : rbacRoles) {
                final Queue<MdpClientMessage> queue = requests.get(role); // matched non-empty queue
                if (!queue.isEmpty()) {
                    return queue.poll();
                }
            }
            final Queue<MdpClientMessage> queue = requests.get(BasicRbacRole.NULL); // default queue
            return queue.isEmpty() ? null : queue.poll();
        }

        protected void putPrioritisedMessage(final MdpClientMessage queuedMessage) {
            if (queuedMessage.hasRbackToken()) {
                // find proper RBAC queue
                final RbacToken rbacToken = RbacToken.from(requireNonNull(queuedMessage.getRbacFrame()));
                final Queue<MdpClientMessage> roleBasedQueue = requests.get(rbacToken.getRole());
                if (roleBasedQueue != null) {
                    roleBasedQueue.offer(queuedMessage);
                }
            } else {
                requests.get(BasicRbacRole.NULL).offer(queuedMessage);
            }
        }
    }

    /**
     * This defines one worker, idle or active.
     */
    protected class Worker {
        protected final Socket socket; // Socket worker is connected to
        protected final byte[] address; // Address ID frame to route to
        protected final String addressHex; // Address ID frame of worker expressed as hex-String

        protected final boolean external;
        protected Service service; // Owning service, if known
        protected long expiry = System.currentTimeMillis() + (long) HEARTBEAT_INTERVAL * (long) HEARTBEAT_LIVENESS; // Expires at unless heartbeat

        public Worker(final Socket socket, final byte[] address, final String addressHex) {
            this.socket = socket;
            this.external = true;
            this.address = address;
            this.addressHex = addressHex;
        }
    }

    protected class InternalWorkerThread extends Thread {
        private final Service service;

        public InternalWorkerThread(final Service service) {
            assert service != null && service.name != null && !service.name.isBlank();
            final String serviceName = service.name + "#" + WORKER_COUNTER.getAndIncrement();
            this.setName(MajordomoBroker.class.getSimpleName() + "-" + InternalWorkerThread.class.getSimpleName() + ":" + serviceName);
            this.setDaemon(true);
            this.service = service;
        }

        public void run() {
            try (final Socket sendSocket = ctx.createSocket(SocketType.DEALER); final Socket receiveSocket = ctx.createSocket(SocketType.PULL)) {
                // register worker with broker
                sendSocket.setSndHWM(0);
                sendSocket.setIdentity(service.name.getBytes(StandardCharsets.UTF_8));
                sendSocket.connect("inproc://intService");

                receiveSocket.connect("inproc://" + service.mdpWorker.getServiceName() + "push");
                receiveSocket.setRcvHWM(0);

                // register poller
                final ZMQ.Poller items = ctx.createPoller(1);
                items.register(receiveSocket, ZMQ.Poller.POLLIN);
                while (run.get() && !this.isInterrupted()) {
                    if (items.poll(HEARTBEAT_INTERVAL) == -1) {
                        break; // interrupted
                    }

                    while (run.get()) {
                        final MdpMessage mdpMessage = receiveMdpMessage(receiveSocket, false);
                        if (mdpMessage == null || mdpMessage.isClient) {
                            break;
                        }
                        MdpWorkerMessage msg = (MdpWorkerMessage) mdpMessage;

                        // execute the user-provided call-back function
                        final MdpMessage reply = service.mdpWorker.processRequest(msg, msg.clientSourceID);

                        if (reply != null) {
                            MajordomoProtocol.sendWorkerMessage(sendSocket, MdpWorkerCommand.W_REPLY, INTERNAL_SENDER_ID, msg.clientSourceID, reply.payload);
                        }
                    }
                }
                items.close();
            } catch (ZMQException e) {
                // process should abort
            }
        }
    }
}
