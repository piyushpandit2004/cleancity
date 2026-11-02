
"use strict";

// DOM Elements
const loginForm = document.getElementById("loginForm");
const passwordInput = document.getElementById("password");
const togglePassword = document.getElementById("togglePassword");
const eyeIcon = document.getElementById("eyeIcon");
const message = document.getElementById("message");
const loginButton = document.getElementById("loginButton");

// Backend Login API
const LOGIN_API_URL = "http://localhost:8080/api/auth/login";

// ------------------------------------
// SHOW / HIDE PASSWORD
// ------------------------------------

if (togglePassword && passwordInput && eyeIcon) {
    togglePassword.addEventListener("click", function () {

        if (passwordInput.type === "password") {

            passwordInput.type = "text";

            togglePassword.setAttribute(
                "aria-label",
                "Hide password"
            );

            togglePassword.setAttribute(
                "aria-pressed",
                "true"
            );

            eyeIcon.innerHTML = `
<path d="M3 3l18 18"/>
    <path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7-10-7-10-7z"/>
    `;

        } else {

            passwordInput.type = "password";

            togglePassword.setAttribute(
                "aria-label",
                "Show password"
            );

            togglePassword.setAttribute(
                "aria-pressed",
                "false"
            );

            eyeIcon.innerHTML = `
    <path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7-10-7-10-7z"/>
    <circle cx="12" cy="12" r="3"/>
    `;
        }
    });
}

// ------------------------------------
// DISPLAY MESSAGE
// ------------------------------------

function showMessage(text, color = "red") {

    if (!message) {
        return;
    }

    message.textContent = text;
    message.style.color = color;
}

// ------------------------------------
// GET ROLE FROM JWT
// ------------------------------------

function getRoleFromToken(token) {

    try {

        const parts = token.split(".");

        if (parts.length !== 3) {
            return null;
        }

        const base64 = parts[1]
            .replace(/-/g, "+")
            .replace(/_/g, "/");

        const paddedBase64 =
            base64 + "=".repeat((4 - base64.length % 4) % 4);

        const binary = atob(paddedBase64);

        const bytes = Uint8Array.from(
            binary,
            character => character.charCodeAt(0)
        );

        const payload = JSON.parse(
            new TextDecoder().decode(bytes)
        );

        let role =
            payload.role ??
            payload.roles ??
            payload.authorities ??
            payload.authority ??
            null;

        if (Array.isArray(role)) {
            role = role[0];
        }

        if (role && typeof role === "object") {
            role = role.authority ?? role.role ?? null;
        }

        if (typeof role !== "string") {
            return null;
        }

        return role
            .replace(/^ROLE_/, "")
            .toUpperCase();

    } catch (error) {

        console.error("JWT role read error:", error);

        return null;
    }
}

// ------------------------------------
// LOGIN
// ------------------------------------

if (loginForm) {

    loginForm.addEventListener("submit", async function (event) {

        event.preventDefault();

        const email = document
            .getElementById("email")
            .value
            .trim();

        const password = passwordInput.value;

        if (!email || !password) {

            showMessage(
                "Please enter email and password."
            );

            return;
        }

        loginButton.disabled = true;
        loginButton.textContent = "Logging in...";

        showMessage("");

        try {

            // Call Spring Boot Login API
            const response = await fetch(LOGIN_API_URL, {

                method: "POST",

                headers: {
                    "Content-Type": "application/json"
                },

                body: JSON.stringify({
                    email: email,
                    password: password
                })
            });

            // Read response safely
            const responseText = await response.text();

            let data = {};

            if (responseText) {

                try {
                    data = JSON.parse(responseText);

                } catch {

                    data = {
                        message: responseText
                    };
                }
            }

            // Handle HTTP errors
            if (!response.ok) {

                if (response.status === 404) {

                    throw new Error(
                        "Login API nahi mili. Backend ka login endpoint check karo: " +
                        LOGIN_API_URL
                    );
                }

                if (response.status === 401) {

                    throw new Error(
                        "Email ya password galat hai."
                    );
                }

                if (response.status === 403) {

                    throw new Error(
                        "Login request forbidden hai. Backend security configuration check karo."
                    );
                }

                throw new Error(
                    data.message ||
                    data.error ||
                    `Login failed. HTTP ${response.status}`
                );
            }

            // Extract JWT token
            const token =
                data.token ??
                data.accessToken ??
                data.jwtToken ??
                null;

            if (!token) {

                console.error(
                    "Login response mein token nahi mila:",
                    data
                );

                throw new Error(
                    "Login response mein JWT token nahi mila. Backend response check karo."
                );
            }

            // Determine role
            let role =
                data.role ??
                data.userRole ??
                null;

            if (Array.isArray(role)) {
                role = role[0];
            }

            if (typeof role === "object" && role !== null) {
                role = role.authority ?? role.role ?? null;
            }

            if (typeof role !== "string" || !role.trim()) {
                role = getRoleFromToken(token);
            }

            role = String(role || "")
                .replace(/^ROLE_/, "")
                .toUpperCase();

            if (!role) {

                console.error(
                    "Login response:",
                    data
                );

                throw new Error(
                    "Login successful, lekin user role nahi mila. Backend login response check karo."
                );
            }

            // Save authentication data
            localStorage.setItem("token", token);

            localStorage.setItem(
                "email",
                data.email || email
            );

            localStorage.setItem("role", role);

            showMessage(
                "Login successful! Opening dashboard...",
                "green"
            );

            // Redirect according to role
            setTimeout(function () {

                if (role === "ADMIN") {

                    // Screenshot ke folder structure ke according
                    window.location.href = "admin.html";

                } else if (role === "WORKER") {

                    window.location.href = "worker-dashboard.html";

                } else if (role === "CITIZEN") {

                    window.location.href = "dashboard.html";

                } else {

                    showMessage(
                        "Unknown user role: " + role
                    );

                    loginButton.disabled = false;
                    loginButton.textContent = "Login →";
                }

            }, 500);

        } catch (error) {

            console.error("Login error:", error);

            if (error instanceof TypeError) {

                showMessage(
                    "Backend se connection nahi ho raha. Spring Boot start hai ya nahi check karo."
                );

            } else {

                showMessage(error.message);
            }

        } finally {

            loginButton.disabled = false;
            loginButton.textContent = "Login →";
        }
    });

} else {

    console.error(
        'Login form nahi mila. login.html mein id="loginForm" check karo.'
    );
}
