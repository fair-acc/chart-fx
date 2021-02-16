package de.gsi.microservice.concepts;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import org.zeromq.*;
import org.zeromq.ZMQ.Socket;

/**
 * Quick Router-Dealer Round-trip demonstrator.
 * Broker, Worker and Client are mocked as separate threads.
 *
 * Example output:
 * Setting up test
 * Synchronous round-trip test (TCP)       :        3838 calls/second
 * Synchronous round-trip test (InProc)    :       12224 calls/second
 * Asynchronous round-trip test (TCP)      :       33444 calls/second
 * Asynchronous round-trip test (InProc)   :       35587 calls/second
 * Subscription (SUB) test                 :      632911 calls/second
 * Subscription (DEALER) test (TCP)        :       43821 calls/second
 * Subscription (DEALER) test (InProc)     :       49900 calls/second
 * Subscription (direct DEALER) test       :     1351351 calls/second
 * finished tests
 *
 * N.B. for >200000 calls/second the code seems to depend largely on the broker/parameters
 * (ie. JIT, whether services are identified by single characters etc.)
 */
class RoundTripAndNotifyEvaluation {
    // private static final String SUB_TOPIC = "x";
    private static final String SUB_TOPIC = "<domain>/<property>?<filter>#<ctx> - a very long topic to test the dependence of pub/sub pairs on topic lengths";
    private static final byte[] SUB_DATA = "D".getBytes(StandardCharsets.UTF_8); // custom minimal data
    private static final byte[] CLIENT_ID = "C".getBytes(StandardCharsets.UTF_8); // client name
    private static final byte[] WORKER_ID = "W".getBytes(StandardCharsets.UTF_8); // worker-service name
    private static final byte[] PUBLISH_ID = "P".getBytes(StandardCharsets.UTF_8); // publish-service name
    private static final byte[] SUBSCRIBER_ID = "S".getBytes(StandardCharsets.UTF_8); // subscriber name
    private static final AtomicBoolean run = new AtomicBoolean(true);
    private static boolean VERBOSE_PRINTOUT = false;
    private static int SAMPLE_SIZE = 10_000;
    private static int SAMPLE_SIZE_PUB = 100_000;

    public static void main(String[] args) {
        if (args.length == 1) {
            SAMPLE_SIZE = Integer.parseInt(args[0]);
            SAMPLE_SIZE_PUB = 10 * SAMPLE_SIZE;
        }
        VERBOSE_PRINTOUT = true;
        Thread brokerThread = new Thread(new Broker());
        Thread workerThread = new Thread(new Worker());
        Thread publishThread = new Thread(new PublishWorker());
        Thread pubDealerThread = new Thread(new PublishDealerWorker());
        Thread directDealerThread = new Thread(new PublishDirectDealerWorker());

        brokerThread.start();
        workerThread.start();
        publishThread.start();
        pubDealerThread.start();
        directDealerThread.start();

        Thread clientThread = new Thread(new Client());
        clientThread.start();

        try {
            clientThread.join();
            run.set(false);
            workerThread.interrupt();
            brokerThread.interrupt();
            publishThread.interrupt();
            pubDealerThread.interrupt();
            directDealerThread.interrupt();

            // wait for threads to finish
            workerThread.join();
            publishThread.join();
            pubDealerThread.join();
            directDealerThread.join();
            brokerThread.join();
        } catch (InterruptedException e) {
            // finishes tests
            assert false : "should not reach here if properly executed";
        }

        if (VERBOSE_PRINTOUT) {
            System.out.println("finished tests");
        }
    }

    private static void measure(final String topic, final int nExec, final Runnable... runnable) {
        final long start = System.currentTimeMillis();

        for (Runnable run : runnable) {
            for (int i = 0; i < nExec; i++) {
                run.run();
            }
        }

        final long stop = System.currentTimeMillis();
        if (VERBOSE_PRINTOUT) {
            System.out.printf("%-40s:  %10d calls/second\n", topic, (1000L * nExec) / (stop - start));
        }
    }

