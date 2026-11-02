"use strict";

/*
 * CleanCity - Worker Dashboard
 * Backend default: http://localhost:8080
 */

const API_BASE = "http://localhost:8080/api";

let availableComplaints = [];
let assignedComplaints = [];

let activePhotoUrls = [];

const elements = {
    workerEmail: document.getElementById("workerEmail"),
    logoutBtn: document.getElementById("logoutBtn"),
    availableCount: document.getElementById("availableCount"),
    assignedCount: document.getElementById("assignedCount"),
    progressCount: document.getElementById("progressCount"),
    availableComplaints: document.getElementById("availableComplaints"),
    assignedComplaints: document.getElementById("assignedComplaints"),
    availableSearch: document.getElementById("availableSearch"),
    assignedSearch: document.getElementById("assignedSearch"),
    refreshBtn: document.getElementById("refreshBtn"),
    errorMessage: document.getElementById("errorMessage"),
    successMessage: document.getElementById("successMessage"),
    complaintModal: document.getElementById("complaintModal"),
    modalDetails: document.getElementById("modalDetails"),
    closeModalBtn: document.getElementById("closeModalBtn")
};

/* -------------------- Authentication -------------------- */

function getToken() {
    return localStorage.getItem("token");
}

function getRole() {
    return (localStorage.getItem("role") || "").toUpperCase();
}

function escapeHtml(value) {
    return String(value ?? "").replace(/[&<>"']/g, character => {
        const entities = {
            "&": "&amp;",
            "<": "&lt;",
            ">": "&gt;",
            '"': "&quot;",
            "'": "&#39;"
        };

        return entities[character];
    });
}

function showError(message) {
    if (elements.errorMessage) {
        elements.errorMessage.textContent = message;
        elements.errorMessage.hidden = false;
    }

    if (elements.successMessage) {
        elements.successMessage.hidden = true;
    }
}

function showSuccess(message) {
    if (elements.successMessage) {
        elements.successMessage.textContent = message;
        elements.successMessage.hidden = false;
    }

    if (elements.errorMessage) {
        elements.errorMessage.hidden = true;
    }

    window.setTimeout(() => {
        if (elements.successMessage) {
            elements.successMessage.hidden = true;
        }
    }, 4000);
}

function clearMessages() {
    if (elements.errorMessage) {
        elements.errorMessage.hidden = true;
    }

    if (elements.successMessage) {
        elements.successMessage.hidden = true;
    }
}

function logout() {
    localStorage.removeItem("token");
    localStorage.removeItem("email");
    localStorage.removeItem("role");

    window.location.href = "./login.html";
}

/* -------------------- API Helper -------------------- */

async function apiRequest(path, options = {}) {
    const token = getToken();

    if (!token) {
        throw new Error(
            "Your session was not found. Please log in again."
        );
    }

    const headers = new Headers(options.headers || {});

    headers.set("Authorization", `Bearer ${token}`);

    if (
        options.body &&
        !(options.body instanceof FormData) &&
        !headers.has("Content-Type")
    ) {
        headers.set("Content-Type", "application/json");
    }

    let response;

    try {
        response = await fetch(`${API_BASE}${path}`, {
            ...options,
            headers
        });
    } catch (error) {
        throw new Error(
            "Cannot connect to the server. Please check that the backend is running."
        );
    }

    if (response.status === 401) {
        throw new Error(
            "401 Unauthorized: Your session may have expired. Please log in again."
        );
    }

    if (response.status === 403) {
        throw new Error(
            "403 Forbidden: You do not have permission to access this. Please check your account access."
        );
    }

    if (response.status === 404) {
        throw new Error(
            `404 Not Found: The requested API was not found (${path}). Please check the API endpoint.`
        );
    }

    if (!response.ok) {
        let details = "";

        try {
            details = await response.text();
        } catch (error) {
            // Response body unavailable.
        }

        throw new Error(
            `API error ${response.status}${
                details ? ": " + details.substring(0, 250) : ""
            }`
        );
    }

    if (response.status === 204) {
        return null;
    }

    const contentType = response.headers.get("content-type") || "";

    if (contentType.includes("application/json")) {
        return response.json();
    }

    return response.text();
}

/* -------------------- Authenticated Photo Helper -------------------- */

