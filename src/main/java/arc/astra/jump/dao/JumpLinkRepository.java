package arc.astra.jump.dao;

import arc.astra.jump.model.JumpLink;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public class JumpLinkRepository {

    private final JdbcTemplate jdbcTemplate;

    public JumpLinkRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(JumpLink jumpLink) {
        String sql = """
                INSERT INTO jump_link (code, url, created_by, created_at, expires_at)
                VALUES (?, ?, ?, ?, ?)
                """;
        jdbcTemplate.update(sql, jumpLink.code(), jumpLink.url(),
                jumpLink.createdBy(), jumpLink.createdAt().toString(),
                jumpLink.expiresAt().toString());
    }

    public Optional<JumpLink> findByCode(String code) {
        String sql = """
                SELECT code, url, created_by, created_at, expires_at
                FROM jump_link
                WHERE code = ?
                """;

        RowMapper<JumpLink> rowMapper = (rs, _) -> new JumpLink(
                rs.getString("code"),
                rs.getString("url"),
                rs.getString("created_by"),
                Instant.parse(rs.getString("created_at")),
                rs.getString("expires_at") != null
                        ? Instant.parse(rs.getString("expires_at"))
                        : null
        );

        return jdbcTemplate.query(sql, rowMapper, code)
                .stream()
                .findFirst();
    }

    public void deleteExpired() {
        String sql = """
                DELETE FROM jump_link
                WHERE expires_at IS NOT NULL
                  AND expires_at <= ?
                """;

        jdbcTemplate.update(sql, Instant.now().toString());
    }
}
