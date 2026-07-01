package com.example.authorizationserver.infrastructure.persistence

import com.example.authorizationserver.domain.user.User
import com.example.authorizationserver.domain.user.UserRepository
import com.example.authorizationserver.infrastructure.kafka.UserSyncEventPublisher
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.ResultSetExtractor
import org.springframework.jdbc.core.queryForObject
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.time.Instant

@Repository
class UserJdbcRepository(
    private val jdbcTemplate: JdbcTemplate,
    private val userSyncEventPublisher: UserSyncEventPublisher
) : UserRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    private val userExtractor = ResultSetExtractor { rs ->
        var user: User? = null
        val authorities = mutableSetOf<String>()
        while (rs.next()) {
            if (user == null) {
                user = User(
                    id = rs.getLong("user_id").takeIf { it != 0L },
                    username = rs.getString("username"),
                    password = rs.getString("password"),
                    email = rs.getString("email"),
                    fullName = rs.getString("full_name"),
                    enabled = rs.getBoolean("enabled"),
                    status = rs.getString("status") ?: "ACTIVE",
                    loginAttempts = rs.getInt("login_attempts"),
                    lockedUntil = rs.getTimestamp("locked_until")?.toInstant(),
                    lastActiveAt = rs.getTimestamp("last_active_at")?.toInstant(),
                    mfaEnabled = rs.getBoolean("mfa_enabled"),
                    mfaSecret = rs.getString("mfa_secret")
                )
            }
            rs.getString("authority")?.let { authorities.add(it) }
        }
        user?.copy(authorities = authorities)
    }

    override fun findByUsername(username: String): User? =
        jdbcTemplate.query(
            """
            SELECT      u.*, ua.authority
            FROM        authorization_db.public.users u
            LEFT JOIN   authorization_db.public.user_authorities ua ON u.user_id = ua.user_id
            WHERE       u.username = ?
            """.trimIndent(),
            userExtractor,
            username
        )

    override fun findById(id: Long): User? =
        jdbcTemplate.query(
            """
            SELECT      u.*, ua.authority
            FROM        authorization_db.public.users u
            LEFT JOIN   authorization_db.public.user_authorities ua ON u.user_id = ua.user_id
            WHERE       u.user_id = ?
            """.trimIndent(),
            userExtractor,
            id
        )

    override fun findByEmail(email: String): User? =
        jdbcTemplate.query(
            """
            SELECT      u.*, ua.authority
            FROM        authorization_db.public.users u
            LEFT JOIN   authorization_db.public.user_authorities ua ON u.user_id = ua.user_id
            WHERE       u.email = ?
            """.trimIndent(),
            userExtractor,
            email
        )

    @Transactional
    override fun save(user: User): User {
        return if (user.id == null) {
            val userId = jdbcTemplate.queryForObject<Long>(
                """
                INSERT INTO authorization_db.public.users
                (username, password, email, full_name, enabled, status, login_attempts, locked_until)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING user_id
                """.trimIndent(),
                user.username, user.password, user.email, user.fullName,
                user.enabled, user.status, user.loginAttempts,
                user.lockedUntil?.let { Timestamp.from(it) }
            )!!
            val saved = user.copy(id = userId)
            syncAuthorities(userId, user.authorities)
            userSyncEventPublisher.publish(userId, user.username, user.enabled, user.status, version = 1L)
            saved
        } else {
            val newVersion = jdbcTemplate.queryForObject<Long>(
                """
                UPDATE  authorization_db.public.users
                SET     password = ?, email = ?, full_name = ?, enabled = ?, status = ?,
                        login_attempts = ?, locked_until = ?, version = version + 1
                WHERE   user_id = ?
                RETURNING version
                """.trimIndent(),
                user.password, user.email, user.fullName,
                user.enabled, user.status, user.loginAttempts,
                user.lockedUntil?.let { Timestamp.from(it) },
                user.id
            )!!
            syncAuthorities(user.id, user.authorities)
            userSyncEventPublisher.publish(user.id, user.username, user.enabled, user.status, newVersion)
            user
        }
    }

    override fun lockUser(username: String, lockedUntil: Instant) {
        jdbcTemplate.update(
            """
            UPDATE  authorization_db.public.users
            SET     locked_until = ?
            WHERE   username = ?
            """.trimIndent(),
            Timestamp.from(lockedUntil), username
        )
    }

    override fun resetLoginAttempts(username: String) {
        jdbcTemplate.update(
            """
            UPDATE  authorization_db.public.users
            SET     login_attempts = 0, locked_until = NULL
            WHERE   username = ?
            """.trimIndent(),
            username
        )
    }

    override fun updateMfaSettings(username: String, mfaEnabled: Boolean, mfaSecret: String?) {
        jdbcTemplate.update(
            """
            UPDATE  authorization_db.public.users
            SET     mfa_enabled = ?, mfa_secret = ?
            WHERE   username = ?
            """.trimIndent(),
            mfaEnabled, mfaSecret, username
        )
    }

    override fun setStatusAndEnabled(userId: Long, enabled: Boolean, status: String) {
        val matched = jdbcTemplate.query(
            """
            UPDATE  authorization_db.public.users
            SET     enabled = ?, status = ?, version = version + 1
            WHERE   user_id = ?
            RETURNING username, version
            """.trimIndent(),
            { rs, _ -> Pair(rs.getString("username"), rs.getLong("version")) },
            enabled, status, userId
        ).firstOrNull()
        if (matched == null) {
            log.warn("setStatusAndEnabled: no user found for userId={}, skipping Kafka publish", userId)
            return
        }
        val (username, version) = matched
        userSyncEventPublisher.publish(userId, username, enabled, status, version)
    }

    override fun updateUsername(userId: Long, newUsername: String) {
        val newVersion = jdbcTemplate.queryForObject<Long>(
            """
            UPDATE  authorization_db.public.users
            SET     username = ?, version = version + 1
            WHERE   user_id = ?
            RETURNING version
            """.trimIndent(),
            newUsername, userId
        )!!
        userSyncEventPublisher.publishUsernameUpdate(userId, newUsername, newVersion)
    }

    private fun syncAuthorities(userId: Long, authorities: Set<String>) {
        jdbcTemplate.update(
            """
            DELETE FROM authorization_db.public.user_authorities
            WHERE   user_id = ?
            """.trimIndent(),
            userId
        )
        if (authorities.isEmpty()) return
        jdbcTemplate.batchUpdate(
            """
            INSERT INTO authorization_db.public.user_authorities (user_id, authority)
            VALUES (?, ?)
            """.trimIndent(),
            authorities.map { arrayOf<Any>(userId, it) }
        )
    }
}
