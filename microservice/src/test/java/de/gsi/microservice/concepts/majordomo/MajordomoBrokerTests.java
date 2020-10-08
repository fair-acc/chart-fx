package de.gsi.microservice.concepts.majordomo;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static de.gsi.microservice.concepts.majordomo.MajordomoProtocol.MdpClientCommand;
import static de.gsi.microservice.concepts.majordomo.MajordomoProtocol.MdpClientMessage;
import static de.gsi.microservice.concepts.majordomo.MajordomoProtocol.MdpMessage;
import static de.gsi.microservice.concepts.majordomo.MajordomoProtocol.MdpSubProtocol;
import static de.gsi.microservice.concepts.majordomo.MajordomoProtocol.receiveMdpMessage;
import static de.gsi.microservice.concepts.majordomo.MajordomoProtocol.sendClientMessage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.zeromq.SocketType;
import org.zeromq.Utils;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import de.gsi.microservice.rbac.BasicRbacRole;
import de.gsi.microservice.rbac.RbacToken;

public class MajordomoBrokerTests {
    private static final byte[] DEFAULT_RBAC_TOKEN = new RbacToken(BasicRbacRole.ADMIN, "HASHCODE").getBytes();
    private static final byte[] DEFAULT_MMI_SERVICE = "mmi.service".getBytes(StandardCharsets.UTF_8);
    private static final byte[] DEFAULT_ECHO_SERVICE = "mmi.echo".getBytes(StandardCharsets.UTF_8);
    private static final String DEFAULT_REQUEST_MESSAGE = "Hello World!";
    private static final byte[] DEFAULT_REQUEST_MESSAGE_BYTES = DEFAULT_REQUEST_MESSAGE.getBytes(StandardCharsets.UTF_8);

    @Test
    public void basicLowLevelRequestReplyTest() throws InterruptedException, IOException {
        MajordomoBroker broker = new MajordomoBroker(1, BasicRbacRole.values());
        // broker.setDaemon(true); // use this if running in another app that controls threads
        final int openPort = Utils.findOpenPort();
        broker.bind("tcp://*:" + openPort);
        assertFalse(broker.isRunning(), "broker not running");
        broker.start();
        assertTrue(broker.isRunning(), "broker running");
        // test interfaces
        assertNotNull(broker.getContext());
        assertNotNull(broker.getInternalRouterSocket());
        assertNotNull(broker.getServices());
        assertEquals(2, broker.getServices().size());
        assertDoesNotThrow(() -> broker.addInternalService(new MajordomoWorker(broker.getContext(), "demoService"), 10));
        assertEquals(3, broker.getServices().size());
        assertDoesNotThrow(() -> broker.removeService("demoService"));
        assertEquals(2, broker.getServices().size());

        // wait until all services are initialised
        Thread.sleep(200);

        final ZMQ.Socket clientSocket = broker.getContext().createSocket(SocketType.DEALER);
        clientSocket.setIdentity("demoClient".getBytes(StandardCharsets.UTF_8));
        clientSocket.connect("tcp://localhost:" + openPort);

        // wait until client is connected
        Thread.sleep(200);

        sendClientMessage(clientSocket, MdpClientCommand.C_UNKNOWN, null, DEFAULT_ECHO_SERVICE, DEFAULT_REQUEST_MESSAGE_BYTES);

        final MdpMessage reply = receiveMdpMessage(clientSocket);
        assertNotNull(reply.toString());
        assertNotNull(reply, "reply message w/o RBAC token not being null");
        assertTrue(reply instanceof MdpClientMessage);
        MdpClientMessage clientMessage = (MdpClientMessage) reply;
        assertNull(clientMessage.senderID); // default dealer socket does not export sender ID (only ROUTER and/or enabled sockets)
        assertEquals(MdpSubProtocol.C_CLIENT, clientMessage.protocol, "equal protocol");
        assertEquals(MdpClientCommand.C_UNKNOWN, clientMessage.command, "matching command");
        assertArrayEquals(DEFAULT_ECHO_SERVICE, clientMessage.serviceNameBytes, "equal service name");
        assertNotNull(clientMessage.payload, "user-data not being null");
        assertArrayEquals(DEFAULT_REQUEST_MESSAGE_BYTES, clientMessage.payload[0], "equal data");
        assertFalse(clientMessage.hasRbackToken());
        assertNull(clientMessage.getRbacFrame());

        broker.stopBroker();
    }

