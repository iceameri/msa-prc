package com.example.authorizationserver.infrastructure.persistence

import com.example.authorizationserver.domain.apikey.ApiKey
import com.example.authorizationserver.domain.apikey.ApiKeyRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForObject
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.Instant

@Repository
class ApiKeyJdbcRepository(private val jdbcTemplate: JdbcTemplate) : ApiKeyRepository {

    override fun findByKeyHash(keyHash: String): ApiKey? =
        jdbcTemplate.query(
            """
            SELECT  id,                
                    tenant_id,         
                    key_hash,          
                    key_prefix,        
                    name,              
                    status,            
                    rate_limit_burst,  
                    rate_limit_refill ,
                    created_at,        
                    expires_at,        
                    last_used_at 
            FROM    authorization_db.public.api_keys
            WHERE   key_hash = ?
            """.trimMargin(),
            rowMapper,
            keyHash
        ).firstOrNull()

    override fun findById(id: Long): ApiKey? =
        jdbcTemplate.query(
            """
            |SELECT id,                
                    tenant_id,         
                    key_hash,          
                    key_prefix,        
                    name,              
                    status,            
                    rate_limit_burst,  
                    rate_limit_refill ,
                    created_at,        
                    expires_at,        
                    last_used_at      
            |FROM   authorization_db.public.api_keys
            |WHERE  id = ?
            |""".trimMargin(),
            rowMapper,
            id
        ).firstOrNull()

    override fun findByTenantId(tenantId: Long): List<ApiKey> =
        jdbcTemplate.query(
            """
            |SELECT id,                
                    tenant_id,         
                    key_hash,          
                    key_prefix,        
                    name,              
                    status,            
                    rate_limit_burst,  
                    rate_limit_refill ,
                    created_at,        
                    expires_at,        
                    last_used_at 
            |FROM   authorization_db.public.api_keys
            |WHERE  tenant_id = ?
            |ORDER BY created_at DESC
            |""".trimMargin(),
            rowMapper,
            tenantId
        )

    override fun save(apiKey: ApiKey): ApiKey {
        val id = jdbcTemplate.queryForObject<Long>(
            """
            INSERT INTO authorization_db.public.api_keys
            (tenant_id, key_hash, key_prefix, name, status, rate_limit_burst, rate_limit_refill, expires_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
            """.trimMargin(),
            apiKey.tenantId, apiKey.keyHash, apiKey.keyPrefix, apiKey.name, apiKey.status,
            apiKey.rateLimitBurst, apiKey.rateLimitRefill,
            apiKey.expiresAt?.let { Timestamp.from(it) }
        )!!
        return apiKey.copy(id = id)
    }

    override fun updateStatus(id: Long, status: String) {
        jdbcTemplate.update(
            """
            |UPDATE authorization_db.public.api_keys
            |SET status = ? WHERE id = ?
            |""".trimMargin(),
            status, id
        )
    }

    override fun updateLastUsedAt(id: Long) {
        jdbcTemplate.update(
            """
            |UPDATE authorization_db.public.api_keys
            |SET last_used_at = ? WHERE id = ?
            |""".trimMargin(),
            Timestamp.from(Instant.now()), id
        )
    }

    private val rowMapper = { rs: java.sql.ResultSet, _: Int ->
        ApiKey(
            id = rs.getLong("id"),
            tenantId = rs.getLong("tenant_id"),
            keyHash = rs.getString("key_hash"),
            keyPrefix = rs.getString("key_prefix"),
            name = rs.getString("name"),
            status = rs.getString("status"),
            rateLimitBurst = rs.getInt("rate_limit_burst"),
            rateLimitRefill = rs.getInt("rate_limit_refill"),
            createdAt = rs.getTimestamp("created_at").toInstant(),
            expiresAt = rs.getTimestamp("expires_at")?.toInstant(),
            lastUsedAt = rs.getTimestamp("last_used_at")?.toInstant()
        )
    }
}
