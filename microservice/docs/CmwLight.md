# Unofficial CMW client protocol implementation

This is an effort of documenting and reimplementing the CMW (Controls Middleware)
binary protocol.

CMW is a project developed at CERN, for more information see its 
[online documentation](https://cmwdoc.web.cern.ch/cmwdoc/) or the [paper](https://cds.cern.ch/record/2305650/files/mobpl05.pdf).

## CMW protocol description

The CMW rda3 protocol consists of individual [ZeroMQ](https://zeromq.org/) messages with
one to four frames.
The frames either contain simple enum-type bytes, a byte encoded string or CMW data encoded binary data.
The CMW data format is reimplemented in the `CmwLightSerialiser`, see the [serialiser documentation](IoSerialiser.md)
for more information.
The following sections describe the byte values and cmw-data field names used by the protocol
and their use for establishing a connection, performing subscribe/get/... actions and receiving the replies.

The first ZFrame of each message contains a single byte defining the type of the Message:

| byte value | message name       | direction | contents |
|------------|--------------------|-----------|----------|
| 0x01       | SERVER_CONNECT_ACK | s -> c    | MessageType, VersionString
| 0x02       | SERVER_REP         | s -> c    | MessageType, Frames(1-3), Descriptor
| 0x03       | SERVER_HB          | s -> c    | MessageType
| 0x20       | CLIENT_CONNECT     | c -> s    | MessageType, VersionString
| 0x21       | CLIENT_REQ         | c -> s    | MessageType, Frames(1-3), Descriptor
| 0x22       | CLIENT_HB          | c -> s    | MessageType

### Establishing the connection

The connection is established by the client sending a ZMsg with the first frame containing
the message type CLIENT_CONNECT, followed a ZFrame containing the version string
"1.0.0" (the version string is unused).
The server acknowledges by sending SERVER_CONNECT_ACK, followed by the version
string ("1.0.0", not used).

### Heartbeats

The rda3 protocol uses heartbeats for connection management.
Client as well as server periodically send messages only consisting of a single one-byte frame containing SERVER_HB/ CLIENT_HB.
If client or server do not receive a heartbeat or any other message for some time, the connection is reset.
By default, both sides send a heartbeat every second and reset the connection if 3 consecutive heartbeats are missed.
Heartbeats are only sent if there is otherwise no communication, so every received package must also reset this timeout.

### Requests/Replies

The client can send requests and the server sends replies, indicated by the types CLIENT_REQ and SERVER_REP.
The message type frame is followed by an arbitrary number of frames, where the last one is the so called descriptor,
which contains one byte for each previous frame, containing the type of the frame contents.

| message name            | byte value |
|-------------------------|------------|
| MT_HEADER               | 0          |
| MT_BODY                 | 1          |
| MT_BODY_DATA_CONTEXT    | 2          |
| MT_BODY_REQUEST_CONTEXT | 3          |
| MT_BODY_EXCEPTION       | 4          |

The second frame is always of type MT_HEADER and its field reqType defines the type of the
request/reply and also the frames present in the data:

| byte | message type          | message | direction | comment |
|------|-----------------------|---------|-----------|---------|
| 0    |RT_GET                 | H, RC   | C->S      |         |
| 1    |RT_SET                 | H,B,RC  | C->S      |         |
| 2    |RT_CONNECT             | H, B    | C->S      |         |
| 3    |RT_REPLY               | H,B     | S->C      | response to connect |
| 3    |RT_REPLY               | H,B,DC  | S->C      |         |
| 4    |RT_EXCEPTION           | H,B     | S->C      |         |
| 5    |RT_SUBSCRIBE           | H, RC   | C->S      |         |
| 5    |RT_SUBSCRIBE           | H       | S->C      | "ack"   |
| 6    |RT_UNSUBSCRIBE         | H, RC   | C->S      |         |
| 7    |RT_NOTIFICATION_DATA   | H,B,DC  | S->C      |         |
| 8    |RT_NOTIFICATION_EXC    | H,B     | S->C      |         |
| 9    |RT_SUBSCRIBE_EXCEPTION | H,B     | S->C      |         |
| 10   |RT_EVENT               | H       | C->S      |         | 
| 10   |RT_EVENT               | H       | S->C      | close   | 
| 11   |RT_SESSION_CONFIRM     | H       | S->C      |         |

#### Header fields:
The header frame is sent as the second frame of each message, but depending on the message and request type,
not all fields are populated and different option fields are present.

| fieldname | enum tag          | type | description |
|-----------|-------------------|------|-------------|
| "2"       | REQ_TYPE_TAG      | byte | see table above     
| "0"       | ID_TAG            | long | id to map back to the request from the reply, for subscription replies set by source id from subscription reply
| "1"       | DEVICE_NAME_TAG   |string| empty for subscription notifications
| "7"       | UPDATE_TYPE_TAG   | byte |
| "d"       | SESSION_ID_TAG    |string| empty for subscription notifications
| "f"       | PROPERTY_NAME_TAG |string| empty for subscription notifications
| "3"       | OPTIONS_TAG       | data | 

The Session Id tag contains the session id which identifies the client:
  - `RemoteHostInfoImpl[name=<application-name>; userName=<username>; appId=[app=<application-name>;uid=<username>;host=<hostname>;pid=<pid>;]`
  
The options tag can contain the following fields
  - optional NOTIFICATION_ID_TAG = "a" type: long; for notification data, counts the notifications
  - optional SOURCE_ID_TAG = "b"; for subscription requests to propagate the id
  - optional SESSION_BODY_TAG = "e" type: cmw-data; for session context/RBAC

#### Request Context Fields
The request context frame is sent for get, set and subscribe requests and specifies on which data the request should act.

| fieldname | enum tag     | type | description |
|-----------|--------------|------|-------------|
| "8"       | SELECTOR_TAG |string|             |
| "c"       | FILTERS_TAG  | data |             |
| "x"       | DATA_TAG     | data |             |

#### Data Context Fields
The data context frame is sent for reply and notification replies and specifies, what context the data belongs to.

| fieldname | enum tag        | type | description |
|-----------|-----------------|------|-------------|
| "4"       | CYCLE_NAME_TAG  |string|             |
| "5"       | ACQ_STAMP_TAG   | data |             |
| "6"       | CYCLE_STAMP_TAG | data |             |
| "x"       | DATA_TAG        | data | fields: "acqStamp", "cycleStamp", "cycleName", "version", "type"|
  
#### connect body
The connection step seems to be optional, it will be established implicitly when opening a subscription.
The server responds with an empty (fields as well as data frame) `REPLY` message.

| fieldname | enum tag        | type | description |
|-----------|-----------------|------|-------------|
| "9"       | CLIENT_INFO_TAG |string|             |

The field contains a string of `#` separated `key#type[#length]#value` entries. Strings are URL encoded.
- `Address:#string#16#tcp:%2F%2FSYSPC004:0`
- `ApplicationId:#string#54#app=fesa%2Dexplorer2;uid=akrimm;host=SYSPC004;pid=17442;`
- `UserName:#string#6#akrimm`
- `ProcessName:#string#14#fesa%2Dexplorer2`
- `Language:#string#4#Java`
- `StartTime:#long#1605172397732`
- `Name:#string#14#fesa%2Dexplorer2`
- `Pid:#int#17442`
- `Version:#string#5#2%2E8%2E1`
  
#### Exception body field
If there is an exception, the server sends a reply of type exception, subscribe exception or notification exception.
The enclosed exception body frame contains the following fields.

| fieldname           | enum tag                           | type | description |
|---------------------|------------------------------------|------|-------------|
| "Message"           |EXCEPTION_MESSAGE_FIELD             |string|             |
| "Type"              |EXCEPTION_TYPE_FIELD                |string|             |
| "Backtrace"         |EXCEPTION_BACKTRACE_FIELD           |string|             |
| "ContextCycleName"  |EXCEPTION_CONTEXT_CYCLE_NAME_FIELD  |string|             |
| "ContextCycleStamp" |EXCEPTION_CONTEXT_CYCLE_STAMP_FIELD | long |             |
| "ContextAcqStamp"   |EXCEPTION_CONTEXT_ACQ_STAMP_FIELD   | long |             |
| "ContextData"       |EXCEPTION_CONTEXT_DATA_FIELD        |string|             |

#### currently unused field names
- MESSAGE_TAG = "message";


## Client implementation

The implementation only cares about the client part and for now only supports property subscriptions.
RBAC is also not supported.

The `CmwLightMessage` implements a generic message which can represent all the messages exchanged between client and server.
It provides methods for generating consistent messages and static instances for the heartbeat messages.
The `CmwLightProtocol` takes care of translating these messages to and from ZeroMQ's `ZMsg` format.

One `CmwLightClient` takes care of one cmw server. It manages connection state, subscriptions and their state.
It is supposed to be embedded into an event loop, which should call the receiveMessage call at least once every `heartbeatInterval`.
This can be facilitated efficiently by registering a ZeroMQ poller to the client's socket.

See `CmwLightPoller` for an example which publishes the subscription notifications into an LMAX disruptor ring buffer.