import { BrowserRouter, Routes, Route } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";
import Login from "./pages/Login";
import Register from "./pages/Register";
import Dashboard from "./pages/Dashboard";
import Editor from "./pages/EditorPage";
import EditorPage from "./pages/EditorPage";

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/editor" element={<EditorPage />} />
          <Route path="/editor/:docId" element={<EditorPage />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;
