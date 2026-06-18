package com.example.opaqueserver.infrastructure.mail

import com.example.opaqueserver.application.port.out.EmailPort
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Component

@Component
class MailAdapter(private val mailSender: JavaMailSender) : EmailPort {

    override fun send(to: String, subject: String, body: String) {
        val message = SimpleMailMessage().apply {
            setTo(to)
            setSubject(subject)
            text = body
        }
        mailSender.send(message)
    }
}
