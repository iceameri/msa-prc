package com.example.authorizationserver.infrastructure.persistence

import com.example.authorizationserver.domain.tenant.Tenant
import com.example.authorizationserver.domain.tenant.TenantRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForObject
import org.springframework.stereotype.Repository

@Repository
class TenantJdbcRepository(private val jdbcTemplate: JdbcTemplate) : TenantRepository {

    override fun findBySlug(slug: String): Tenant? =
        jdbcTemplate.query(
            """
            |SELECT id,
                    name,
                    slug,
                    status,
                    created_at
            |FROM   authorization_db.public.tenants
            |WHERE  slug = ?
            |""".trimMargin(),
            rowMapper,
            slug
        ).firstOrNull()

    override fun findById(id: Long): Tenant? =
        jdbcTemplate.query(
            """
            |SELECT id,
                    name,
                    slug,
                    status,
                    created_at
            |FROM   authorization_db.public.tenants
            |WHERE  id = ?
            |""".trimMargin(),
            rowMapper,
            id
        ).firstOrNull()

    override fun save(tenant: Tenant): Tenant {
        val id = jdbcTemplate.queryForObject<Long>(
            """
            INSERT INTO authorization_db.public.tenants (name, slug, status)
            VALUES (?, ?, ?)
            RETURNING id
            """.trimMargin(),
            tenant.name, tenant.slug, tenant.status
        )!!
        return tenant.copy(id = id)
    }

    private val rowMapper = { rs: java.sql.ResultSet, _: Int ->
        Tenant(
            id = rs.getLong("id"),
            name = rs.getString("name"),
            slug = rs.getString("slug"),
            status = rs.getString("status"),
            createdAt = rs.getTimestamp("created_at").toInstant()
        )
    }
}
