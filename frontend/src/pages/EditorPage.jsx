import React, { useEffect, useRef, useState, useContext } from "react";
import { AuthContext } from "../context/AuthContext";
import SockJS from "sockjs-client";
import { Client } from "@stomp/stompjs";
import { useParams } from "react-router-dom";
import "./editor.css";

export default function EditorPage() {
  const { docId } = useParams();
  const { token, user } = useContext(AuthContext);

  const [presence, setPresence] = useState({});
  const [content, setContent] = useState("");
  const [loaded, setLoaded] = useState(false);

  const stompRef = useRef(null);
  const textareaRef = useRef(null);

  const clientIdRef = useRef(
    sessionStorage.getItem("clientId") ||
      (() => {
        const id = crypto.randomUUID();
        sessionStorage.setItem("clientId", id);
        return id;
      })()
  );

  useEffect(() => {
    if (!docId || !token) return;

    fetch(`http://localhost:8080/api/docs/${docId}`, {
      headers: { Authorization: `Bearer ${token}` }
    })
      .then((r) => {
        if (!r.ok) throw new Error("Failed to load doc");
        return r.json();
      })
      .then((doc) => {
        setContent(doc.content || "");
        setLoaded(true);
      })
      .catch(console.error);
  }, [docId, token]);


  useEffect(() => {
    if (!loaded || stompRef.current) return;

    const socket = new SockJS(`http://localhost:8080/ws?token=${token}`);

    const client = new Client({
      webSocketFactory: () => socket,
      connectHeaders: { Authorization: `Bearer ${token}` },

      onConnect: () => {
        client.subscribe(`/topic/doc.${docId}`, (msg) => {
          const data = JSON.parse(msg.body);
          if (data.clientId === clientIdRef.current) return;
          if (typeof data.content === "string") {
            setContent(data.content);
          }
        });
        client.subscribe(`/topic/presence.${docId}`, (msg) => {
          handlePresenceEvent(JSON.parse(msg.body));
        });

        sendPresence("join");
      }
    });

    client.activate();
    stompRef.current = client;

    return () => {
      if (stompRef.current?.connected) {
        sendPresence("leave");
        stompRef.current.deactivate();
        stompRef.current = null;
      }
    };
  }, [loaded, docId, token]);

  const sendEdit = (value) => {
    stompRef.current?.publish({
      destination: "/app/edit",
      body: JSON.stringify({
        docId: String(docId),
        user: user?.email,
        clientId: clientIdRef.current,
        content: value,
        timestamp: Date.now()
      })
    });
  };

  const sendPresence = (type, cursor = null) => {
    stompRef.current?.publish({
      destination: "/app/presence",
      body: JSON.stringify({
        type,
        docId: String(docId),
        user: user?.email,
        clientId: clientIdRef.current,
        cursor
      })
    });
  };

  const handlePresenceEvent = (msg) => {
    if (!msg.clientId || !msg.user) return;

    setPresence((prev) => {
      const next = { ...prev };

      if (msg.type === "leave") {
        delete next[msg.clientId];
        return next;
      }

      next[msg.clientId] = {
        user: msg.user,
        cursor: msg.cursor || null
      };

      return next;
    });
  };


  const handleCursorMove = () => {
    const el = textareaRef.current;
    if (!el) return;
    sendPresence("cursor", { position: el.selectionStart });
  };

  const renderRemoteCursors = () => {
  const textarea = textareaRef.current;
  if (!textarea) return null;

  const lines = content.split("\n");

  return Object.entries(presence).map(([clientId, p]) => {
    if (!p.cursor) return null;
    if (p.user === user?.email) return null; // hide own cursor

    let pos = p.cursor.position;
    let row = 0;
    let col = pos;

    for (let i = 0; i < lines.length; i++) {
      if (col <= lines[i].length) {
        row = i;
        break;
      }
      col -= lines[i].length + 1;
    }

    const top = row * 20 + 12;  
    const left = col * 9 + 12;   

    return (
      <div
        key={clientId}
        style={{
          position: "absolute",
          top,
          left,
          pointerEvents: "none",
          zIndex: 10
        }}
      >
        <div
          style={{
            position: "absolute",
            top: -18,
            left: 0,
            background: "#ef4444",
            color: "white",
            fontSize: 11,
            padding: "2px 6px",
            borderRadius: 4,
            whiteSpace: "nowrap"
          }}
        >
          {p.user}
        </div>

        <div
          style={{
            width: 2,
            height: 20,
            background: "#ef4444",
            animation: "blink 1s steps(1) infinite"
          }}
        />
      </div>
    );
  });
};
  return (
    <div className="editor-page">
      <div className="presence-bar">
        {Object.entries(presence)
          .filter(([id]) => id !== clientIdRef.current)
          .map(([id, p]) => (
            <div key={id} className="presence-pill">
              {p.user}
            </div>
          ))}
      </div>

      <div className="editor-container">
        <div className="editor-wrapper">
          <textarea
            ref={textareaRef}
            value={content}
            wrap="off"
            onChange={(e) => {
              setContent(e.target.value);
              sendEdit(e.target.value);
            }}
            onKeyUp={handleCursorMove}
            onClick={handleCursorMove}
            onSelect={handleCursorMove}
            className="editor-textarea"
          />
          {renderRemoteCursors()}
        </div>
      </div>
    </div>
  );
}
