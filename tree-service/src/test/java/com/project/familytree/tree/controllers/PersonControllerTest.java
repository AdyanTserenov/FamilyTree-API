package com.project.familytree.tree.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.familytree.auth.security.JwtUtils;
import com.project.familytree.auth.services.UserService;
import com.project.familytree.tree.dto.PersonDTO;
import com.project.familytree.tree.dto.PersonRelationshipRequest;
import com.project.familytree.tree.dto.PersonRequest;
import com.project.familytree.tree.impls.Gender;
import com.project.familytree.tree.impls.RelationshipType;
import com.project.familytree.tree.models.Person;
import com.project.familytree.tree.services.TreeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.AccessDeniedException;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc slice tests for {@link PersonController}.
 *
 * Uses @WebMvcTest to load only the web layer.
 * TreeService and UserService are mocked with @MockBean.
 * CSRF is disabled in the auth-starter SecurityConfig.
 * @WithMockUser provides a UserDetails principal for the controller.
 * JwtUtils is mocked so TokenFilter (real bean) won't validate any JWT token
 * and won't override the SecurityContext set by @WithMockUser.
 */
@WebMvcTest(PersonController.class)
class PersonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TreeService treeService;

    // From auth-starter — must be mocked so the security filter chain can be built.
    // UserService is used by both the controller and TokenFilter.
    @MockBean
    private UserService userService;

    // JwtUtils is mocked so TokenFilter won't validate any JWT token
    // (validateJwtToken returns false by default), leaving @WithMockUser's
    // SecurityContext intact.
    @MockBean
    private JwtUtils jwtUtils;

    private static final Long USER_ID = 42L;
    private static final Long TREE_ID = 1L;
    private static final Long PERSON_ID = 10L;

    /** A minimal valid PersonRequest (firstName, lastName, gender are @NotBlank/@NotNull). */
    private PersonRequest validRequest() {
        return new PersonRequest(
                "Иван", "Иванов", "Иванович",
                LocalDate.of(1990, 1, 1), null,
                "Москва", null, "Биография", Gender.MALE);
    }

    /** A minimal PersonDTO returned by the service. */
    private PersonDTO personDTO() {
        return new PersonDTO(
                PERSON_ID, TREE_ID, "Иван", "Иванов", "Иванович",
                LocalDate.of(1990, 1, 1), null,
                "Москва", null, "Биография", null,
                Gender.MALE, List.of(), "Иванов Иван Иванович");
    }

    @BeforeEach
    void setUp() {
        when(userService.findIdByDetails(any(UserDetails.class))).thenReturn(USER_ID);
    }

    // ─── POST /trees/{treeId}/persons ─────────────────────────────────────────

    @Test
    @WithMockUser(username = "user@test.com")
    void createPerson_returns200_withValidRequest() throws Exception {
        Person mockPerson = mock(Person.class);
        when(mockPerson.getId()).thenReturn(PERSON_ID);
        when(treeService.createPerson(eq(TREE_ID), any(PersonRequest.class), eq(USER_ID)))
                .thenReturn(mockPerson);
        when(treeService.convertToDTO(mockPerson, TREE_ID)).thenReturn(personDTO());

        mockMvc.perform(post("/trees/{treeId}/persons", TREE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.firstName").value("Иван"))
                .andExpect(jsonPath("$.data.lastName").value("Иванов"));

        verify(treeService).createPerson(eq(TREE_ID), any(PersonRequest.class), eq(USER_ID));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void createPerson_returns400_whenFirstNameIsBlank() throws Exception {
        PersonRequest bad = new PersonRequest(
                "", "Иванов", null, null, null, null, null, null, Gender.MALE);

        mockMvc.perform(post("/trees/{treeId}/persons", TREE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void createPerson_returns400_whenGenderIsNull() throws Exception {
        // gender is @NotNull — omit it from JSON
        String json = "{\"firstName\":\"Иван\",\"lastName\":\"Иванов\"}";

        mockMvc.perform(post("/trees/{treeId}/persons", TREE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void createPerson_returns403_whenAccessDenied() throws Exception {
        when(treeService.createPerson(eq(TREE_ID), any(PersonRequest.class), eq(USER_ID)))
                .thenThrow(new AccessDeniedException("Нет прав"));

        mockMvc.perform(post("/trees/{treeId}/persons", TREE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden());
    }

    // ─── GET /trees/{treeId}/persons ──────────────────────────────────────────

    @Test
    @WithMockUser(username = "user@test.com")
    void getPersons_returns200_withList() throws Exception {
        when(treeService.getPersons(TREE_ID, USER_ID)).thenReturn(List.of(personDTO()));

        mockMvc.perform(get("/trees/{treeId}/persons", TREE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].firstName").value("Иван"));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void getPersons_returnsEmptyList_whenNoPersons() throws Exception {
        when(treeService.getPersons(TREE_ID, USER_ID)).thenReturn(List.of());

        mockMvc.perform(get("/trees/{treeId}/persons", TREE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void getPersons_returns403_whenAccessDenied() throws Exception {
        when(treeService.getPersons(TREE_ID, USER_ID))
                .thenThrow(new AccessDeniedException("Нет прав"));

        mockMvc.perform(get("/trees/{treeId}/persons", TREE_ID))
                .andExpect(status().isForbidden());
    }

    // ─── GET /trees/{treeId}/persons/{personId} ───────────────────────────────

    @Test
    @WithMockUser(username = "user@test.com")
    void getPerson_returns200() throws Exception {
        when(treeService.getPerson(TREE_ID, PERSON_ID, USER_ID)).thenReturn(personDTO());

        mockMvc.perform(get("/trees/{treeId}/persons/{personId}", TREE_ID, PERSON_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(PERSON_ID))
                .andExpect(jsonPath("$.data.fullName").value("Иванов Иван Иванович"));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void getPerson_returns403_whenAccessDenied() throws Exception {
        when(treeService.getPerson(TREE_ID, PERSON_ID, USER_ID))
                .thenThrow(new AccessDeniedException("Нет прав"));

        mockMvc.perform(get("/trees/{treeId}/persons/{personId}", TREE_ID, PERSON_ID))
                .andExpect(status().isForbidden());
    }

    // ─── PUT /trees/{treeId}/persons/{personId} ───────────────────────────────

    @Test
    @WithMockUser(username = "user@test.com")
    void updatePerson_returns200_withValidRequest() throws Exception {
        Person mockPerson = mock(Person.class);
        when(treeService.updatePerson(eq(TREE_ID), eq(PERSON_ID), any(PersonRequest.class), eq(USER_ID)))
                .thenReturn(mockPerson);
        when(treeService.convertToDTO(mockPerson, TREE_ID)).thenReturn(personDTO());

        mockMvc.perform(put("/trees/{treeId}/persons/{personId}", TREE_ID, PERSON_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName").value("Иван"));

        verify(treeService).updatePerson(eq(TREE_ID), eq(PERSON_ID), any(PersonRequest.class), eq(USER_ID));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void updatePerson_returns400_whenLastNameIsBlank() throws Exception {
        PersonRequest bad = new PersonRequest(
                "Иван", "", null, null, null, null, null, null, Gender.MALE);

        mockMvc.perform(put("/trees/{treeId}/persons/{personId}", TREE_ID, PERSON_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void updatePerson_returns403_whenAccessDenied() throws Exception {
        when(treeService.updatePerson(eq(TREE_ID), eq(PERSON_ID), any(PersonRequest.class), eq(USER_ID)))
                .thenThrow(new AccessDeniedException("Нет прав"));

        mockMvc.perform(put("/trees/{treeId}/persons/{personId}", TREE_ID, PERSON_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden());
    }

    // ─── DELETE /trees/{treeId}/persons/{personId} ────────────────────────────

    @Test
    @WithMockUser(username = "user@test.com")
    void deletePerson_returns200() throws Exception {
        doNothing().when(treeService).deletePerson(TREE_ID, PERSON_ID, USER_ID);

        mockMvc.perform(delete("/trees/{treeId}/persons/{personId}", TREE_ID, PERSON_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(treeService).deletePerson(TREE_ID, PERSON_ID, USER_ID);
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void deletePerson_returns403_whenAccessDenied() throws Exception {
        doThrow(new AccessDeniedException("Нет прав"))
                .when(treeService).deletePerson(TREE_ID, PERSON_ID, USER_ID);

        mockMvc.perform(delete("/trees/{treeId}/persons/{personId}", TREE_ID, PERSON_ID))
                .andExpect(status().isForbidden());
    }

    // ─── POST /trees/{treeId}/persons/relationships ───────────────────────────

    @Test
    @WithMockUser(username = "user@test.com")
    void addRelationship_returns200_withValidRequest() throws Exception {
        PersonRelationshipRequest request =
                new PersonRelationshipRequest(10L, 20L, RelationshipType.PARENT_CHILD);

        doNothing().when(treeService).addRelationship(eq(TREE_ID), any(PersonRelationshipRequest.class), eq(USER_ID));

        mockMvc.perform(post("/trees/{treeId}/persons/relationships", TREE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(treeService).addRelationship(eq(TREE_ID), any(PersonRelationshipRequest.class), eq(USER_ID));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void addRelationship_returns400_whenPerson1IdIsNull() throws Exception {
        // person1Id is @NotNull
        String json = "{\"person2Id\":20,\"type\":\"PARENT_CHILD\"}";

        mockMvc.perform(post("/trees/{treeId}/persons/relationships", TREE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void addRelationship_returns403_whenAccessDenied() throws Exception {
        PersonRelationshipRequest request =
                new PersonRelationshipRequest(10L, 20L, RelationshipType.PARTNERSHIP);

        doThrow(new AccessDeniedException("Нет прав"))
                .when(treeService).addRelationship(eq(TREE_ID), any(PersonRelationshipRequest.class), eq(USER_ID));

        mockMvc.perform(post("/trees/{treeId}/persons/relationships", TREE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ─── DELETE /trees/{treeId}/persons/relationships ─────────────────────────

    @Test
    @WithMockUser(username = "user@test.com")
    void removeRelationship_returns200() throws Exception {
        PersonRelationshipRequest request =
                new PersonRelationshipRequest(10L, 20L, RelationshipType.PARENT_CHILD);

        doNothing().when(treeService).removeRelationship(eq(TREE_ID), any(PersonRelationshipRequest.class), eq(USER_ID));

        mockMvc.perform(delete("/trees/{treeId}/persons/relationships", TREE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(treeService).removeRelationship(eq(TREE_ID), any(PersonRelationshipRequest.class), eq(USER_ID));
    }

    // ─── GET /trees/{treeId}/persons/graph ────────────────────────────────────

    @Test
    @WithMockUser(username = "user@test.com")
    void getTreeGraph_returns200_withPersonList() throws Exception {
        when(treeService.getTreeGraph(TREE_ID, USER_ID)).thenReturn(List.of(personDTO()));

        mockMvc.perform(get("/trees/{treeId}/persons/graph", TREE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(PERSON_ID));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void getTreeGraph_returns403_whenAccessDenied() throws Exception {
        when(treeService.getTreeGraph(TREE_ID, USER_ID))
                .thenThrow(new AccessDeniedException("Нет прав"));

        mockMvc.perform(get("/trees/{treeId}/persons/graph", TREE_ID))
                .andExpect(status().isForbidden());
    }

    // ─── GET /trees/{treeId}/persons/search ───────────────────────────────────

    @Test
    @WithMockUser(username = "user@test.com")
    void searchPersons_returns200_withResults() throws Exception {
        when(treeService.searchPersons(eq(TREE_ID), eq("Иван"), isNull(), isNull(), isNull(), isNull(), eq(USER_ID)))
                .thenReturn(List.of(personDTO()));

        mockMvc.perform(get("/trees/{treeId}/persons/search", TREE_ID)
                        .param("q", "Иван"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].firstName").value("Иван"));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void searchPersons_returnsEmptyList_whenNoMatch() throws Exception {
        when(treeService.searchPersons(eq(TREE_ID), eq("Несуществующий"), isNull(), isNull(), isNull(), isNull(), eq(USER_ID)))
                .thenReturn(List.of());

        mockMvc.perform(get("/trees/{treeId}/persons/search", TREE_ID)
                        .param("q", "Несуществующий"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    // ─── POST /trees/{treeId}/persons/{personId}/avatar ───────────────────────

    @Test
    @WithMockUser(username = "user@test.com")
    void uploadAvatar_returns200_withPersonDTO() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.jpg", MediaType.IMAGE_JPEG_VALUE, "fake-image-bytes".getBytes());

        PersonDTO withAvatar = new PersonDTO(
                PERSON_ID, TREE_ID, "Иван", "Иванов", null,
                null, null, null, null, null,
                "https://s3.example.com/avatar.jpg",
                Gender.MALE, List.of(), "Иванов Иван");
        when(treeService.uploadAvatar(eq(TREE_ID), eq(PERSON_ID), any(), eq(USER_ID)))
                .thenReturn(withAvatar);

        mockMvc.perform(multipart("/trees/{treeId}/persons/{personId}/avatar", TREE_ID, PERSON_ID)
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.avatarUrl").value("https://s3.example.com/avatar.jpg"));

        verify(treeService).uploadAvatar(eq(TREE_ID), eq(PERSON_ID), any(), eq(USER_ID));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void uploadAvatar_returns403_whenAccessDenied() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.jpg", MediaType.IMAGE_JPEG_VALUE, "fake-image-bytes".getBytes());

        when(treeService.uploadAvatar(eq(TREE_ID), eq(PERSON_ID), any(), eq(USER_ID)))
                .thenThrow(new AccessDeniedException("Нет прав"));

        mockMvc.perform(multipart("/trees/{treeId}/persons/{personId}/avatar", TREE_ID, PERSON_ID)
                        .file(file))
                .andExpect(status().isForbidden());
    }
}
