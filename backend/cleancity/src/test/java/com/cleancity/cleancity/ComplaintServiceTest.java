package com.cleancity.cleancity;

import com.cleancity.cleancity.dto.DuplicateComplaintResponseDto;
import com.cleancity.cleancity.entity.Complaint;
import com.cleancity.cleancity.entity.ComplaintStatus;
import com.cleancity.cleancity.entity.Role;
import com.cleancity.cleancity.entity.User;
import com.cleancity.cleancity.repository.ComplaintRepository;
import com.cleancity.cleancity.repository.UserRepository;

import com.cleancity.cleancity.service.ComplaintService;
import com.cleancity.cleancity.storage.ComplaintPhotoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplaintServiceTest {

    @Mock
    private ComplaintRepository complaintRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ComplaintPhotoService complaintPhotoService;

    @InjectMocks
    private ComplaintService complaintService;

    private User citizen;
    private User worker;
    private Complaint complaint;

    @BeforeEach
    void setUp() {

        citizen = new User();
        citizen.setId(1L);
        citizen.setName("Test Citizen");
        citizen.setEmail("citizen@test.com");
        citizen.setRole(Role.CITIZEN);

        worker = new User();
        worker.setId(2L);
        worker.setName("Test Worker");
        worker.setEmail("worker@test.com");
        worker.setRole(Role.WORKER);

        complaint = new Complaint();
        complaint.setId(10L);
        complaint.setTitle("Garbage Problem");
        complaint.setDescription("Garbage is lying on the road");
        complaint.setLocation("Bhilai");
        complaint.setCitizen(citizen);
        complaint.setStatus(ComplaintStatus.PENDING);
    }

    // 1. Complaint successfully create honi chahiye

    @Test
    void createComplaint_Success() {

        when(userRepository.findByEmail("citizen@test.com"))
                .thenReturn(Optional.of(citizen));

        when(complaintRepository.save(any(Complaint.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Complaint result = complaintService.createComplaint(
                "Garbage Problem",
                "Garbage is lying on the road",
                "Bhilai",
                "citizen@test.com"
        );

        assertNotNull(result);
        assertEquals("Garbage Problem", result.getTitle());
        assertEquals("Bhilai", result.getLocation());
        assertEquals(ComplaintStatus.PENDING, result.getStatus());
        assertEquals(citizen, result.getCitizen());

        verify(complaintRepository).save(any(Complaint.class));
    }

    // 2. Citizen nahi milne par exception

    @Test
    void createComplaint_CitizenNotFound() {

        when(userRepository.findByEmail("missing@test.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> complaintService.createComplaint(
                        "Garbage Problem",
                        "Garbage on road",
                        "Bhilai",
                        "missing@test.com"
                )
        );

        verify(complaintRepository, never()).save(any());
    }

    // 3. Non-citizen complaint create nahi kar sake

    @Test
    void createComplaint_NonCitizen() {

        when(userRepository.findByEmail("worker@test.com"))
                .thenReturn(Optional.of(worker));

        assertThrows(
                IllegalArgumentException.class,
                () -> complaintService.createComplaint(
                        "Garbage Problem",
                        "Garbage on road",
                        "Bhilai",
                        "worker@test.com"
                )
        );

        verify(complaintRepository, never()).save(any());
    }

    // 4. Complaint save method

    @Test
    void saveComplaint_Success() {

        when(complaintRepository.save(complaint))
                .thenReturn(complaint);

        Complaint result = complaintService.saveComplaint(complaint);

        assertNotNull(result);
        assertEquals(10L, result.getId());

        verify(complaintRepository).save(complaint);
    }

    // 5. Citizen ki complaints milni chahiye

    @Test
    void getCitizenComplaints_Success() {

        when(userRepository.findByEmail("citizen@test.com"))
                .thenReturn(Optional.of(citizen));

        when(complaintRepository.findByCitizen(citizen))
                .thenReturn(List.of(complaint));

        List<Complaint> result =
                complaintService.getCitizenComplaints("citizen@test.com");

        assertEquals(1, result.size());
        assertEquals(complaint, result.get(0));
    }

    // 6. Saari complaints milni chahiye

    @Test
    void getAllComplaints_Success() {

        when(complaintRepository.findAll())
                .thenReturn(List.of(complaint));

        List<Complaint> result = complaintService.getAllComplaints();

        assertEquals(1, result.size());
        assertEquals(complaint, result.get(0));
    }

    // 7. Complaint ID se complaint milni chahiye

    @Test
    void getComplaintById_Success() {

        when(complaintRepository.findById(10L))
                .thenReturn(Optional.of(complaint));

        Complaint result = complaintService.getComplaintById(10L);

        assertNotNull(result);
        assertEquals(10L, result.getId());
    }

    // 8. Invalid complaint ID

    @Test
    void getComplaintById_NotFound() {

        when(complaintRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> complaintService.getComplaintById(99L)
        );
    }

    // 9. Complaint ko worker assign karna

    @Test
    void assignWorker_Success() {

        when(complaintRepository.findById(10L))
                .thenReturn(Optional.of(complaint));

        when(userRepository.findByEmail("worker@test.com"))
                .thenReturn(Optional.of(worker));

        when(complaintRepository.save(any(Complaint.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Complaint result = complaintService.assignWorker(
                10L,
                "worker@test.com"
        );

        assertEquals(worker, result.getAssignedWorker());
        assertEquals(ComplaintStatus.ASSIGNED, result.getStatus());

        verify(complaintRepository).save(complaint);
    }

    // 10. Assigned complaint ko dobara assign nahi karna

    @Test
    void assignWorker_ComplaintAlreadyAssigned() {

        complaint.setAssignedWorker(worker);
        complaint.setStatus(ComplaintStatus.ASSIGNED);

        when(complaintRepository.findById(10L))
                .thenReturn(Optional.of(complaint));

        when(userRepository.findByEmail("worker@test.com"))
                .thenReturn(Optional.of(worker));

        assertThrows(
                IllegalArgumentException.class,
                () -> complaintService.assignWorker(
                        10L,
                        "worker@test.com"
                )
        );

        verify(complaintRepository, never()).save(any());
    }

    // 11. Available complaints milni chahiye

    @Test
    void getAvailableComplaints_Success() {

        when(userRepository.findByEmail("worker@test.com"))
                .thenReturn(Optional.of(worker));

        when(complaintRepository.findByStatusAndAssignedWorkerIsNull(
                ComplaintStatus.PENDING
        )).thenReturn(List.of(complaint));

        List<Complaint> result =
                complaintService.getAvailableComplaints("worker@test.com");

        assertEquals(1, result.size());
        assertEquals(ComplaintStatus.PENDING, result.get(0).getStatus());
    }

    // 12. Worker complaint accept kare

    @Test
    void acceptComplaint_Success() {

        when(userRepository.findByEmail("worker@test.com"))
                .thenReturn(Optional.of(worker));

        when(complaintRepository.findById(10L))
                .thenReturn(Optional.of(complaint));

        when(complaintRepository.save(any(Complaint.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Complaint result = complaintService.acceptComplaint(
                10L,
                "worker@test.com"
        );

        assertEquals(worker, result.getAssignedWorker());
        assertEquals(ComplaintStatus.ASSIGNED, result.getStatus());
    }

    // 13. Worker apni assigned complaints dekhe

    @Test
    void getWorkerComplaints_Success() {

        complaint.setAssignedWorker(worker);
        complaint.setStatus(ComplaintStatus.ASSIGNED);

        when(userRepository.findByEmail("worker@test.com"))
                .thenReturn(Optional.of(worker));

        when(complaintRepository.findByAssignedWorker(worker))
                .thenReturn(List.of(complaint));

        List<Complaint> result =
                complaintService.getWorkerComplaints("worker@test.com");

        assertEquals(1, result.size());
        assertEquals(worker, result.get(0).getAssignedWorker());
    }

    // 14. Assigned complaint ka status IN_PROGRESS karna

    @Test
    void updateComplaintStatus_Success() {

        complaint.setAssignedWorker(worker);
        complaint.setStatus(ComplaintStatus.ASSIGNED);

        when(complaintRepository.findById(10L))
                .thenReturn(Optional.of(complaint));

        when(userRepository.findByEmail("worker@test.com"))
                .thenReturn(Optional.of(worker));

        when(complaintRepository.save(any(Complaint.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Complaint result = complaintService.updateComplaintStatus(
                10L,
                "worker@test.com",
                ComplaintStatus.IN_PROGRESS
        );

        assertEquals(ComplaintStatus.IN_PROGRESS, result.getStatus());
    }

    // 15. Doosra worker status update na kar sake

    @Test
    void updateComplaintStatus_WrongWorker() {

        User anotherWorker = new User();
        anotherWorker.setId(3L);
        anotherWorker.setEmail("another@test.com");
        anotherWorker.setRole(Role.WORKER);

        complaint.setAssignedWorker(worker);
        complaint.setStatus(ComplaintStatus.ASSIGNED);

        when(complaintRepository.findById(10L))
                .thenReturn(Optional.of(complaint));

        when(userRepository.findByEmail("another@test.com"))
                .thenReturn(Optional.of(anotherWorker));

        assertThrows(
                IllegalArgumentException.class,
                () -> complaintService.updateComplaintStatus(
                        10L,
                        "another@test.com",
                        ComplaintStatus.IN_PROGRESS
                )
        );

        verify(complaintRepository, never()).save(any());
    }

    // 16. Status ke according complaints milni chahiye

    @Test
    void getComplaintsByStatus_Success() {

        when(complaintRepository.findByStatus(ComplaintStatus.PENDING))
                .thenReturn(List.of(complaint));

        List<Complaint> result =
                complaintService.getComplaintsByStatus(
                        ComplaintStatus.PENDING
                );

        assertEquals(1, result.size());
        assertEquals(ComplaintStatus.PENDING, result.get(0).getStatus());
    }

    // 17. Complaint reject karna

    @Test
    void rejectComplaint_Success() {

        when(complaintRepository.findById(10L))
                .thenReturn(Optional.of(complaint));

        when(complaintRepository.save(any(Complaint.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Complaint result = complaintService.rejectComplaint(
                10L,
                "Insufficient details"
        );

        assertEquals(ComplaintStatus.REJECTED, result.getStatus());
        assertEquals("Insufficient details", result.getRejectionReason());
    }

    // 18. Empty rejection reason invalid hai

    @Test
    void rejectComplaint_EmptyReason() {

        when(complaintRepository.findById(10L))
                .thenReturn(Optional.of(complaint));

        assertThrows(
                IllegalArgumentException.class,
                () -> complaintService.rejectComplaint(10L, " ")
        );

        verify(complaintRepository, never()).save(any());
    }

    // 19. Complaint statistics

    @Test
    void getComplaintStatistics_Success() {

        when(complaintRepository.count()).thenReturn(10L);

        when(complaintRepository.countByStatus(ComplaintStatus.PENDING))
                .thenReturn(4L);

        when(complaintRepository.countByStatus(ComplaintStatus.ASSIGNED))
                .thenReturn(2L);

        when(complaintRepository.countByStatus(ComplaintStatus.IN_PROGRESS))
                .thenReturn(1L);

        when(complaintRepository.countByStatus(ComplaintStatus.RESOLVED))
                .thenReturn(2L);

        when(complaintRepository.countByStatus(ComplaintStatus.REJECTED))
                .thenReturn(1L);

        assertNotNull(complaintService.getComplaintStatistics());

        verify(complaintRepository).count();
        verify(complaintRepository, times(5)).countByStatus(any());
    }

    // 20. Public complaints

    @Test
    void getPublicComplaints_Success() {

        when(complaintRepository.findAll())
                .thenReturn(List.of(complaint));

        assertEquals(1, complaintService.getPublicComplaints().size());
    }

    // 21. Location ke according complaints

    @Test
    void getPublicComplaintsByLocation_Success() {

        when(complaintRepository.findByLocationContainingIgnoreCase("Bhilai"))
                .thenReturn(List.of(complaint));

        assertEquals(
                1,
                complaintService.getPublicComplaintsByLocation("Bhilai").size()
        );
    }

    // 22. Citizen apni pending complaint edit kar sake

    @Test
    void updateCitizenComplaint_Success() {

        when(complaintRepository.findById(10L))
                .thenReturn(Optional.of(complaint));

        when(complaintRepository.save(any(Complaint.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Complaint result = complaintService.updateCitizenComplaint(
                10L,
                "citizen@test.com",
                "Updated Title",
                "Updated description",
                "Raipur"
        );

        assertEquals("Updated Title", result.getTitle());
        assertEquals("Updated description", result.getDescription());
        assertEquals("Raipur", result.getLocation());
    }

    // 23. Citizen doosre user ki complaint edit nahi kar sake

    @Test
    void updateCitizenComplaint_Unauthorized() {

        when(complaintRepository.findById(10L))
                .thenReturn(Optional.of(complaint));

        assertThrows(
                IllegalArgumentException.class,
                () -> complaintService.updateCitizenComplaint(
                        10L,
                        "other@test.com",
                        "Updated Title",
                        "Updated description",
                        "Raipur"
                )
        );

        verify(complaintRepository, never()).save(any());
    }

    // 24. Admin complaint details update kare

    @Test
    void updateAdminComplaint_Success() {

        when(complaintRepository.findById(10L))
                .thenReturn(Optional.of(complaint));

        when(complaintRepository.save(any(Complaint.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Complaint result = complaintService.updateAdminComplaint(
                10L,
                "Admin Updated Title",
                "Admin Updated Description",
                "Durg"
        );

        assertEquals("Admin Updated Title", result.getTitle());
        assertEquals("Admin Updated Description", result.getDescription());
        assertEquals("Durg", result.getLocation());
    }

    // 25. Blank admin complaint details invalid hain

    @Test
    void updateAdminComplaint_BlankFields() {

        when(complaintRepository.findById(10L))
                .thenReturn(Optional.of(complaint));

        assertThrows(
                IllegalArgumentException.class,
                () -> complaintService.updateAdminComplaint(
                        10L,
                        "",
                        "Description",
                        "Bhilai"
                )
        );

        verify(complaintRepository, never()).save(any());
    }
}