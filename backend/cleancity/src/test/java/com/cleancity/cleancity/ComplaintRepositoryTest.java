
        package com.cleancity.cleancity;

import com.cleancity.cleancity.entity.Complaint;
import com.cleancity.cleancity.entity.ComplaintStatus;
import com.cleancity.cleancity.entity.Role;
import com.cleancity.cleancity.entity.User;
import com.cleancity.cleancity.repository.ComplaintRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class ComplaintRepositoryTest {

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User citizen;
    private User worker;

    @BeforeEach
    void setUp() {

        // Create and save Citizen
        citizen = new User();
        citizen.setName("Test Citizen");
        citizen.setEmail("citizen@test.com");
        citizen.setPassword("testPassword");
        citizen.setRole(Role.CITIZEN);

        citizen = entityManager.persistAndFlush(citizen);

        // Create and save Worker
        worker = new User();
        worker.setName("Test Worker");
        worker.setEmail("worker@test.com");
        worker.setPassword("testPassword");
        worker.setRole(Role.WORKER);

        worker = entityManager.persistAndFlush(worker);

        // Create complaint assigned to Worker
        Complaint complaint = new Complaint();
        complaint.setTitle("Garbage Collection");
        complaint.setDescription("Garbage is not collected");
        complaint.setLocation("Bhilai");
        complaint.setStatus(ComplaintStatus.PENDING);
        complaint.setCitizen(citizen);
        complaint.setAssignedWorker(worker);

        entityManager.persistAndFlush(complaint);

        // Create complaint without an assigned Worker
        Complaint unassignedComplaint = new Complaint();
        unassignedComplaint.setTitle("Road Cleaning");
        unassignedComplaint.setDescription("Road needs cleaning");
        unassignedComplaint.setLocation("Durg");
        unassignedComplaint.setStatus(ComplaintStatus.PENDING);
        unassignedComplaint.setCitizen(citizen);
        unassignedComplaint.setAssignedWorker(null);

        entityManager.persistAndFlush(unassignedComplaint);

        // Clear Persistence Context
        entityManager.clear();
    }

    @Test
    void findByCitizen_shouldReturnList() {

        // Reload Citizen into the Persistence Context
        User savedCitizen = entityManager.find(
                User.class,
                citizen.getId()
        );

        List<Complaint> complaints =
                complaintRepository.findByCitizen(savedCitizen);

        assertThat(complaints).hasSize(2);

        assertThat(complaints)
                .allMatch(c ->
                        c.getCitizen().getId()
                                .equals(savedCitizen.getId())
                );
    }

    @Test
    void findByAssignedWorker_shouldReturnList() {

        // Reload Worker into the Persistence Context
        User savedWorker = entityManager.find(
                User.class,
                worker.getId()
        );

        List<Complaint> complaints =
                complaintRepository.findByAssignedWorker(savedWorker);

        assertThat(complaints).hasSize(1);

        assertThat(complaints.get(0).getAssignedWorker().getId())
                .isEqualTo(savedWorker.getId());
    }

    @Test
    void findByStatus_shouldReturnMatchingComplaints() {

        List<Complaint> complaints =
                complaintRepository.findByStatus(
                        ComplaintStatus.PENDING
                );

        assertThat(complaints).hasSize(2);

        assertThat(complaints)
                .allMatch(c ->
                        c.getStatus() == ComplaintStatus.PENDING
                );
    }

    @Test
    void findByStatusAndAssignedWorkerIsNull_shouldReturnAvailableComplaints() {

        List<Complaint> complaints =
                complaintRepository.findByStatusAndAssignedWorkerIsNull(
                        ComplaintStatus.PENDING
                );

        assertThat(complaints).hasSize(1);

        assertThat(complaints.get(0).getAssignedWorker())
                .isNull();
    }

    @Test
    void findByLocationContainingIgnoreCase_shouldReturnMatchingComplaints() {

        List<Complaint> complaints =
                complaintRepository.findByLocationContainingIgnoreCase(
                        "bhilai"
                );

        assertThat(complaints)
                .isNotEmpty();

        assertThat(complaints)
                .allMatch(c ->
                        c.getLocation()
                                .toLowerCase()
                                .contains("bhilai")
                );
    }


    @Test
    void findByLocationIgnoreCaseAndStatusIn_shouldReturnMatchingComplaints() {

        List<Complaint> complaints =
                complaintRepository.findByLocationIgnoreCaseAndStatusIn(
                        "bhilai",
                        List.of(ComplaintStatus.PENDING)
                );

        assertThat(complaints).hasSize(1);

        assertThat(complaints.get(0).getLocation())
                .isEqualTo("Bhilai");
    }

    @Test
    void countByStatus_shouldReturnCount() {

        long count =
                complaintRepository.countByStatus(
                        ComplaintStatus.PENDING
                );

        assertThat(count).isEqualTo(2);
    }
}