async function loadAuthenticatedPhoto(photoUrl) {
    const token = getToken();

    if (!token) {
        throw new Error(
            "Your session was not found. Please log in again."
        );
    }

    const response = await fetch(photoUrl, {
        method: "GET",
        headers: {
            Authorization: `Bearer ${token}`
        }
    });

    if (response.status === 401) {
        throw new Error(
            "Your session has expired. Please log in again."
        );
    }

    if (response.status === 403) {
        throw new Error(
            "You do not have permission to view this photo. Please check the complaint assignment."
        );
    }

    if (response.status === 404) {
        throw new Error(
            "The photo for this complaint is not available yet."
        );
    }

    if (!response.ok) {
        throw new Error(
            `Could not load the photo. Error: ${response.status}`
        );
    }

    const blob = await response.blob();

    if (!blob.type.startsWith("image/")) {
        throw new Error(
            "The server did not return a valid image."
        );
    }

    const objectUrl = URL.createObjectURL(blob);

    activePhotoUrls.push(objectUrl);

    return objectUrl;
}

function releasePhotoUrls() {
    activePhotoUrls.forEach(url => URL.revokeObjectURL(url));
    activePhotoUrls = [];
}

/* -------------------- Data Helpers -------------------- */

function normalizeList(data) {
    if (Array.isArray(data)) {
        return data;
    }

    if (data && Array.isArray(data.content)) {
        return data.content;
    }

    if (data && Array.isArray(data.data)) {
        return data.data;
    }

    return [];
}

function getComplaintId(complaint) {
    return complaint.id ?? complaint.complaintId ?? "";
}

function getStatus(complaint) {
    return String(complaint.status || "UNKNOWN").toUpperCase();
}

function statusClass(status) {
    return {
        PENDING: "status-pending",
        ASSIGNED: "status-pending",
        ACCEPTED: "status-pending",
        IN_PROGRESS: "status-in-progress",
        RESOLVED: "status-resolved",
        COMPLETED: "status-resolved",
        REJECTED: "status-rejected"
    }[status] || "";
}

function formatDate(value) {
    if (!value) {
        return "Not available";
    }

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return String(value);
    }

    return date.toLocaleString();
}

function matchesSearch(complaint, query) {
    if (!query) {
        return true;
    }

    const searchableText = [
        getComplaintId(complaint),
        complaint.title,
        complaint.description,
        complaint.location,
        complaint.status,
        complaint.citizenEmail
    ].join(" ").toLowerCase();

    return searchableText.includes(query.toLowerCase());
}

/* -------------------- Load Complaints -------------------- */

async function loadComplaints() {
    clearMessages();

    if (elements.availableComplaints) {
        elements.availableComplaints.innerHTML =
            '<div class="loading">Loading available complaints...</div>';
    }

    if (elements.assignedComplaints) {
        elements.assignedComplaints.innerHTML =
            '<div class="loading">Loading your complaints...</div>';
    }

    if (elements.refreshBtn) {
        elements.refreshBtn.disabled = true;
    }

    try {
        const results = await Promise.allSettled([
            apiRequest("/worker/complaints/available"),
            apiRequest("/worker/complaints")
        ]);

        const availableResult = results[0];
        const assignedResult = results[1];

        if (availableResult.status === "fulfilled") {
            availableComplaints = normalizeList(
                availableResult.value
            );
        } else {
            availableComplaints = [];

            if (elements.availableComplaints) {
                elements.availableComplaints.innerHTML =
                    '<div class="empty-state">Could not load available complaints.</div>';
            }

            showError(availableResult.reason.message);
        }

        if (assignedResult.status === "fulfilled") {
            assignedComplaints = normalizeList(
                assignedResult.value
            );
        } else {
            assignedComplaints = [];

            if (elements.assignedComplaints) {
                elements.assignedComplaints.innerHTML =
                    '<div class="empty-state">Could not load assigned complaints.</div>';
            }

            showError(assignedResult.reason.message);
        }

        updateStats();
        renderAvailableComplaints();
        renderAssignedComplaints();

        if (
            availableResult.status === "fulfilled" &&
            assignedResult.status === "fulfilled"
        ) {
            clearMessages();
        }
    } catch (error) {
        showError(error.message);
    } finally {
        if (elements.refreshBtn) {
            elements.refreshBtn.disabled = false;
        }
    }
}

/* -------------------- Statistics -------------------- */

function updateStats() {
    if (elements.availableCount) {
        elements.availableCount.textContent =
            availableComplaints.length;
    }

    if (elements.assignedCount) {
        elements.assignedCount.textContent =
            assignedComplaints.length;
    }

    if (elements.progressCount) {
        elements.progressCount.textContent =
            assignedComplaints.filter(
                complaint => getStatus(complaint) === "IN_PROGRESS"
            ).length;
    }
}

