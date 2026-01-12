package com.project.familytree.tree.controllers;

import com.project.familytree.auth.dto.CustomApiResponse;
import com.project.familytree.auth.services.UserService;
import com.project.familytree.tree.dto.PersonDTO;
import com.project.familytree.tree.dto.PersonRelationshipRequest;
import com.project.familytree.tree.dto.PersonRequest;
import com.project.familytree.tree.services.TreeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<CustomApiResponse<PersonDTO>> createPerson(
            @PathVariable Long treeId,
            @Valid @RequestBody PersonRequest personRequest) throws AccessDeniedException {

        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userService.findIdByDetails(userDetails);
        log.info("Creating person in tree {} for user {}", treeId, userId);

        var person = treeService.createPerson(treeId, personRequest, userId);
        var personDTO = treeService.convertToDTO(person);

        log.info("Created person with id {} in tree {}", person.getId(), treeId);
        return ResponseEntity.ok(CustomApiResponse.successData(personDTO));
    }

    @GetMapping
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
    public ResponseEntity<CustomApiResponse<PersonDTO>> updatePerson(
            @PathVariable Long treeId,
            @PathVariable Long personId,
            @Valid @RequestBody PersonRequest personRequest) throws AccessDeniedException {

        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userService.findIdByDetails(userDetails);
        log.info("Updating person {} in tree {} by user {}", personId, treeId, userId);

        var person = treeService.updatePerson(treeId, personId, personRequest, userId);
        var personDTO = treeService.convertToDTO(person);

        log.info("Updated person {} in tree {}", personId, treeId);
        return ResponseEntity.ok(CustomApiResponse.successData(personDTO));
    }

    @DeleteMapping("/{personId}")
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
    public ResponseEntity<CustomApiResponse<String>> addRelationship(
            @PathVariable Long treeId,
            @Valid @RequestBody PersonRelationshipRequest relationshipRequest) throws AccessDeniedException {

        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userService.findIdByDetails(userDetails);
        log.info("Adding relationship in tree {} by user {}: parent {} -> child {}",
                treeId, userId, relationshipRequest.getParentId(), relationshipRequest.getChildId());

        treeService.addRelationship(treeId, relationshipRequest, userId);
        return ResponseEntity.ok(CustomApiResponse.successMessage("Связь добавлена"));
    }

    @DeleteMapping("/relationships")
    public ResponseEntity<CustomApiResponse<String>> removeRelationship(
            @PathVariable Long treeId,
            @Valid @RequestBody PersonRelationshipRequest relationshipRequest) throws AccessDeniedException {

        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userService.findIdByDetails(userDetails);
        log.info("Removing relationship in tree {} by user {}: parent {} -> child {}",
                treeId, userId, relationshipRequest.getParentId(), relationshipRequest.getChildId());

        treeService.removeRelationship(treeId, relationshipRequest, userId);
        return ResponseEntity.ok(CustomApiResponse.successMessage("Связь удалена"));
    }

    @GetMapping("/graph")
    public ResponseEntity<CustomApiResponse<List<PersonDTO>>> getTreeGraph(
            @PathVariable Long treeId) throws AccessDeniedException {

        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userService.findIdByDetails(userDetails);
        log.info("Getting graph for tree {} by user {}", treeId, userId);

        List<PersonDTO> graph = treeService.getTreeGraph(treeId, userId);
        return ResponseEntity.ok(CustomApiResponse.successData(graph));
    }
}