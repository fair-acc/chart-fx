package de.gsi.microservice.concepts.cmwlight;

import java.util.Map;
import java.util.Objects;

import org.zeromq.ZFrame;

/**
 * Data representation for all Messages exchanged between CMW client and server
 */
public class CmwLightMessage {
    // general fields
    public CmwLightProtocol.MessageType messageType;

    // Connection Req/Ack
    public String version;

    // header data
    public CmwLightProtocol.RequestType requestType;
    public long id;
    public String deviceName;
    public CmwLightProtocol.UpdateType updateType;
    public String sessionId;
    public String propertyName;
    public Map<String, Object> options;
    public Map<String, Object> data;

    // additional data
    public ZFrame bodyData;
    public ExceptionMessage exceptionMessage;
    public RequestContext requestContext;
    public DataContext dataContext;

    // Subscription Update
    public long notificationId;

    // subscription established
    public long sourceId;
    public Map<String, Object> sessionBody;

    // static instances for low level message types
    public static final CmwLightMessage SERVER_HB = new CmwLightMessage(CmwLightProtocol.MessageType.SERVER_HB);
    public static final CmwLightMessage CLIENT_HB = new CmwLightMessage(CmwLightProtocol.MessageType.CLIENT_HB);
    // static functions to get certain message types
    public static CmwLightMessage connectAck(final String version) {
        final CmwLightMessage msg = new CmwLightMessage(CmwLightProtocol.MessageType.SERVER_CONNECT_ACK);
        msg.version = version;
        return msg;
    }

    public static CmwLightMessage connect(final String version) {
        final CmwLightMessage msg = new CmwLightMessage(CmwLightProtocol.MessageType.CLIENT_CONNECT);
        msg.version = version;
        return msg;
    }
    public static CmwLightMessage subscribeRequest(String sessionId, long id, String device, String property, final Map<String, Object> options, RequestContext requestContext, CmwLightProtocol.UpdateType updateType) {
        final CmwLightMessage msg = new CmwLightMessage(CmwLightProtocol.MessageType.CLIENT_REQ);
        msg.requestType = CmwLightProtocol.RequestType.SUBSCRIBE;
        msg.id = id;
        msg.options = options;
        msg.sessionId = sessionId;
        msg.deviceName = device;
        msg.propertyName = property;
        msg.requestContext = requestContext;
        msg.updateType = updateType;
        return msg;
    }
    public static CmwLightMessage unsubscribeRequest(String sessionId, long id, String device, String property, final Map<String, Object> options, CmwLightProtocol.UpdateType updateType) {
        final CmwLightMessage msg = new CmwLightMessage(CmwLightProtocol.MessageType.CLIENT_REQ);
        msg.requestType = CmwLightProtocol.RequestType.UNSUBSCRIBE;
        msg.id = id;
        msg.options = options;
        msg.sessionId = sessionId;
        msg.deviceName = device;
        msg.propertyName = property;
        msg.updateType = updateType;
        return msg;
    }
    public static CmwLightMessage getRequest(String sessionId, long id, String device, String property, RequestContext requestContext) {
        final CmwLightMessage msg = new CmwLightMessage();
        msg.messageType = CmwLightProtocol.MessageType.CLIENT_REQ;
        msg.requestType = CmwLightProtocol.RequestType.GET;
        msg.id = id;
        msg.sessionId = sessionId;
        msg.deviceName = device;
        msg.propertyName = property;
        msg.requestContext = requestContext;
        msg.updateType = CmwLightProtocol.UpdateType.NORMAL;
        return msg;
    }

    public static CmwLightMessage setRequest(final String sessionId, final long id, final String device, final String property, final ZFrame data, final RequestContext requestContext) {
        final CmwLightMessage msg = new CmwLightMessage();
        msg.messageType = CmwLightProtocol.MessageType.CLIENT_REQ;
        msg.requestType = CmwLightProtocol.RequestType.SET;
        msg.id = id;
        msg.sessionId = sessionId;
        msg.deviceName = device;
        msg.propertyName = property;
        msg.requestContext = requestContext;
        msg.updateType = CmwLightProtocol.UpdateType.NORMAL;
        msg.bodyData = data;
        return msg;
    }

