// pages/Login.jsx
import { useState, useContext } from "react";
import { Link, useNavigate } from "react-router-dom";
import { AuthContext } from "../context/AuthContext";
import "./login.css";  // Updated CSS file name

export default function Login() {
  const navigate = useNavigate();
  const { login } = useContext(AuthContext);

  const [form, setForm] = useState({
    email: "",
    password: "",
  });

  const [error, setError] = useState("");

  const handleChange = (e) =>
    setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    const res = await login(form.email, form.password);

    if (res.success) {
      navigate("/dashboard");
    } else {
      setError(res.message || "Login failed");
    }
  };

  return (
    <div className="login-page">
      <div className="login-card">

        <h2>Welcome Back</h2>
        <p className="login-subtitle">
          New user? <Link to="/register">Create an account</Link>
        </p>

        {error && <p className="login-error">{error}</p>}

        <form onSubmit={handleSubmit}>
          
          {/* Username/Email */}
          <label>Email Address</label>
          <div className="input-wrapper">
            <span className="input-icon">📧</span>
            <input
              type="email"
              name="email"
              value={form.email}
              onChange={handleChange}
              placeholder="Enter your email"
              required
            />
          </div>

          {/* Password */}
          <label>Password</label>
          <div className="input-wrapper">
            <span className="input-icon">🔒</span>
            <input
              type="password"
              name="password"
              value={form.password}
              onChange={handleChange}
              placeholder="Enter your password"
              required
            />
          </div>

          <div className="login-options">
            <label>
              <input type="checkbox" /> Remember me
            </label>
            <a href="#">Forgot Password?</a>
          </div>

          <button type="submit">LOGIN</button>
        </form>
      </div>
    </div>
  );
}
