package com.project.familytree.tree.controllers;

import com.project.familytree.auth.dto.CustomApiResponse;
import com.project.familytree.auth.services.UserService;
import com.project.familytree.tree.dto.PersonDTO;
import com.project.familytree.tree.dto.PersonRelationshipRequest;
import com.project.familytree.tree.dto.PersonRequest;
import com.project.familytree.tree.services.TreeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.List;

@RestController
@RequestMapping("/trees/{treeId}/persons")
@Tag(name = "Person Controller", description = "API для управления персонами в семейном древе")
public class PersonController {

    private static final Logger log = LoggerFactory.getLogger(PersonController.class);

    private final TreeService treeService;
    private final UserService userService;

    public PersonController(TreeService treeService, UserService userService) {
        this.treeService = treeService;
        this.userService = userService;
    }

    @PostMapping
    @Operation(summary = "Создать персону в дереве",
               description = "Создаёт новую персону в указанном семейном дереве. Требует роль EDITOR или OWNER.")
    public ResponseEntity<CustomApiResponse<PersonDTO>> createPerson(
            @PathVariable Long treeId,
            @Valid @RequestBody PersonRequest personRequest) throws AccessDeniedException {

        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userService.findIdByDetails(userDetails);
        log.info("Creating person in tree {} for user {}", treeId, userId);

        var person = treeService.createPerson(treeId, personRequest, userId);
        var personDTO = treeService.convertToDTO(person, treeId);

        log.info("Created person with id {} in tree {}", person.getId(), treeId);
        return ResponseEntity.ok(CustomApiResponse.successData(personDTO));
    }

    @GetMapping
    @Operation(summary = "Получить список персон дерева",
               description = "Возвращает все персоны дерева, отсортированные по фамилии и имени. Требует роль VIEWER или выше.")
    public ResponseEntity<CustomApiResponse<List<PersonDTO>>> getPersons(
            @PathVariable Long treeId) throws AccessDeniedException {

        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userService.findIdByDetails(userDetails);
        log.info("Getting persons for tree {} by user {}", treeId, userId);

        List<PersonDTO> persons = treeService.getPersons(treeId, userId);
        log.info("Found {} persons in tree {}", persons.size(), treeId);

        return ResponseEntity.ok(CustomApiResponse.successData(persons));
    }

    @GetMapping("/{personId}")
    @Operation(summary = "Получить персону по ID",
               description = "Возвращает данные конкретной персоны вместе со всеми её связями.")
    public ResponseEntity<CustomApiResponse<PersonDTO>> getPerson(
            @PathVariable Long treeId,
            @PathVariable Long personId) throws AccessDeniedException {

        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userService.findIdByDetails(userDetails);
        log.info("Getting person {} from tree {} by user {}", personId, treeId, userId);

        PersonDTO person = treeService.getPerson(treeId, personId, userId);
        return ResponseEntity.ok(CustomApiResponse.successData(person));
    }

    @PutMapping("/{personId}")
    @Operation(summary = "Обновить данные персоны",
               description = "Обновляет все поля персоны. Требует роль EDITOR или OWNER.")
    public ResponseEntity<CustomApiResponse<PersonDTO>> updatePerson(
            @PathVariable Long treeId,
            @PathVariable Long personId,
            @Valid @RequestBody PersonRequest personRequest) throws AccessDeniedException {

        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userService.findIdByDetails(userDetails);
        log.info("Updating person {} in tree {} by user {}", personId, treeId, userId);

        var person = treeService.updatePerson(treeId, personId, personRequest, userId);
        var personDTO = treeService.convertToDTO(person, treeId);

        log.info("Updated person {} in tree {}", personId, treeId);
        return ResponseEntity.ok(CustomApiResponse.successData(personDTO));
    }

    @DeleteMapping("/{personId}")
    @Operation(summary = "Удалить персону",
               description = "Удаляет персону и все её связи и медиафайлы. Требует роль EDITOR или OWNER.")
    public ResponseEntity<CustomApiResponse<String>> deletePerson(
            @PathVariable Long treeId,
            @PathVariable Long personId) throws AccessDeniedException {

        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userService.findIdByDetails(userDetails);
        log.info("Deleting person {} from tree {} by user {}", personId, treeId, userId);

        treeService.deletePerson(treeId, personId, userId);
        log.info("Deleted person {} from tree {}", personId, treeId);

        return ResponseEntity.ok(CustomApiResponse.successMessage("Персона удалена"));
    }