    public static CmwLightMessage exceptionReply(final String sessionId, final long id, final String device, final String property, final String message, final long contextAcqStamp, final long contextCycleStamp, final byte type) {
        final CmwLightMessage msg = new CmwLightMessage(CmwLightProtocol.MessageType.SERVER_REP);
        msg.requestType = CmwLightProtocol.RequestType.EXCEPTION;
        msg.id = id;
        msg.sessionId = sessionId;
        msg.deviceName = device;
        msg.propertyName = property;
        msg.updateType = CmwLightProtocol.UpdateType.NORMAL;
        msg.exceptionMessage = new ExceptionMessage(contextAcqStamp, contextCycleStamp, message, type);
        return msg;
    }

    public static CmwLightMessage subscribeExceptionReply(final String sessionId, final long id, final String device, final String property, final String message, final long contextAcqStamp, final long contextCycleStamp, final byte type) {
        final CmwLightMessage msg = new CmwLightMessage(CmwLightProtocol.MessageType.SERVER_REP);
        msg.requestType = CmwLightProtocol.RequestType.SUBSCRIBE_EXCEPTION;
        msg.id = id;
        msg.sessionId = sessionId;
        msg.deviceName = device;
        msg.propertyName = property;
        msg.updateType = CmwLightProtocol.UpdateType.NORMAL;
        msg.exceptionMessage = new ExceptionMessage(contextAcqStamp, contextCycleStamp, message, type);
        return msg;
    }

    public static CmwLightMessage notificationExceptionReply(final String sessionId, final long id, final String device, final String property, final String message, final long contextAcqStamp, final long contextCycleStamp, final byte type) {
        final CmwLightMessage msg = new CmwLightMessage(CmwLightProtocol.MessageType.SERVER_REP);
        msg.requestType = CmwLightProtocol.RequestType.NOTIFICATION_EXC;
        msg.id = id;
        msg.sessionId = sessionId;
        msg.deviceName = device;
        msg.propertyName = property;
        msg.updateType = CmwLightProtocol.UpdateType.NORMAL;
        msg.exceptionMessage = new ExceptionMessage(contextAcqStamp, contextCycleStamp, message, type);
        return msg;
    }

    public static CmwLightMessage notificationReply(final String sessionId, final long id, final String device, final String property, final ZFrame data, final long notificationId, final DataContext requestContext, final CmwLightProtocol.UpdateType updateType) {
        final CmwLightMessage msg = new CmwLightMessage(CmwLightProtocol.MessageType.SERVER_REP);
        msg.requestType = CmwLightProtocol.RequestType.NOTIFICATION_DATA;
        msg.id = id;
        msg.sessionId = sessionId;
        msg.deviceName = device;
        msg.propertyName = property;
        msg.notificationId = notificationId;
        msg.options = Map.of(CmwLightProtocol.FieldName.NOTIFICATION_ID_TAG.value(), notificationId);
        msg.dataContext = requestContext;
        msg.updateType = updateType;
        msg.bodyData = data;
        return msg;
    }

    public static CmwLightMessage getReply(final String sessionId, final long id, final String device, final String property, final ZFrame data, final DataContext requestContext) {
        final CmwLightMessage msg = new CmwLightMessage(CmwLightProtocol.MessageType.SERVER_REP);
        msg.requestType = CmwLightProtocol.RequestType.REPLY;
        msg.id = id;
        msg.sessionId = sessionId;
        msg.deviceName = device;
        msg.propertyName = property;
        msg.dataContext = requestContext;
        msg.bodyData = data;
        return msg;
    }

    public static CmwLightMessage sessionConfirmReply(final String sessionId, final long id, final String device, final String property, final Map<String, Object> options) {
        final CmwLightMessage msg = new CmwLightMessage();
        msg.messageType = CmwLightProtocol.MessageType.SERVER_REP;
        msg.requestType = CmwLightProtocol.RequestType.SESSION_CONFIRM;
        msg.id = id;
        msg.options = options;
        msg.sessionId = sessionId;
        msg.deviceName = device;
        msg.propertyName = property;
        msg.updateType = CmwLightProtocol.UpdateType.NORMAL;
        return msg;
    }

