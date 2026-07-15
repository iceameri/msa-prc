package com.example.opaqueserver.infrastructure.persistence

import com.example.opaqueserver.domain.payment.PaymentSaga
import com.example.opaqueserver.domain.payment.SagaStatus
import com.example.opaqueserver.domain.payment.SagaStep
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository
import java.sql.ResultSet

@Repository
class PaymentSagaJdbcRepository(private val jdbcTemplate: JdbcTemplate) {

    fun save(saga: PaymentSaga): PaymentSaga {
        val keyHolder = GeneratedKeyHolder()
        jdbcTemplate.update({ con ->
            con.prepareStatement(
                """
                |INSERT INTO opaque_db.public.payment_sagas (payment_id, step, status, detail)
                |VALUES (?, ?, ?, ?)
                |""".trimMargin(), arrayOf("id")
            ).apply {
                setLong(1, saga.paymentId)
                setString(2, saga.step.name)
                setString(3, saga.status.name)
                setString(4, saga.detail)
            }
        }, keyHolder)
        return saga.copy(id = keyHolder.key!!.toLong())
    }

    fun findByPaymentId(paymentId: Long): List<PaymentSaga> =
        jdbcTemplate.query(
            """
            |SELECT id,
                    payment_id,
                    step,
                    status,
                    detail,
                    created_at
            |FROM   opaque_db.public.payment_sagas
            |WHERE  payment_id = ?
            |ORDER BY created_at
            |""".trimMargin(),
            ::mapRow, paymentId
        )

    private fun mapRow(rs: ResultSet, @Suppress("UNUSED_PARAMETER") n: Int) = PaymentSaga(
        id = rs.getLong("id"),
        paymentId = rs.getLong("payment_id"),
        step = SagaStep.valueOf(rs.getString("step")),
        status = SagaStatus.valueOf(rs.getString("status")),
        detail = rs.getString("detail"),
        createdAt = rs.getTimestamp("created_at").toInstant()
    )
}