    static class Broker implements Runnable {
        private static final int TIMEOUT = 1000;
        @Override
        public void run() { // NOPMD single-loop broker ... simplifies reading
            try (ZContext ctx = new ZContext()) {
                Socket tcpFrontend = ctx.createSocket(SocketType.ROUTER);
                Socket tcpBackend = ctx.createSocket(SocketType.ROUTER);
                Socket inprocBackend = ctx.createSocket(SocketType.ROUTER);
                tcpFrontend.setHWM(0);
                tcpBackend.setHWM(0);
                inprocBackend.setHWM(0);
                tcpFrontend.bind("tcp://*:5555");
                tcpBackend.bind("tcp://*:5556");
                inprocBackend.bind("inproc://broker");

                Thread internalWorkerThread = new Thread(new InternalWorker(ctx));
                internalWorkerThread.setDaemon(true);
                internalWorkerThread.start();
                Thread internalPublishDealerWorkerThread = new Thread(new InternalPublishDealerWorker(ctx));
                internalPublishDealerWorkerThread.setDaemon(true);
                internalPublishDealerWorkerThread.start();

                while (run.get() && !Thread.currentThread().isInterrupted()) {
                    // create poller
                    ZMQ.Poller items = ctx.createPoller(3);
                    items.register(tcpFrontend, ZMQ.Poller.POLLIN);
                    items.register(tcpBackend, ZMQ.Poller.POLLIN);
                    items.register(inprocBackend, ZMQ.Poller.POLLIN);

                    if (items.poll(TIMEOUT) == -1)
                        break; // Interrupted

                    if (items.pollin(0)) {
                        ZMsg msg = ZMsg.recvMsg(tcpFrontend);
                        if (msg == null)
                            break; // Interrupted
                        ZFrame address = msg.pop();
                        ZFrame internal = msg.pop();
                        if (address.getData()[0] == CLIENT_ID[0]) {
                            if ('E' == internal.getData()[0]) {
                                msg.addFirst(new ZFrame(WORKER_ID));
                                msg.send(tcpBackend);
                            } else if ('I' == internal.getData()[0]) {
                                msg.addFirst(new ZFrame(WORKER_ID));
                                msg.send(inprocBackend);
                            }
                        } else {
                            if ('E' == internal.getData()[0]) {
                                msg.addFirst(new ZFrame(PUBLISH_ID));
                                msg.send(tcpBackend);
                            } else if ('I' == internal.getData()[0]) {
                                msg.addFirst(new ZFrame(PUBLISH_ID));
                                msg.send(inprocBackend);
                            }
                        }
                        address.destroy();
                    }

                    if (items.pollin(1)) {
                        ZMsg msg = ZMsg.recvMsg(tcpBackend);
                        if (msg == null) {
                            break; // Interrupted
                        }
                        ZFrame address = msg.pop();

                        if (address.getData()[0] == WORKER_ID[0]) {
                            msg.addFirst(new ZFrame(CLIENT_ID));
                        } else {
                            msg.addFirst(new ZFrame(SUBSCRIBER_ID));
                        }
                        msg.send(tcpFrontend);
                        address.destroy();
                    }

                    if (items.pollin(2)) {
                        ZMsg msg = ZMsg.recvMsg(inprocBackend);
                        if (msg == null)
                            break; // Interrupted
                        ZFrame address = msg.pop();

                        if (address.getData()[0] == WORKER_ID[0]) {
                            msg.addFirst(new ZFrame(CLIENT_ID));
                        } else {
                            msg.addFirst(new ZFrame(SUBSCRIBER_ID));
                        }
                        address.destroy();
                        msg.send(tcpFrontend);
                    }

                    items.close();
                }

                internalWorkerThread.interrupt();
                internalPublishDealerWorkerThread.interrupt();
                if (!internalWorkerThread.isInterrupted()) {
                    internalWorkerThread.join();
                }
                if (!internalPublishDealerWorkerThread.isInterrupted()) {
                    internalPublishDealerWorkerThread.join();
                }
            } catch (InterruptedException | IllegalStateException e) {
                // terminated broker via interrupt
            }
        }

        static class InternalWorker implements Runnable {
            private final ZContext ctx;
            public InternalWorker(ZContext ctx) {
                this.ctx = ctx;
            }

