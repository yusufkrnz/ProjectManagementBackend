# 💬 MESSAGE MODULE - SENARYO BAZLI VERİ AKIŞ DOKÜMANTASYONU

## 📁 **KLASÖR YAPISI**
```
Message/
├── Controller/     ← Message REST endpoints
├── Service/        ← Message business logic
├── Repository/     ← Message data access
├── Entity/         ← Message entities
├── Config/         ← WebSocket configuration
├── Dto/           ← Message DTOs
├── WebSocket/     ← Real-time messaging
└── Exception/     ← Message exceptions
```

---

## 🎯 **SENARYO 1: SEND MESSAGE - Direct Mesaj Gönderme**

### **📥 Frontend Request**
```json
POST /api/v1/messages/send
{
  "recipientId": "550e8400-e29b-41d4-a716-446655440001",
  "content": "Proje toplantısı için hazır mısın?",
  "messageType": "DIRECT"
}
```

### **🔄 Kesintisiz Yatay Veri Akışı**

```
Frontend → MessageController → MessageServiceImpl → UserRepository → PostgreSQL
    ↓           ↓                    ↓                   ↓               ↓
SendMessageReq @PostMapping      sendMessage()       findById()      SELECT query
    ↓           ↓                    ↓                   ↓               ↓
JSON Body   @Valid check        validation          recipient check  users table
    ↓           ↓                    ↓                   ↓               ↓
message data userId extract     permission check    user lookup      row data

MessageServiceImpl → Message Entity → MessageRepository → Database → WebSocket
       ↓                  ↓                ↓                ↓           ↓
   Builder.build()    message creation   save()         INSERT     real-time push
       ↓                  ↓                ↓                ↓           ↓
   content, metadata  entity instance   JPA persist    messages    WebSocket event
       ↓                  ↓                ↓                ↓           ↓
   timestamp, status  validation       transaction     row created  notify recipient

WebSocket → Connected Clients → MessageController → Frontend
    ↓             ↓                    ↓               ↓
message event  real-time update   response build   JSON Response
    ↓             ↓                    ↓               ↓
STOMP message  UI notification    MessageResponse  HTTP 201
    ↓             ↓                    ↓               ↓
push to client instant delivery  success format   client update
```

### **🔍 Detaylı Sınıf İçi İşlemler**

**1️⃣ Controller/MessageController.java**
```java
@PostMapping("/send")
↓ @Valid SendMessageRequest validation
↓ Authentication.getName() → UUID senderId
↓ messageService.sendMessage(senderId, request.recipientId, request.content, request.messageType)
↓ MessageResponse.fromEntity(sentMessage)
↓ ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response))
```

**2️⃣ Service/impl/MessageServiceImpl.java**
```java
sendMessage() method
↓ log.info("Sending {} message from {} to {}", messageType, senderId, recipientId)
↓ User sender = userRepository.findById(senderId).orElseThrow(...)
↓ User recipient = userRepository.findById(recipientId).orElseThrow(...)
↓ validateMessagePermissions(sender, recipient, messageType)
↓ Message message = Message.builder()
    .sender(sender).recipient(recipient).content(content)
    .messageType(MessageType.valueOf(messageType))
    .status(MessageStatus.SENT).sentAt(LocalDateTime.now()).build()
↓ Message savedMessage = messageRepository.save(message)
↓ webSocketService.sendMessageNotification(recipientId, savedMessage) → real-time
↓ notificationService.createMessageNotification(savedMessage) → async
↓ return savedMessage
```

---

## 🎯 **SENARYO 2: WEBSOCKET REAL-TIME - Anlık Mesajlaşma**

### **📥 WebSocket Connection**
```javascript
// Frontend WebSocket bağlantısı
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);
stompClient.subscribe('/user/queue/messages', handleMessage);
```

### **🔄 Kesintisiz Yatay Veri Akışı**

```
Frontend → WebSocket Connection → MessageWebSocketHandler → Spring Security → Authentication
    ↓              ↓                        ↓                      ↓               ↓
SockJS connect  handshake               afterConnectionEstablished() JWT validate  user session
    ↓              ↓                        ↓                      ↓               ↓
/ws endpoint   upgrade request          connection established   token check     authenticated
    ↓              ↓                        ↓                      ↓               ↓
HTTP → WS      protocol switch          session storage         user context    session active

Message Send → MessageService → WebSocketService → STOMP Broker → Connected Clients
     ↓              ↓                ↓                 ↓                ↓
sendMessage()  real-time notify  messagingTemplate  message queue   user subscriptions
     ↓              ↓                ↓                 ↓                ↓
business logic WebSocket push    STOMP send        /user/queue      specific user
     ↓              ↓                ↓                 ↓                ↓
save to DB     async notification message format    routing         UI update
```

### **🔍 Detaylı Sınıf İçi İşlemler**

