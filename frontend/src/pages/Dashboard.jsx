import { useContext, useEffect, useState } from "react";
import { AuthContext } from "../context/AuthContext";
import api from "../api/axios";
import { Link, useNavigate } from "react-router-dom";
import "./Dashboard.css";

export default function Dashboard() {
  const { logout, user } = useContext(AuthContext);
  const navigate = useNavigate();

  const [me, setMe] = useState(null);
  const [workspaces, setWorkspaces] = useState([]);
  const [selectedWorkspace, setSelectedWorkspace] = useState(null);
  const [docs, setDocs] = useState([]);
  const [newWsName, setNewWsName] = useState("");
  const [newDocTitle, setNewDocTitle] = useState("");

  useEffect(() => {
    api.get("/api/auth/me").then(res => {
      setMe(res.data);
      // load workspaces by owner id (backend endpoint)
      api.get(`/api/workspaces/owner/${res.data.id}`).then(r => {
        setWorkspaces(r.data);
      }).catch(() => {
        // fallback: list all
        api.get("/api/workspaces").then(r => setWorkspaces(r.data));
      });
    }).catch(() => {
      logout();
      window.location.href = "/login";
    });
  }, []);

  useEffect(() => {
    if (!selectedWorkspace) {
      setDocs([]);
      return;
    }
    api.get(`/api/docs/workspace/${selectedWorkspace.id}`).then(r => {
      setDocs(r.data);
    });
  }, [selectedWorkspace]);

  const handleCreateWorkspace = async () => {
    if (!newWsName.trim() || !me) return;
    const res = await api.post("/api/workspaces", {
      name: newWsName,
      owner: { id: me.id }
    });
    setWorkspaces(prev => [...prev, res.data]);
    setNewWsName("");
  };

  const handleCreateDoc = async () => {
    if (!newDocTitle.trim() || !selectedWorkspace) return;
    const res = await api.post(`/api/docs/workspace/${selectedWorkspace.id}`, {
      title: newDocTitle
    });
    setDocs(prev => [...prev, res.data]);
    setNewDocTitle("");
  };

  const openDoc = (doc) => {
    navigate(`/editor/${doc.id}`);
  };

  const handleLogout = async () => {
    try { await api.post("/api/auth/logout"); } catch (e) {}
    logout();
    window.location.href = "/login";
  };

  if (!me) return null;

  return (
    <div className="dash-page">
      <aside className="dash-sidebar">
        <div className="dash-user">
          <div className="dash-avatar">{me.username?.[0]?.toUpperCase()}</div>
          <h3>{me.username}</h3>
          <p>{me.email}</p>
        </div>

        <div style={{ margin: "10px 0" }}>
          <input value={newWsName} onChange={e => setNewWsName(e.target.value)} placeholder="New workspace name" />
          <button onClick={handleCreateWorkspace}>Create Workspace</button>
        </div>

        <div>
          <h4>Your Workspaces</h4>
          <ul>
            {workspaces.map(ws => (
              <li key={ws.id}>
                <button onClick={() => setSelectedWorkspace(ws)} style={{ fontWeight: selectedWorkspace?.id === ws.id ? "bold" : "normal" }}>
                  {ws.name}
                </button>
              </li>
            ))}
          </ul>
        </div>

        <button className="dash-logout" onClick={handleLogout}>Logout</button>
      </aside>

      <main className="dash-main">
        <h1>Workspace: {selectedWorkspace ? selectedWorkspace.name : "— select one"}</h1>

        {selectedWorkspace && (
          <>
            <div>
              <input placeholder="New doc title" value={newDocTitle} onChange={e => setNewDocTitle(e.target.value)} />
              <button onClick={handleCreateDoc}>Create Doc</button>
            </div>

            <h3>Docs</h3>
            <ul>
              {docs.map(d => (
                <li key={d.id}>
                  <span>{d.title}</span>
                  <button style={{ marginLeft: 8 }} onClick={() => openDoc(d)}>Open</button>
                </li>
              ))}
            </ul>
          </>
        )}

      </main>
    </div>
  );
}
