package com.desiato.germanMailer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class GermanEmailSender {

    private final JavaMailSender mailSender;

    @Value("${mail.to}")
    private String recipient;

    public GermanEmailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void send(String lessonBody) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(recipient);

        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        message.setSubject("Dein Deutsch von heute - " + date);
        message.setText(lessonBody);

        mailSender.send(message);
    }
}