**1️⃣ WebSocket/MessageWebSocketHandler.java**
```java
@MessageMapping("/message.send")
↓ @SendToUser("/queue/messages") annotation
↓ public void handleMessage(@Payload SendMessageRequest request, Principal principal)
↓ UUID senderId = UUID.fromString(principal.getName())
↓ Message message = messageService.sendMessage(senderId, request.recipientId, request.content, request.messageType)
↓ MessageNotification notification = MessageNotification.builder()
    .messageId(message.getId()).senderId(senderId)
    .content(message.getContent()).timestamp(LocalDateTime.now()).build()
↓ return notification // Automatically sent to /user/{recipientId}/queue/messages
```

**2️⃣ Config/WebSocketConfig.java**
```java
@EnableWebSocketMessageBroker
↓ registerStompEndpoints() → registry.addEndpoint("/ws").withSockJS()
↓ configureMessageBroker() → 
    config.enableSimpleBroker("/topic", "/queue")
    config.setApplicationDestinationPrefixes("/app")
    config.setUserDestinationPrefix("/user")
```

---

## 🎯 **SENARYO 3: GROUP MESSAGE - Grup Mesajı**

### **📥 Frontend Request**
```json
POST /api/v1/messages/group
{
  "groupId": "550e8400-e29b-41d4-a716-446655440002",
  "content": "Toplantı 15:00'te başlayacak",
  "messageType": "GROUP"
}
```

### **🔄 Kesintisiz Yatay Veri Akışı**

```
Frontend → MessageController → MessageServiceImpl → GroupRepository → PostgreSQL
    ↓           ↓                    ↓                   ↓                ↓
GroupMsgReq @PostMapping        sendGroupMessage()   findById()       SELECT query
    ↓           ↓                    ↓                   ↓                ↓
JSON Body   validation          group validation    group lookup     groups table
    ↓           ↓                    ↓                   ↓                ↓
group data  @Valid check        permission check    entity fetch     row data

MessageServiceImpl → GroupMemberRepository → Message Entities → MessageRepository → Database
       ↓                      ↓                    ↓                   ↓               ↓
getGroupMembers()         findByGroupId()      multiple builds     saveAll()        BATCH INSERT
       ↓                      ↓                    ↓                   ↓               ↓
member lookup             JPA query            List<Message>       batch operation  messages table
       ↓                      ↓                    ↓                   ↓               ↓
active members            member entities      per member msg      JPA batch        multiple rows

Database → WebSocketService → STOMP Broker → All Group Members → UI Updates
    ↓              ↓                ↓               ↓                  ↓
batch success  broadcast message  message queue   connected users    real-time
    ↓              ↓                ↓               ↓                  ↓
transaction ok  async notification group routing  subscriptions      notifications
    ↓              ↓                ↓               ↓                  ↓
all saved      WebSocket push     /topic/group    member clients     instant delivery
```

### **🔍 Detaylı Sınıf İçi İşlemler**

**1️⃣ Service/impl/MessageServiceImpl.java**
```java
sendGroupMessage() method
↓ log.info("Sending group message to group {} from user {}", groupId, senderId)
↓ Group group = groupRepository.findById(groupId).orElseThrow(...)
↓ validateGroupMessagePermissions(senderId, group)
↓ List<GroupMember> members = groupMemberRepository.findByGroupIdAndIsActiveTrue(groupId)
↓ List<Message> messages = members.stream()
    .filter(member -> !member.getUserId().equals(senderId)) // Don't send to self
    .map(member -> Message.builder()
        .sender(sender).recipient(member.getUser()).content(content)
        .messageType(MessageType.GROUP).groupId(groupId)
        .status(MessageStatus.SENT).sentAt(LocalDateTime.now()).build())
    .collect(Collectors.toList())
↓ List<Message> savedMessages = messageRepository.saveAll(messages)
↓ webSocketService.broadcastGroupMessage(groupId, savedMessages.get(0)) → real-time
↓ return savedMessages
```

**2️⃣ WebSocket/GroupMessageHandler.java**
```java
broadcastGroupMessage() method
↓ GroupMessageNotification notification = GroupMessageNotification.builder()
    .groupId(groupId).senderId(message.getSender().getId())
    .senderName(message.getSender().getName()).content(message.getContent())
    .timestamp(message.getSentAt()).build()
↓ messagingTemplate.convertAndSend("/topic/group/" + groupId, notification)
↓ log.debug("Broadcasted group message to topic: /topic/group/{}", groupId)
```

---

## 🎯 **SENARYO 4: MESSAGE HISTORY - Mesaj Geçmişi**

### **📥 Frontend Request**
```json
GET /api/v1/messages/conversation/550e8400-e29b-41d4-a716-446655440001?page=0&size=20
```

### **🔄 Kesintisiz Yatay Veri Akışı**

