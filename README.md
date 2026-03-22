# CollaborationTool — Real-time Collaborative Document Editor

A full-stack real-time collaborative editing platform built with **Spring Boot**, **React.js**, **Redis**, and **PostgreSQL**. Multiple users can edit documents simultaneously with live presence indicators, secured by JWT authentication enforced at both the HTTP and WebSocket layers.

> Solo project · Deployed frontend on Vercel

---

## System Architecture

```
React.js Frontend
       │
       ├── REST API (HTTP + JWT)          ← Document CRUD, Auth, Workspace mgmt
       │
       └── WebSocket (STOMP + SockJS)     ← Real-time edits & presence
              │
              ▼
       Spring Boot Backend
              │
       ┌──────┴──────┐
       │             │
  PostgreSQL       Redis
  (documents,    (Pub/Sub for
   users,         presence
   workspaces)    broadcasting)
```

### Key Design Decisions

**Why JWT on the WebSocket handshake?**
Standard Spring Security only protects HTTP endpoints. WebSocket connections upgrade from HTTP and then operate independently — meaning a valid JWT at connection time doesn't guarantee the user still has access mid-session. A custom `JwtHandshakeInterceptor` intercepts every STOMP frame, validating the JWT on `CONNECT` and checking workspace membership on every `SUBSCRIBE`. This prevents unauthorized users from joining document sessions even if they know the topic name.

**Why Redis Pub/Sub for presence?**
Each backend instance only knows about WebSocket sessions connected to it. If you scale to multiple instances, a user on instance A wouldn't see presence updates from users on instance B. Redis Pub/Sub on a `presence:*` pattern topic ensures presence events are broadcast across all instances — making the system horizontally scalable.

**Why three edit message formats?**
The `CollabController` handles full-content sync, step-based updates, and delta (from/to/text) updates. This was an iterative design — full-content sync is the simplest to implement and most reliable for conflict resolution (last-write-wins), while step and delta formats reduce payload size for large documents.

**Workspace-scoped permissions**
Every API endpoint and WebSocket subscription checks whether the authenticated user is an owner or member of the relevant workspace via `PermissionService`. This means documents are always scoped to workspaces and can't be accessed across workspace boundaries.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 21, Spring Boot 3.x |
| Real-time | WebSockets (STOMP), SockJS |
| Auth | JWT (HS256), Spring Security |
| Queue/Pub-Sub | Redis 7 (Lettuce client) |
| Database | PostgreSQL 16, Spring Data JPA |
| Frontend | React.js |
| Containerization | Docker Compose |
| Build | Maven |

---

## Features

- **Real-time collaborative editing** — multiple users edit the same document simultaneously
- **Live presence indicators** — see who is currently viewing or editing a document
- **JWT-secured WebSockets** — authentication enforced at STOMP CONNECT and SUBSCRIBE frames
- **Workspace management** — create workspaces, invite members, manage access
- **Role-based access control** — owner vs. member permissions across all endpoints
- **Auto-disconnect handling** — presence cleans up automatically when users disconnect
- **Document persistence** — every edit saved transactionally to PostgreSQL
- **Containerized** — all services orchestrated with Docker Compose

---

## Getting Started

### Prerequisites
- Java 21
- PostgreSQL 16
- Redis 7
- Node.js 18+

### 1. Clone the repository
```bash
git clone https://github.com/ujjwalJh/CollaborationTool.git
cd CollaborationTool
```

### 2. Set up environment variables

Create `backend/src/main/resources/application-local.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/collaboration_db
spring.datasource.username=postgres
spring.datasource.password=yourpassword
spring.redis.host=localhost
spring.redis.port=6379
```

### 3. Start with Docker Compose
```bash
docker-compose up -d
```

### 4. Run the backend
```bash
cd backend
mvn spring-boot:run
```

### 5. Run the frontend
```bash
cd frontend
npm install
npm start
```

Frontend runs on `http://localhost:3000`, backend on `http://localhost:8080`.

---

## API Reference

### Auth
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register a new user |
| POST | `/api/auth/login` | Login, returns JWT |
| GET | `/api/auth/me` | Get current authenticated user |

### Workspaces
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/workspaces` | Create workspace |
| GET | `/api/workspaces/{id}` | Get workspace |
| PUT | `/api/workspaces/{id}/rename` | Rename (owner only) |
| DELETE | `/api/workspaces/{id}` | Delete (owner only) |
| POST | `/api/workspaces/{id}/addMember/{userId}` | Add member (owner only) |
| POST | `/api/workspaces/{id}/removeMember/{userId}` | Remove member (owner only) |
| POST | `/api/workspaces/{id}/leave` | Leave workspace |

### Documents
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/docs/workspace/{workspaceId}` | Create document |
| GET | `/api/docs/workspace/{workspaceId}` | List documents |
| GET | `/api/docs/{docId}` | Get document |
| POST | `/api/docs/{docId}/save` | Save document content |

### WebSocket Topics
| Topic | Description |
|-------|-------------|
| `/app/edit` | Send document edits |
| `/app/presence` | Send presence events (join/leave/cursor) |
| `/topic/doc.{docId}` | Receive document edits |
| `/topic/presence.{docId}` | Receive presence updates |

---

## Project Structure

```
backend/
├── config/
│   ├── JwtHandshakeInterceptor.java   ← JWT auth on every STOMP frame
│   ├── RedisConfig.java               ← Redis Pub/Sub setup
│   ├── SecurityConfig.java            ← Spring Security + CORS
│   └── WebSocketConfig.java           ← STOMP endpoint configuration
├── controller/
│   ├── AuthController.java            ← Register, login, me
│   ├── DocController.java             ← Document CRUD
│   ├── PresenceController.java        ← WebSocket presence handler
│   └── WorkspaceController.java       ← Workspace management
├── service/
│   ├── PermissionService.java         ← Owner/member access checks
│   ├── PresenceService.java           ← Redis Pub/Sub + local broadcast
│   └── PresenceRedisSubscriber.java   ← Receives presence from Redis
├── ws/
│   ├── CollabController.java          ← Real-time edit broadcasting
│   ├── EditMessage.java               ← Edit event data class
│   ├── PresenceMessage.java           ← Presence event data class
│   └── SessionPresenceListener.java   ← Auto-cleanup on disconnect
└── model/
    ├── User.java
    ├── Workspace.java
    └── Doc.java
```

---

## How Real-time Editing Works

```
User A types               User B types
     │                          │
     ▼                          ▼
STOMP /app/edit            STOMP /app/edit
     │                          │
     ▼                          ▼
CollabController ──────────────────────────────►  /topic/doc.{id}
     │                                                   │
     ▼                                            ┌──────┴──────┐
PostgreSQL                                      User A        User B
(persisted)                                   (receives)    (receives)
```

1. User sends edit via WebSocket to `/app/edit`
2. `CollabController` broadcasts to `/topic/doc.{docId}`
3. All subscribers (other users) receive the update instantly
4. Content is persisted to PostgreSQL asynchronously

---

## How Presence Works

```
User joins doc
     │
     ▼
STOMP /app/presence {type: "join"}
     │
     ▼
PresenceService.publish()
     ├── publishLocal() → WebSocket broadcast on this instance
     └── redis.publish(presence:{docId}) → broadcast to other instances
                                                │
                                                ▼
                                    PresenceRedisSubscriber
                                    .handleMessage()
                                         │
                                         ▼
                                    publishLocal() on other instances
```