/* -------------------- Complaint Card -------------------- */

function createComplaintCard(complaint, type) {
    const id = getComplaintId(complaint);
    const status = getStatus(complaint);

    const card = document.createElement("article");
    card.className = "complaint-card";

    const title = escapeHtml(
        complaint.title || "Untitled complaint"
    );

    const description = escapeHtml(
        complaint.description || "No description provided."
    );

    const location = escapeHtml(
        complaint.location || "Not specified"
    );

    const citizen = escapeHtml(
        complaint.citizenEmail || "Not available"
    );

    let actions = "";

    if (type === "available") {
        actions = `
            <div class="card-actions">
                <button
                    class="action-btn secondary-btn"
                    data-action="view"
                    data-id="${escapeHtml(id)}">
                    View Details
                </button>

                <button
                    class="action-btn primary-btn"
                    data-action="accept"
                    data-id="${escapeHtml(id)}">
                    Accept Complaint
                </button>
            </div>
        `;
    } else if (
        status === "ASSIGNED" ||
        status === "ACCEPTED" ||
        status === "PENDING"
    ) {
        actions = `
            <div class="card-actions">
                <button
                    class="action-btn secondary-btn"
                    data-action="view"
                    data-id="${escapeHtml(id)}">
                    View Details
                </button>

                <button
                    class="action-btn primary-btn"
                    data-action="start"
                    data-id="${escapeHtml(id)}">
                    Start Work
                </button>
            </div>
        `;
    } else if (status === "IN_PROGRESS") {
        actions = `
            <div class="card-actions">
                <button
                    class="action-btn secondary-btn"
                    data-action="view"
                    data-id="${escapeHtml(id)}">
                    View Details
                </button>
            </div>

            <div class="photo-upload">
                <label for="photo-${escapeHtml(id)}">
                    Upload completion photo
                </label>

                <input
                    type="file"
                    id="photo-${escapeHtml(id)}"
                    accept="image/jpeg,image/png,image/webp"
                >

                <button
                    class="action-btn primary-btn"
                    data-action="complete"
                    data-id="${escapeHtml(id)}">
                    Complete Work
                </button>
            </div>
        `;
    } else {
        actions = `
            <div class="card-actions">
                <button
                    class="action-btn secondary-btn"
                    data-action="view"
                    data-id="${escapeHtml(id)}">
                    View Details
                </button>
            </div>
        `;
    }

    card.innerHTML = `
        <h3>${title}</h3>

        <p class="complaint-id">
            Complaint ID: ${escapeHtml(id)}
        </p>

        <p>
            <strong>Description:</strong>
            ${description}
        </p>

        <p>
            <strong>Location:</strong>
            ${location}
        </p>

        <p>
            <strong>Citizen:</strong>
            ${citizen}
        </p>

        <p>
            <strong>Status:</strong><br>
            <span class="status-badge ${statusClass(status)}">
                ${escapeHtml(status.replaceAll("_", " "))}
            </span>
        </p>

        <p>
            <strong>Created:</strong>
            ${escapeHtml(formatDate(complaint.createdAt))}
        </p>

        ${actions}
    `;

    return card;
}
/* -------------------- Render Available -------------------- */

function renderAvailableComplaints() {
    if (!elements.availableComplaints) {
        return;
    }

    const query = elements.availableSearch
        ? elements.availableSearch.value.trim()
        : "";

    const filtered = availableComplaints.filter(complaint =>
        matchesSearch(complaint, query)
    );

    elements.availableComplaints.replaceChildren();

    if (filtered.length === 0) {
        elements.availableComplaints.innerHTML = `
            <div class="empty-state">
                ${
            query
                ? "No complaints found for this location."
                : "No complaints are available right now."
        }
            </div>
        `;

        return;
    }

    filtered.forEach(complaint => {
        elements.availableComplaints.appendChild(
            createComplaintCard(complaint, "available")
        );
    });
}

/* -------------------- Render Assigned -------------------- */

