package com.project.familytree.models;

import com.project.familytree.impls.TokenDetails;
import com.project.familytree.impls.TokenType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;


@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tokens",
        uniqueConstraints =
        @UniqueConstraint(name = "uk_token_hash", columnNames = "token_hash")
)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type",
        discriminatorType = DiscriminatorType.STRING,
        length = 20)
public abstract class Token {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter
    @Embedded
    TokenDetails details;

    public abstract TokenType getType();

    public static String hash(String rawToken) {
        return DigestUtils.sha256Hex(rawToken);
    }
}