    @Test
    public void basicSynchronousRequestReplyTest() throws InterruptedException, IOException {
        MajordomoBroker broker = new MajordomoBroker(1, BasicRbacRole.values());
        // broker.setDaemon(true); // use this if running in another app that controls threads
        final int openPort = Utils.findOpenPort();
        broker.bind("tcp://*:" + openPort);
        broker.start();
        assertEquals(2, broker.getServices().size());

        // add external (albeit inproc) Majordomo worker to the broker
        MajordomoWorker internal = new MajordomoWorker(broker.getContext(), "inproc.echo", BasicRbacRole.ADMIN);
        internal.registerHandler(input -> input); //  output = input : echo service is complex :-)
        internal.start();

        // add external Majordomo worker to the broker
        MajordomoWorker external = new MajordomoWorker(broker.getContext(), "ext.echo", BasicRbacRole.ADMIN);
        external.registerHandler(input -> input); //  output = input : echo service is complex :-)
        external.start();

        // add external (albeit inproc) Majordomo worker to the broker
        MajordomoWorker exceptionService = new MajordomoWorker(broker.getContext(), "inproc.exception", BasicRbacRole.ADMIN);
        exceptionService.registerHandler(input -> { throw new IllegalAccessError("autsch"); }); //  allways throw an exception
        exceptionService.start();

        // wait until all services are initialised
        Thread.sleep(200);
        assertEquals(5, broker.getServices().size());

        // using simple synchronous client
        MajordomoClientV1 clientSession = new MajordomoClientV1("tcp://localhost:" + openPort, "customClientName");
        assertEquals(3, clientSession.getRetries());
        assertDoesNotThrow(() -> clientSession.setRetries(4));
        assertEquals(4, clientSession.getRetries());
        assertEquals(2500, clientSession.getTimeout());
        assertDoesNotThrow(() -> clientSession.setTimeout(2000));
        assertEquals(2000, clientSession.getTimeout());
        assertNotNull(clientSession.getUniqueID());

        {
            final byte[] serviceBytes = "mmi.echo".getBytes(StandardCharsets.UTF_8);
            final ZMsg replyWithoutRbac = clientSession.send(serviceBytes, DEFAULT_REQUEST_MESSAGE_BYTES); // w/o RBAC
            assertNotNull(replyWithoutRbac, "reply message w/o RBAC token not being null");
            assertNotNull(replyWithoutRbac.peekLast(), "user-data not being null");
            assertArrayEquals(DEFAULT_REQUEST_MESSAGE_BYTES, replyWithoutRbac.pollLast().getData(), "equal data");
        }

        {
            final byte[] serviceBytes = "inproc.echo".getBytes(StandardCharsets.UTF_8);
            final ZMsg replyWithoutRbac = clientSession.send(serviceBytes, DEFAULT_REQUEST_MESSAGE_BYTES); // w/o RBAC
            assertNotNull(replyWithoutRbac, "reply message w/o RBAC token not being null");
            assertNotNull(replyWithoutRbac.peekLast(), "user-data not being null");
            assertArrayEquals(DEFAULT_REQUEST_MESSAGE_BYTES, replyWithoutRbac.pollLast().getData(), "equal data");
        }

        {
            final byte[] serviceBytes = "ext.echo".getBytes(StandardCharsets.UTF_8);
            final ZMsg replyWithoutRbac = clientSession.send(serviceBytes, DEFAULT_REQUEST_MESSAGE_BYTES); // w/o RBAC
            assertNotNull(replyWithoutRbac, "reply message w/o RBAC token not being null");
            assertNotNull(replyWithoutRbac.peekLast(), "user-data not being null");
            assertArrayEquals(DEFAULT_REQUEST_MESSAGE_BYTES, replyWithoutRbac.pollLast().getData(), "equal data");
        }

        {
            final byte[] serviceBytes = "inproc.exception".getBytes(StandardCharsets.UTF_8);
            final ZMsg replyWithoutRbac = clientSession.send(serviceBytes, DEFAULT_REQUEST_MESSAGE_BYTES); // w/o RBAC
            assertNotNull(replyWithoutRbac, "reply message w/o RBAC token not being null");
            assertNotNull(replyWithoutRbac.peekLast(), "user-data not being null");
            // future: check exception type
        }

        {
            final byte[] serviceBytes = "mmi.echo".getBytes(StandardCharsets.UTF_8);
            final ZMsg replyWithRbac = clientSession.send(serviceBytes, DEFAULT_REQUEST_MESSAGE_BYTES, DEFAULT_RBAC_TOKEN); // with RBAC
            assertNotNull(replyWithRbac, "reply message with RBAC token not being null");
            assertNotNull(replyWithRbac.peekLast(), "RBAC token not being null");
            assertArrayEquals(DEFAULT_RBAC_TOKEN, replyWithRbac.pollLast().getData(), "equal RBAC token");
            assertNotNull(replyWithRbac.peekLast(), "user-data not being null");
            assertArrayEquals(DEFAULT_REQUEST_MESSAGE_BYTES, replyWithRbac.pollLast().getData(), "equal data");
        }

        internal.stopWorker();
        external.stopWorker();
        exceptionService.stopWorker();
        broker.stopBroker();
    }