function renderAssignedComplaints() {
    if (!elements.assignedComplaints) {
        return;
    }

    const query = elements.assignedSearch
        ? elements.assignedSearch.value.trim()
        : "";

    const filtered = assignedComplaints.filter(complaint =>
        matchesSearch(complaint, query)
    );

    elements.assignedComplaints.replaceChildren();

    if (filtered.length === 0) {
        elements.assignedComplaints.innerHTML = `
            <div class="empty-state">
                ${
            query
                ? "No complaints found for this location."
                : "No complaints have been assigned to you yet."
        }
            </div>
        `;

        return;
    }

    filtered.forEach(complaint => {
        elements.assignedComplaints.appendChild(
            createComplaintCard(complaint, "assigned")
        );
    });
}

/* -------------------- Accept Complaint -------------------- */

async function acceptComplaint(id) {
    if (!confirm(`Do you want to accept complaint #${id}?`)) {
        return;
    }

    clearMessages();

    try {
        await apiRequest(
            `/worker/complaints/${encodeURIComponent(id)}/accept`,
            {
                method: "PUT"
            }
        );

        showSuccess(
            `Complaint #${id} accepted successfully.`
        );

        await loadComplaints();
    } catch (error) {
        showError(error.message);
    }
}

/* -------------------- Start Work -------------------- */

async function startWork(id) {
    if (!confirm(`Do you want to start work on complaint #${id}?`)) {
        return;
    }

    clearMessages();

    try {
        await apiRequest(
            `/worker/complaints/${encodeURIComponent(id)}/status?status=IN_PROGRESS`,
            {
                method: "PUT"
            }
        );

        showSuccess(
            `Work started for complaint #${id}.`
        );

        await loadComplaints();
    } catch (error) {
        showError(error.message);
    }
}

/* -------------------- Complete Work + Photo -------------------- */

async function completeComplaint(id) {
    const input = document.getElementById(`photo-${id}`);
    const file = input?.files?.[0];

    if (!file) {
        showError(
            "Please select a completion photo first."
        );
        return;
    }

    const allowedTypes = [
        "image/jpeg",
        "image/png",
        "image/webp"
    ];

    if (!allowedTypes.includes(file.type)) {
        showError(
            "Please upload a JPG, PNG, or WEBP image only."
        );
        return;
    }

    if (file.size > 2 * 1024 * 1024) {
        showError(
            "Photo size must be less than 2 MB."
        );
        return;
    }

    if (!confirm(`Do you want to mark complaint #${id} as complete?`)) {
        return;
    }

    clearMessages();

    const formData = new FormData();
    formData.append("photo", file);

    try {
        await apiRequest(
            `/worker/complaints/${encodeURIComponent(id)}/complete`,
            {
                method: "PUT",
                body: formData
            }
        );

        showSuccess(
            `Complaint #${id} completed successfully. The completion photo was uploaded.`
        );

        await loadComplaints();
    } catch (error) {
        showError(error.message);
    }
}

/* -------------------- Find Complaint -------------------- */

function findComplaint(id) {
    return [
        ...availableComplaints,
        ...assignedComplaints
    ].find(
        complaint =>
            String(getComplaintId(complaint)) === String(id)
    );
}

/* -------------------- Modal Detail Helper -------------------- */

function addModalDetail(label, value) {
    if (
        value === null ||
        value === undefined ||
        value === ""
    ) {
        return;
    }

    const row = document.createElement("div");
    row.className = "modal-detail-row";

    const heading = document.createElement("strong");
    heading.textContent = label;

    const content = document.createElement("span");
    content.textContent = String(value);

    row.append(heading, content);

    elements.modalDetails.appendChild(row);
}

/* -------------------- Modal Photo Helper -------------------- */

async function addModalPhoto(label, photoUrl) {
    const section = document.createElement("div");
    section.className = "modal-photo-section";

    const heading = document.createElement("h4");
    heading.textContent = label;

    const message = document.createElement("p");
    message.textContent = "Loading photo...";

    section.append(heading, message);
    elements.modalDetails.appendChild(section);

    try {
        const objectUrl = await loadAuthenticatedPhoto(photoUrl);

        if (!elements.complaintModal.classList.contains("open")) {
            return;
        }

        const image = document.createElement("img");

        image.src = objectUrl;
        image.alt = label;
        image.loading = "lazy";

        image.style.display = "block";
        image.style.width = "100%";
        image.style.maxWidth = "500px";
        image.style.maxHeight = "350px";
        image.style.objectFit = "contain";
        image.style.marginTop = "10px";
        image.style.borderRadius = "8px";

        message.remove();
        section.appendChild(image);
    } catch (error) {
        message.textContent = error.message;
        message.className = "photo-error";
    }
}

/* -------------------- View Complaint Details -------------------- */

