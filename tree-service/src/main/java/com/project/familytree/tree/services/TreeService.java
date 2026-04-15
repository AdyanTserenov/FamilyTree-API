package com.project.familytree.tree.services;

import com.project.familytree.auth.services.MailSenderService;
import com.project.familytree.auth.services.UserService;
import com.project.familytree.tree.dto.PersonDTO;
import com.project.familytree.tree.dto.PersonHistoryDTO;
import com.project.familytree.tree.dto.PersonRelationshipRequest;
import com.project.familytree.tree.dto.PersonRequest;
import com.project.familytree.tree.dto.RelationshipDTO;
import com.project.familytree.tree.dto.TreeDTO;
import com.project.familytree.tree.dto.TreeMemberDTO;
import com.project.familytree.tree.impls.HistoryAction;
import com.project.familytree.tree.impls.NotificationType;
import com.project.familytree.tree.impls.RelationshipType;
import com.project.familytree.tree.impls.TreeRole;
import com.project.familytree.tree.models.Invitation;
import com.project.familytree.tree.models.Person;
import com.project.familytree.tree.models.PersonHistory;
import com.project.familytree.tree.models.Relationship;
import com.project.familytree.tree.models.Tree;
import com.project.familytree.tree.models.TreeMembership;
import com.project.familytree.tree.repositories.InvitationRepository;
import com.project.familytree.tree.repositories.MediaFileRepository;
import com.project.familytree.tree.repositories.PersonHistoryRepository;
import com.project.familytree.tree.repositories.PersonRepository;
import com.project.familytree.tree.repositories.RelationshipRepository;
import com.project.familytree.tree.repositories.TreeMembershipRepository;
import com.project.familytree.tree.repositories.TreeRepository;
import com.project.familytree.tree.exceptions.BusinessException;
import com.project.familytree.tree.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TreeService {

    private final UserService userService;
    private final TreeRepository treeRepository;
    private final TreeMembershipRepository membershipRepository;
    private final MailSenderService mailSenderService;
    private final InvitationRepository invitationRepository;
    private final PersonRepository personRepository;
    private final RelationshipRepository relationshipRepository;
    private final MediaFileRepository mediaFileRepository;
    private final S3Service s3Service;
    private final PersonHistoryRepository personHistoryRepository;
    private final NotificationService notificationService;

    @org.springframework.beans.factory.annotation.Value("${app.base-url:http://localhost:3000}")
    private String baseUrl;

    public TreeService(UserService userService,
                       TreeRepository treeRepository,
                       TreeMembershipRepository membershipRepository,
                       MailSenderService mailSenderService,
                       InvitationRepository invitationRepository,
                       PersonRepository personRepository,
                       RelationshipRepository relationshipRepository,
                       MediaFileRepository mediaFileRepository,
                       S3Service s3Service,
                       PersonHistoryRepository personHistoryRepository,
                       NotificationService notificationService) {
        this.userService = userService;
        this.treeRepository = treeRepository;
        this.membershipRepository = membershipRepository;
        this.mailSenderService = mailSenderService;
        this.invitationRepository = invitationRepository;
        this.personRepository = personRepository;
        this.relationshipRepository = relationshipRepository;
        this.mediaFileRepository = mediaFileRepository;
        this.s3Service = s3Service;
        this.personHistoryRepository = personHistoryRepository;
        this.notificationService = notificationService;
    }

    // ─── Tree management ────────────────────────────────────────────────────────

    @Transactional
    public void createTree(String treeName, Long ownerId) {
        Tree tree = new Tree();
        tree.setName(treeName);
        tree = treeRepository.save(tree);

        TreeMembership owner = new TreeMembership();
        owner.setTree(tree);
        owner.setUser(userService.findById(ownerId));
        owner.setRole(TreeRole.OWNER);
        membershipRepository.save(owner);
    }

    public Tree getById(Long treeId) {
        return treeRepository.findById(treeId)
                .orElseThrow(() -> new ResourceNotFoundException("Дерево не найдено"));
    }

    @Transactional
    public TreeDTO updateTree(Long treeId, String newName, Long userId) throws AccessDeniedException {
        if (!isOwner(treeId, userId)) {
            throw new AccessDeniedException("Только владелец может изменять дерево");
        }
        Tree tree = getById(treeId);
        tree.setName(newName);
        tree = treeRepository.save(tree);
        return new TreeDTO(tree.getId(), tree.getName(), tree.getCreatedAt(), TreeRole.OWNER, tree.getPublicLinkToken());
    }

    @Transactional
    public void deleteTree(Long treeId, Long userId) throws AccessDeniedException {
        if (!isOwner(treeId, userId)) {
            throw new AccessDeniedException("Только владелец может удалять дерево");
        }
        Tree tree = getById(treeId);
        treeRepository.delete(tree);
    }

    @Transactional
    public void addMember(Long treeId, Long userId, TreeRole role) {
        TreeMembership treeMembership = new TreeMembership();
        treeMembership.setTree(getById(treeId));
        treeMembership.setUser(userService.findById(userId));
        treeMembership.setRole(role);
        membershipRepository.save(treeMembership);
    }

    public boolean hasRole(Long treeId, Long userId, TreeRole required) {
        return membershipRepository.findByTreeIdAndUserId(treeId, userId)
                .map(m -> m.getRole().hasPermission(required))
                .orElse(false);
    }

    // VIEWER, EDITOR и OWNER могут просматривать
    public boolean canView(Long treeId, Long userId) {
        return hasRole(treeId, userId, TreeRole.VIEWER);
    }

    // EDITOR и OWNER могут редактировать (OWNER.ordinal >= EDITOR.ordinal)
    public boolean canEdit(Long treeId, Long userId) {
        return hasRole(treeId, userId, TreeRole.EDITOR);
    }

    public boolean isOwner(Long treeId, Long userId) {
        return hasRole(treeId, userId, TreeRole.OWNER);
    }

    public List<TreeDTO> getUserTrees(Long userId) {
        List<TreeMembership> memberships = membershipRepository.findByUserId(userId);

        // Bulk-fetch person counts for all trees in a single query (no N+1)
        List<Long> treeIds = memberships.stream()
                .map(tm -> tm.getTree().getId())
                .collect(Collectors.toList());
        Map<Long, Long> countMap = personRepository.countByTreeIds(treeIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        return memberships.stream()
                .map(tm -> {
                    Tree t = tm.getTree();
                    long personCount = countMap.getOrDefault(t.getId(), 0L);
                    return new TreeDTO(t.getId(), t.getName(), t.getCreatedAt(), tm.getRole(), t.getPublicLinkToken(), personCount);
                })
                .toList();
    }

    public List<TreeMemberDTO> getMembers(Long treeId) {
        List<TreeMembership> memberships = membershipRepository.findByTreeId(treeId);
        return memberships.stream()
                .map(tm -> {
                    com.project.familytree.auth.models.User u = tm.getUser();
                    return new TreeMemberDTO(
                            u.getId(), u.getFirstName(), u.getLastName(), u.getMiddleName(),
                            u.getEmail(), tm.getRole(), tm.getCreatedAt());
                })
                .toList();
    }

    // ─── Public link ─────────────────────────────────────────────────────────────

    @Transactional
    public String generatePublicLink(Long treeId, Long userId) throws AccessDeniedException {
        if (!isOwner(treeId, userId)) {
            throw new AccessDeniedException("Только владелец может управлять публичной ссылкой");
        }
        Tree tree = getById(treeId);
        if (tree.getPublicLinkToken() == null) {
            tree.setPublicLinkToken(UUID.randomUUID().toString());
            treeRepository.save(tree);
        }
        return tree.getPublicLinkToken();
    }

    @Transactional
    public void revokePublicLink(Long treeId, Long userId) throws AccessDeniedException {
        if (!isOwner(treeId, userId)) {
            throw new AccessDeniedException("Только владелец может управлять публичной ссылкой");
        }
        Tree tree = getById(treeId);
        tree.setPublicLinkToken(null);
        treeRepository.save(tree);
    }

    public List<PersonDTO> getPublicTree(String token) {
        Tree tree = treeRepository.findByPublicLinkToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Дерево не найдено или ссылка недействительна"));
        List<Person> persons = personRepository.findByTreeIdOrderByLastNameAscFirstNameAsc(tree.getId());
        return persons.stream()
                .map(p -> convertToDTO(p, tree.getId()))
                .toList();
    }

    // ─── Invitations ─────────────────────────────────────────────────────────────

    public String createInviteToken(Long treeId, String email, TreeRole role, Long inviterId) throws AccessDeniedException {
        if (!isOwner(treeId, inviterId)) {
            throw new AccessDeniedException("Только владелец может приглашать");
        }

        Invitation invitation = new Invitation();
        invitation.setTree(getById(treeId));
        invitation.setEmail(email);
        invitation.setRole(role);
        invitation.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        invitation = invitationRepository.save(invitation);

        return invitation.getToken();
    }

    private String buildInviteLink(String token) {
        return baseUrl + "/invite/" + token;
    }

    public void sendInviteByEmail(Long treeId, String email, TreeRole role, Long inviterId) throws AccessDeniedException {
        String token = createInviteToken(treeId, email, role, inviterId);
        String link = buildInviteLink(token);

        String treeName = getById(treeId).getName();
        String subject = "Приглашение в семейное дерево " + treeName;
        String text = "Вы приглашены в дерево '" + treeName + "' с ролью " + role + ". Перейдите по ссылке: " + link;
        mailSenderService.sendEmail(email, subject, text);
    }

    public void acceptInvitation(String token, Long currentUserId) throws AccessDeniedException {
        Invitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Неверная или просроченная ссылка"));

        if (invitation.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException("Срок действия приглашения истёк");
        }

        com.project.familytree.auth.models.User currentUser = userService.findById(currentUserId);
        if (!currentUser.getEmail().equals(invitation.getEmail())) {
            throw new AccessDeniedException("Вы не можете принять это приглашение");
        }

        Long treeId = invitation.getTree().getId();
        addMember(treeId, currentUserId, invitation.getRole());
        invitation.setAccepted(true);
        invitationRepository.save(invitation);

        // Notify the tree OWNER that a new member has joined
        membershipRepository.findByTreeId(treeId).stream()
                .filter(m -> m.getRole() == TreeRole.OWNER)
                .map(m -> m.getUser().getId())
                .forEach(ownerId -> {
                    String joinerName = currentUser.getFirstName() + " " + currentUser.getLastName();
                    String content = joinerName + " принял(а) приглашение в дерево «" + invitation.getTree().getName() + "»";
                    String link = "/trees/" + treeId;
                    notificationService.createNotification(ownerId, NotificationType.MEMBER_JOINED, content, link);
                });
    }

    // ─── Person management ───────────────────────────────────────────────────────

    @Transactional
    public Person createPerson(Long treeId, PersonRequest request, Long userId) throws AccessDeniedException {
        if (!canEdit(treeId, userId)) {
            throw new AccessDeniedException("Нет прав на редактирование дерева");
        }

        Tree tree = getById(treeId);
        Person person = new Person(tree, request.getFirstName(), request.getLastName(),
                request.getMiddleName(), request.getGender());
        person.setBirthDate(request.getBirthDate());
        person.setDeathDate(request.getDeathDate());
        person.setBirthPlace(request.getBirthPlace());
        person.setDeathPlace(request.getDeathPlace());
        person.setBiography(request.getBiography());

        person = personRepository.save(person);

        // Record CREATE history
        com.project.familytree.auth.models.User user = userService.findById(userId);
        String userName = user.getFirstName() + " " + user.getLastName();
        recordHistory(person.getId(), treeId, userId, userName, HistoryAction.CREATE, null, null, null);

        // Notify all tree members (except the creator) about the new person
        final Long savedPersonId = person.getId();
        final String personFullName = person.getFullName();
        List<Long> recipientIds = membershipRepository.findByTreeId(treeId).stream()
                .map(m -> m.getUser().getId())
                .filter(id -> !id.equals(userId))
                .collect(Collectors.toList());
        if (!recipientIds.isEmpty()) {
            String content = "Добавлена новая персона: " + personFullName;
            String link = "/trees/" + treeId + "/persons/" + savedPersonId;
            notificationService.createNotificationsForUsers(
                    recipientIds, NotificationType.PERSON_ADDED, content, link);
        }

        return person;
    }

    public List<PersonDTO> getPersons(Long treeId, Long userId) throws AccessDeniedException {
        if (!canView(treeId, userId)) {
            throw new AccessDeniedException("Нет прав на просмотр дерева");
        }

        List<Person> persons = personRepository.findByTreeIdOrderByLastNameAscFirstNameAsc(treeId);
        return persons.stream()
                .map(p -> convertToDTO(p, treeId))
                .toList();
    }

    public List<PersonDTO> searchPersons(Long treeId, String query, Long userId) throws AccessDeniedException {
        if (!canView(treeId, userId)) {
            throw new AccessDeniedException("Нет прав на просмотр дерева");
        }
        List<Person> persons = personRepository.searchByName(treeId, query);
        return persons.stream()
                .map(p -> convertToDTO(p, treeId))
                .toList();
    }

    public PersonDTO getPerson(Long treeId, Long personId, Long userId) throws AccessDeniedException {
        if (!canView(treeId, userId)) {
            throw new AccessDeniedException("Нет прав на просмотр дерева");
        }

        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new ResourceNotFoundException("Персона не найдена"));

        if (!person.getTree().getId().equals(treeId)) {
            throw new AccessDeniedException("Персона не принадлежит этому дереву");
        }

        return convertToDTO(person, treeId);
    }

    @Transactional
    public Person updatePerson(Long treeId, Long personId, PersonRequest request, Long userId) throws AccessDeniedException {
        if (!canEdit(treeId, userId)) {
            throw new AccessDeniedException("Нет прав на редактирование дерева");
        }

        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new ResourceNotFoundException("Персона не найдена"));

        if (!person.getTree().getId().equals(treeId)) {
            throw new AccessDeniedException("Персона не принадлежит этому дереву");
        }

        com.project.familytree.auth.models.User user = userService.findById(userId);
        String userName = user.getFirstName() + " " + user.getLastName();

        // Record field-level changes
        recordFieldChange(personId, treeId, userId, userName, "firstName",
                person.getFirstName(), request.getFirstName());
        recordFieldChange(personId, treeId, userId, userName, "lastName",
                person.getLastName(), request.getLastName());
        recordFieldChange(personId, treeId, userId, userName, "middleName",
                person.getMiddleName(), request.getMiddleName());
        recordFieldChange(personId, treeId, userId, userName, "gender",
                person.getGender() != null ? person.getGender().name() : null,
                request.getGender() != null ? request.getGender().name() : null);
        recordFieldChange(personId, treeId, userId, userName, "birthDate",
                person.getBirthDate() != null ? person.getBirthDate().toString() : null,
                request.getBirthDate() != null ? request.getBirthDate().toString() : null);
        recordFieldChange(personId, treeId, userId, userName, "deathDate",
                person.getDeathDate() != null ? person.getDeathDate().toString() : null,
                request.getDeathDate() != null ? request.getDeathDate().toString() : null);
        recordFieldChange(personId, treeId, userId, userName, "birthPlace",
                person.getBirthPlace(), request.getBirthPlace());
        recordFieldChange(personId, treeId, userId, userName, "biography",
                person.getBiography(), request.getBiography());

        person.setFirstName(request.getFirstName());
        person.setLastName(request.getLastName());
        person.setMiddleName(request.getMiddleName());
        person.setBirthDate(request.getBirthDate());
        person.setDeathDate(request.getDeathDate());
        person.setBirthPlace(request.getBirthPlace());
        person.setDeathPlace(request.getDeathPlace());
        person.setBiography(request.getBiography());
        person.setGender(request.getGender());

        return personRepository.save(person);
    }

    @Transactional
    public void deletePerson(Long treeId, Long personId, Long userId) throws AccessDeniedException {
        if (!canEdit(treeId, userId)) {
            throw new AccessDeniedException("Нет прав на редактирование дерева");
        }

        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new ResourceNotFoundException("Персона не найдена"));

        if (!person.getTree().getId().equals(treeId)) {
            throw new AccessDeniedException("Персона не принадлежит этому дереву");
        }

        // Record DELETE history before deleting
        com.project.familytree.auth.models.User user = userService.findById(userId);
        String userName = user.getFirstName() + " " + user.getLastName();
        recordHistory(personId, treeId, userId, userName, HistoryAction.DELETE, null, null, null);

        // Удаляем все связи персоны
        List<Relationship> relationships = relationshipRepository.findByTreeIdAndPersonId(treeId, personId);
        relationshipRepository.deleteAll(relationships);

        // Удаляем все медиафайлы персоны
        mediaFileRepository.deleteByPersonId(personId);

        personRepository.delete(person);
    }

    // ─── Relationship management ─────────────────────────────────────────────────

    @Transactional
    public void addRelationship(Long treeId, PersonRelationshipRequest request, Long userId) throws AccessDeniedException {
        if (!canEdit(treeId, userId)) {
            throw new AccessDeniedException("Нет прав на редактирование дерева");
        }

        Person person1 = personRepository.findById(request.getPerson1Id())
                .orElseThrow(() -> new ResourceNotFoundException("Персона 1 не найдена"));
        Person person2 = personRepository.findById(request.getPerson2Id())
                .orElseThrow(() -> new ResourceNotFoundException("Персона 2 не найдена"));

        if (!person1.getTree().getId().equals(treeId) || !person2.getTree().getId().equals(treeId)) {
            throw new AccessDeniedException("Персоны принадлежат разным деревьям");
        }

        // Проверяем, что такая связь ещё не существует
        boolean exists = relationshipRepository
                .findByTreeIdAndPersonsAndType(treeId, request.getPerson1Id(), request.getPerson2Id(), request.getType())
                .isPresent();
        if (exists) {
            throw new BusinessException("Такая связь уже существует");
        }

        Tree tree = getById(treeId);
        Relationship relationship = new Relationship(tree, person1, person2, request.getType());
        relationshipRepository.save(relationship);
    }

    @Transactional
    public void removeRelationship(Long treeId, PersonRelationshipRequest request, Long userId) throws AccessDeniedException {
        if (!canEdit(treeId, userId)) {
            throw new AccessDeniedException("Нет прав на редактирование дерева");
        }

        Relationship relationship = relationshipRepository
                .findByTreeIdAndPersonsAndType(treeId, request.getPerson1Id(), request.getPerson2Id(), request.getType())
                .orElseThrow(() -> new ResourceNotFoundException("Связь не найдена"));

        relationshipRepository.delete(relationship);
    }

    public List<PersonDTO> getTreeGraph(Long treeId, Long userId) throws AccessDeniedException {
        if (!canView(treeId, userId)) {
            throw new AccessDeniedException("Нет прав на просмотр дерева");
        }

        List<Person> persons = personRepository.findByTreeId(treeId);
        return persons.stream()
                .map(p -> convertToDTO(p, treeId))
                .toList();
    }

    // ─── Avatar upload ────────────────────────────────────────────────────────────

    @Transactional
    public PersonDTO uploadAvatar(Long treeId, Long personId, MultipartFile file,
                                  Long userId) throws AccessDeniedException, IOException {
        if (!canEdit(treeId, userId)) {
            throw new AccessDeniedException("Нет прав на редактирование дерева");
        }

        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new ResourceNotFoundException("Персона не найдена"));

        if (!person.getTree().getId().equals(treeId)) {
            throw new AccessDeniedException("Персона не принадлежит этому дереву");
        }

        // Удаляем старый аватар из S3 если есть
        String oldAvatarKey = person.getAvatarUrl();
        if (oldAvatarKey != null && oldAvatarKey.startsWith("trees/")) {
            try {
                s3Service.delete(oldAvatarKey);
            } catch (Exception e) {
                // не критично — продолжаем загрузку нового
            }
        }

        // Загружаем новый аватар в S3: trees/{treeId}/avatars/{uuid}.ext
        String originalFilename = file.getOriginalFilename();
        String extension = (originalFilename != null && originalFilename.contains("."))
                ? originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase()
                : ".jpg";
        String s3Key = "trees/" + treeId + "/avatars/" + UUID.randomUUID() + extension;

        s3Service.upload(s3Key, file.getInputStream(),
                file.getContentType() != null ? file.getContentType() : "image/jpeg",
                file.getSize());

        // Сохраняем S3-ключ как avatarUrl — presigned URL генерируется при чтении
        person.setAvatarUrl(s3Key);
        person = personRepository.save(person);
        return convertToDTO(person, treeId);
    }

    // ─── History ─────────────────────────────────────────────────────────────────

    public List<PersonHistoryDTO> getPersonHistory(Long treeId, Long personId, Long userId) throws AccessDeniedException {
        if (!canEdit(treeId, userId)) {
            throw new AccessDeniedException("Нет прав на просмотр истории");
        }

        List<PersonHistory> history = personHistoryRepository
                .findByPersonIdAndTreeIdOrderByCreatedAtDesc(personId, treeId);

        return history.stream()
                .limit(50)
                .map(h -> new PersonHistoryDTO(
                        h.getId(),
                        h.getAction().name(),
                        h.getFieldName(),
                        h.getOldValue(),
                        h.getNewValue(),
                        h.getUserName(),
                        h.getCreatedAt() != null ? h.getCreatedAt().toString() : null
                ))
                .toList();
    }

    private void recordHistory(Long personId, Long treeId, Long userId, String userName,
                               HistoryAction action, String fieldName,
                               String oldValue, String newValue) {
        PersonHistory history = new PersonHistory();
        history.setPersonId(personId);
        history.setTreeId(treeId);
        history.setUserId(userId);
        history.setUserName(userName);
        history.setAction(action);
        history.setFieldName(fieldName);
        history.setOldValue(oldValue);
        history.setNewValue(newValue);
        personHistoryRepository.save(history);
    }

    private void recordFieldChange(Long personId, Long treeId, Long userId, String userName,
                                   String fieldName, String oldValue, String newValue) {
        // Normalize empty strings to null for comparison
        String normalizedOld = (oldValue != null && oldValue.isBlank()) ? null : oldValue;
        String normalizedNew = (newValue != null && newValue.isBlank()) ? null : newValue;
        if (!Objects.equals(normalizedOld, normalizedNew)) {
            recordHistory(personId, treeId, userId, userName, HistoryAction.UPDATE,
                    fieldName, normalizedOld, normalizedNew);
        }
    }

    // ─── DTO conversion ──────────────────────────────────────────────────────────

    public PersonDTO convertToDTO(Person person) {
        return convertToDTO(person, person.getTree().getId());
    }

    public PersonDTO convertToDTO(Person person, Long treeId) {
        List<Relationship> relationships = relationshipRepository.findByTreeIdAndPersonId(treeId, person.getId());

        List<RelationshipDTO> relationshipDTOs = relationships.stream()
                .map(r -> new RelationshipDTO(
                        r.getId(),
                        r.getPerson1().getId(),
                        r.getPerson2().getId(),
                        r.getType(),
                        new RelationshipDTO.PersonSummary(
                                r.getPerson1().getId(),
                                r.getPerson1().getFirstName(),
                                r.getPerson1().getLastName()),
                        new RelationshipDTO.PersonSummary(
                                r.getPerson2().getId(),
                                r.getPerson2().getFirstName(),
                                r.getPerson2().getLastName())))
                .toList();

        // Если avatarUrl — S3-ключ (начинается с "trees/"), генерируем presigned URL
        String avatarUrl = person.getAvatarUrl();
        if (avatarUrl != null && avatarUrl.startsWith("trees/")) {
            try {
                avatarUrl = s3Service.generatePresignedUrl(avatarUrl);
            } catch (Exception e) {
                avatarUrl = null; // не критично — просто не показываем аватар
            }
        }

        return new PersonDTO(
                person.getId(),
                person.getTree().getId(),
                person.getFirstName(),
                person.getLastName(),
                person.getMiddleName(),
                person.getBirthDate(),
                person.getDeathDate(),
                person.getBirthPlace(),
                person.getDeathPlace(),
                person.getBiography(),
                avatarUrl,
                person.getGender(),
                relationshipDTOs,
                person.getFullName()
        );
    }
}
