package de.gsi.microservice.concepts.cmwlight;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.zeromq.ZFrame;
import org.zeromq.ZMsg;

/**
 * Test serialisation and deserialisation of cmw protocol messages.
 */
class CmwLightProtocolTest {
    @Test
    void testConnectRequest() throws CmwLightProtocol.RdaLightException {
        final CmwLightMessage subscriptionMsg = CmwLightMessage.connectRequest("testsession",
                1337L,
                "testdev",
                "testprop");
        ZMsg serialised = CmwLightProtocol.serialiseMsg(subscriptionMsg);
        final CmwLightMessage restored = CmwLightProtocol.parseMsg(serialised);
        assertEquals(subscriptionMsg, restored);
    }

    @Test
    void testEventRequest() throws CmwLightProtocol.RdaLightException {
        final CmwLightMessage subscriptionMsg = CmwLightMessage.eventRequest("testsession",
                1337L,
                "testdev",
                "testprop");
        ZMsg serialised = CmwLightProtocol.serialiseMsg(subscriptionMsg);
        final CmwLightMessage restored = CmwLightProtocol.parseMsg(serialised);
        assertEquals(subscriptionMsg, restored);
    }

    @Test
    void testEventReply() throws CmwLightProtocol.RdaLightException {
        final CmwLightMessage subscriptionMsg = CmwLightMessage.eventReply("testsession",
                1337L,
                "testdev",
                "testprop");
        ZMsg serialised = CmwLightProtocol.serialiseMsg(subscriptionMsg);
        final CmwLightMessage restored = CmwLightProtocol.parseMsg(serialised);
        assertEquals(subscriptionMsg, restored);
    }

    @Test
    @Disabled // issues with the empty map in options in the cmw light serialiser
    void testSessionConfirmReply() throws CmwLightProtocol.RdaLightException {
        final CmwLightMessage subscriptionMsg = CmwLightMessage.sessionConfirmReply("testsession",
                1337L,
                "testdev",
                "testprop",
                Map.of(CmwLightProtocol.FieldName.SESSION_BODY_TAG.value(), Collections.<String, Object>emptyMap()));
        ZMsg serialised = CmwLightProtocol.serialiseMsg(subscriptionMsg);
        serialised.stream().forEach(frame -> System.out.println(frame.getString(Charset.defaultCharset())));
        serialised.stream().forEach(frame -> System.out.println(Arrays.toString(frame.getData())));
        final CmwLightMessage restored = CmwLightProtocol.parseMsg(serialised);
        assertEquals(subscriptionMsg, restored);
    }

    @Test
    void testSessionGetReply() throws CmwLightProtocol.RdaLightException {
        final ZFrame data = new ZFrame(new byte[] { 0, 1, 4, 7, 8 });
        final CmwLightMessage subscriptionMsg = CmwLightMessage.getReply("testsession",
                1338L,
                "testdev",
                "testprop",
                data,
                new CmwLightMessage.DataContext("testCycleName", 4242, 2323, null));
        ZMsg serialised = CmwLightProtocol.serialiseMsg(subscriptionMsg);
        final CmwLightMessage restored = CmwLightProtocol.parseMsg(serialised);
        assertEquals(subscriptionMsg, restored);
    }

    @Test
    void testNotificationReply() throws CmwLightProtocol.RdaLightException {
        final ZFrame data = new ZFrame(new byte[] { 0, 1, 4, 7, 8 });
        final CmwLightMessage subscriptionMsg = CmwLightMessage.notificationReply("testsession",
                1337L,
                "testdev",
                "testprop",
                data,
                7,
                new CmwLightMessage.DataContext("testCycleName", 4242, 2323, null),
                CmwLightProtocol.UpdateType.IMMEDIATE_UPDATE);
        ZMsg serialised = CmwLightProtocol.serialiseMsg(subscriptionMsg);
        final CmwLightMessage restored = CmwLightProtocol.parseMsg(serialised);
        assertEquals(subscriptionMsg, restored);
    }

    @Test
    void testExceptionReply() throws CmwLightProtocol.RdaLightException {
        final CmwLightMessage subscriptionMsg = CmwLightMessage.exceptionReply(
                "testsession", 1337L, "testdev", "testprop",
                "test exception message", 314, 981, (byte) 3);
        ZMsg serialised = CmwLightProtocol.serialiseMsg(subscriptionMsg);
        final CmwLightMessage restored = CmwLightProtocol.parseMsg(serialised);
        assertEquals(subscriptionMsg, restored);
    }

    @Test
    void testNotificationExceptionReply() throws CmwLightProtocol.RdaLightException {
        final CmwLightMessage subscriptionMsg = CmwLightMessage.notificationExceptionReply(
                "testsession", 1337L, "testdev", "testprop",
                "test exception message", 314, 981, (byte) 3);
        ZMsg serialised = CmwLightProtocol.serialiseMsg(subscriptionMsg);
        final CmwLightMessage restored = CmwLightProtocol.parseMsg(serialised);
        assertEquals(subscriptionMsg, restored);
    }

