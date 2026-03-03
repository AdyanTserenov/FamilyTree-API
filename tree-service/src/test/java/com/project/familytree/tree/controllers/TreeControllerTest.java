package com.project.familytree.tree.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.familytree.auth.security.JwtUtils;
import com.project.familytree.auth.services.UserService;
import com.project.familytree.tree.dto.InviteRequest;
import com.project.familytree.tree.dto.TreeDTO;
import com.project.familytree.tree.dto.TreeMemberDTO;
import com.project.familytree.tree.dto.TreeRequest;
import com.project.familytree.tree.impls.TreeRole;
import com.project.familytree.tree.services.TreeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.AccessDeniedException;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc slice tests for {@link TreeController}.
 *
 * Uses @WebMvcTest to load only the web layer (no full Spring context).
 * TreeService and UserService are mocked with @MockBean.
 * CSRF is disabled in the auth-starter SecurityConfig, so no csrf() needed.
 * @WithMockUser provides a UserDetails principal so the controller can cast it.
 * JwtUtils is mocked so TokenFilter (real bean) won't validate any JWT and
 * won't override the SecurityContext set by @WithMockUser.
 */
@WebMvcTest(TreeController.class)
class TreeControllerTest {

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

    @BeforeEach
    void setUp() {
        // userService.findIdByDetails() is called by every controller method
        when(userService.findIdByDetails(any(UserDetails.class))).thenReturn(USER_ID);
    }

    // ─── GET /trees ───────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "user@test.com")
    void getUserTrees_returnsListOfTrees() throws Exception {
        TreeDTO tree1 = new TreeDTO(1L, "Дерево Ивановых", Instant.now(), TreeRole.OWNER);
        TreeDTO tree2 = new TreeDTO(2L, "Дерево Петровых", Instant.now(), TreeRole.VIEWER);
        when(treeService.getUserTrees(USER_ID)).thenReturn(List.of(tree1, tree2));

        mockMvc.perform(get("/trees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("Дерево Ивановых"))
                .andExpect(jsonPath("$.data[1].name").value("Дерево Петровых"));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void getUserTrees_returnsEmptyList_whenNoTrees() throws Exception {
        when(treeService.getUserTrees(USER_ID)).thenReturn(List.of());

        mockMvc.perform(get("/trees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    // ─── POST /trees ──────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "user@test.com")
    void createTree_returns200_withValidRequest() throws Exception {
        TreeRequest request = new TreeRequest();
        request.setName("Новое дерево");

        mockMvc.perform(post("/trees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(treeService).createTree("Новое дерево", USER_ID);
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void createTree_returns400_whenNameIsBlank() throws Exception {
        TreeRequest request = new TreeRequest();
        request.setName("");

        mockMvc.perform(post("/trees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void createTree_returns400_whenNameIsNull() throws Exception {
        // Send JSON with null name — @NotBlank should reject it
        mockMvc.perform(post("/trees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":null}"))
                .andExpect(status().isBadRequest());
    }

    // ─── PUT /trees/{treeId} ──────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "user@test.com")
    void updateTree_returns200_withValidRequest() throws Exception {
        TreeDTO updated = new TreeDTO(1L, "Переименованное дерево", Instant.now(), TreeRole.OWNER);
        when(treeService.updateTree(eq(1L), eq("Переименованное дерево"), eq(USER_ID)))
                .thenReturn(updated);

        TreeRequest request = new TreeRequest();
        request.setName("Переименованное дерево");

        mockMvc.perform(put("/trees/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Переименованное дерево"));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void updateTree_returns400_whenNameIsBlank() throws Exception {
        TreeRequest request = new TreeRequest();
        request.setName("   ");

        mockMvc.perform(put("/trees/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void updateTree_propagatesAccessDenied_whenNotOwner() throws Exception {
        when(treeService.updateTree(eq(1L), anyString(), eq(USER_ID)))
                .thenThrow(new AccessDeniedException("Нет прав"));

        TreeRequest request = new TreeRequest();
        request.setName("Дерево");

        mockMvc.perform(put("/trees/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ─── DELETE /trees/{treeId} ───────────────────────────────────────────────

    @Test
    @WithMockUser(username = "user@test.com")
    void deleteTree_returns200() throws Exception {
        doNothing().when(treeService).deleteTree(1L, USER_ID);

        mockMvc.perform(delete("/trees/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(treeService).deleteTree(1L, USER_ID);
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void deleteTree_propagatesAccessDenied_whenNotOwner() throws Exception {
        doThrow(new AccessDeniedException("Нет прав")).when(treeService).deleteTree(1L, USER_ID);

        mockMvc.perform(delete("/trees/1"))
                .andExpect(status().isForbidden());
    }

    // ─── POST /trees/{treeId}/invite ──────────────────────────────────────────

    @Test
    @WithMockUser(username = "user@test.com")
    void inviteByEmail_returns200_withValidRequest() throws Exception {
        InviteRequest request = new InviteRequest("guest@example.com", TreeRole.VIEWER);

        doNothing().when(treeService).sendInviteByEmail(
                eq(1L), eq("guest@example.com"), eq(TreeRole.VIEWER), eq(USER_ID));

        mockMvc.perform(post("/trees/1/invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(treeService).sendInviteByEmail(1L, "guest@example.com", TreeRole.VIEWER, USER_ID);
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void inviteByEmail_returns400_whenEmailInvalid() throws Exception {
        InviteRequest request = new InviteRequest("not-an-email", TreeRole.VIEWER);

        mockMvc.perform(post("/trees/1/invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ─── POST /trees/{treeId}/invite-link ─────────────────────────────────────

    @Test
    @WithMockUser(username = "user@test.com")
    void generateInviteLink_returns200_withToken() throws Exception {
        InviteRequest request = new InviteRequest("guest@example.com", TreeRole.EDITOR);
        when(treeService.createInviteToken(eq(1L), eq("guest@example.com"), eq(TreeRole.EDITOR), eq(USER_ID)))
                .thenReturn("abc123token");

        mockMvc.perform(post("/trees/1/invite-link")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.inviteLink").value(org.hamcrest.Matchers.containsString("abc123token")));
    }

    // ─── GET /trees/invite/{token} ────────────────────────────────────────────

    @Test
    @WithMockUser(username = "user@test.com")
    void acceptInvite_returns200() throws Exception {
        doNothing().when(treeService).acceptInvitation("abc123token", USER_ID);

        mockMvc.perform(get("/trees/invite/abc123token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(treeService).acceptInvitation("abc123token", USER_ID);
    }

    // ─── GET /trees/{treeId}/members ──────────────────────────────────────────

    @Test
    @WithMockUser(username = "user@test.com")
    void getMembers_returns200_whenUserCanView() throws Exception {
        when(treeService.canView(1L, USER_ID)).thenReturn(true);
        TreeMemberDTO member = new TreeMemberDTO(
                USER_ID, "Иван", "Иванов", null, "user@test.com", TreeRole.OWNER, Instant.now());
        when(treeService.getMembers(1L)).thenReturn(List.of(member));

        mockMvc.perform(get("/trees/1/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].role").value("OWNER"))
                .andExpect(jsonPath("$.data[0].firstName").value("Иван"));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void getMembers_returns403_whenUserCannotView() throws Exception {
        when(treeService.canView(1L, USER_ID)).thenReturn(false);

        mockMvc.perform(get("/trees/1/members"))
                .andExpect(status().isForbidden());
    }
}