    @PostMapping("/relationships")
    @Operation(summary = "Добавить связь между персонами",
               description = "Создаёт связь типа PARENT_CHILD (person1=родитель, person2=ребёнок) " +
                             "или PARTNERSHIP (person1 и person2 — партнёры). Требует роль EDITOR или OWNER.")
    public ResponseEntity<CustomApiResponse<String>> addRelationship(
            @PathVariable Long treeId,
            @Valid @RequestBody PersonRelationshipRequest relationshipRequest) throws AccessDeniedException {

        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userService.findIdByDetails(userDetails);
        log.info("Adding {} relationship in tree {} by user {}: person1={} -> person2={}",
                relationshipRequest.getType(), treeId, userId,
                relationshipRequest.getPerson1Id(), relationshipRequest.getPerson2Id());

        treeService.addRelationship(treeId, relationshipRequest, userId);
        return ResponseEntity.ok(CustomApiResponse.successMessage("Связь добавлена"));
    }

    @DeleteMapping("/relationships")
    @Operation(summary = "Удалить связь между персонами",
               description = "Удаляет существующую связь между двумя персонами. Требует роль EDITOR или OWNER.")
    public ResponseEntity<CustomApiResponse<String>> removeRelationship(
            @PathVariable Long treeId,
            @Valid @RequestBody PersonRelationshipRequest relationshipRequest) throws AccessDeniedException {

        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userService.findIdByDetails(userDetails);
        log.info("Removing {} relationship in tree {} by user {}: person1={} -> person2={}",
                relationshipRequest.getType(), treeId, userId,
                relationshipRequest.getPerson1Id(), relationshipRequest.getPerson2Id());

        treeService.removeRelationship(treeId, relationshipRequest, userId);
        return ResponseEntity.ok(CustomApiResponse.successMessage("Связь удалена"));
    }

    @GetMapping("/graph")
    @Operation(summary = "Получить граф семейного дерева",
               description = "Возвращает все персоны дерева вместе со всеми их связями для построения графа.")
    public ResponseEntity<CustomApiResponse<List<PersonDTO>>> getTreeGraph(
            @PathVariable Long treeId) throws AccessDeniedException {

        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userService.findIdByDetails(userDetails);
        log.info("Getting graph for tree {} by user {}", treeId, userId);

        List<PersonDTO> graph = treeService.getTreeGraph(treeId, userId);
        return ResponseEntity.ok(CustomApiResponse.successData(graph));
    }

    @GetMapping("/search")
    @Operation(summary = "Поиск персон по имени",
               description = "Ищет персон в дереве по имени, фамилии или отчеству (регистронезависимо). Требует роль VIEWER или выше.")
    public ResponseEntity<CustomApiResponse<List<PersonDTO>>> searchPersons(
            @PathVariable Long treeId,
            @RequestParam("q") String query) throws AccessDeniedException {

        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userService.findIdByDetails(userDetails);
        log.info("Searching persons in tree {} with query '{}' by user {}", treeId, query, userId);

        List<PersonDTO> results = treeService.searchPersons(treeId, query, userId);
        log.info("Found {} persons matching '{}'", results.size(), query);
        return ResponseEntity.ok(CustomApiResponse.successData(results));
    }

    @PostMapping(value = "/{personId}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Загрузить аватар персоны",
               description = "Загружает изображение и устанавливает его как аватар персоны. Требует роль EDITOR или OWNER.")
    public ResponseEntity<CustomApiResponse<PersonDTO>> uploadAvatar(
            @PathVariable Long treeId,
            @PathVariable Long personId,
            @RequestParam("file") MultipartFile file) throws AccessDeniedException, IOException {

        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userService.findIdByDetails(userDetails);
        log.info("Uploading avatar for person {} in tree {} by user {}", personId, treeId, userId);

        PersonDTO personDTO = treeService.uploadAvatar(treeId, personId, file, userId);
        log.info("Avatar uploaded for person {} in tree {}", personId, treeId);
        return ResponseEntity.ok(CustomApiResponse.successData(personDTO));
    }
}