            @Override
            public void run() {
                try {
                    Socket worker = ctx.createSocket(SocketType.DEALER);
                    worker.setHWM(0);
                    worker.setIdentity(WORKER_ID);
                    worker.connect("inproc://broker");
                    while (run.get() && !Thread.currentThread().isInterrupted()) {
                        ZMsg msg = ZMsg.recvMsg(worker);
                        msg.send(worker);
                    }
                } catch (ZMQException e) {
                    // terminate internal worker
                }
            }
        }

        static class InternalPublishDealerWorker implements Runnable {
            private final ZContext ctx;
            public InternalPublishDealerWorker(ZContext ctx) {
                this.ctx = ctx;
            }

            @Override
            public void run() {
                try {
                    Socket worker = ctx.createSocket(SocketType.DEALER);
                    worker.setHWM(0);
                    worker.setIdentity(PUBLISH_ID);
                    worker.connect("inproc://broker");
                    while (run.get() && !Thread.currentThread().isInterrupted()) {
                        ZMsg msg = ZMsg.recvMsg(worker);
                        if ("start".equals(msg.getFirst().getString(ZMQ.CHARSET))) {
                            // System.err.println("dealer (indirect): start pushing");
                            for (int requests = 0; requests < SAMPLE_SIZE_PUB; requests++) {
                                worker.send(SUB_TOPIC, ZMQ.SNDMORE);
                                worker.send(SUB_DATA);
                            }
                        }
                    }
                } catch (ZMQException | IllegalStateException e) {
                    // terminate internal publish worker
                }
            }
        }
    }

    static class Worker implements Runnable {
        @Override
        public void run() {
            try (ZContext ctx = new ZContext()) {
                Socket worker = ctx.createSocket(SocketType.DEALER);
                worker.setHWM(0);
                worker.setIdentity(WORKER_ID);
                worker.connect("tcp://localhost:5556");
                while (run.get() && !Thread.currentThread().isInterrupted()) {
                    ZMsg msg = ZMsg.recvMsg(worker);
                    msg.send(worker);
                }
            } catch (ZMQException e) {
                // terminate worker
            }
        }
    }

    static class PublishWorker implements Runnable {
        @Override
        public void run() {
            try (ZContext ctx = new ZContext()) {
                Socket worker = ctx.createSocket(SocketType.PUB);
                worker.setHWM(0);
                worker.bind("tcp://localhost:5557");
                // System.err.println("PublishWorker: start publishing");
                while (run.get() && !Thread.currentThread().isInterrupted()) {
                    worker.send(SUB_TOPIC, ZMQ.SNDMORE);
                    worker.send(SUB_DATA);
                }
            } catch (ZMQException | IllegalStateException e) {
                // terminate pub-Dealer worker
            }
        }
    }

    static class PublishDealerWorker implements Runnable {
        @Override
        public void run() {
            try (ZContext ctx = new ZContext()) {
                Socket worker = ctx.createSocket(SocketType.DEALER);
                worker.setHWM(0);
                worker.setIdentity(PUBLISH_ID);
                //worker.bind("tcp://localhost:5558");
                worker.connect("tcp://localhost:5556");
                while (run.get() && !Thread.currentThread().isInterrupted()) {
                    ZMsg msg = ZMsg.recvMsg(worker);
                    if ("start".equals(msg.getFirst().getString(ZMQ.CHARSET))) {
                        // System.err.println("dealer (indirect): start pushing");
                        for (int requests = 0; requests < SAMPLE_SIZE_PUB; requests++) {
                            worker.send(SUB_TOPIC, ZMQ.SNDMORE);
                            worker.send(SUB_DATA);
                        }
                    }
                }
            } catch (ZMQException | IllegalStateException e) {
                // terminate publish worker
            }
        }
    }

    static class PublishDirectDealerWorker implements Runnable {
        @Override
        public void run() {
            try (ZContext ctx = new ZContext()) {
                Socket worker = ctx.createSocket(SocketType.DEALER);
                worker.setHWM(0);
                worker.setIdentity(PUBLISH_ID);
                worker.bind("tcp://localhost:5558");
                while (run.get() && !Thread.currentThread().isInterrupted()) {
                    ZMsg msg = ZMsg.recvMsg(worker);
                    if ("start".equals(msg.getFirst().getString(ZMQ.CHARSET))) {
                        // System.err.println("dealer (direct): start pushing");
                        for (int requests = 0; requests < SAMPLE_SIZE_PUB; requests++) {
                            worker.send(SUB_TOPIC, ZMQ.SNDMORE);
                            worker.send(SUB_DATA);
                        }
                    }
                }
            } catch (ZMQException | IllegalStateException e) {
                // terminate publish worker
            }
        }
    }

