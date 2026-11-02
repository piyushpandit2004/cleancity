"use strict";



const API_BASE_URL = "http://localhost:8080";

/* --------------------------------
   TOKEN MANAGEMENT
--------------------------------- */

function getToken() {
    const possibleKeys = [
        "token",
        "jwtToken",
        "accessToken",
        "jwt",
        "authToken"
    ];

    for (const key of possibleKeys) {
        const value = localStorage.getItem(key);

        if (value) {
            return value;
        }
    }

    return null;
}

function getCurrentUserRole() {
    return (
        localStorage.getItem("role") ||
        localStorage.getItem("userRole") ||
        ""
    ).toUpperCase();
}

/* --------------------------------
   APPLICATION STATE
--------------------------------- */

let allComplaints = [];
let statusChart = null;
let barChart = null;
let messageTimeout = null;

/* --------------------------------
   API HELPER
--------------------------------- */

async function apiRequest(endpoint, options = {}) {
    const token = getToken();

    const headers = {
        ...(options.headers || {})
    };

    if (token) {
        headers.Authorization = `Bearer ${token}`;
    }

    const response = await fetch(
        `${API_BASE_URL}${endpoint}`,
        {
            ...options,
            headers
        }
    );

    if (response.status === 401 || response.status === 403) {
        throw new Error(
            response.status === 401
                ? "Login session invalid hai. Dobara login karo."
                : "Access denied. Admin role aur JWT token check karo."
        );
    }

    if (!response.ok) {
        const errorText = await response.text();

        throw new Error(
            errorText || `Request failed: ${response.status}`
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

/* --------------------------------
   INITIALIZATION
--------------------------------- */

document.addEventListener("DOMContentLoaded", () => {
    document
        .getElementById("refreshBtn")
        .addEventListener("click", loadDashboard);

    document
        .getElementById("searchBtn")
        .addEventListener("click", applyFilters);

    document
        .getElementById("searchInput")
        .addEventListener("input", applyFilters);

    document
        .getElementById("statusFilter")
        .addEventListener("change", applyFilters);

    document
        .getElementById("logoutBtn")
        .addEventListener("click", logout);

    document
        .getElementById("closeModalBtn")
        .addEventListener("click", closeModal);

    document
        .getElementById("detailsModal")
        .addEventListener("click", event => {
            if (event.target.id === "detailsModal") {
                closeModal();
            }
        });

    const token = getToken();
    const role = getCurrentUserRole();

    if (!token) {
        showMessage(
            "Login token nahi mila. Pehle admin login karo.",
            "error"
        );

        document.getElementById("complaintsTableBody").innerHTML = `
            <tr>
                <td colspan="8" class="empty-cell">
                    Please login to load dashboard data.
                </td>
            </tr>
        `;

        return;
    }

    if (role && role !== "ADMIN") {
        showMessage(
            "Ye dashboard sirf ADMIN role ke liye hai.",
            "error"
        );

        return;
    }

    loadDashboard();
});

/* --------------------------------
   LOAD DASHBOARD
--------------------------------- */

async function loadDashboard() {
    const refreshButton = document.getElementById("refreshBtn");

    refreshButton.disabled = true;
    refreshButton.textContent = "Loading...";

    try {
        const [statisticsResult, complaintsResult] =
            await Promise.allSettled([
                apiRequest("/api/admin/complaints/statistics"),
                apiRequest("/api/admin/complaints")
            ]);

        let successCount = 0;

        if (statisticsResult.status === "fulfilled") {
            renderStatistics(statisticsResult.value);
            successCount++;
        } else {
            console.error(
                "Statistics API error:",
                statisticsResult.reason
            );
        }

        if (complaintsResult.status === "fulfilled") {
            allComplaints = extractComplaints(
                complaintsResult.value
            );

            applyFilters();
            successCount++;
        } else {
            console.error(
                "Complaints API error:",
                complaintsResult.reason
            );

            document.getElementById(
                "complaintsTableBody"
            ).innerHTML = `
                <tr>
                    <td colspan="8" class="empty-cell">
                        Complaints load nahi hui.
                        API endpoint aur backend logs check karo.
                    </td>
                </tr>
            `;
        }

        if (successCount === 2) {
            showMessage(
                "Dashboard successfully updated.",
                "success"
            );
        } else if (successCount === 1) {
            showMessage(
                "Dashboard partially loaded. Console mein API error check karo.",
                "error"
            );
        } else {
            showMessage(
                "Dashboard load nahi hua. Backend aur JWT check karo.",
                "error"
            );
        }

        document.getElementById("lastUpdated").textContent =
            "Last updated: " +
            new Date().toLocaleTimeString();

    } catch (error) {
        console.error(error);
        showMessage(error.message, "error");

    } finally {
        refreshButton.disabled = false;
        refreshButton.textContent = "↻ Refresh Data";
    }
}

/* --------------------------------
   STATISTICS
--------------------------------- */

function readNumber(data, ...keys) {
    for (const key of keys) {
        if (
            data &&
            data[key] !== undefined &&
            data[key] !== null
        ) {
            const value = Number(data[key]);

            if (Number.isFinite(value)) {
                return value;
            }
        }
    }

    return 0;
}

function renderStatistics(data) {
    const statistics = {
        total: readNumber(
            data,
            "totalComplaints",
            "total"
        ),

        pending: readNumber(
            data,
            "pendingComplaints",
            "pending"
        ),

        assigned: readNumber(
            data,
            "assignedComplaints",
            "assigned"
        ),

        inProgress: readNumber(
            data,
            "inProgressComplaints",
            "inProgress"
        ),

        resolved: readNumber(
            data,
            "resolvedComplaints",
            "resolved"
        ),

        rejected: readNumber(
            data,
            "rejectedComplaints",
            "rejected"
        )
    };

    document.getElementById("totalComplaints").textContent =
        statistics.total;

    document.getElementById("pendingComplaints").textContent =
        statistics.pending;

    document.getElementById("assignedComplaints").textContent =
        statistics.assigned;

    document.getElementById("inProgressComplaints").textContent =
        statistics.inProgress;

    document.getElementById("resolvedComplaints").textContent =
        statistics.resolved;

    document.getElementById("rejectedComplaints").textContent =
        statistics.rejected;

    renderCharts(statistics);
}

/* --------------------------------
   CHARTS
--------------------------------- */

function renderCharts(statistics) {
    const labels = [
        "Pending",
        "Assigned",
        "In Progress",
        "Resolved",
        "Rejected"
    ];

    const values = [
        statistics.pending,
        statistics.assigned,
        statistics.inProgress,
        statistics.resolved,
        statistics.rejected
    ];

    if (statusChart) {
        statusChart.destroy();
    }

    if (barChart) {
        barChart.destroy();
    }

    const statusCanvas = document.getElementById("statusChart");
    const barCanvas = document.getElementById("barChart");

    statusChart = new Chart(statusCanvas, {
        type: "doughnut",

        data: {
            labels,

            datasets: [{
                data: values,

                backgroundColor: [
                    "#f59e0b",
                    "#8b5cf6",
                    "#3b82f6",
                    "#22c55e",
                    "#ef4444"
                ],

                borderWidth: 2,
                borderColor: "#ffffff"
            }]
        },

        options: {
            responsive: true,
            maintainAspectRatio: false,

            plugins: {
                legend: {
                    position: "bottom"
                }
            }
        }
    });

    barChart = new Chart(barCanvas, {
        type: "bar",

        data: {
            labels,

            datasets: [{
                label: "Complaints",
                data: values,

                backgroundColor: [
                    "#f59e0b",
                    "#8b5cf6",
                    "#3b82f6",
                    "#22c55e",
                    "#ef4444"
                ],

                borderRadius: 6
            }]
        },

        options: {
            responsive: true,
            maintainAspectRatio: false,

            scales: {
                y: {
                    beginAtZero: true,

                    ticks: {
                        precision: 0
                    }
                }
            },

            plugins: {
                legend: {
                    display: false
                }
            }
        }
    });
}

/* --------------------------------
   COMPLAINT RESPONSE HANDLING
--------------------------------- */

function extractComplaints(result) {
    if (Array.isArray(result)) {
        return result;
    }

    if (result && Array.isArray(result.content)) {
        return result.content;
    }

    if (result && Array.isArray(result.data)) {
        return result.data;
    }

    if (result && Array.isArray(result.complaints)) {
        return result.complaints;
    }

    return [];
}

function getComplaintId(complaint) {
    return complaint.id ?? complaint.complaintId ?? "";
}

function getComplaintTitle(complaint) {
    return complaint.title ?? complaint.subject ?? "Untitled";
}

function getComplaintLocation(complaint) {
    return complaint.location ?? "Not provided";
}

function getComplaintStatus(complaint) {
    return String(complaint.status ?? "PENDING").toUpperCase();
}

function getCitizenName(complaint) {
    const citizen = complaint.citizen;

    if (typeof citizen === "string") {
        return citizen;
    }

    return citizen?.name ||
        citizen?.fullName ||
        citizen?.email ||
        complaint.citizenName ||
        complaint.citizenEmail ||
        "Unknown";
}

function getWorkerName(complaint) {
    const worker = complaint.assignedWorker;

    if (typeof worker === "string") {
        return worker;
    }

    return worker?.name ||
        worker?.fullName ||
        worker?.email ||
        complaint.workerName ||
        complaint.assignedWorkerEmail ||
        "Not assigned";
}

function getComplaintDate(complaint) {
    const value =
        complaint.createdAt ??
        complaint.createdDate ??
        complaint.date;

    if (!value) {
        return "—";
    }

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return String(value);
    }

    return date.toLocaleDateString();
}

/* --------------------------------
   FILTERS
--------------------------------- */

function applyFilters() {
    const search = document
        .getElementById("searchInput")
        .value
        .trim()
        .toLowerCase();

    const selectedStatus = document
        .getElementById("statusFilter")
        .value;

    const filtered = allComplaints.filter(complaint => {
        const id = String(getComplaintId(complaint));

        const title = getComplaintTitle(complaint).toLowerCase();

        const location =
            getComplaintLocation(complaint).toLowerCase();

        const matchesSearch =
            id.toLowerCase().includes(search) ||
            title.includes(search) ||
            location.includes(search);

        const matchesStatus =
            selectedStatus === "ALL" ||
            getComplaintStatus(complaint) === selectedStatus;

        return matchesSearch && matchesStatus;
    });

    renderComplaints(filtered);
}

/* --------------------------------
   HTML ESCAPING
--------------------------------- */

function escapeHTML(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

/* --------------------------------
   RENDER COMPLAINTS TABLE
   Desktop: normal table
   Mobile: cards using data-label
--------------------------------- */

function renderComplaints(complaints) {
    const tbody = document.getElementById(
        "complaintsTableBody"
    );

    const complaintCount = document.getElementById(
        "complaintCount"
    );

    if (!tbody) {
        console.error("complaintsTableBody element nahi mila.");
        return;
    }

    if (!Array.isArray(complaints) || complaints.length === 0) {
        if (complaintCount) {
            complaintCount.textContent = "0 complaints";
        }

        tbody.innerHTML = `
            <tr>
                <td colspan="8" class="empty-cell">
                    No complaints found.
                </td>
            </tr>
        `;

        return;
    }

    if (complaintCount) {
        complaintCount.textContent =
            `${complaints.length} complaints`;
    }

    tbody.innerHTML = complaints.map(complaint => {
        const id = getComplaintId(complaint);
        const escapedId = escapeHTML(id);

        const status = getComplaintStatus(complaint);
        const statusClass = status.toLowerCase();

        const title = escapeHTML(
            getComplaintTitle(complaint)
        );

        const location = escapeHTML(
            getComplaintLocation(complaint)
        );

        const citizen = escapeHTML(
            getCitizenName(complaint)
        );

        const worker = escapeHTML(
            getWorkerName(complaint)
        );

        const date = escapeHTML(
            getComplaintDate(complaint)
        );

        const escapedStatus = escapeHTML(status);

        let actions = `
            <button
                type="button"
                class="action-btn"
                data-action="details"
                data-id="${escapedId}">
                View
            </button>

            <button
                type="button"
                class="action-btn"
                data-action="edit"
                data-id="${escapedId}">
                Edit
            </button>

            <button
                type="button"
                class="action-btn"
                data-action="changeStatus"
                data-id="${escapedId}">
                Change Status
            </button>
        `;

        if (status === "PENDING") {
            actions += `
                <button
                    type="button"
                    class="action-btn action-assign"
                    data-action="assign"
                    data-id="${escapedId}">
                    Assign
                </button>

                <button
                    type="button"
                    class="action-btn action-reject"
                    data-action="reject"
                    data-id="${escapedId}">
                    Reject
                </button>
            `;
        }

        return `
            <tr>
                <td data-label="Complaint ID">
                    #${escapedId}
                </td>

                <td data-label="Title">
                    <div class="complaint-title">
                        ${title}
                    </div>
                </td>

                <td data-label="Location">
                    ${location}
                </td>

                <td data-label="Citizen">
                    ${citizen}
                </td>

                <td data-label="Assigned Worker">
                    ${worker}
                </td>

                <td data-label="Status">
                    <span class="status-badge status-${statusClass}">
                        ${escapedStatus.replaceAll("_", " ")}
                    </span>
                </td>

                <td data-label="Created Date">
                    ${date}
                </td>

                <td data-label="Actions">
                    <div class="action-buttons">
                        ${actions}
                    </div>
                </td>
            </tr>
        `;
    }).join("");
}

/* --------------------------------
   TABLE ACTIONS
--------------------------------- */

document
    .getElementById("complaintsTableBody")
    .addEventListener("click", async event => {

        const button = event.target.closest("button[data-action]");

        if (!button) {
            return;
        }

        const id = button.dataset.id;
        const action = button.dataset.action;

        const complaint = allComplaints.find(item =>
            String(getComplaintId(item)) === String(id)
        );

        if (!complaint) {
            showMessage("Complaint nahi mili.", "error");
            return;
        }

        if (action === "details") {
            showComplaintDetails(complaint);
        }

        if (action === "assign") {
            await assignWorker(id);
        }

        if (action === "reject") {
            await rejectComplaint(id);
        }

        if (action === "edit") {
            await editComplaint(complaint);
        }

        if (action === "changeStatus") {
            await changeComplaintStatus(complaint);
        }
    });

/* --------------------------------
   ASSIGN WORKER
--------------------------------- */

async function assignWorker(complaintId) {
    const workerEmail = prompt(
        "Worker ka registered email address enter karo:"
    );

    if (!workerEmail) {
        return;
    }

    const email = workerEmail.trim();

    if (!email || !email.includes("@")) {
        showMessage(
            "Valid worker email enter karo.",
            "error"
        );

        return;
    }

    const confirmed = confirm(
        `Kya tum complaint #${complaintId} ko ${email} assign karna chahte ho?`
    );

    if (!confirmed) {
        return;
    }

    try {
        const endpoint =
            `/api/admin/complaints/${encodeURIComponent(complaintId)}` +
            `/assign?workerEmail=${encodeURIComponent(email)}`;

        await apiRequest(endpoint, {
            method: "PUT"
        });

        showMessage(
            "Worker successfully assigned.",
            "success"
        );

        await loadDashboard();

    } catch (error) {
        console.error(error);
        showMessage(error.message, "error");
    }
}

/* --------------------------------
   EDIT COMPLAINT DETAILS (ADMIN)
--------------------------------- */

async function editComplaint(complaint) {
    const complaintId = getComplaintId(complaint);

    const title = prompt(
        "Complaint title:",
        getComplaintTitle(complaint)
    );

    if (title === null) {
        return;
    }

    const description = prompt(
        "Complaint description:",
        complaint.description || ""
    );

    if (description === null) {
        return;
    }

    const location = prompt(
        "Complaint location:",
        getComplaintLocation(complaint)
    );

    if (location === null) {
        return;
    }

    if (
        !title.trim() ||
        !description.trim() ||
        !location.trim()
    ) {
        showMessage(
            "Title, description aur location required hain.",
            "error"
        );

        return;
    }

    const confirmed = confirm(
        `Kya tum complaint #${complaintId} update karna chahte ho?`
    );

    if (!confirmed) {
        return;
    }

    try {
        await apiRequest(
            `/api/admin/complaints/${encodeURIComponent(complaintId)}/edit`,
            {
                method: "PUT",

                headers: {
                    "Content-Type": "application/json"
                },

                body: JSON.stringify({
                    title: title.trim(),
                    description: description.trim(),
                    location: location.trim()
                })
            }
        );

        showMessage(
            "Complaint successfully updated.",
            "success"
        );

        await loadDashboard();

    } catch (error) {
        console.error(error);
        showMessage(error.message, "error");
    }
}
/* --------------------------------
   CHANGE COMPLAINT STATUS (ADMIN)
--------------------------------- */

async function changeComplaintStatus(complaint) {
    const complaintId = getComplaintId(complaint);
    const currentStatus = getComplaintStatus(complaint);

    const allowedStatuses = [
        "PENDING",
        "ASSIGNED",
        "IN_PROGRESS",
        "RESOLVED",
        "REJECTED"
    ];

    const enteredStatus = prompt(
        `Current status: ${currentStatus}\n` +
        `Available statuses: ${allowedStatuses.join(", ")}\n` +
        "Enter new status:"
    );

    if (enteredStatus === null) {
        return;
    }

    const status = enteredStatus.trim().toUpperCase();

    if (!allowedStatuses.includes(status)) {
        showMessage(
            `Invalid status. Available: ${allowedStatuses.join(", ")}`,
            "error"
        );

        return;
    }

    if (status === currentStatus) {
        showMessage(
            "Complaint ka status pehle se yehi hai.",
            "info"
        );

        return;
    }

    let rejectionReason = null;

    if (status === "REJECTED") {
        rejectionReason = prompt(
            "Complaint reject karne ka reason enter karo:"
        );

        if (
            rejectionReason === null ||
            !rejectionReason.trim()
        ) {
            showMessage(
                "Rejection reason required hai.",
                "error"
            );

            return;
        }

        rejectionReason = rejectionReason.trim();
    }

    if (status === "ASSIGNED" || status === "IN_PROGRESS") {
        const assignedWorker =
            complaint.assignedWorkerEmail ||
            complaint.workerEmail ||
            complaint.assignedWorker?.email;

        if (!assignedWorker) {
            showMessage(
                "Pehle complaint ko worker assign karo.",
                "error"
            );

            return;
        }
    }

    const confirmed = confirm(
        `Complaint #${complaintId} ka status ${status} karna hai?`
    );

    if (!confirmed) {
        return;
    }

    try {
        await apiRequest(
            `/api/admin/complaints/${encodeURIComponent(complaintId)}/status`,
            {
                method: "PUT",

                headers: {
                    "Content-Type": "application/json"
                },

                body: JSON.stringify({
                    status: status,
                    rejectionReason: rejectionReason
                })
            }
        );

        showMessage(
            "Complaint status successfully updated.",
            "success"
        );

        await loadDashboard();

    } catch (error) {
        console.error(error);
        showMessage(error.message, "error");
    }
}

/* --------------------------------
   REJECT COMPLAINT
--------------------------------- */

async function rejectComplaint(complaintId) {
    const reason = prompt(
        "Complaint reject karne ka reason enter karo:"
    );

    if (!reason || !reason.trim()) {
        showMessage(
            "Rejection reason required hai.",
            "error"
        );

        return;
    }

    const confirmed = confirm(
        `Kya tum complaint #${complaintId} reject karna chahte ho?`
    );

    if (!confirmed) {
        return;
    }

    try {
        const endpoint =
            `/api/admin/complaints/${encodeURIComponent(complaintId)}` +
            `/reject?rejectionReason=${encodeURIComponent(reason.trim())}`;

        await apiRequest(endpoint, {
            method: "PUT"
        });

        showMessage(
            "Complaint successfully rejected.",
            "success"
        );

        await loadDashboard();

    } catch (error) {
        console.error(error);
        showMessage(error.message, "error");
    }
}

/* --------------------------------
   COMPLAINT DETAILS
--------------------------------- */

function showComplaintDetails(complaint) {
    const details = document.getElementById("complaintDetails");

    const id = escapeHTML(getComplaintId(complaint));
    const title = escapeHTML(getComplaintTitle(complaint));

    const description = escapeHTML(
        complaint.description ?? "No description"
    );

    const location = escapeHTML(
        getComplaintLocation(complaint)
    );

    const status = escapeHTML(
        getComplaintStatus(complaint)
    );

    const citizen = escapeHTML(
        getCitizenName(complaint)
    );

    const worker = escapeHTML(
        getWorkerName(complaint)
    );

    const date = escapeHTML(
        getComplaintDate(complaint)
    );

    const rejectionReason = escapeHTML(
        complaint.rejectionReason ?? "—"
    );

    // Before and after photo fields.
    const beforePhotoPath = complaint.photoUrl ?? null;
    const afterPhotoPath = complaint.completionPhotoUrl ?? null;

    details.innerHTML = `
        <div class="detail-row">
            <div class="detail-label">Complaint ID</div>
            <div class="detail-value">#${id}</div>
        </div>

        <div class="detail-row">
            <div class="detail-label">Title</div>
            <div class="detail-value">${title}</div>
        </div>

        <div class="detail-row">
            <div class="detail-label">Description</div>
            <div class="detail-value">${description}</div>
        </div>

        <div class="detail-row">
            <div class="detail-label">Location</div>
            <div class="detail-value">${location}</div>
        </div>

        <div class="detail-row">
            <div class="detail-label">Citizen</div>
            <div class="detail-value">${citizen}</div>
        </div>

        <div class="detail-row">
            <div class="detail-label">Assigned Worker</div>
            <div class="detail-value">${worker}</div>
        </div>

        <div class="detail-row">
            <div class="detail-label">Status</div>
            <div class="detail-value">
                ${status.replaceAll("_", " ")}
            </div>
        </div>

        <div class="detail-row">
            <div class="detail-label">Created Date</div>
            <div class="detail-value">${date}</div>
        </div>

        <div class="detail-row">
            <div class="detail-label">Rejection Reason</div>
            <div class="detail-value">${rejectionReason}</div>
        </div>

        <hr>

        <h2>Complaint Photos</h2>

        <div class="complaint-photos"
             style="display:flex;flex-wrap:wrap;gap:20px;">

            <section class="photo-card"
                     style="flex:1 1 260px;min-width:220px;">

                <h3>📷 Before Photo</h3>

                <div id="beforePhotoStatus" role="status">
                    Loading photo...
                </div>

                <img
                    id="beforePhotoImage"
                    alt="Before Photo"
                    style="display:none;width:100%;max-width:350px;
                           height:220px;object-fit:cover;
                           border-radius:10px;cursor:zoom-in;"
                >

                <br>

                <button
                    type="button"
                    id="viewBeforePhotoBtn"
                    class="action-btn"
                    style="display:none;">
                    View Full Photo ↗
                </button>
            </section>

            <section class="photo-card"
                     style="flex:1 1 260px;min-width:220px;">

                <h3>📸 After Photo</h3>

                <div id="afterPhotoStatus" role="status">
                    Loading photo...
                </div>

                <img
                    id="afterPhotoImage"
                    alt="After Photo"
                    style="display:none;width:100%;max-width:350px;
                           height:220px;object-fit:cover;
                           border-radius:10px;cursor:zoom-in;"
                >

                <br>

                <button
                    type="button"
                    id="viewAfterPhotoBtn"
                    class="action-btn"
                    style="display:none;">
                    View Full Photo ↗
                </button>
            </section>
        </div>
    `;

    document.getElementById("detailsModal").classList.add("show");

    loadComplaintPhoto(
        beforePhotoPath,
        "beforePhotoImage",
        "beforePhotoStatus",
        "viewBeforePhotoBtn"
    );

    loadComplaintPhoto(
        afterPhotoPath,
        "afterPhotoImage",
        "afterPhotoStatus",
        "viewAfterPhotoBtn"
    );
}

/* --------------------------------
   LOAD PROTECTED COMPLAINT PHOTO
--------------------------------- */

async function loadComplaintPhoto(
    photoPath,
    imageId,
    statusId,
    buttonId
) {
    const image = document.getElementById(imageId);
    const status = document.getElementById(statusId);
    const viewButton = document.getElementById(buttonId);

    if (!photoPath) {
        status.textContent = "Not uploaded yet.";
        return;
    }

    let photoUrl;

    try {
        photoUrl = new URL(photoPath, API_BASE_URL).href;
    } catch (error) {
        console.error("Invalid complaint photo URL:", error);

        status.textContent = "Invalid photo URL.";
        return;
    }

    try {
        const token = getToken();

        const headers = token
            ? { Authorization: `Bearer ${token}` }
            : {};

        const response = await fetch(photoUrl, { headers });

        if (!response.ok) {
            if (
                response.status === 401 ||
                response.status === 403
            ) {
                status.textContent =
                    "Photo access denied. Admin photo API permissions check karo.";

            } else if (response.status === 404) {
                status.textContent =
                    "Photo endpoint nahi mila (404). Backend mapping check karo.";

            } else {
                status.textContent =
                    `Photo load nahi hui (${response.status}).`;
            }

            console.error(
                `Photo request failed (${response.status}):`,
                photoUrl
            );

            return;
        }

        const blob = await response.blob();

        if (!blob.type.startsWith("image/")) {
            status.textContent =
                "Server ne image return nahi ki. Photo endpoint check karo.";

            console.error(
                "Photo endpoint returned non-image content:",
                blob.type,
                photoUrl
            );

            return;
        }

        const objectUrl = URL.createObjectURL(blob);

        image.src = objectUrl;
        image.style.display = "block";
        image.style.cursor = "zoom-in";

        status.textContent = "";

        const openFullPhoto = () => {
            window.open(
                objectUrl,
                "_blank",
                "noopener,noreferrer"
            );
        };

        image.addEventListener("click", openFullPhoto);
        viewButton.addEventListener("click", openFullPhoto);

        viewButton.style.display = "inline-block";

    } catch (error) {
        console.error("Error loading complaint photo:", error);

        status.textContent =
            "Photo load nahi hui. Backend connection check karo.";
    }
}

/* --------------------------------
   CLOSE DETAILS MODAL
--------------------------------- */

function closeModal() {
    document
        .getElementById("detailsModal")
        .classList.remove("show");
}

/* --------------------------------
   MESSAGE HELPER
--------------------------------- */

function showMessage(message, type) {
    const box = document.getElementById("messageBox");

    box.textContent = message;
    box.className = `message-box ${type}`;

    if (messageTimeout) {
        clearTimeout(messageTimeout);
    }

    messageTimeout = setTimeout(() => {
        box.className = "message-box";
    }, 6000);
}

/* --------------------------------
   LOGOUT
--------------------------------- */

function logout() {
    const confirmed = confirm(
        "Kya tum logout karna chahte ho?"
    );

    if (!confirmed) {
        return;
    }

    [
        "token",
        "jwtToken",
        "accessToken",
        "jwt",
        "authToken",
        "role",
        "userRole",
        "email"
    ].forEach(key => {
        localStorage.removeItem(key);
    });

    window.location.href = "login.html";
}