
        package com.cleancity.cleancity;

import com.cleancity.cleancity.controller.ComplaintController;
import com.cleancity.cleancity.dto.DuplicateComplaintResponseDto;
import com.cleancity.cleancity.dto.PublicComplaintResponseDto;
import com.cleancity.cleancity.entity.Complaint;
import com.cleancity.cleancity.entity.ComplaintStatus;
import com.cleancity.cleancity.security.JwtService;
import com.cleancity.cleancity.service.ComplaintService;
import com.cleancity.cleancity.storage.ComplaintPhotoService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ComplaintController.class)
@AutoConfigureMockMvc
class ComplaintControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ComplaintService complaintService;

    @MockitoBean
    private ComplaintPhotoService complaintPhotoService;

    // Required by JwtAuthenticationFilter
    @MockitoBean
    private JwtService jwtService;


    // 1. Create complaint successfully
    @Test
    void createComplaint_success() throws Exception {

        DuplicateComplaintResponseDto response =
                new DuplicateComplaintResponseDto(
                        false,
                        "Complaint submitted successfully",
                        null,
                        true,
                        null
                );

        when(complaintService.createComplaintWithDuplicateCheck(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyBoolean()
        )).thenReturn(response);

        mockMvc.perform(
                        multipart("/api/citizen/complaints")
                                .param("title", "Garbage Problem")
                                .param("description", "Garbage is not collected")
                                .param("location", "Bhilai")
                                .param("confirmDuplicate", "false")
                                .with(user("citizen@example.com").roles("CITIZEN"))
                                .with(csrf())
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.submitted").value(true))
                .andExpect(jsonPath("$.duplicateFound").value(false));
    }


    // 2. Duplicate complaint found
    @Test
    void createComplaint_duplicateFound() throws Exception {

        DuplicateComplaintResponseDto response =
                new DuplicateComplaintResponseDto(
                        true,
                        "Duplicate complaint found",
                        1L,
                        false,
                        null
                );

        when(complaintService.createComplaintWithDuplicateCheck(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyBoolean()
        )).thenReturn(response);

        mockMvc.perform(
                        multipart("/api/citizen/complaints")
                                .param("title", "Garbage Problem")
                                .param("description", "Garbage is not collected")
                                .param("location", "Bhilai")
                                .with(user("citizen@example.com").roles("CITIZEN"))
                                .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duplicateFound").value(true))
                .andExpect(jsonPath("$.submitted").value(false))
                .andExpect(jsonPath("$.existingComplaintId").value(1));
    }


    // 3. Missing title
    @Test
    void createComplaint_missingTitle() throws Exception {

        mockMvc.perform(
                        multipart("/api/citizen/complaints")
                                .param("title", "")
                                .param("description", "Garbage is not collected")
                                .param("location", "Bhilai")
                                .with(user("citizen@example.com").roles("CITIZEN"))
                                .with(csrf())
                )
                .andExpect(status().isBadRequest());
    }


    // 4. Missing description
    @Test
    void createComplaint_missingDescription() throws Exception {

        mockMvc.perform(
                        multipart("/api/citizen/complaints")
                                .param("title", "Garbage Problem")
                                .param("description", "")
                                .param("location", "Bhilai")
                                .with(user("citizen@example.com").roles("CITIZEN"))
                                .with(csrf())
                )
                .andExpect(status().isBadRequest());
    }


    // 5. Missing location
    @Test
    void createComplaint_missingLocation() throws Exception {

        mockMvc.perform(
                        multipart("/api/citizen/complaints")
                                .param("title", "Garbage Problem")
                                .param("description", "Garbage is not collected")
                                .param("location", "")
                                .with(user("citizen@example.com").roles("CITIZEN"))
                                .with(csrf())
                )
                .andExpect(status().isBadRequest());
    }


    // 6. Get citizen complaints
    @Test
    void getMyComplaints_success() throws Exception {

        when(complaintService.getCitizenComplaints("citizen@example.com"))
                .thenReturn(List.of());

        mockMvc.perform(
                        get("/api/citizen/complaints")
                                .with(user("citizen@example.com").roles("CITIZEN"))
                )
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }


    // 7. Get public complaints
    @Test
    void getPublicComplaints_success() throws Exception {

        when(complaintService.getPublicComplaints())
                .thenReturn(List.<PublicComplaintResponseDto>of());

        mockMvc.perform(
                        get("/api/citizen/complaints/public")
                                .with(user("citizen@example.com").roles("CITIZEN"))
                )
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }


    // 8. Filter public complaints by status
    @Test
    void getPublicComplaintsByStatus_success() throws Exception {

        when(complaintService.getPublicComplaintsByStatus(
                ComplaintStatus.PENDING
        )).thenReturn(List.<PublicComplaintResponseDto>of());

        mockMvc.perform(
                        get("/api/citizen/complaints/public/filter")
                                .param("status", "PENDING")
                                .with(user("citizen@example.com").roles("CITIZEN"))
                )
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }


    // 9. Filter public complaints by location
    @Test
    void getPublicComplaintsByLocation_success() throws Exception {

        when(complaintService.getPublicComplaintsByLocation("Bhilai"))
                .thenReturn(List.<PublicComplaintResponseDto>of());

        mockMvc.perform(
                        get("/api/citizen/complaints/public/location")
                                .param("location", "Bhilai")
                                .with(user("citizen@example.com").roles("CITIZEN"))
                )
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }


    // 10. Complaint photo not uploaded
    @Test
    void getComplaintPhoto_noPhoto() throws Exception {

        Complaint complaint = mock(Complaint.class);

        when(complaintService.getComplaintById(1L))
                .thenReturn(complaint);

        when(complaint.getPhotoPath()).thenReturn(null);

        mockMvc.perform(
                        get("/api/citizen/complaints/1/photo")
                                .with(user("citizen@example.com").roles("CITIZEN"))
                )
                .andExpect(status().isNotFound());
    }


    // 11. Public complaint photo not uploaded
    @Test
    void getPublicComplaintPhoto_noPhoto() throws Exception {

        Complaint complaint = mock(Complaint.class);

        when(complaintService.getComplaintById(1L))
                .thenReturn(complaint);

        when(complaint.getPhotoPath()).thenReturn("");

        mockMvc.perform(
                        get("/api/citizen/complaints/public/1/photo")
                                .with(user("citizen@example.com").roles("CITIZEN"))
                )
                .andExpect(status().isNotFound());
    }


    // 12. Complaint photo available
    @Test
    void getComplaintPhoto_success() throws Exception {

        Complaint complaint = mock(Complaint.class);
        Resource resource = mock(Resource.class);

        when(complaintService.getComplaintById(1L))
                .thenReturn(complaint);

        when(complaint.getPhotoPath())
                .thenReturn("uploads/photo.jpg");

        when(complaintPhotoService.loadPhoto("uploads/photo.jpg"))
                .thenReturn(resource);

        when(resource.exists()).thenReturn(true);
        when(resource.isReadable()).thenReturn(true);

        mockMvc.perform(
                        get("/api/citizen/complaints/1/photo")
                                .with(user("citizen@example.com").roles("CITIZEN"))
                )
                .andExpect(status().isOk());
    }


    // 13. Complaint photo resource missing
    @Test
    void getComplaintPhoto_resourceMissing() throws Exception {

        Complaint complaint = mock(Complaint.class);

        when(complaintService.getComplaintById(1L))
                .thenReturn(complaint);

        when(complaint.getPhotoPath())
                .thenReturn("uploads/missing.jpg");

        when(complaintPhotoService.loadPhoto("uploads/missing.jpg"))
                .thenReturn(null);

        mockMvc.perform(
                        get("/api/citizen/complaints/1/photo")
                                .with(user("citizen@example.com").roles("CITIZEN"))
                )
                .andExpect(status().isNotFound());
    }


    // 14. Completion photo not uploaded
    @Test
    void getCompletionPhoto_noPhoto() throws Exception {

        when(complaintService.getCompletionPhotoPath(
                1L,
                "worker@example.com"
        )).thenReturn(null);

        mockMvc.perform(
                        get("/api/citizen/complaints/1/completion-photo")
                                .with(user("worker@example.com").roles("CITIZEN"))
                )
                .andExpect(status().isNotFound());
    }


    // 15. Completion photo resource missing
    @Test
    void getCompletionPhoto_resourceMissing() throws Exception {

        when(complaintService.getCompletionPhotoPath(
                1L,
                "worker@example.com"
        )).thenReturn("uploads/completion.jpg");

        when(complaintPhotoService.loadPhoto("uploads/completion.jpg"))
                .thenReturn(null);

        mockMvc.perform(
                        get("/api/citizen/complaints/1/completion-photo")
                                .with(user("worker@example.com").roles("CITIZEN"))
                )
                .andExpect(status().isNotFound());
    }


    // 16. Update citizen complaint successfully
    @Test
    void updateCitizenComplaint_success() throws Exception {

        Complaint complaint = mock(Complaint.class);

        when(complaintService.updateCitizenComplaint(
                eq(1L),
                eq("citizen@example.com"),
                eq("Updated title"),
                eq("Updated description"),
                eq("Bhilai")
        )).thenReturn(complaint);

        mockMvc.perform(
                        put("/api/citizen/complaints/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "Updated title",
                                          "description": "Updated description",
                                          "location": "Bhilai"
                                        }
                                        """)
                                .with(user("citizen@example.com").roles("CITIZEN"))
                                .with(csrf())
                )
                .andExpect(status().isOk());
    }


    // 17. Invalid update request
    @Test
    void updateCitizenComplaint_invalidRequest() throws Exception {

        mockMvc.perform(
                        put("/api/citizen/complaints/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "",
                                          "description": "",
                                          "location": ""
                                        }
                                        """)
                                .with(user("citizen@example.com").roles("CITIZEN"))
                                .with(csrf())
                )
                .andExpect(status().isBadRequest());
    }
}