async function viewComplaint(id) {
    const complaint = findComplaint(id);

    if (!complaint) {
        showError(
            "Complaint details not found. Please refresh the dashboard."
        );

        return;
    }

    if (!elements.complaintModal || !elements.modalDetails) {
        showError(
            "Complaint details window was not found on the page."
        );
        return;
    }

    releasePhotoUrls();

    const fields = [
        ["Complaint ID", getComplaintId(complaint)],
        ["Title", complaint.title],
        ["Description", complaint.description],
        ["Location", complaint.location],
        ["Status", complaint.status],
        ["Citizen Email", complaint.citizenEmail],
        ["Assigned Worker", complaint.assignedWorkerEmail],
        ["Rejection Reason", complaint.rejectionReason],
        ["Created At", formatDate(complaint.createdAt)],
        ["Updated At", formatDate(complaint.updatedAt)],
        ["Resolved At", formatDate(complaint.resolvedAt)]
    ];

    elements.modalDetails.replaceChildren();

    fields.forEach(([label, value]) => {
        addModalDetail(label, value);
    });

    elements.complaintModal.classList.add("open");
    elements.complaintModal.setAttribute("aria-hidden", "false");

    const idValue = encodeURIComponent(getComplaintId(complaint));
    const status = getStatus(complaint);

    const isAssignedComplaint = assignedComplaints.some(
        item =>
            String(getComplaintId(item)) ===
            String(getComplaintId(complaint))
    );

    if (isAssignedComplaint) {
        await addModalPhoto(
            "Original Complaint Photo",
            `${API_BASE}/worker/complaints/${idValue}/original-photo`
        );
    } else {
        const note = document.createElement("p");
        note.textContent =
            "Please accept the complaint first to view the original photo.";

        elements.modalDetails.appendChild(note);
    }

    if (
        status === "RESOLVED" ||
        status === "COMPLETED" ||
        complaint.completionPhotoUrl
    ) {
        await addModalPhoto(
            "Cleaning Completion Photo",
            `${API_BASE}/worker/complaints/${idValue}/completion-photo`
        );
    }
}

/* -------------------- Close Modal -------------------- */

function closeModal() {
    if (!elements.complaintModal) {
        return;
    }

    elements.complaintModal.classList.remove("open");
    elements.complaintModal.setAttribute("aria-hidden", "true");

    releasePhotoUrls();
}

/* -------------------- Event Listeners -------------------- */

function handleComplaintAction(event) {
    const button = event.target.closest("button[data-action]");

    if (!button) {
        return;
    }

    const id = button.dataset.id;
    const action = button.dataset.action;

    if (action === "view") {
        viewComplaint(id);
    } else if (action === "accept") {
        acceptComplaint(id);
    } else if (action === "start") {
        startWork(id);
    } else if (action === "complete") {
        completeComplaint(id);
    }
}

if (elements.availableSearch) {
    elements.availableSearch.addEventListener(
        "input",
        renderAvailableComplaints
    );
}

if (elements.assignedSearch) {
    elements.assignedSearch.addEventListener(
        "input",
        renderAssignedComplaints
    );
}

if (elements.availableComplaints) {
    elements.availableComplaints.addEventListener(
        "click",
        handleComplaintAction
    );
}

if (elements.assignedComplaints) {
    elements.assignedComplaints.addEventListener(
        "click",
        handleComplaintAction
    );
}

if (elements.refreshBtn) {
    elements.refreshBtn.addEventListener(
        "click",
        loadComplaints
    );
}

if (elements.logoutBtn) {
    elements.logoutBtn.addEventListener("click", logout);
}

if (elements.closeModalBtn) {
    elements.closeModalBtn.addEventListener("click", closeModal);
}

if (elements.complaintModal) {
    elements.complaintModal.addEventListener("click", event => {
        if (event.target === elements.complaintModal) {
            closeModal();
        }
    });
}

document.addEventListener("keydown", event => {
    if (event.key === "Escape") {
        closeModal();
    }
});

/* -------------------- Initialize Dashboard -------------------- */

function initializeDashboard() {
    const token = getToken();
    const role = getRole();

    if (!token) {
        window.location.href = "./login.html";
        return;
    }

    if (role !== "WORKER") {
        showError(
            "This dashboard is for WORKER accounts only. Please log in with a Worker account."
        );

        return;
    }

    if (elements.workerEmail) {
        elements.workerEmail.textContent =
            localStorage.getItem("email") || "Worker";
    }

    loadComplaints();
}

initializeDashboard();