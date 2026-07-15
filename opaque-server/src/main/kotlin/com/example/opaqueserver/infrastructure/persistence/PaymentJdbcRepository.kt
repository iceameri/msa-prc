package com.example.opaqueserver.infrastructure.persistence

import com.example.opaqueserver.domain.payment.Payment
import com.example.opaqueserver.domain.payment.PaymentStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForObject
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.sql.ResultSet

@Repository
class PaymentJdbcRepository(private val jdbcTemplate: JdbcTemplate) {

    fun findById(id: Long): Payment? =
        jdbcTemplate.query(
            """
            SELECT  id,
                    user_id,
                    order_id,
                    amount,
                    status,
                    created_at,
                    updated_at
            FROM    opaque_db.public.payments
            WHERE   id = ?
            """.trimMargin(),
            ::mapRow, id
        ).firstOrNull()

    fun findByOrderId(orderId: String): Payment? =
        jdbcTemplate.query(
            """
            SELECT  id,
                    user_id,
                    order_id,
                    amount,
                    status,
                    created_at,
                    updated_at
            FROM    opaque_db.public.payments
            WHERE   order_id = ?
            """.trimMargin(),
            ::mapRow, orderId
        ).firstOrNull()

    fun findByIdAndUserId(id: Long, userId: Long): Payment? =
        jdbcTemplate.query(
            """
            SELECT  id,
                    user_id,
                    order_id,
                    amount,
                    status,
                    created_at,
                    updated_at
            FROM    opaque_db.public.payments
            WHERE   id = ? AND user_id = ?
            """.trimMargin(),
            ::mapRow, id, userId
        ).firstOrNull()

    fun findByUserId(userId: Long): List<Payment> =
        jdbcTemplate.query(
            """
            SELECT  id,
                    user_id,
                    order_id,
                    amount,
                    status,
                    created_at,
                    updated_at
            FROM    opaque_db.public.payments
            WHERE   user_id = ?
            ORDER BY created_at DESC
            """.trimMargin(),
            ::mapRow, userId
        )

    fun save(payment: Payment): Payment {
        val keyHolder = GeneratedKeyHolder()
        jdbcTemplate.update({ con ->
            con.prepareStatement(
                """
                INSERT INTO opaque_db.public.payments (user_id, order_id, amount, status)
                VALUES (?, ?, ?, ?)
                """.trimMargin(),
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

    fun updateStatus(id: Long, status: PaymentStatus) {
        jdbcTemplate.update(
            """
            UPDATE  opaque_db.public.payments
            SET     status = ?, updated_at = NOW()
            WHERE   id = ?
            """.trimMargin(),
            status.name, id
        )
    }

    fun sumCompletedAmount(): BigDecimal =
        jdbcTemplate.queryForObject<BigDecimal>(
            """
            SELECT  COALESCE(SUM(amount), 0)
            FROM    opaque_db.public.payments
            WHERE   status = 'COMPLETED'
            """.trimMargin()
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