    static class Client implements Runnable {
        @Override
        public void run() {
            try (ZContext ctx = new ZContext()) {
                Socket client = ctx.createSocket(SocketType.DEALER);
                client.setHWM(0);
                client.setIdentity(CLIENT_ID);
                client.connect("tcp://localhost:5555");

                Socket subClient = ctx.createSocket(SocketType.SUB);
                subClient.setHWM(0);
                subClient.connect("tcp://localhost:5557");

                if (VERBOSE_PRINTOUT) {
                    System.out.println("Setting up test");
                }
                // check and wait until broker is up and running
                ZMsg.newStringMsg("E").addString("hello").send(client);
                ZMsg.recvMsg(client).destroy();

                measure("Synchronous round-trip test (TCP)", SAMPLE_SIZE, () -> {
                    ZMsg req = new ZMsg();
                    req.addString("E");
                    req.addString("hello");
                    req.send(client);
                    ZMsg.recvMsg(client).destroy();
                });

                measure("Synchronous round-trip test (InProc)", SAMPLE_SIZE, () -> {
                    ZMsg req = new ZMsg();
                    req.addString("I");
                    req.addString("hello");
                    req.send(client);
                    ZMsg.recvMsg(client).destroy();
                });

                measure("Asynchronous round-trip test (TCP)", SAMPLE_SIZE, () -> {
                    // send messages
                    ZMsg req = new ZMsg();
                    req.addString("E");
                    req.addString("hello");
                    req.send(client); }, () -> {
                    // receive messages
                    ZMsg.recvMsg(client).destroy(); });

                measure("Asynchronous round-trip test (InProc)", SAMPLE_SIZE, () -> {
                    // send messages
                    ZMsg req = new ZMsg();
                    req.addString("I");
                    req.addString("hello");
                    req.send(client); }, () -> {
                    // receive messages
                    ZMsg.recvMsg(client).destroy(); });

                subClient.subscribe(SUB_TOPIC.getBytes(ZMQ.CHARSET));
                // first loop to empty potential queues/HWM
                for (int requests = 0; requests < SAMPLE_SIZE_PUB; requests++) {
                    ZMsg req = ZMsg.recvMsg(subClient);
                    req.destroy();
                }
                // start actual subscription loop
                measure("Subscription (SUB) test", SAMPLE_SIZE_PUB, () -> {
                    ZMsg req = ZMsg.recvMsg(subClient);
                    req.destroy();
                });
                subClient.unsubscribe(SUB_TOPIC.getBytes(ZMQ.CHARSET));

                client.disconnect("tcp://localhost:5555");
                client.setIdentity(SUBSCRIBER_ID);
                client.connect("tcp://localhost:5555");
                ZMsg.newStringMsg("start").addFirst("E").send(client);
                measure("Subscription (DEALER) test (TCP)", SAMPLE_SIZE_PUB, () -> {
                    ZMsg req = ZMsg.recvMsg(client);
                    req.destroy();
                });

                ZMsg.newStringMsg("start").addFirst("I").send(client);
                measure("Subscription (DEALER) test (InProc)", SAMPLE_SIZE_PUB, () -> {
                    ZMsg req = ZMsg.recvMsg(client);
                    req.destroy();
                });

                client.disconnect("tcp://localhost:5555");
                client.connect("tcp://localhost:5558");
                ZMsg.newStringMsg("start").send(client);
                measure("Subscription (direct DEALER) test", SAMPLE_SIZE_PUB, () -> {
                    ZMsg req = ZMsg.recvMsg(client);
                    req.destroy();
                });

            } catch (ZMQException e) {
                if (VERBOSE_PRINTOUT) {
                    System.out.println("terminate client");
                }
            }
        }
    }
}