```
Frontend → MessageController → MessageServiceImpl → MessageRepository → PostgreSQL
    ↓           ↓                    ↓                   ↓                 ↓
GET request @GetMapping        getConversation()    findConversation()  COMPLEX query
    ↓           ↓                    ↓                   ↓                 ↓
query params path variable     pagination          JPA query           messages table
    ↓           ↓                    ↓                   ↓                 ↓
page, size   userId extract     conversation build  ORDER BY sent_at   sorted results

PostgreSQL → MessageRepository → MessageServiceImpl → MessageController → Frontend
     ↓              ↓                   ↓                    ↓               ↓
JOIN query     Page<Message>       conversation build   response format   JSON Response
     ↓              ↓                   ↓                    ↓               ↓
sender/recipient entity mapping    ConversationResponse pagination wrap   HTTP 200
     ↓              ↓                   ↓                    ↓               ↓
LEFT JOIN users  lazy loading     message grouping     page metadata     client display
```

### **🔍 Detaylı Sınıf İçi İşlemler**

**1️⃣ Repository/MessageRepository.java**
```java
@Query("""
    SELECT m FROM Message m 
    LEFT JOIN FETCH m.sender 
    LEFT JOIN FETCH m.recipient 
    WHERE ((m.sender.id = :userId AND m.recipient.id = :otherUserId) 
       OR (m.sender.id = :otherUserId AND m.recipient.id = :userId))
    AND m.messageType = 'DIRECT'
    ORDER BY m.sentAt DESC
""")
↓ Page<Message> findConversationBetweenUsers(@Param("userId") UUID userId, 
    @Param("otherUserId") UUID otherUserId, Pageable pageable)
↓ JPA query execution with pagination
↓ PostgreSQL: complex JOIN with ORDER BY and LIMIT
↓ ResultSet mapping with eager loading
↓ Page<Message> return with total count
```

**2️⃣ Service/impl/MessageServiceImpl.java**
```java
getConversation() method
↓ log.debug("Getting conversation between {} and {}", userId, otherUserId)
↓ validateConversationAccess(userId, otherUserId)
↓ Page<Message> messages = messageRepository.findConversationBetweenUsers(userId, otherUserId, pageable)
↓ markMessagesAsRead(messages.getContent(), userId) → update READ status
↓ ConversationResponse response = ConversationResponse.builder()
    .messages(messages.getContent().stream().map(MessageResponse::fromEntity).collect(Collectors.toList()))
    .totalElements(messages.getTotalElements()).totalPages(messages.getTotalPages())
    .currentPage(messages.getNumber()).pageSize(messages.getSize()).build()
↓ return response
```

---

## 🎯 **SENARYO 5: MESSAGE SEARCH - Mesaj Arama**

### **📥 Frontend Request**
```json
GET /api/v1/messages/search?q=toplantı&type=ALL&startDate=2024-01-01&endDate=2024-12-31
```

### **🔄 Kesintisiz Yatay Veri Akışı**

```
Frontend → MessageController → MessageServiceImpl → MessageRepository → PostgreSQL
    ↓           ↓                    ↓                   ↓                 ↓
GET params  @GetMapping        searchMessages()     searchByContent()   FULL TEXT search
    ↓           ↓                    ↓                   ↓                 ↓
search query parameter extract search criteria     JPA query           LIKE/ILIKE
    ↓           ↓                    ↓                   ↓                 ↓
filters     validation         filter building     dynamic WHERE       text matching

PostgreSQL → MessageRepository → MessageServiceImpl → MessageController → Frontend
     ↓              ↓                   ↓                    ↓               ↓
search results List<Message>       result processing    response build   JSON Response
     ↓              ↓                   ↓                    ↓               ↓
relevance sort entity mapping      highlight matches    SearchResponse   HTTP 200
     ↓              ↓                   ↓                    ↓               ↓
text ranking  JPA results          search metadata      result format    client display
```

### **🔍 Detaylý Sınıf İçi İşlemler**

**1️⃣ Repository/MessageRepository.java**
```java
@Query("""
    SELECT m FROM Message m 
    WHERE (m.sender.id = :userId OR m.recipient.id = :userId)
    AND (:searchTerm IS NULL OR LOWER(m.content) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
    AND (:messageType IS NULL OR m.messageType = :messageType)
    AND (:startDate IS NULL OR m.sentAt >= :startDate)
    AND (:endDate IS NULL OR m.sentAt <= :endDate)
    ORDER BY m.sentAt DESC
""")
↓ List<Message> searchMessages parameters
↓ Dynamic query building based on non-null parameters
↓ PostgreSQL: complex WHERE clause with multiple conditions
↓ Full-text search with ILIKE for case-insensitive matching
↓ Date range filtering and message type filtering
```

Bu Message modülü, gerçek zamanlı mesajlaşma, grup iletişimi ve mesaj arama özelliklerini destekleyen kapsamlı bir iletişim sistemidir.
