package com.project.familytree.tree.services;

import com.project.familytree.auth.models.User;
import com.project.familytree.auth.services.MailSenderService;
import com.project.familytree.auth.services.UserService;
import com.project.familytree.tree.dto.PersonDTO;
import com.project.familytree.tree.dto.PersonRelationshipRequest;
import com.project.familytree.tree.dto.PersonRequest;
import com.project.familytree.tree.impls.Gender;
import com.project.familytree.tree.impls.RelationshipType;
import com.project.familytree.tree.impls.TreeRole;
import com.project.familytree.tree.models.Person;
import com.project.familytree.tree.models.Relationship;
import com.project.familytree.tree.models.Tree;
import com.project.familytree.tree.models.TreeMembership;
import com.project.familytree.tree.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.AccessDeniedException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TreeServiceTest {

    @Mock private UserService userService;
    @Mock private TreeRepository treeRepository;
    @Mock private TreeMembershipRepository membershipRepository;
    @Mock private MailSenderService mailSenderService;
    @Mock private InvitationRepository invitationRepository;
    @Mock private PersonRepository personRepository;
    @Mock private RelationshipRepository relationshipRepository;
    @Mock private MediaFileRepository mediaFileRepository;
    @Mock private S3Service s3Service;

    @InjectMocks
    private TreeService treeService;

    private Tree tree;
    private User user;
    private Person person;

    @BeforeEach
    void setUp() {
        tree = new Tree();
        tree.setId(1L);
        tree.setName("Тестовое дерево");

        user = new User();
        user.setId(10L);
        user.setFirstName("Иван");
        user.setLastName("Иванов");
        user.setEmail("ivan@test.com");

        person = new Person(tree, "Иван", "Иванов", "Иванович", Gender.MALE);
        person.setId(100L);
        person.setBirthDate(LocalDate.of(1990, 1, 1));
        person.setBirthPlace("Москва");
        person.setBiography("Тестовая биография");
    }

    // ─── createTree ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createTree: создаёт дерево и назначает владельца")
    void createTree_savesTreeAndOwnerMembership() {
        when(treeRepository.save(any(Tree.class))).thenReturn(tree);
        when(userService.findById(10L)).thenReturn(user);
        when(membershipRepository.save(any(TreeMembership.class))).thenAnswer(i -> i.getArgument(0));

        treeService.createTree("Тестовое дерево", 10L);

        verify(treeRepository).save(any(Tree.class));
        verify(membershipRepository).save(argThat(m -> m.getRole() == TreeRole.OWNER));
    }

    // ─── canEdit / canView ────────────────────────────────────────────────────────

    @Test
    @DisplayName("canEdit: возвращает true для EDITOR")
    void canEdit_returnsTrueForEditor() {
        TreeMembership membership = new TreeMembership();
        membership.setRole(TreeRole.EDITOR);
        when(membershipRepository.findByTreeIdAndUserId(1L, 10L)).thenReturn(Optional.of(membership));

        assertThat(treeService.canEdit(1L, 10L)).isTrue();
    }

    @Test
    @DisplayName("canEdit: возвращает false для VIEWER")
    void canEdit_returnsFalseForViewer() {
        TreeMembership membership = new TreeMembership();
        membership.setRole(TreeRole.VIEWER);
        when(membershipRepository.findByTreeIdAndUserId(1L, 10L)).thenReturn(Optional.of(membership));

        assertThat(treeService.canEdit(1L, 10L)).isFalse();
    }

    @Test
    @DisplayName("canView: возвращает true для VIEWER")
    void canView_returnsTrueForViewer() {
        TreeMembership membership = new TreeMembership();
        membership.setRole(TreeRole.VIEWER);
        when(membershipRepository.findByTreeIdAndUserId(1L, 10L)).thenReturn(Optional.of(membership));

        assertThat(treeService.canView(1L, 10L)).isTrue();
    }

    // ─── createPerson ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createPerson: создаёт персону с новыми полями")
    void createPerson_savesPersonWithAllFields() throws AccessDeniedException {
        TreeMembership editorMembership = new TreeMembership();
        editorMembership.setRole(TreeRole.EDITOR);
        when(membershipRepository.findByTreeIdAndUserId(1L, 10L)).thenReturn(Optional.of(editorMembership));
        when(treeRepository.findById(1L)).thenReturn(Optional.of(tree));
        when(personRepository.save(any(Person.class))).thenAnswer(i -> {
            Person p = i.getArgument(0);
            p.setId(100L);
            return p;
        });

        PersonRequest request = new PersonRequest(
                "Иван", "Иванов", "Иванович",
                LocalDate.of(1990, 1, 1), null,
                "Москва", null, null, "Биография",
                Gender.MALE
        );

        Person result = treeService.createPerson(1L, request, 10L);

        assertThat(result.getFirstName()).isEqualTo("Иван");
        assertThat(result.getLastName()).isEqualTo("Иванов");
        assertThat(result.getBirthPlace()).isEqualTo("Москва");
        assertThat(result.getBiography()).isEqualTo("Биография");
        assertThat(result.getGender()).isEqualTo(Gender.MALE);
        verify(personRepository).save(any(Person.class));
    }

    @Test
    @DisplayName("createPerson: бросает AccessDeniedException для VIEWER")
    void createPerson_throwsForViewer() {
        TreeMembership viewerMembership = new TreeMembership();
        viewerMembership.setRole(TreeRole.VIEWER);
        when(membershipRepository.findByTreeIdAndUserId(1L, 10L)).thenReturn(Optional.of(viewerMembership));

        PersonRequest request = new PersonRequest(
                "Иван", "Иванов", null, null, null, null, null, null, null, Gender.MALE
        );

        assertThatThrownBy(() -> treeService.createPerson(1L, request, 10L))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ─── updatePerson ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("updatePerson: обновляет все поля включая новые")
    void updatePerson_updatesAllFields() throws AccessDeniedException {
        TreeMembership editorMembership = new TreeMembership();
        editorMembership.setRole(TreeRole.EDITOR);
        when(membershipRepository.findByTreeIdAndUserId(1L, 10L)).thenReturn(Optional.of(editorMembership));
        when(personRepository.findById(100L)).thenReturn(Optional.of(person));
        when(personRepository.save(any(Person.class))).thenAnswer(i -> i.getArgument(0));

        PersonRequest request = new PersonRequest(
                "Пётр", "Петров", "Петрович",
                LocalDate.of(1985, 5, 15), LocalDate.of(2020, 3, 10),
                "Санкт-Петербург", "Новосибирск", null, "Обновлённая биография",
                Gender.MALE
        );

        Person result = treeService.updatePerson(1L, 100L, request, 10L);

        assertThat(result.getFirstName()).isEqualTo("Пётр");
        assertThat(result.getBirthPlace()).isEqualTo("Санкт-Петербург");
        assertThat(result.getDeathPlace()).isEqualTo("Новосибирск");
        assertThat(result.getBiography()).isEqualTo("Обновлённая биография");
    }

    // ─── addRelationship ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("addRelationship: создаёт PARENT_CHILD связь")
    void addRelationship_createsParentChildRelationship() throws AccessDeniedException {
        Person parent = new Person(tree, "Родитель", "Иванов", null, Gender.MALE);
        parent.setId(1L);
        Person child = new Person(tree, "Ребёнок", "Иванов", null, Gender.FEMALE);
        child.setId(2L);

        TreeMembership editorMembership = new TreeMembership();
        editorMembership.setRole(TreeRole.EDITOR);
        when(membershipRepository.findByTreeIdAndUserId(1L, 10L)).thenReturn(Optional.of(editorMembership));
        when(personRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(personRepository.findById(2L)).thenReturn(Optional.of(child));
        when(relationshipRepository.findByTreeIdAndPersonsAndType(1L, 1L, 2L, RelationshipType.PARENT_CHILD))
                .thenReturn(Optional.empty());
        when(treeRepository.findById(1L)).thenReturn(Optional.of(tree));
        when(relationshipRepository.save(any(Relationship.class))).thenAnswer(i -> i.getArgument(0));

        PersonRelationshipRequest request = new PersonRelationshipRequest(1L, 2L, RelationshipType.PARENT_CHILD);
        treeService.addRelationship(1L, request, 10L);

        verify(relationshipRepository).save(argThat(r ->
                r.getType() == RelationshipType.PARENT_CHILD &&
                r.getPerson1().getId().equals(1L) &&
                r.getPerson2().getId().equals(2L)
        ));
    }

    @Test
    @DisplayName("addRelationship: бросает исключение если связь уже существует")
    void addRelationship_throwsIfAlreadyExists() {
        Person parent = new Person(tree, "Родитель", "Иванов", null, Gender.MALE);
        parent.setId(1L);
        Person child = new Person(tree, "Ребёнок", "Иванов", null, Gender.FEMALE);
        child.setId(2L);

        TreeMembership editorMembership = new TreeMembership();
        editorMembership.setRole(TreeRole.EDITOR);
        when(membershipRepository.findByTreeIdAndUserId(1L, 10L)).thenReturn(Optional.of(editorMembership));
        when(personRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(personRepository.findById(2L)).thenReturn(Optional.of(child));

        Relationship existing = new Relationship(tree, parent, child, RelationshipType.PARENT_CHILD);
        when(relationshipRepository.findByTreeIdAndPersonsAndType(1L, 1L, 2L, RelationshipType.PARENT_CHILD))
                .thenReturn(Optional.of(existing));

        PersonRelationshipRequest request = new PersonRelationshipRequest(1L, 2L, RelationshipType.PARENT_CHILD);

        assertThatThrownBy(() -> treeService.addRelationship(1L, request, 10L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("уже существует");
    }

    // ─── convertToDTO ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("convertToDTO: корректно маппит все поля персоны")
    void convertToDTO_mapsAllFields() {
        person.setBirthPlace("Москва");
        person.setDeathPlace(null);
        person.setBiography("Биография");

        when(relationshipRepository.findByTreeIdAndPersonId(1L, 100L)).thenReturn(List.of());

        PersonDTO dto = treeService.convertToDTO(person, 1L);

        assertThat(dto.getId()).isEqualTo(100L);
        assertThat(dto.getTreeId()).isEqualTo(1L);
        assertThat(dto.getFirstName()).isEqualTo("Иван");
        assertThat(dto.getLastName()).isEqualTo("Иванов");
        assertThat(dto.getMiddleName()).isEqualTo("Иванович");
        assertThat(dto.getBirthPlace()).isEqualTo("Москва");
        assertThat(dto.getDeathPlace()).isNull();
        assertThat(dto.getBiography()).isEqualTo("Биография");
        assertThat(dto.getGender()).isEqualTo(Gender.MALE);
        assertThat(dto.getFullName()).isEqualTo("Иванов Иван Иванович");
        assertThat(dto.getRelationships()).isEmpty();
    }

    @Test
    @DisplayName("convertToDTO: включает связи персоны")
    void convertToDTO_includesRelationships() {
        Person parent = new Person(tree, "Родитель", "Иванов", null, Gender.MALE);
        parent.setId(50L);

        Relationship rel = new Relationship(tree, parent, person, RelationshipType.PARENT_CHILD);
        rel.setId(200L);

        when(relationshipRepository.findByTreeIdAndPersonId(1L, 100L)).thenReturn(List.of(rel));

        PersonDTO dto = treeService.convertToDTO(person, 1L);

        assertThat(dto.getRelationships()).hasSize(1);
        assertThat(dto.getRelationships().get(0).getType()).isEqualTo(RelationshipType.PARENT_CHILD);
        assertThat(dto.getRelationships().get(0).getPerson1Id()).isEqualTo(50L);
        assertThat(dto.getRelationships().get(0).getPerson2Id()).isEqualTo(100L);
    }

    // ─── deletePerson ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deletePerson: удаляет персону, её связи и медиафайлы")
    void deletePerson_deletesPersonAndRelationshipsAndMedia() throws AccessDeniedException {
        TreeMembership editorMembership = new TreeMembership();
        editorMembership.setRole(TreeRole.EDITOR);
        when(membershipRepository.findByTreeIdAndUserId(1L, 10L)).thenReturn(Optional.of(editorMembership));
        when(personRepository.findById(100L)).thenReturn(Optional.of(person));
        when(relationshipRepository.findByTreeIdAndPersonId(1L, 100L)).thenReturn(List.of());

        treeService.deletePerson(1L, 100L, 10L);

        verify(relationshipRepository).deleteAll(anyList());
        verify(mediaFileRepository).deleteByPersonId(100L);
        verify(personRepository).delete(person);
    }
}
