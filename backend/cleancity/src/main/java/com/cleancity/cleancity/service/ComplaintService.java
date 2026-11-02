package com.cleancity.cleancity.service;

import com.cleancity.cleancity.dto.ComplaintStatisticsDto;
import com.cleancity.cleancity.dto.ComplaintResponseDto;
import com.cleancity.cleancity.dto.DuplicateComplaintResponseDto;
import com.cleancity.cleancity.dto.PublicComplaintResponseDto;
import com.cleancity.cleancity.entity.Complaint;
import com.cleancity.cleancity.entity.ComplaintStatus;
import com.cleancity.cleancity.entity.Role;
import com.cleancity.cleancity.entity.User;
import com.cleancity.cleancity.repository.ComplaintRepository;
import com.cleancity.cleancity.repository.UserRepository;
import com.cleancity.cleancity.storage.ComplaintPhotoService;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final ComplaintPhotoService complaintPhotoService;

    public ComplaintService(
            ComplaintRepository complaintRepository,
            UserRepository userRepository,
            ComplaintPhotoService complaintPhotoService) {

        this.complaintRepository = complaintRepository;
        this.userRepository = userRepository;
        this.complaintPhotoService = complaintPhotoService;
    }

    // 1. Create a new complaint
    public Complaint createComplaint(
            String title,
            String description,
            String location,
            String citizenEmail) {

        User citizen = userRepository.findByEmail(citizenEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException("Citizen not found"));

        if (citizen.getRole() != Role.CITIZEN) {
            throw new IllegalArgumentException(
                    "Only citizens can create complaints");
        }

        Complaint complaint = new Complaint();

        complaint.setTitle(title);
        complaint.setDescription(description);
        complaint.setLocation(location);
        complaint.setCitizen(citizen);
        complaint.setStatus(ComplaintStatus.PENDING);

        return complaintRepository.save(complaint);
    }

    // 2. Create complaint with duplicate detection
    public DuplicateComplaintResponseDto createComplaintWithDuplicateCheck(
            String title,
            String description,
            String location,
            String citizenEmail,
            boolean confirmDuplicate) {

        User citizen = userRepository.findByEmail(citizenEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException("Citizen not found"));

        if (citizen.getRole() != Role.CITIZEN) {
            throw new IllegalArgumentException(
                    "Only citizens can create complaints");
        }

        if (title == null || title.isBlank()
                || description == null || description.isBlank()
                || location == null || location.isBlank()) {

            throw new IllegalArgumentException(
                    "Title, description, and location are required");
        }

        String cleanTitle = title.trim();
        String cleanDescription = description.trim();
        String cleanLocation = location.trim();

        // Active complaint statuses
        List<ComplaintStatus> activeStatuses = List.of(
                ComplaintStatus.PENDING,
                ComplaintStatus.ASSIGNED,
                ComplaintStatus.IN_PROGRESS
        );

        // Find complaints at the same location
        List<Complaint> complaintsAtLocation =
                complaintRepository.findByLocationIgnoreCaseAndStatusIn(
                        cleanLocation,
                        activeStatuses
                );

        // Check whether title or description already exists
        Complaint duplicate = complaintsAtLocation.stream()
                .filter(existing -> {

                    boolean sameTitle =
                            existing.getTitle() != null
                                    && existing.getTitle()
                                    .trim()
                                    .equalsIgnoreCase(cleanTitle);

                    boolean sameDescription =
                            existing.getDescription() != null
                                    && existing.getDescription()
                                    .trim()
                                    .equalsIgnoreCase(cleanDescription);

                    return sameTitle || sameDescription;
                })
                .findFirst()
                .orElse(null);

        // Duplicate found: ask for confirmation first
        if (duplicate != null && !confirmDuplicate) {

            return new DuplicateComplaintResponseDto(
                    true,
                    "A similar active complaint already exists. "
                            + "Confirm if you still want to submit.",
                    duplicate.getId(),
                    false,
                    null
            );
        }

        // Create the new complaint
        Complaint complaint = new Complaint();

        complaint.setTitle(cleanTitle);
        complaint.setDescription(cleanDescription);
        complaint.setLocation(cleanLocation);
        complaint.setCitizen(citizen);
        complaint.setStatus(ComplaintStatus.PENDING);

        Complaint savedComplaint =
                complaintRepository.save(complaint);

        // Return the response
        return new DuplicateComplaintResponseDto(
                duplicate != null,
                duplicate != null
                        ? "Complaint submitted despite the duplicate warning."
                        : "Complaint created successfully.",
                duplicate != null ? duplicate.getId() : null,
                true,
                new ComplaintResponseDto(savedComplaint)
        );
    }

    // 3. Save a complaint, including its original photo path
    public Complaint saveComplaint(Complaint complaint) {
        return complaintRepository.save(complaint);
    }

    // 4. Get complaints created by a citizen
    public List<Complaint> getCitizenComplaints(String citizenEmail) {

        User citizen = userRepository.findByEmail(citizenEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException("Citizen not found"));

        return complaintRepository.findByCitizen(citizen);
    }

    // 5. Get all complaints
    public List<Complaint> getAllComplaints() {
        return complaintRepository.findAll();
    }

    // 6. Admin assigns a complaint to a worker
    @Transactional
    public Complaint assignWorker(
            Long complaintId,
            String workerEmail) {

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Complaint not found"));

        User worker = userRepository.findByEmail(workerEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException("Worker not found"));

        if (worker.getRole() != Role.WORKER) {
            throw new IllegalArgumentException(
                    "Selected user is not a worker");
        }

        if (complaint.getStatus() != ComplaintStatus.PENDING
                || complaint.getAssignedWorker() != null) {

            throw new IllegalArgumentException(
                    "Complaint is not available for assignment");
        }

        complaint.setAssignedWorker(worker);
        complaint.setStatus(ComplaintStatus.ASSIGNED);

        return complaintRepository.save(complaint);
    }

    // 7. Get available complaints for workers
    public List<Complaint> getAvailableComplaints(String workerEmail) {

        User worker = userRepository.findByEmail(workerEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException("Worker not found"));

        if (worker.getRole() != Role.WORKER) {
            throw new IllegalArgumentException("User is not a worker");
        }

        return complaintRepository.findByStatusAndAssignedWorkerIsNull(
                ComplaintStatus.PENDING
        );
    }

    // 8. Worker accepts a complaint
    @Transactional
    public Complaint acceptComplaint(
            Long complaintId,
            String workerEmail) {

        User worker = userRepository.findByEmail(workerEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException("Worker not found"));

        if (worker.getRole() != Role.WORKER) {
            throw new IllegalArgumentException("User is not a worker");
        }

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Complaint not found"));

        if (complaint.getStatus() != ComplaintStatus.PENDING
                || complaint.getAssignedWorker() != null) {

            throw new IllegalArgumentException(
                    "Complaint is no longer available. "
                            + "Another worker may have accepted it.");
        }

        complaint.setAssignedWorker(worker);
        complaint.setStatus(ComplaintStatus.ASSIGNED);

        return complaintRepository.save(complaint);
    }

    // 9. Get complaints assigned to a worker
    public List<Complaint> getWorkerComplaints(String workerEmail) {

        User worker = userRepository.findByEmail(workerEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException("Worker not found"));

        if (worker.getRole() != Role.WORKER) {
            throw new IllegalArgumentException("User is not a worker");
        }

        return complaintRepository.findByAssignedWorker(worker);
    }

    // 10. Update complaint status by assigned worker
    @Transactional
    public Complaint updateComplaintStatus(
            Long complaintId,
            String workerEmail,
            ComplaintStatus newStatus) {

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Complaint not found"));

        User worker = userRepository.findByEmail(workerEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException("Worker not found"));

        validateAssignedWorker(complaint, worker);

        if (complaint.getStatus() == ComplaintStatus.RESOLVED) {
            throw new IllegalArgumentException(
                    "Resolved complaint cannot be modified");
        }

        if (newStatus != ComplaintStatus.IN_PROGRESS) {
            throw new IllegalArgumentException(
                    "To resolve a complaint, upload the completion photo");
        }

        if (complaint.getStatus() != ComplaintStatus.ASSIGNED
                && complaint.getStatus() != ComplaintStatus.IN_PROGRESS) {

            throw new IllegalArgumentException(
                    "Complaint must be assigned before work starts");
        }

        complaint.setStatus(ComplaintStatus.IN_PROGRESS);

        return complaintRepository.save(complaint);
    }

    // 11. Complete complaint with after-cleaning photo
    @Transactional
    public Complaint completeComplaint(
            Long complaintId,
            String workerEmail,
            MultipartFile completionPhoto) {

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Complaint not found"));

        User worker = userRepository.findByEmail(workerEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException("Worker not found"));

        validateAssignedWorker(complaint, worker);

        if (complaint.getStatus() != ComplaintStatus.IN_PROGRESS) {
            throw new IllegalArgumentException(
                    "Only IN_PROGRESS complaints can be completed");
        }

        if (complaint.getCompletionPhotoPath() != null) {
            throw new IllegalArgumentException(
                    "Completion photo has already been uploaded");
        }

        String completionPhotoPath =
                complaintPhotoService.saveCompletionPhoto(completionPhoto);

        complaint.setCompletionPhotoPath(completionPhotoPath);
        complaint.setResolvedAt(LocalDateTime.now());
        complaint.setStatus(ComplaintStatus.RESOLVED);

        return complaintRepository.save(complaint);
    }

    // 12. Validate that the complaint belongs to this worker
    private void validateAssignedWorker(
            Complaint complaint,
            User worker) {

        if (worker.getRole() != Role.WORKER) {
            throw new IllegalArgumentException("User is not a worker");
        }

        if (complaint.getAssignedWorker() == null
                || !complaint.getAssignedWorker()
                .getId()
                .equals(worker.getId())) {

            throw new IllegalArgumentException(
                    "This complaint is not assigned to you");
        }
    }

    // 13. Get complaints by status
    public List<Complaint> getComplaintsByStatus(
            ComplaintStatus status) {

        return complaintRepository.findByStatus(status);
    }

    // 14. Reject a complaint
    @Transactional
    public Complaint rejectComplaint(
            Long complaintId,
            String rejectionReason) {

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Complaint not found"));

        if (complaint.getStatus() == ComplaintStatus.RESOLVED) {
            throw new IllegalArgumentException(
                    "Resolved complaint cannot be rejected");
        }

        if (rejectionReason == null || rejectionReason.isBlank()) {
            throw new IllegalArgumentException(
                    "Rejection reason is required");
        }

        complaint.setStatus(ComplaintStatus.REJECTED);
        complaint.setRejectionReason(rejectionReason);

        return complaintRepository.save(complaint);
    }

    // 15. Complaint statistics for admin
    public ComplaintStatisticsDto getComplaintStatistics() {

        ComplaintStatisticsDto statistics =
                new ComplaintStatisticsDto();

        statistics.setTotalComplaints(complaintRepository.count());

        statistics.setPendingComplaints(
                complaintRepository.countByStatus(
                        ComplaintStatus.PENDING));

        statistics.setAssignedComplaints(
                complaintRepository.countByStatus(
                        ComplaintStatus.ASSIGNED));

        statistics.setInProgressComplaints(
                complaintRepository.countByStatus(
                        ComplaintStatus.IN_PROGRESS));

        statistics.setResolvedComplaints(
                complaintRepository.countByStatus(
                        ComplaintStatus.RESOLVED));

        statistics.setRejectedComplaints(
                complaintRepository.countByStatus(
                        ComplaintStatus.REJECTED));

        return statistics;
    }

    // 16. Get all public complaints
    public List<PublicComplaintResponseDto> getPublicComplaints() {

        return complaintRepository.findAll()
                .stream()
                .map(PublicComplaintResponseDto::new)
                .toList();
    }

    // 17. Get public complaints by status
    public List<PublicComplaintResponseDto> getPublicComplaintsByStatus(
            ComplaintStatus status) {

        return complaintRepository.findByStatus(status)
                .stream()
                .map(PublicComplaintResponseDto::new)
                .toList();
    }

    // 18. Get public complaints by location
    public List<PublicComplaintResponseDto> getPublicComplaintsByLocation(
            String location) {

        return complaintRepository
                .findByLocationContainingIgnoreCase(location)
                .stream()
                .map(PublicComplaintResponseDto::new)
                .toList();
    }

    // 19. Get a complaint by ID
    public Complaint getComplaintById(Long complaintId) {

        return complaintRepository.findById(complaintId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Complaint not found"));
    }

    // 20. Update a citizen's own pending complaint
    @Transactional
    public Complaint updateCitizenComplaint(
            Long complaintId,
            String citizenEmail,
            String title,
            String description,
            String location) {

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Complaint not found"));

        if (!complaint.getCitizen().getEmail().equals(citizenEmail)) {
            throw new IllegalArgumentException(
                    "You are not allowed to edit this complaint");
        }

        if (complaint.getStatus() != ComplaintStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Only PENDING complaints can be edited");
        }

        complaint.setTitle(title);
        complaint.setDescription(description);
        complaint.setLocation(location);

        return complaintRepository.save(complaint);
    }

// 21. Get completion photo path after checking permission
    @Transactional(readOnly = true)
    public String getCompletionPhotoPath(
            Long complaintId,
            String userEmail) {

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Complaint not found"));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        // Check whether the logged-in user owns this complaint
        boolean isCitizenOwner =
                user.getRole() == Role.CITIZEN
                        && complaint.getCitizen() != null
                        && complaint.getCitizen().getId()
                        .equals(user.getId());

        // Check whether the logged-in user is the assigned worker
        boolean isAssignedWorker =
                user.getRole() == Role.WORKER
                        && complaint.getAssignedWorker() != null
                        && complaint.getAssignedWorker().getId()
                        .equals(user.getId());

        // Check whether the logged-in user is an Admin
        boolean isAdmin = user.getRole() == Role.ADMIN;

        // Allow the complaint owner, assigned worker, or Admin
        if (!isCitizenOwner && !isAssignedWorker && !isAdmin) {
            throw new AccessDeniedException(
                    "You are not allowed to view this completion photo");
        }

        // Check whether the completion photo exists
        String photoPath = complaint.getCompletionPhotoPath();

        if (photoPath == null || photoPath.isBlank()) {
            return null;
        }

        return photoPath;
    }

    // Get original complaint photo with access validation
    @Transactional(readOnly = true)
    public String getOriginalPhotoPath(
            Long complaintId,
            String userEmail) {

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Complaint not found"));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        boolean isCitizenOwner =
                user.getRole() == Role.CITIZEN
                        && complaint.getCitizen() != null
                        && complaint.getCitizen().getId()
                        .equals(user.getId());

        boolean isAssignedWorker =
                user.getRole() == Role.WORKER
                        && complaint.getAssignedWorker() != null
                        && complaint.getAssignedWorker().getId()
                        .equals(user.getId());

        if (!isCitizenOwner && !isAssignedWorker) {
            throw new AccessDeniedException(
                    "You are not allowed to view this complaint photo");
        }

        String photoPath = complaint.getPhotoPath();

        if (photoPath == null || photoPath.isBlank()) {
            return null;
        }

        return photoPath;
    }

    // Admin edits complaint details
    @Transactional
    public Complaint updateAdminComplaint(
            Long complaintId,
            String title,
            String description,
            String location) {

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Complaint not found"));

        if (title == null || title.isBlank()
                || description == null || description.isBlank()
                || location == null || location.isBlank()) {

            throw new IllegalArgumentException(
                    "Title, description, and location are required");
        }

        complaint.setTitle(title.trim());
        complaint.setDescription(description.trim());
        complaint.setLocation(location.trim());

        return complaintRepository.save(complaint);
    }

    // Admin changes complaint status
    @Transactional
    public Complaint updateAdminComplaintStatus(
            Long complaintId,
            ComplaintStatus newStatus,
            String rejectionReason) {

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Complaint not found"));

        if (newStatus == null) {
            throw new IllegalArgumentException("Status is required");
        }

        // Worker must be assigned before starting work
        if ((newStatus == ComplaintStatus.ASSIGNED
                || newStatus == ComplaintStatus.IN_PROGRESS)
                && complaint.getAssignedWorker() == null) {

            throw new IllegalArgumentException(
                    "Assign a worker before setting this status");
        }

        // Completion photo is required before resolving
        if (newStatus == ComplaintStatus.RESOLVED
                && (complaint.getCompletionPhotoPath() == null
                || complaint.getCompletionPhotoPath().isBlank())) {

            throw new IllegalArgumentException(
                    "Completion photo is required before resolving");
        }

        // Rejection reason is required for REJECTED status
        if (newStatus == ComplaintStatus.REJECTED) {

            if (rejectionReason == null || rejectionReason.isBlank()) {
                throw new IllegalArgumentException(
                        "Rejection reason is required");
            }

            complaint.setRejectionReason(rejectionReason.trim());

        } else {
            complaint.setRejectionReason(null);
        }

        // Update resolved time
        if (newStatus == ComplaintStatus.RESOLVED) {

            if (complaint.getResolvedAt() == null) {
                complaint.setResolvedAt(LocalDateTime.now());
            }

        } else {
            complaint.setResolvedAt(null);
        }

        complaint.setStatus(newStatus);

        return complaintRepository.save(complaint);
    }

}