    @Test
    public void basicMmiTests() throws IOException {
        MajordomoBroker broker = new MajordomoBroker(1, BasicRbacRole.values());
        // broker.setDaemon(true); // use this if running in another app that controls threads
        final int openPort = Utils.findOpenPort();
        broker.bind("tcp://*:" + openPort);
        broker.start();

        // using simple synchronous client
        MajordomoClientV1 clientSession = new MajordomoClientV1("tcp://localhost:" + openPort, "customClientName");

        {
            final ZMsg replyWithoutRbac = clientSession.send("mmi.echo", DEFAULT_REQUEST_MESSAGE_BYTES); // w/o RBAC
            assertNotNull(replyWithoutRbac, "reply message w/o RBAC token not being null");
            assertNotNull(replyWithoutRbac.peekLast(), "user-data not being null");
            assertArrayEquals(DEFAULT_REQUEST_MESSAGE_BYTES, replyWithoutRbac.pollLast().getData(), "MMI echo service request");
        }

        {
            final ZMsg replyWithoutRbac = clientSession.send(DEFAULT_MMI_SERVICE, DEFAULT_MMI_SERVICE); // w/o RBAC
            assertNotNull(replyWithoutRbac, "reply message w/o RBAC token not being null");
            assertNotNull(replyWithoutRbac.peekLast(), "user-data not being null");
            assertEquals("200", new String(replyWithoutRbac.pollLast().getData(), StandardCharsets.UTF_8), "known MMI service request");
        }

        {
            final ZMsg replyWithoutRbac = clientSession.send(DEFAULT_MMI_SERVICE, DEFAULT_ECHO_SERVICE); // w/o RBAC
            assertNotNull(replyWithoutRbac, "reply message w/o RBAC token not being null");
            assertNotNull(replyWithoutRbac.peekLast(), "user-data not being null");
            assertEquals("200", new String(replyWithoutRbac.pollLast().getData(), StandardCharsets.UTF_8), "known MMI service request");
        }

        {
            // MMI service request: service should not exist
            final ZMsg replyWithoutRbac = clientSession.send(DEFAULT_MMI_SERVICE, DEFAULT_REQUEST_MESSAGE_BYTES); // w/o RBAC
            assertNotNull(replyWithoutRbac, "reply message w/o RBAC token not being null");
            assertNotNull(replyWithoutRbac.peekLast(), "user-data not being null");
            assertEquals("400", new String(replyWithoutRbac.pollLast().getData(), StandardCharsets.UTF_8), "unknown MMI service request");
        }

        {
            // unknown service name
            final ZMsg replyWithoutRbac = clientSession.send("unknownService".getBytes(StandardCharsets.UTF_8), DEFAULT_REQUEST_MESSAGE_BYTES); // w/o RBAC
            assertNotNull(replyWithoutRbac, "reply message w/o RBAC token not being null");
            assertNotNull(replyWithoutRbac.peekLast(), "user-data not being null");
            assertEquals("501", new String(replyWithoutRbac.pollLast().getData()), "unknown service");
        }

        broker.stopBroker();
    }

    @Test
    public void basicASynchronousRequestReplyTest() throws IOException {
        MajordomoBroker broker = new MajordomoBroker(1, BasicRbacRole.values());
        // broker.setDaemon(true); // use this if running in another app that controls threads
        final int openPort = Utils.findOpenPort();
        broker.bind("tcp://*:" + openPort);
        broker.start();

        final AtomicInteger counter = new AtomicInteger(0);
        new Thread(() -> {
            // using simple synchronous client
            MajordomoClientV2 clientSession = new MajordomoClientV2("tcp://localhost:" + openPort);
            assertEquals(2500, clientSession.getTimeout());
            assertDoesNotThrow(() -> clientSession.setTimeout(2000));
            assertEquals(2000, clientSession.getTimeout());

            // send bursts of 10 messages
            for (int i = 0; i < 5; i++) {
                clientSession.send("mmi.echo", DEFAULT_REQUEST_MESSAGE_BYTES);
                clientSession.send(DEFAULT_ECHO_SERVICE, DEFAULT_REQUEST_MESSAGE_BYTES);
            }

            // send bursts of 10 messages
            for (int i = 0; i < 10; i++) {
                final ZMsg reply = clientSession.recv();
                assertNotNull(reply, "reply message w/o RBAC token not being null");
                assertNotNull(reply.peekLast(), "user-data not being null");
                assertArrayEquals(DEFAULT_REQUEST_MESSAGE_BYTES, reply.getLast().getData());
                counter.getAndIncrement();
            }
        }).start();

        await().alias("wait for reply messages").atMost(1, TimeUnit.SECONDS).until(counter::get, equalTo(10));
        assertEquals(10, counter.get(), "received expected number of replies");

        broker.stopBroker();
    }
}
