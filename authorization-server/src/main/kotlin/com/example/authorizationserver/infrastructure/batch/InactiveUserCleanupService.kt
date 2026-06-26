package com.example.authorizationserver.infrastructure.batch

import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForList
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class InactiveUserCleanupService(
    private val jdbcTemplate: JdbcTemplate,
    private val kafkaTemplate: KafkaTemplate<String, String>
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0 3 * * *")
    fun runScheduled() = run()

    fun run(): Int {
        val userIds = jdbcTemplate.queryForList<Long>(
            """
            SELECT  user_id
            FROM    authorization_db.public.user_activity
            WHERE   last_active_at < NOW() - INTERVAL '90 days'
            """.trimIndent()
        )
        userIds.filterNotNull().forEach { userId ->
            kafkaTemplate.send("user-management", userId.toString(), """{"action":"SUSPEND","userId":$userId}""")
        }
        log.info("InactiveUserCleanup: {} users queued for suspension", userIds.size)
        return userIds.size
    }
}
