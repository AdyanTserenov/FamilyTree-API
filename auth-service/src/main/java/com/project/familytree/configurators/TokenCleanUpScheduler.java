package com.project.familytree.configurators;

import com.project.familytree.impls.TreeRole;
import com.project.familytree.models.TreeMembership;
import com.project.familytree.models.User;
import com.project.familytree.repositories.TokenRepository;
import com.project.familytree.repositories.TreeMembershipRepository;
import com.project.familytree.repositories.UserRepository;
import com.project.familytree.services.MailSenderService;
import com.project.familytree.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanUpScheduler {

    private final TokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final MailSenderService mailSenderService;
    private final UserService userService;
    private final TreeMembershipRepository treeMembershipRepository;
    private final JdbcTemplate jdbcTemplate;

    private static final DateTimeFormatter GEDCOM_DATE_FORMAT =
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    @Scheduled(cron = "0 0 3 * * ?") // Каждый день в 3:00
    public void cleanupExpiredTokens() {
        try {
            int deletedCount = tokenRepository.deleteExpired();
            if (deletedCount > 0) {
                log.info("Cleaned up {} expired tokens", deletedCount);
            } else {
                log.debug("No expired tokens to clean up");
            }
        } catch (Exception e) {
            log.error("Error cleaning up expired tokens", e);
        }
    }

    @Scheduled(cron = "0 0 3 * * ?") // Каждый день в 3:00
    public void checkInactiveAccounts() {
        log.info("Starting inactive account check...");

        // Check A: warn users inactive for 10+ years, no warning sent yet
        LocalDateTime tenYearsAgo = LocalDateTime.now().minusYears(10);
        List<User> toWarn = userRepository.findAll().stream()
                .filter(u -> u.getDeletionWarningSentAt() == null)
                .filter(u -> {
                    LocalDateTime lastActivity = u.getLastLoginAt() != null
                            ? u.getLastLoginAt()
                            : u.getCreatedAt();
                    return lastActivity != null && lastActivity.isBefore(tenYearsAgo);
                })
                .collect(Collectors.toList());

        log.info("Found {} users to warn about inactivity", toWarn.size());

        for (User user : toWarn) {
            try {
                mailSenderService.sendDeletionWarningEmail(user.getEmail(), user.getFirstName());
            } catch (Exception e) {
                log.error("Failed to send warning email to {}: {}", user.getEmail(), e.getMessage());
            }
            user.setDeletionWarningSentAt(LocalDateTime.now());
            userRepository.save(user);
        }

        // Check B: delete users who didn't login after warning (7+ days ago)
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<User> toDelete = userRepository.findAll().stream()
                .filter(u -> u.getDeletionWarningSentAt() != null)
                .filter(u -> u.getDeletionWarningSentAt().isBefore(sevenDaysAgo))
                .filter(u -> u.getLastLoginAt() == null
                        || u.getLastLoginAt().isBefore(u.getDeletionWarningSentAt()))
                .collect(Collectors.toList());

        log.info("Found {} users to delete due to inactivity", toDelete.size());

        for (User user : toDelete) {
            // Generate GEDCOM for each tree owned by this user
            List<byte[]> gedcomFiles = new ArrayList<>();
            List<String> fileNames = new ArrayList<>();

            try {
                List<TreeMembership> memberships = treeMembershipRepository.findByUserId(user.getId());
                for (TreeMembership membership : memberships) {
                    if (membership.getRole() == TreeRole.OWNER) {
                        Long treeId = membership.getTree().getId();
                        String treeName = membership.getTree().getName();
                        try {
                            String gedcom = generateGedcomFromDb(treeId);
                            gedcomFiles.add(gedcom.getBytes(StandardCharsets.UTF_8));
                            // Sanitize tree name for filename
                            String safeFileName = treeName.replaceAll("[^a-zA-Zа-яА-Я0-9_\\-]", "_") + ".ged";
                            fileNames.add(safeFileName);
                        } catch (Exception e) {
                            log.error("Failed to generate GEDCOM for tree {} (user {}): {}",
                                    treeId, user.getEmail(), e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Failed to fetch trees for user {}: {}", user.getEmail(), e.getMessage());
            }

            // Send deletion confirmation email (with or without attachments)
            try {
                mailSenderService.sendDeletionConfirmationEmail(
                        user.getEmail(), user.getFirstName(), gedcomFiles, fileNames);
            } catch (Exception e) {
                log.error("Failed to send deletion email to {}: {}", user.getEmail(), e.getMessage());
            }

            // Delete the account (tokens + user record)
            try {
                userService.deleteAccount(user.getEmail());
                log.info("Deleted inactive account: {}", user.getEmail());
            } catch (Exception e) {
                log.error("Failed to delete account {}: {}", user.getEmail(), e.getMessage());
            }
        }

        log.info("Inactive account check completed. Warned: {}, Deleted: {}",
                toWarn.size(), toDelete.size());
    }

    /**
     * Generates a GEDCOM 5.5.1 string for the given tree using raw SQL via JdbcTemplate.
     * This avoids a cross-service dependency on tree-service JPA entities.
     */
    private String generateGedcomFromDb(Long treeId) {
        StringBuilder sb = new StringBuilder();

        // HEAD
        sb.append("0 HEAD\n");
        sb.append("1 SOUR FamilyTree\n");
        sb.append("2 VERS 1.0\n");
        sb.append("1 GEDC\n");
        sb.append("2 VERS 5.5.1\n");
        sb.append("2 FORM LINEAGE-LINKED\n");
        sb.append("1 CHAR UTF-8\n");

        // Fetch persons
        List<Map<String, Object>> persons = jdbcTemplate.queryForList(
                "SELECT id, first_name, last_name, birth_date, death_date, birth_place, death_place, gender " +
                "FROM persons WHERE tree_id = ?", treeId);

        for (Map<String, Object> person : persons) {
            Long personId = toLong(person.get("id"));
            String firstName = toStr(person.get("first_name"));
            String lastName = toStr(person.get("last_name"));
            String gender = toStr(person.get("gender"));
            Object birthDateObj = person.get("birth_date");
            Object deathDateObj = person.get("death_date");
            String birthPlace = toStr(person.get("birth_place"));
            String deathPlace = toStr(person.get("death_place"));

            sb.append("0 @I").append(personId).append("@ INDI\n");
            sb.append("1 NAME ").append(firstName).append(" /").append(lastName).append("/\n");

            if ("MALE".equalsIgnoreCase(gender)) {
                sb.append("1 SEX M\n");
            } else if ("FEMALE".equalsIgnoreCase(gender)) {
                sb.append("1 SEX F\n");
            }

            if (birthDateObj != null || (birthPlace != null && !birthPlace.isBlank())) {
                sb.append("1 BIRT\n");
                if (birthDateObj != null) {
                    sb.append("2 DATE ").append(formatGedcomDate(birthDateObj)).append("\n");
                }
                if (birthPlace != null && !birthPlace.isBlank()) {
                    sb.append("2 PLAC ").append(birthPlace).append("\n");
                }
            }

            if (deathDateObj != null || (deathPlace != null && !deathPlace.isBlank())) {
                sb.append("1 DEAT\n");
                if (deathDateObj != null) {
                    sb.append("2 DATE ").append(formatGedcomDate(deathDateObj)).append("\n");
                }
                if (deathPlace != null && !deathPlace.isBlank()) {
                    sb.append("2 PLAC ").append(deathPlace).append("\n");
                }
            }
        }

        // Fetch PARTNERSHIP relationships
        List<Map<String, Object>> partnerships = jdbcTemplate.queryForList(
                "SELECT r.id, r.person1_id, r.person2_id, " +
                "p1.gender AS gender1, p2.gender AS gender2 " +
                "FROM relationships r " +
                "JOIN persons p1 ON p1.id = r.person1_id " +
                "JOIN persons p2 ON p2.id = r.person2_id " +
                "WHERE r.tree_id = ? AND r.type = 'PARTNERSHIP'", treeId);

        // Fetch PARENT_CHILD relationships
        List<Map<String, Object>> parentChildRels = jdbcTemplate.queryForList(
                "SELECT person1_id AS parent_id, person2_id AS child_id " +
                "FROM relationships WHERE tree_id = ? AND type = 'PARENT_CHILD'", treeId);

        long famId = 1;
        for (Map<String, Object> partnership : partnerships) {
            Long p1Id = toLong(partnership.get("person1_id"));
            Long p2Id = toLong(partnership.get("person2_id"));
            String gender1 = toStr(partnership.get("gender1"));
            String gender2 = toStr(partnership.get("gender2"));

            sb.append("0 @F").append(famId).append("@ FAM\n");

            // Determine HUSB/WIFE by gender
            if ("FEMALE".equalsIgnoreCase(gender1) && !"FEMALE".equalsIgnoreCase(gender2)) {
                sb.append("1 WIFE @I").append(p1Id).append("@\n");
                sb.append("1 HUSB @I").append(p2Id).append("@\n");
            } else {
                sb.append("1 HUSB @I").append(p1Id).append("@\n");
                sb.append("1 WIFE @I").append(p2Id).append("@\n");
            }

            // Find children of this couple
            List<Long> childIds = new ArrayList<>();
            for (Map<String, Object> pc : parentChildRels) {
                Long parentId = toLong(pc.get("parent_id"));
                Long childId = toLong(pc.get("child_id"));
                if ((p1Id.equals(parentId) || p2Id.equals(parentId)) && !childIds.contains(childId)) {
                    childIds.add(childId);
                }
            }
            for (Long childId : childIds) {
                sb.append("1 CHIL @I").append(childId).append("@\n");
            }

            famId++;
        }

        sb.append("0 TRLR\n");
        return sb.toString();
    }

    private String formatGedcomDate(Object dateObj) {
        if (dateObj == null) return "";
        // dateObj is typically java.sql.Date or LocalDate
        try {
            if (dateObj instanceof java.sql.Date sqlDate) {
                return sqlDate.toLocalDate()
                        .format(GEDCOM_DATE_FORMAT)
                        .toUpperCase(Locale.ENGLISH);
            } else if (dateObj instanceof java.time.LocalDate ld) {
                return ld.format(GEDCOM_DATE_FORMAT).toUpperCase(Locale.ENGLISH);
            }
            // Fallback: parse from string
            return java.time.LocalDate.parse(dateObj.toString())
                    .format(GEDCOM_DATE_FORMAT)
                    .toUpperCase(Locale.ENGLISH);
        } catch (Exception e) {
            return dateObj.toString();
        }
    }

    private Long toLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Long l) return l;
        if (obj instanceof Number n) return n.longValue();
        return Long.parseLong(obj.toString());
    }

    private String toStr(Object obj) {
        if (obj == null) return "";
        return obj.toString();
    }
}
