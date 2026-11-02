
const registerForm = document.getElementById("registerForm");
const message = document.getElementById("message");
const registerButton = document.getElementById("registerButton");

// Show custom validation message
function showMessage(text, color = "red") {
    message.textContent = text;
    message.style.color = color;
}

// Email validation
function isValidEmail(email) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

// Password validation
// Requirements:
// 1. At least one uppercase letter (A-Z)
// 2. At least one lowercase letter (a-z)
// 3. At least one digit (0-9)
// 4. At least one special character
// 5. Length between 8 and 20 characters
function isValidPassword(password) {
    const passwordPattern =
        /^(?=.*[A-Z])(?=.*[a-z])(?=.*\d)(?=.*[^A-Za-z0-9\s]).{8,20}$/;

    return passwordPattern.test(password);
}

// Show / Hide Password
document.querySelectorAll(".toggle-password").forEach(button => {
    button.addEventListener("click", function () {
        const input = document.getElementById(this.dataset.target);
        const icon = this.querySelector(".eye-icon");

        const show = input.type === "password";

        input.type = show ? "text" : "password";

        icon.innerHTML = show
            ? `<path d="M3 3l18 18"/><path d="M10.6 10.6a2 2 0 0 0 2.8 2.8"/>`
            : `<path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7-10-7-10-7z"/><circle cx="12" cy="12" r="3"/>`;

        this.setAttribute(
            "aria-label",
            show ? "Hide password" : "Show password"
        );
    });
});

// Disable default browser validation popup
registerForm.setAttribute("novalidate", "true");

// Registration Form Submit
registerForm.addEventListener("submit", async function (event) {
    event.preventDefault();

    // Get input values
    const name = document.getElementById("name").value.trim();
    const email = document.getElementById("email").value.trim();
    const password = document.getElementById("password").value;
    const confirmPassword =
        document.getElementById("confirmPassword").value;

    // 1. Name Validation
    if (!name) {
        showMessage("Please enter your full name.");
        return;
    }

    // 2. Email Validation
    if (!email) {
        showMessage("Please enter your email address.");
        return;
    }

    if (!isValidEmail(email)) {
        showMessage(
            "Please enter a valid email address, e.g., piyush@example.com."
        );
        return;
    }

    // 3. Password Validation
    if (!password) {
        showMessage("Please enter your password.");
        return;
    }

    if (!isValidPassword(password)) {
        showMessage(
            "Password must be 8–20 characters long and include at least one uppercase letter, one lowercase letter, one digit, and one special character."
        );
        return;
    }

    // 4. Confirm Password Validation
    if (!confirmPassword) {
        showMessage("Please confirm your password.");
        return;
    }

    if (password !== confirmPassword) {
        showMessage("Passwords do not match. Please try again.");
        return;
    }

    // Disable button while request is processing
    registerButton.disabled = true;
    registerButton.textContent = "Creating account...";
    showMessage("");

    try {
        // Send registration request to Spring Boot backend
        const response = await fetch(
            "http://localhost:8080/api/auth/register",
            {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    name,
                    email,
                    password
                })
            }
        );

        // Read response safely
        const data = await response.json();

        // Handle backend errors

if (!response.ok) {
    const errorMessage = data.message || "";

    if (/already registered|email already exists|email already in use|duplicate email/i.test(errorMessage)) {
        showMessage(
            "This email is already registered. Please use a different email address or log in to your existing account."
        );
    } else if (/password/i.test(errorMessage)) {
        showMessage(
            "Password must contain at least 1 capital letter (A-Z), " +
            "1 small letter (a-z), 1 digit (0-9), and 1 special character. " +
            "Password length must be between 8 and 20 characters."
        );
    } else {
        showMessage(
            errorMessage || "Registration failed. Please try again."
        );
    }

    return;
}


        // Registration successful
        showMessage(
            "Registration successful! You can now log in.",
            "green"
        );

        // Clear form
        registerForm.reset();

    } catch (error) {
        showMessage(
            error instanceof TypeError
                ? "Cannot connect to backend. Please check the server and CORS settings."
                : error.message
        );
    } finally {
        // Enable button again
        registerButton.disabled = false;
        registerButton.textContent = "Create Account →";
    }
});
