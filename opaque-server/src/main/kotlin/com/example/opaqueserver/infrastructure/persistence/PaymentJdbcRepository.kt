package com.example.opaqueserver.infrastructure.persistence

import com.example.opaqueserver.domain.payment.Payment
import com.example.opaqueserver.domain.payment.PaymentRepository
import com.example.opaqueserver.domain.payment.PaymentStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForObject
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.sql.ResultSet

@Repository
class PaymentJdbcRepository(private val jdbcTemplate: JdbcTemplate) : PaymentRepository {

    override fun findById(id: Long): Payment? =
        jdbcTemplate.query(
            """
            SELECT  *
            FROM    opaque_db.public.payments
            WHERE   id = ?
            """.trimIndent(),
            ::mapRow, id
        ).firstOrNull()

    override fun findByOrderId(orderId: String): Payment? =
        jdbcTemplate.query(
            """
            SELECT  *
            FROM    opaque_db.public.payments
            WHERE   order_id = ?
            """.trimIndent(),
            ::mapRow, orderId
        ).firstOrNull()

    override fun findByIdAndUserId(id: Long, userId: Long): Payment? =
        jdbcTemplate.query(
            """
            SELECT  *
            FROM    opaque_db.public.payments
            WHERE   id = ? AND user_id = ?
            """.trimIndent(),
            ::mapRow, id, userId
        ).firstOrNull()

    override fun findByUserId(userId: Long): List<Payment> =
        jdbcTemplate.query(
            """
            SELECT  *
            FROM    opaque_db.public.payments
            WHERE   user_id = ?
            ORDER BY created_at DESC
            """.trimIndent(),
            ::mapRow, userId
        )

    override fun save(payment: Payment): Payment {
        val keyHolder = GeneratedKeyHolder()
        jdbcTemplate.update({ con ->
            con.prepareStatement(
                """
                INSERT INTO opaque_db.public.payments (user_id, order_id, amount, status)
                VALUES (?, ?, ?, ?)
                """.trimIndent(),
                arrayOf("id")
            ).apply {
                setLong(1, payment.userId)
                setString(2, payment.orderId)
                setBigDecimal(3, payment.amount)
                setString(4, payment.status.name)
            }
        }, keyHolder)
        return payment.copy(id = keyHolder.key!!.toLong())
    }

    override fun updateStatus(id: Long, status: PaymentStatus) {
        jdbcTemplate.update(
            """
            UPDATE  opaque_db.public.payments
            SET     status = ?, updated_at = NOW()
            WHERE   id = ?
            """.trimIndent(),
            status.name, id
        )
    }

    override fun sumCompletedAmount(): BigDecimal =
        jdbcTemplate.queryForObject<BigDecimal>(
            """
            SELECT  COALESCE(SUM(amount), 0)
            FROM    opaque_db.public.payments
            WHERE   status = 'COMPLETED'
            """.trimIndent()
        ) ?: BigDecimal.ZERO

    private fun mapRow(rs: ResultSet, @Suppress("UNUSED_PARAMETER") n: Int) = Payment(
        id = rs.getLong("id"),
        userId = rs.getLong("user_id"),
        orderId = rs.getString("order_id"),
        amount = rs.getBigDecimal("amount"),
        status = PaymentStatus.valueOf(rs.getString("status")),
        createdAt = rs.getTimestamp("created_at").toInstant(),
        updatedAt = rs.getTimestamp("updated_at").toInstant()
    )
}
