package it.itsincom.webdev2023.persistence.model;

import io.quarkus.mailer.Mail;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.quarkus.mailer.Mailer;

@ApplicationScoped
public class MailService {

    @Inject
    Mailer mailer;

    public void sendVerificationEmail(String email, String subject, String body) {
        Mail mail = Mail.withText(email, subject, body);
        mailer.send(mail);
    }

}
