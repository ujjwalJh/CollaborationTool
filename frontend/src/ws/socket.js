// frontend/src/ws/socket.js
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";

let stompClient = null;
let state = "idle"; 
// idle | connecting | connected | disconnecting

const WS_URL = "http://localhost:8080/ws";

/* -------------------------------------------------------
   CONNECT
-------------------------------------------------------- */
export async function connectWs({ token, docId, onConnect, onMessage }) {
  console.log("[WS] connectWs() called — state:", state);

  // Prevent race conditions
  if (state === "connecting") {
    console.log("[WS] Already connecting…");
    return;
  }
  if (state === "disconnecting") {
    console.log("[WS] Cannot connect — disconnect in progress");
    return;
  }
  if (state === "connected") {
    console.log("[WS] Already connected");
    return;
  }

  state = "connecting";

  stompClient = new Client({
    debug: (msg) => console.log("[STOMP]", msg),
    reconnectDelay: 0, // no auto reconnect
    webSocketFactory: () => new SockJS(WS_URL),

    connectHeaders: {
      Authorization: `Bearer ${token}`,
    },

    onConnect: () => {
      console.log("[WS] CONNECTED");
      state = "connected";

      onConnect?.();

      stompClient.subscribe(`/topic/doc.${docId}`, (msg) => {
        if (!msg.body) return;
        try {
          onMessage?.(JSON.parse(msg.body));
        } catch (e) {
          console.error("[WS] Failed to parse message:", e);
        }
      });
    },

    onWebSocketClose: () => {
      console.warn("[WS] WebSocket closed");
      if (state !== "disconnecting") {
        state = "idle";
      }
    },

    onStompError: (frame) => {
      console.error("[WS] STOMP ERROR:", frame.headers.message);
      state = "idle";
    },
  });

  try {
    stompClient.activate();
  } catch (e) {
    console.error("[WS] Activation failed:", e);
    state = "idle";
  }
}

/* -------------------------------------------------------
   DISCONNECT
-------------------------------------------------------- */
export async function disconnectWs() {
  console.log("[WS] disconnectWs() called — state:", state);

  if (!stompClient || state !== "connected") {
    console.log("[WS] Nothing to disconnect");
    return;
  }

  state = "disconnecting";

  const client = stompClient;
  stompClient = null;

  try {
    await client.deactivate();
  } catch (e) {
    console.warn("[WS] Error during deactivate:", e);
  }

  console.log("[WS] Disconnected");
  state = "idle";
}

/* -------------------------------------------------------
   SEND EDIT
-------------------------------------------------------- */
export function sendEdit(payload) {
  if (state !== "connected" || !stompClient) {
    console.warn("[WS] Cannot send edit — not connected");
    return;
  }

  stompClient.publish({
    destination: "/app/edit",
    body: JSON.stringify(payload),
  });
}

/* -------------------------------------------------------
   SEND PRESENCE
-------------------------------------------------------- */
export function sendPresence(payload) {
  if (state !== "connected" || !stompClient) {
    console.warn("[WS] Cannot send presence — not connected");
    return;
  }

  stompClient.publish({
    destination: "/app/presence",
    body: JSON.stringify(payload),
  });
}