    public static CmwLightMessage eventReply(final String sessionId, final long id, final String device, final String property) {
        final CmwLightMessage msg = new CmwLightMessage();
        msg.messageType = CmwLightProtocol.MessageType.SERVER_REP;
        msg.requestType = CmwLightProtocol.RequestType.EVENT;
        msg.id = id;
        msg.sessionId = sessionId;
        msg.deviceName = device;
        msg.propertyName = property;
        msg.updateType = CmwLightProtocol.UpdateType.NORMAL;
        return msg;
    }

    public static CmwLightMessage eventRequest(final String sessionId, final long id, final String device, final String property) {
        final CmwLightMessage msg = new CmwLightMessage();
        msg.messageType = CmwLightProtocol.MessageType.CLIENT_REQ;
        msg.requestType = CmwLightProtocol.RequestType.EVENT;
        msg.id = id;
        msg.sessionId = sessionId;
        msg.deviceName = device;
        msg.propertyName = property;
        msg.updateType = CmwLightProtocol.UpdateType.NORMAL;
        return msg;
    }

    public static CmwLightMessage connectRequest(final String sessionId, final long id, final String device, final String property) {
        final CmwLightMessage msg = new CmwLightMessage();
        msg.messageType = CmwLightProtocol.MessageType.CLIENT_REQ;
        msg.requestType = CmwLightProtocol.RequestType.CONNECT;
        msg.id = id;
        msg.sessionId = sessionId;
        msg.deviceName = device;
        msg.propertyName = property;
        msg.updateType = CmwLightProtocol.UpdateType.NORMAL;
        return msg;
    }

    protected CmwLightMessage() {
        // Constructor only accessible from within serialiser and factory methods to only allow valid messages
    }

