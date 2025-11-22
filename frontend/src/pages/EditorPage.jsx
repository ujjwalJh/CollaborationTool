// frontend/src/pages/EditorPage.jsx
import React, { useEffect, useRef, useContext, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";

import { EditorContent, useEditor } from "@tiptap/react";
import StarterKit from "@tiptap/starter-kit";

import api from "../api/axios";
import { AuthContext } from "../context/AuthContext";

import {
  connectWs,
  disconnectWs,
  sendEdit,
  sendPresence
} from "../ws/socket";

export default function EditorPage() {
  const { docId } = useParams();
  const navigate = useNavigate();
  const { user, token } = useContext(AuthContext);

  const clientIdRef = useRef(Math.random().toString(36).substring(2));
  const sendTimeoutRef = useRef(null);

  const [connected, setConnected] = useState(false);
  const [docMeta, setDocMeta] = useState(null);

  // ------------------------------
  // CREATE TIPTAP EDITOR
  // ------------------------------
  const editor = useEditor({
    extensions: [StarterKit],
    content: "<p>Loading…</p>",

    onUpdate: ({ editor }) => {
      if (sendTimeoutRef.current) clearTimeout(sendTimeoutRef.current);

      sendTimeoutRef.current = setTimeout(() => {
        const json = editor.getJSON();
        const payload = JSON.stringify(json);

        sendEdit({
          docId,
          user: user?.email || "anonymous",
          clientId: clientIdRef.current,
          content: payload,
          timestamp: Date.now(),
        });
      }, 400);
    },
  });

  // ------------------------------
  // LOAD INITIAL CONTENT FROM DB
  // ------------------------------
  useEffect(() => {
    if (!docId || !editor) return;

    api.get(`/api/docs/${docId}`).then((res) => {
      setDocMeta(res.data);

      let content = res.data.content;

      if (!content || content === "[0,0]") {
        content = JSON.stringify({
          type: "doc",
          content: [{ type: "paragraph", content: [] }],
        });
      }

      try {
        editor.commands.setContent(JSON.parse(content), false);
      } catch (e) {
        console.warn("Invalid stored content. Resetting.");

        editor.commands.setContent(
          {
            type: "doc",
            content: [{ type: "paragraph", content: [] }],
          },
          false
        );
      }
    });
  }, [docId, editor]);

  // ------------------------------
  // CONNECT TO STOMP WEBSOCKET
  // ------------------------------
  // ---- CONNECT WS ----
useEffect(() => {
  if (!token || !docId || !editor) return;

  let mounted = true;

  connectWs({
    token,
    docId,

    onConnect: () => {
      if (!mounted) return;

      setConnected(true);

      sendPresence({
        docId,
        user: user?.email || "anonymous",
        action: "join",
        timestamp: Date.now(),
      });
    },

    onMessage: (msg) => {
      if (!mounted) return;
      if (!msg) return;
      if (msg.action) return;
      if (msg.clientId === clientIdRef.current) return;

      if (msg.content) {
        try {
          editor.commands.setContent(JSON.parse(msg.content), false);
        } catch {}
      }
    },
  });

  return () => {
    mounted = false;

    // Only send presence if fully connected
    if (connected) {
      sendPresence({
        docId,
        user: user?.email || "anonymous",
        action: "leave",
        timestamp: Date.now(),
      });
    }

    disconnectWs();
    setConnected(false);
  };
}, [token, docId, editor]);


  // ------------------------------
  // RENDER
  // ------------------------------
  if (!editor)
    return <div style={{ padding: 20 }}>Loading editor…</div>;

  return (
    <div style={{ padding: 20, color: "white" }}>
      <h2>{docMeta?.title || `Document #${docId}`}</h2>

      <div style={{ marginBottom: 10 }}>
        Realtime:{" "}
        <span style={{ color: connected ? "#22c55e" : "#ef4444" }}>
          {connected ? "Connected" : "Disconnected"}
        </span>
      </div>

      <EditorContent editor={editor} />
    </div>
  );
}
