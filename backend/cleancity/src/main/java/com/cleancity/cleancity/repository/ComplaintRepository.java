package com.cleancity.cleancity.repository;

import com.cleancity.cleancity.entity.Complaint;
import com.cleancity.cleancity.entity.ComplaintStatus;
import com.cleancity.cleancity.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplaintRepository
        extends JpaRepository<Complaint, Long> {

    // Citizen apni complaints dekh sakega
    List<Complaint> findByCitizen(User citizen);

    // Worker ko assigned complaints milengi
    List<Complaint> findByAssignedWorker(User worker);

    // Status ke according complaints find karna
    List<Complaint> findByStatus(ComplaintStatus status);

    // Sirf available complaints: PENDING aur worker assign nahi hua
    List<Complaint> findByStatusAndAssignedWorkerIsNull(
            ComplaintStatus status
    );

    // Kisi particular status ki total complaints count karna
    long countByStatus(ComplaintStatus status);

    // Particular location ke according complaints find karna
    List<Complaint> findByLocationContainingIgnoreCase(String location);

    List<Complaint> findByLocationIgnoreCaseAndStatusIn(
            String location,
            List<ComplaintStatus> statuses
    );

}