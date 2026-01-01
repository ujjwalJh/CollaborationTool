import { useContext, useEffect, useState } from "react";
import { AuthContext } from "../context/AuthContext";
import api from "../api/axios";
import { useNavigate } from "react-router-dom";
import "./Dashboard.css";

export default function Dashboard() {
  const { logout } = useContext(AuthContext);
  const navigate = useNavigate();

  const [me, setMe] = useState(null);
  const [ownedWorkspaces, setOwnedWorkspaces] = useState([]);
  const [memberWorkspaces, setMemberWorkspaces] = useState([]);
  const [selectedWorkspace, setSelectedWorkspace] = useState(null);

  const [docs, setDocs] = useState([]);
  const [newWsName, setNewWsName] = useState("");
  const [newDocTitle, setNewDocTitle] = useState("");
  const [addMemberEmail, setAddMemberEmail] = useState("");
  const [renameName, setRenameName] = useState("");
  const [isRenaming, setIsRenaming] = useState(false);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    setLoading(true);
    api
      .get("/api/auth/me")
      .then((res) => {
        setMe(res.data);
        api
          .get(`/api/workspaces/owner/${res.data.id}`)
          .then((r) => setOwnedWorkspaces(r.data))
          .catch(() => setOwnedWorkspaces([]));
        api
          .get(`/api/workspaces/member/${res.data.id}`)
          .then((r) => {
            const filtered = r.data.filter(ws => ws.owner?.id !== res.data.id);
            setMemberWorkspaces(filtered);
          })        
      })
      .catch(() => {
    
        logout();
        window.location.href = "/login";
      })
      .finally(() => setLoading(false));
    
  }, []);

  
  useEffect(() => {
    if (!selectedWorkspace) {
      setDocs([]);
      return;
    }
    api
      .get(`/api/docs/workspace/${selectedWorkspace.id}`)
      .then((r) => setDocs(r.data))
      .catch((e) => {
        console.error("Failed to load docs:", e);
        setDocs([]);
      });
  }, [selectedWorkspace]);

  
  const refreshWorkspaces = async () => {
    if (!me) return;
    try {
      const [ownedRes, memberRes] = await Promise.all([
        api.get(`/api/workspaces/owner/${me.id}`),
        api.get(`/api/workspaces/member/${me.id}`),
      ]);
      setOwnedWorkspaces(ownedRes.data);
      setMemberWorkspaces(memberRes.data);
      if (
        selectedWorkspace &&
        ![...ownedRes.data, ...memberRes.data].some((w) => w.id === selectedWorkspace.id)
      ) {
        setSelectedWorkspace(null);
      }
    } catch (e) {
      console.error("refreshWorkspaces failed", e);
    }
  };

  const handleCreateWorkspace = async () => {
    if (!newWsName.trim()) return alert("Workspace name required");
    try {
      const res = await api.post("/api/workspaces", { name: newWsName.trim() });
      setOwnedWorkspaces((prev) => [...prev, res.data]);
      setNewWsName("");
    } catch (e) {
      console.error(e);
      alert("Failed to create workspace");
    }
  };

  const handleCreateDoc = async () => {
    if (!newDocTitle.trim() || !selectedWorkspace) return;
    try {
      const res = await api.post(`/api/docs/workspace/${selectedWorkspace.id}`, {
        title: newDocTitle.trim(),
      });
      setDocs((prev) => [...prev, res.data]);
      setNewDocTitle("");
    } catch (e) {
      console.error(e);
      alert("Failed to create document");
    }
  };

  const openDoc = (doc) => {
    navigate(`/editor/${doc.id}`);
  };

  const handleLogout = async () => {
    try {
      await api.post("/api/auth/logout");
    } catch (e) {}
    logout();
    window.location.href = "/login";
  };
  const handleAddMember = async () => {
    if (!addMemberEmail.trim() || !selectedWorkspace) return alert("Provide member email");
    try {
    
      const userRes = await api.get(`/api/users/${encodeURIComponent(addMemberEmail.trim())}`);
      const user = userRes.data;
      await api.post(`/api/workspaces/${selectedWorkspace.id}/addMember/${user.id}`);
      alert("Member added");
      setAddMemberEmail("");
      await refreshWorkspaces();
    
      const refreshed = (await api.get(`/api/workspaces/${selectedWorkspace.id}`)).data;
      setSelectedWorkspace(refreshed);
    } catch (err) {
      console.error("Add member failed", err);
      alert(
        err?.response?.data || "Failed to add member. Ensure user exists and you are the owner."
      );
    }
  };

  
  const handleRemoveMember = async (userId) => {
    if (!selectedWorkspace) return;
    if (!confirm("Remove this member from workspace?")) return;
    try {
      await api.post(`/api/workspaces/${selectedWorkspace.id}/removeMember/${userId}`);
      alert("Member removed");
      await refreshWorkspaces();
      const refreshed = (await api.get(`/api/workspaces/${selectedWorkspace.id}`)).data;
      setSelectedWorkspace(refreshed);
    } catch (e) {
      console.error(e);
      alert("Failed to remove member");
    }
  };

  
  const handleLeaveWorkspace = async () => {
    if (!selectedWorkspace) return;
    if (selectedWorkspace.owner && selectedWorkspace.owner.id === me.id) {
      return alert("Owner cannot leave; transfer ownership first");
    }
    if (!confirm(`Leave workspace "${selectedWorkspace.name}"?`)) return;
    try {
      
      await api.post(
        `/api/workspaces/${selectedWorkspace.id}/removeMember/${me.id}`
      );
      alert("You left the workspace");
      await refreshWorkspaces();
      setSelectedWorkspace(null);
    } catch (e) {
      console.error(e);
      alert("Failed to leave workspace");
    }
  };

  const handleRenameWorkspace = async () => {
    if (!renameName.trim() || !selectedWorkspace) return alert("New name required");
    try {
      const res = await api.put(`/api/workspaces/${selectedWorkspace.id}/rename`, {
        name: renameName.trim(),
      });
      alert("Workspace renamed");
      setIsRenaming(false);
      setRenameName("");
      await refreshWorkspaces();
      setSelectedWorkspace(res.data);
    } catch (e) {
      console.error(e);
      alert("Failed to rename workspace");
    }
  };

  const handleDeleteWorkspace = async () => {
    if (!selectedWorkspace) return;
    if (!confirm(`Permanently delete workspace "${selectedWorkspace.name}"? This is irreversible.`))
      return;
    try {
      await api.delete(`/api/workspaces/${selectedWorkspace.id}`);
      alert("Workspace deleted");
      await refreshWorkspaces();
      setSelectedWorkspace(null);
    } catch (e) {
      console.error(e);
      alert("Failed to delete workspace");
    }
  };
  const isOwner = (ws) => ws && me && ws.owner && ws.owner.id === me.id;
  const isMember = (ws) =>
    ws && ws.members && me && ws.members.some((m) => m.id === me.id);
  if (loading) {
    return <div style={{ padding: 40 }}>Loading...</div>;
  }

  if (!me) {
    return null;
  }

  return (
    <div className="dash-page">
      <aside className="dash-sidebar">
        <div className="dash-user">
          <div className="dash-avatar">{me.username?.[0]?.toUpperCase()}</div>
          <h3>{me.username}</h3>
          <p>{me.email}</p>
        </div>

        <div style={{ margin: "10px 0", width: "100%" }}>
          <input
            style={{ width: "100%", padding: 8, borderRadius: 6, border: "none", marginBottom: 8 }}
            value={newWsName}
            onChange={(e) => setNewWsName(e.target.value)}
            placeholder="New workspace name"
          />
          <button style={{ width: "100%" }} onClick={handleCreateWorkspace}>
            Create Workspace
          </button>
        </div>

        <div style={{ width: "100%", marginTop: 18 }}>
          <h4>Your Workspaces</h4>
          <ul style={{ listStyle: "none", padding: 0 }}>
            {ownedWorkspaces.map((ws) => (
              <li key={ws.id} style={{ marginBottom: 8 }}>
                <button
                  onClick={() => setSelectedWorkspace(ws)}
                  style={{
                    width: "100%",
                    textAlign: "left",
                    fontWeight: selectedWorkspace?.id === ws.id ? "bold" : "normal",
                    background: "transparent",
                    border: "none",
                    color: "inherit",
                    cursor: "pointer",
                    padding: "6px 8px",
                    borderRadius: 6,
                  }}
                >
                  {ws.name} <span style={{ fontSize: 12, color: "#bbb" }}> (owner)</span>
                </button>
              </li>
            ))}
          </ul>
        </div>

        <div style={{ width: "100%", marginTop: 20 }}>
          <h4>Shared With You</h4>
          <ul style={{ listStyle: "none", padding: 0 }}>
            {memberWorkspaces.map((ws) => (
              <li key={ws.id} style={{ marginBottom: 8 }}>
                <button
                  onClick={() => setSelectedWorkspace(ws)}
                  style={{
                    width: "100%",
                    textAlign: "left",
                    fontWeight: selectedWorkspace?.id === ws.id ? "bold" : "normal",
                    background: "transparent",
                    border: "none",
                    color: "inherit",
                    cursor: "pointer",
                    padding: "6px 8px",
                    borderRadius: 6,
                  }}
                >
                  {ws.name} <span style={{ fontSize: 12, color: "#bbb" }}> (member)</span>
                </button>
              </li>
            ))}
          </ul>
        </div>

        <button className="dash-logout" onClick={handleLogout}>
          Logout
        </button>
      </aside>

      <main className="dash-main">
        <h1>Workspace: {selectedWorkspace ? selectedWorkspace.name : "— select one"}</h1>
        {!selectedWorkspace && <p>Select a workspace to view docs and members.</p>}

        {selectedWorkspace && (
          <>
            <div style={{ display: "flex", gap: 12, marginBottom: 16, alignItems: "center" }}>
              {isOwner(selectedWorkspace) ? (
                <>
                  <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
                    <input
                      placeholder="Rename workspace..."
                      value={isRenaming ? renameName : ""}
                      onChange={(e) => setRenameName(e.target.value)}
                      style={{ padding: 8, borderRadius: 6 }}
                    />
                    <button onClick={() => {
                      setIsRenaming(true);
                      setRenameName(selectedWorkspace.name || "");
                    }}>
                      Edit name
                    </button>
                    {isRenaming && (
                      <>
                        <button onClick={handleRenameWorkspace}>Save name</button>
                        <button onClick={() => { setIsRenaming(false); setRenameName(""); }}>Cancel</button>
                      </>
                    )}
                  </div>

                  <div style={{ marginLeft: "auto", display: "flex", gap: 8 }}>
                    <button onClick={() => {
                      const el = document.getElementById("add-member-email");
                      if (el) el.focus();
                    }}>Add member</button>
                    <button onClick={handleDeleteWorkspace} style={{ background: "#b92f2f", color: "white" }}>
                      Delete workspace
                    </button>
                  </div>
                </>
              ) : (
                <>
                  <div style={{ display: "flex", gap: 8 }}>
                    <button onClick={handleLeaveWorkspace} style={{ background: "#ff8a4d" }}>
                      Leave workspace
                    </button>
                  </div>
                </>
              )}
            </div>

            <div style={{ display: "grid", gridTemplateColumns: "360px 1fr", gap: 20 }}>
              
              <div className="dash-card">
                <h3>Members</h3>
                <p style={{ marginTop: 6, marginBottom: 12, color: "#c1c1c1" }}>
                  Owner: <strong>{selectedWorkspace.owner?.username || selectedWorkspace.owner?.email}</strong>
                </p>

                <ul style={{ listStyle: "none", padding: 0 }}>
                  {selectedWorkspace.members?.map((m) => (
                    <li key={m.id} style={{ display: "flex", alignItems: "center", marginBottom: 8 }}>
                      <div style={{ flex: 1 }}>
                        <div style={{ fontWeight: 500 }}>{m.username || m.email}</div>
                        <div style={{ fontSize: 12, color: "#aaa" }}>{m.email}</div>
                      </div>
                      {isOwner(selectedWorkspace) && selectedWorkspace.owner?.id !== m.id && (
                        <button
                          onClick={() => handleRemoveMember(m.id)}
                          style={{
                            marginLeft: 8,
                            background: "#ff5f5f",
                            color: "white",
                            borderRadius: 6,
                            padding: "6px 8px",
                            border: "none",
                            cursor: "pointer",
                          }}
                        >
                          Remove
                        </button>
                      )}
                      {selectedWorkspace.owner?.id === m.id && (
                        <span style={{ marginLeft: 8, fontSize: 12, color: "#bbb" }}>owner</span>
                      )}
                    </li>
                  ))}
                </ul>

              
                {isOwner(selectedWorkspace) && (
                  <div style={{ marginTop: 12 }}>
                    <input
                      id="add-member-email"
                      placeholder="Member email"
                      value={addMemberEmail}
                      onChange={(e) => setAddMemberEmail(e.target.value)}
                      style={{ width: "100%", padding: 8, borderRadius: 6, marginBottom: 8 }}
                    />
                    <button style={{ width: "100%" }} onClick={handleAddMember}>
                      Add member by email
                    </button>
                  </div>
                )}
              </div>

              
              <div>
                <div className="dash-card" style={{ marginBottom: 16 }}>
                  <h3>Documents</h3>
                  <div style={{ display: "flex", gap: 8, marginTop: 8 }}>
                    <input
                      placeholder="New doc title"
                      value={newDocTitle}
                      onChange={(e) => setNewDocTitle(e.target.value)}
                      style={{ flex: 1, padding: 8, borderRadius: 6 }}
                    />
                    <button onClick={handleCreateDoc}>Create Doc</button>
                  </div>
                </div>

                <div className="dash-card">
                  <h3>Your Docs</h3>
                  <ul style={{ listStyle: "none", padding: 0 }}>
                    {docs.map((d) => (
                      <li key={d.id} style={{ display: "flex", alignItems: "center", marginBottom: 8 }}>
                        <div style={{ flex: 1 }}>
                          <div style={{ fontWeight: 500 }}>{d.title}</div>
                          <div style={{ fontSize: 12, color: "#aaa" }}>
                            Updated: {new Date(d.updatedAt).toLocaleString()}
                          </div>
                        </div>
                        <button onClick={() => openDoc(d)} style={{ marginLeft: 8 }}>
                          Open
                        </button>
                      </li>
                    ))}
                    {docs.length === 0 && <li style={{ color: "#bbb" }}>No documents yet</li>}
                  </ul>
                </div>
              </div>
            </div>
          </>
        )}
      </main>
    </div>
  );
}