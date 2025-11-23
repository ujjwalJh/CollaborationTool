import React, { useEffect, useRef, useState, useContext } from "react";
import { AuthContext } from "../context/AuthContext";
import SockJS from "sockjs-client";
import { Client } from "@stomp/stompjs";

export default function EditorPage({ docId }) {
  const { token, user } = useContext(AuthContext);

  const [presence, setPresence] = useState({});
  const [content, setContent] = useState("");

  const stompRef = useRef(null);
  const clientIdRef = useRef(null);
  const textareaRef = useRef(null);

  const docIdRef = useRef(docId);
  useEffect(() => (docIdRef.current = docId), [docId]);

  // -----------------------
  // CONNECT TO STOMP
  // -----------------------
  useEffect(() => {
    const socket = new SockJS(`http://localhost:8080/ws?token=${token}`);

    const stompClient = new Client({
      webSocketFactory: () => socket,
      connectHeaders: { Authorization: `Bearer ${token}` },
      debug: (msg) => console.log(msg),
      onConnect: () => {
        console.log("STOMP CONNECTED");

        // Generate client ID
        if (!clientIdRef.current) {
          clientIdRef.current =
            crypto.randomUUID?.() ||
            Math.random().toString(36).substring(2, 10);
        }

        // Subscribe to presence updates
        stompClient.subscribe(
          `/topic/presence.${docIdRef.current}`,
          (message) => {
            const data = JSON.parse(message.body);
            console.log("PRESENCE EVENT:", data);

            handlePresenceEvent(data);
          }
        );

        // Announce join
        sendPresence("join");
      },
      onStompError: (frame) => console.error("STOMP ERROR", frame)
    });

    stompClient.activate();
    stompRef.current = stompClient;

    return () => {
      if (stompRef.current?.connected) {
        sendPresence("leave");
        stompRef.current.deactivate();
      }
    };
  }, [docId, token]);

  // -----------------------
  // SEND JOIN / LEAVE / CURSOR
  // -----------------------
  const sendPresence = (type, cursor = null) => {
    if (!stompRef.current?.connected) return;

    const msg = {
      type,
      docId: String(docIdRef.current),
      user: user?.username || user?.email,
      userId: user?.id,
      clientId: clientIdRef.current,
      cursor // { position }
    };

    stompRef.current.publish({
      destination: "/app/presence",
      body: JSON.stringify(msg)
    });
  };

  // -----------------------
  // HANDLE INCOMING PRESENCE EVENTS
  // -----------------------
  const handlePresenceEvent = (msg) => {
    setPresence((prev) => {
      const next = { ...prev };

      if (msg.type === "join") {
        next[msg.clientId] = {
          user: msg.user,
          cursor: null
        };
      }

      if (msg.type === "leave") {
        delete next[msg.clientId];
      }

      if (msg.type === "cursor") {
        if (next[msg.clientId]) {
          next[msg.clientId].cursor = msg.cursor;
        }
      }

      return next;
    });
  };

  // -----------------------
  // SEND CURSOR ON TEXTAREA CHANGE
  // -----------------------
  const handleCursorMove = () => {
    const textarea = textareaRef.current;
    if (!textarea) return;

    const cursorPos = textarea.selectionStart;

    sendPresence("cursor", { position: cursorPos });
  };

  // -----------------------
  // RENDER REMOTE CURSORS
  // -----------------------
  const renderRemoteCursors = () => {
    const textarea = textareaRef.current;
    if (!textarea) return null;

    const text = content;
    const lines = text.split("\n");
    const cursorMarkers = [];

    Object.entries(presence).forEach(([id, p]) => {
      if (!p.cursor || id === clientIdRef.current) return;

      const pos = p.cursor.position;

      // Convert cursor position to row/column
      let row = 0, col = pos;
      for (let i = 0; i < lines.length; i++) {
        if (col <= lines[i].length) {
          row = i;
          break;
        }
        col -= lines[i].length + 1;
      }

      cursorMarkers.push(
        <div
          key={id}
          style={{
            position: "absolute",
            top: row * 20,
            left: col * 9,
            background: "red",
            width: "2px",
            height: "20px"
          }}
        ></div>
      );
    });

    return cursorMarkers;
  };

  return (
    <div className="editor-page" style={{ display: "flex", flexDirection: "column" }}>
      
      {/* Presence Bar */}
      <div className="presence-bar" style={{ padding: "10px", display: "flex", gap: "10px" }}>
        {Object.keys(presence).map((id) => (
          <div key={id} style={{ padding: "4px 8px", background: "#ddd", borderRadius: "6px" }}>
            {presence[id].user}
          </div>
        ))}
      </div>

      {/* Editor */}
      <div style={{ position: "relative", padding: "20px" }}>
        <textarea
          ref={textareaRef}
          value={content}
          onChange={(e) => setContent(e.target.value)}
          onKeyUp={handleCursorMove}
          onClick={handleCursorMove}
          onSelect={handleCursorMove}
          style={{
            width: "100%",
            height: "500px",
            fontSize: "16px",
            lineHeight: "20px"
          }}
        />

        {/* Remote cursors */}
        {renderRemoteCursors()}
      </div>
    </div>
  );
}
