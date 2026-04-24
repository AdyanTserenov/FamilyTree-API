package com.project.familytree.tree.services;

import com.project.familytree.tree.impls.Gender;
import com.project.familytree.tree.impls.RelationshipType;
import com.project.familytree.tree.models.Person;
import com.project.familytree.tree.models.Relationship;
import com.project.familytree.tree.models.Tree;
import com.project.familytree.tree.repositories.PersonRepository;
import com.project.familytree.tree.repositories.RelationshipRepository;
import com.project.familytree.tree.repositories.TreeRepository;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class GedcomService {

    private static final DateTimeFormatter GEDCOM_DATE_FORMAT =
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    private final TreeRepository treeRepository;
    private final PersonRepository personRepository;
    private final RelationshipRepository relationshipRepository;

    public GedcomService(TreeRepository treeRepository,
                         PersonRepository personRepository,
                         RelationshipRepository relationshipRepository) {
        this.treeRepository = treeRepository;
        this.personRepository = personRepository;
        this.relationshipRepository = relationshipRepository;
    }

    /**
     * Generates a GEDCOM 5.5.1 string for the given tree.
     *
     * @param treeId the ID of the tree to export
     * @return GEDCOM-formatted string
     */
    public String generateGedcom(Long treeId) {
        Tree tree = treeRepository.findById(treeId)
                .orElseThrow(() -> new IllegalArgumentException("Tree not found: " + treeId));

        List<Person> persons = personRepository.findByTreeId(treeId);
        List<Relationship> relationships = relationshipRepository.findByTreeId(treeId);

        StringBuilder sb = new StringBuilder();

        // HEAD
        sb.append("0 HEAD\n");
        sb.append("1 SOUR FamilyTree\n");
        sb.append("2 VERS 1.0\n");
        sb.append("1 GEDC\n");
        sb.append("2 VERS 5.5.1\n");
        sb.append("2 FORM LINEAGE-LINKED\n");
        sb.append("1 CHAR UTF-8\n");

        // INDI records for each person
        for (Person person : persons) {
            sb.append("0 @I").append(person.getId()).append("@ INDI\n");

            // NAME: FirstName /LastName/
            String firstName = person.getFirstName() != null ? person.getFirstName() : "";
            String lastName = person.getLastName() != null ? person.getLastName() : "";
            sb.append("1 NAME ").append(firstName).append(" /").append(lastName).append("/\n");

            // SEX
            if (person.getGender() == Gender.MALE) {
                sb.append("1 SEX M\n");
            } else if (person.getGender() == Gender.FEMALE) {
                sb.append("1 SEX F\n");
            }
            // OTHER gender is omitted per spec

            // BIRT
            if (person.getBirthDate() != null || person.getBirthPlace() != null) {
                sb.append("1 BIRT\n");
                if (person.getBirthDate() != null) {
                    sb.append("2 DATE ").append(
                            person.getBirthDate().format(GEDCOM_DATE_FORMAT).toUpperCase(Locale.ENGLISH)
                    ).append("\n");
                }
                if (person.getBirthPlace() != null && !person.getBirthPlace().isBlank()) {
                    sb.append("2 PLAC ").append(person.getBirthPlace()).append("\n");
                }
            }

            // DEAT
            if (person.getDeathDate() != null || person.getDeathPlace() != null) {
                sb.append("1 DEAT\n");
                if (person.getDeathDate() != null) {
                    sb.append("2 DATE ").append(
                            person.getDeathDate().format(GEDCOM_DATE_FORMAT).toUpperCase(Locale.ENGLISH)
                    ).append("\n");
                }
                if (person.getDeathPlace() != null && !person.getDeathPlace().isBlank()) {
                    sb.append("2 PLAC ").append(person.getDeathPlace()).append("\n");
                }
            }
        }

        // FAM records for PARTNERSHIP relationships
        List<Relationship> partnerships = relationships.stream()
                .filter(r -> r.getType() == RelationshipType.PARTNERSHIP)
                .collect(Collectors.toList());

        // PARENT_CHILD relationships for finding children of couples
        List<Relationship> parentChildRels = relationships.stream()
                .filter(r -> r.getType() == RelationshipType.PARENT_CHILD)
                .collect(Collectors.toList());

        long famId = 1;
        for (Relationship partnership : partnerships) {
            Person p1 = partnership.getPerson1();
            Person p2 = partnership.getPerson2();

            sb.append("0 @F").append(famId).append("@ FAM\n");

            // Determine HUSB/WIFE by gender
            // If p1 is MALE → HUSB, p2 → WIFE
            // If p1 is FEMALE → WIFE, p2 → HUSB
            // If same gender or OTHER, use HUSB for p1, WIFE for p2
            if (p1.getGender() == Gender.FEMALE && p2.getGender() != Gender.FEMALE) {
                sb.append("1 WIFE @I").append(p1.getId()).append("@\n");
                sb.append("1 HUSB @I").append(p2.getId()).append("@\n");
            } else {
                sb.append("1 HUSB @I").append(p1.getId()).append("@\n");
                sb.append("1 WIFE @I").append(p2.getId()).append("@\n");
            }

            // Find children: persons whose parent is p1 OR p2 (PARENT_CHILD where person1=parent, person2=child)
            List<Long> childIds = new ArrayList<>();
            for (Relationship pc : parentChildRels) {
                Long parentId = pc.getPerson1().getId();
                Long childId = pc.getPerson2().getId();
                if ((parentId.equals(p1.getId()) || parentId.equals(p2.getId()))
                        && !childIds.contains(childId)) {
                    childIds.add(childId);
                }
            }
            for (Long childId : childIds) {
                sb.append("1 CHIL @I").append(childId).append("@\n");
            }

            famId++;
        }

        // TRLR
        sb.append("0 TRLR\n");

        return sb.toString();
    }
}
