// pages/Register.jsx
import { useState, useContext } from "react";
import { Link, useNavigate } from "react-router-dom";
import { AuthContext } from "../context/AuthContext";
import "./register.css";

export default function Register() {
  const navigate = useNavigate();
  const { register } = useContext(AuthContext);

  const [username, setUsername] = useState("");
  const [email, setEmail]       = useState("");
  const [password, setPassword] = useState("");
  const [error, setError]       = useState("");

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    const res = await register(username, email, password);

    if (res.success) {
      navigate("/dashboard");
    } else {
      setError(res.message || "Registration failed");
    }
  };

  return (
    <div className="register-page">
      <div className="register-card">

        <h2>Create Account</h2>
        <p className="register-subtitle">
          Already have an account? <Link to="/login">Login</Link>
        </p>

        {error && <p className="register-error">{error}</p>}

        <form onSubmit={handleSubmit}>

          {/* Username */}
          <label>Username</label>
          <div className="input-wrapper">
            <span className="input-icon">👤</span>
            <input
              type="text"
              placeholder="Enter username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
            />
          </div>

          {/* Email */}
          <label>Email Address</label>
          <div className="input-wrapper">
            <span className="input-icon">📧</span>
            <input
              type="email"
              placeholder="Enter your email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>

          {/* Password */}
          <label>Password</label>
          <div className="input-wrapper">
            <span className="input-icon">🔒</span>
            <input
              type="password"
              placeholder="Enter your password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>

          <button type="submit">REGISTER</button>

        </form>

      </div>
    </div>
  );
}
