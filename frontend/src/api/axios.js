import axios from "axios";
const api = axios.create({
    baseURL: "http://localhost:8080",
    withCredentials: false,
});

const noAuthNeeded = [
    "/api/auth/login",
    "/api/auth/register",
    "/api/auth/refresh"
];

api.interceptors.request.use(
    (config) => {
        if (!noAuthNeeded.some((url) => config.url.includes(url))) {
            const token = localStorage.getItem("token");
            if (token) {
                config.headers["Authorization"] = `Bearer ${token}`;
            }
        }
        config.headers["Content-Type"] = "application/json";
        return config;
    },
    (error) => Promise.reject(error)
);

let isRefreshing = false;
let failedQueue = [];

const processQueue = (error, token = null) => {
    failedQueue.forEach((prom) => {
        if (error) prom.reject(error);
        else prom.resolve(token);
    });

    failedQueue = [];
};

api.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest = error.config;

        const backendMessage =
            error?.response?.data?.message ||
            error?.response?.data?.error ||
            "Something went wrong.";

        if (error.response?.status === 401 && !originalRequest._retry) {
            if (noAuthNeeded.some((url) => originalRequest.url.includes(url))) {
                return Promise.reject(error);
            }

            
            if (isRefreshing) {
                return new Promise((resolve, reject) => {
                    failedQueue.push({ resolve, reject });
                })
                    .then((token) => {
                        originalRequest.headers["Authorization"] = "Bearer " + token;
                        return api(originalRequest);
                    })
                    .catch((err) => Promise.reject(err));
            }

            originalRequest._retry = true;
            isRefreshing = true;

            const refreshToken = localStorage.getItem("refreshToken");
            if (!refreshToken) {
                logoutAndRedirect();
                return Promise.reject(error);
            }

            try {
                const res = await api.post("/api/auth/refresh", {
                    refreshToken,
                });

                const newToken = res.data.token;
                const newRefreshToken = res.data.refreshToken;

                
                localStorage.setItem("token", newToken);
                localStorage.setItem("refreshToken", newRefreshToken);

                api.defaults.headers.common["Authorization"] = "Bearer " + newToken;

                processQueue(null, newToken);

                return api(originalRequest);
            } catch (refreshError) {
                processQueue(refreshError, null);
                logoutAndRedirect();
                return Promise.reject(refreshError);
            } finally {
                isRefreshing = false;
            }
        }


        if (error.response?.status === 403) {
            logoutAndRedirect();
        }

        return Promise.reject({
            message: backendMessage,
            status: error.response?.status || 500,
        });
    }
);
function logoutAndRedirect() {
    localStorage.removeItem("token");
    localStorage.removeItem("refreshToken");
    window.location.href = "/login"; // redirect to login
}

export default api;