    protected CmwLightMessage(final CmwLightProtocol.MessageType messageType) {
        this.messageType = messageType;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (!(o instanceof CmwLightMessage))
            return false;
        final CmwLightMessage that = (CmwLightMessage) o;
        return id == that.id && notificationId == that.notificationId && sourceId == that.sourceId && messageType == that.messageType && Objects.equals(version, that.version) && requestType == that.requestType && Objects.equals(deviceName, that.deviceName) && updateType == that.updateType && Objects.equals(sessionId, that.sessionId) && Objects.equals(propertyName, that.propertyName) && Objects.equals(options, that.options) && Objects.equals(data, that.data) && Objects.equals(bodyData, that.bodyData) && Objects.equals(exceptionMessage, that.exceptionMessage) && Objects.equals(requestContext, that.requestContext) && Objects.equals(dataContext, that.dataContext) && Objects.equals(sessionBody, that.sessionBody);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageType, version, requestType, id, deviceName, updateType, sessionId, propertyName, options, data, bodyData, exceptionMessage, requestContext, dataContext, notificationId, sourceId, sessionBody);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CmwMessage: ");
        switch (messageType) {
        case CLIENT_CONNECT:
            sb.append("Connection request, client version='").append(version).append('\'');
            break;
        case SERVER_CONNECT_ACK:
            sb.append("Connection ack, server version='").append(version).append('\'');
            break;
        case CLIENT_HB:
            sb.append("client heartbeat");
            break;
        case SERVER_HB:
            sb.append("server heartbeat");
            break;
        case SERVER_REP:
            sb.append("server reply: ").append(requestType.name());
        case CLIENT_REQ:
            if (messageType == CmwLightProtocol.MessageType.CLIENT_REQ)
                sb.append("client request: ").append(requestType.name());
            sb.append(" id: ").append(id).append(" deviceName=").append(deviceName).append(", updateType=").append(updateType).append(", sessionId='").append(sessionId).append('\'').append(", propertyName='").append(propertyName).append('\'').append(", options=").append(options).append(", data=").append(data).append(", sourceId=").append(sourceId);
            switch (requestType) {
            case GET:
            case SET:
            case SUBSCRIBE:
            case UNSUBSCRIBE:
                sb.append("\n  requestContext=").append(requestContext);
                break;
            case CONNECT:
                break;
            case REPLY:
            case NOTIFICATION_DATA:
                sb.append(", notificationId=").append(notificationId).append("\n  bodyData=").append(bodyData).append("\n  dataContext=").append(dataContext);
                break;
            case EXCEPTION:
            case NOTIFICATION_EXC:
            case SUBSCRIBE_EXCEPTION:
                sb.append("\n  exceptionMessage=").append(exceptionMessage);
                break;
            case EVENT:
                break;
            case SESSION_CONFIRM:
                sb.append(", sessionBody='").append(sessionBody).append('\'');
                break;
            default:
                throw new IllegalStateException("unknown client request message type: " + messageType);
            }
        default:
            throw new IllegalStateException("unknown message type: " + messageType);
        }
        return sb.toString();
    }

    public static class RequestContext {
        public String selector;
        public Map<String, Object> data;
        public Map<String, Object> filters;

        public RequestContext(final String selector, final Map<String, Object> filters, final Map<String, Object> data) {
            this.selector = selector;
            this.filters = filters;
            this.data = data;
        }

        protected RequestContext() {
            // default constructor only available to protocol (de)serialisers
        }

        @Override
        public String toString() {
            return "RequestContext{"
                    + "selector='" + selector + '\'' + ", data=" + data + ", filters=" + filters + '}';
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o)
                return true;
            if (!(o instanceof RequestContext))
                return false;
            final RequestContext that = (RequestContext) o;
            return selector.equals(that.selector) && Objects.equals(data, that.data) && Objects.equals(filters, that.filters);
        }

        @Override
        public int hashCode() {
            return Objects.hash(selector, data, filters);
        }
    }

    public static class DataContext {
        public String cycleName;
        public long cycleStamp;
        public long acqStamp;
        public Map<String, Object> data;

        public DataContext(final String cycleName, final long cycleStamp, final long acqStamp, final Map<String, Object> data) {
            this.cycleName = cycleName;
            this.cycleStamp = cycleStamp;
            this.acqStamp = acqStamp;
            this.data = data;
        }

        protected DataContext() {
            // allow only protocol serialiser to create empty object
        }

        @Override
        public String toString() {
            return "DataContext{"
                    + "cycleName='" + cycleName + '\'' + ", cycleStamp=" + cycleStamp + ", acqStamp=" + acqStamp + ", data=" + data + '}';
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o)
                return true;
            if (!(o instanceof DataContext))
                return false;
            final DataContext that = (DataContext) o;
            return cycleStamp == that.cycleStamp && acqStamp == that.acqStamp && cycleName.equals(that.cycleName) && Objects.equals(data, that.data);
        }

        @Override
        public int hashCode() {
            return Objects.hash(cycleName, cycleStamp, acqStamp, data);
        }
    }

    public static class ExceptionMessage {
        public long contextAcqStamp;
        public long contextCycleStamp;
        public String message;
        public byte type;

        public ExceptionMessage(final long contextAcqStamp, final long contextCycleStamp, final String message, final byte type) {
            this.contextAcqStamp = contextAcqStamp;
            this.contextCycleStamp = contextCycleStamp;
            this.message = message;
            this.type = type;
        }

        protected ExceptionMessage() {
            // allow only protocol serialiser to create empty object
        }

        @Override
        public String toString() {
            return "ExceptionMessage{"
                    + "contextAcqStamp=" + contextAcqStamp + ", contextCycleStamp=" + contextCycleStamp + ", message='" + message + '\'' + ", type=" + type + '}';
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o)
                return true;
            if (!(o instanceof ExceptionMessage))
                return false;
            final ExceptionMessage that = (ExceptionMessage) o;
            return contextAcqStamp == that.contextAcqStamp && contextCycleStamp == that.contextCycleStamp && type == that.type && message.equals(that.message);
        }

        @Override
        public int hashCode() {
            return Objects.hash(contextAcqStamp, contextCycleStamp, message, type);
        }
    }
}
