// src/context/AuthContext.jsx
import React, { createContext, useState, useEffect } from "react";
import api from "../api/axios";
import { jwtDecode } from "jwt-decode"; // <- fixed import (default)

export const AuthContext = createContext();

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(() => localStorage.getItem("token"));

  // Load user from token on refresh
  useEffect(() => {
    if (!token) {
      setUser(null);
      return;
    }

    try {
      const decoded = jwtDecode(token);

      // token structure usually has `exp` in seconds
      if (decoded && decoded.exp && decoded.exp * 1000 < Date.now()) {
        logout();
        return;
      }

      setUser({ email: decoded.sub, username: decoded.username ?? null });
    } catch (err) {
      // If decode fails we must remove bad token
      console.warn("Invalid token, logging out", err);
      logout();
    }
  }, [token]);

  // Login
  const login = async (email, password) => {
    try {
      const res = await api.post("/api/auth/login", { email, password });

      const jwt = res.data.token;
      localStorage.setItem("token", jwt);
      setToken(jwt);

      const decoded = jwtDecode(jwt);
      setUser({ email: decoded.sub, username: decoded.username ?? null });

      return { success: true };
    } catch (err) {
      return { success: false, message: err.response?.data || "Login failed" };
    }
  };

  // Register
  const register = async (username, email, password) => {
    try {
      const res = await api.post("/api/auth/register", {
        username,
        email,
        password,
      });

      const jwt = res.data.token;
      localStorage.setItem("token", jwt);
      setToken(jwt);

      const decoded = jwtDecode(jwt);
      setUser({ email: decoded.sub, username });

      return { success: true };
    } catch (err) {
      return {
        success: false,
        message: err.response?.data || "Registration failed",
      };
    }
  };

  // Logout
  const logout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("refreshToken");
    setToken(null);
    setUser(null);
    // don't forcibly redirect here — let callers decide. If you want redirect:
    // window.location.href = "/login";
  };

  return (
    <AuthContext.Provider value={{ user, token, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export default AuthContext;