    @Test
    void testSubscribeExceptionReply() throws CmwLightProtocol.RdaLightException {
        final CmwLightMessage subscriptionMsg = CmwLightMessage.subscribeExceptionReply(
                "testsession", 1337L, "testdev", "testprop",
                "test exception message", 314, 981, (byte) 3);
        ZMsg serialised = CmwLightProtocol.serialiseMsg(subscriptionMsg);
        final CmwLightMessage restored = CmwLightProtocol.parseMsg(serialised);
        assertEquals(subscriptionMsg, restored);
    }

    @Test
    void testSubscribeRequest() throws CmwLightProtocol.RdaLightException {
        final CmwLightMessage subscriptionMsg = CmwLightMessage.subscribeRequest(
                "testsession", 1337L, "testdev", "testprop",
                Map.of("b", 1337L),
                new CmwLightMessage.RequestContext("testselector", Map.of("testfilter", 5L), null),
                CmwLightProtocol.UpdateType.NORMAL);
        ZMsg serialised = CmwLightProtocol.serialiseMsg(subscriptionMsg);
        // System.out.println(serialised);
        final CmwLightMessage restored = CmwLightProtocol.parseMsg(serialised);
        assertEquals(subscriptionMsg, restored);
    }

    @Test
    void testUnsubscribeRequest() throws CmwLightProtocol.RdaLightException {
        final CmwLightMessage subscriptionMsg = CmwLightMessage.unsubscribeRequest("testsession",
                1337L,
                "testdev",
                "testprop",
                Map.of("b", 1337L),
                CmwLightProtocol.UpdateType.NORMAL);
        ZMsg serialised = CmwLightProtocol.serialiseMsg(subscriptionMsg);
        final CmwLightMessage restored = CmwLightProtocol.parseMsg(serialised);
        assertEquals(subscriptionMsg, restored);
    }

    @Test
    void testGetRequest() throws CmwLightProtocol.RdaLightException {
        final CmwLightMessage subscriptionMsg = CmwLightMessage.getRequest("testsession",
                1337L,
                "testdev",
                "testprop",
                new CmwLightMessage.RequestContext("testselector", Map.of("testfilter", 5L), null));
        ZMsg serialised = CmwLightProtocol.serialiseMsg(subscriptionMsg);
        final CmwLightMessage restored = CmwLightProtocol.parseMsg(serialised);
        assertEquals(subscriptionMsg, restored);
    }

    @Test
    void testSetRequest() throws CmwLightProtocol.RdaLightException {
        final ZFrame data = new ZFrame(new byte[] { 0, 1, 4, 7, 8 });
        final CmwLightMessage subscriptionMsg = CmwLightMessage.setRequest("testsession",
                1337L,
                "testdev",
                "testprop",
                data,
                new CmwLightMessage.RequestContext("testselector", Map.of("testfilter", 5L), null));
        ZMsg serialised = CmwLightProtocol.serialiseMsg(subscriptionMsg);
        final CmwLightMessage restored = CmwLightProtocol.parseMsg(serialised);
        assertEquals(subscriptionMsg, restored);
    }

    @Test
    void testHbServerMsg() throws CmwLightProtocol.RdaLightException {
        final CmwLightMessage msg = CmwLightMessage.SERVER_HB;
        ZMsg serialised = CmwLightProtocol.serialiseMsg(msg);
        final CmwLightMessage restored = CmwLightProtocol.parseMsg(serialised);
        assertEquals(msg, restored);
    }

    @Test
    void testHbClientMsg() throws CmwLightProtocol.RdaLightException {
        final CmwLightMessage msg = CmwLightMessage.CLIENT_HB;
        ZMsg serialised = CmwLightProtocol.serialiseMsg(msg);
        final CmwLightMessage restored = CmwLightProtocol.parseMsg(serialised);
        assertEquals(msg, restored);
    }

    @Test
    void testConnectMsg() throws CmwLightProtocol.RdaLightException {
        final CmwLightMessage msg = CmwLightMessage.connect("1.3.7");
        ZMsg serialised = CmwLightProtocol.serialiseMsg(msg);
        final CmwLightMessage restored = CmwLightProtocol.parseMsg(serialised);
        assertEquals(msg, restored);
    }

    @Test
    void testConnectAckMsg() throws CmwLightProtocol.RdaLightException {
        final CmwLightMessage msg = CmwLightMessage.connectAck("1.3.7");
        ZMsg serialised = CmwLightProtocol.serialiseMsg(msg);
        final CmwLightMessage restored = CmwLightProtocol.parseMsg(serialised);
        assertEquals(msg, restored);
    }
}
