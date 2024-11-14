package it.itsincom.webdev2023.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class SmsService {
    @ConfigProperty(name = "twilio.account-sid")
    String accountSid;
    @ConfigProperty(name = "twilio.auth-token")
    String authToken;
    @ConfigProperty(name = "twilio.phone-number")
    String fromPhoneNumber;

    @PostConstruct
    public void init() {
        Twilio.init(accountSid, authToken);
    }

    public void sendVerificationSms(String phone, String messageBody) {
        try {
            Message.creator(new PhoneNumber(phone), new PhoneNumber(fromPhoneNumber), messageBody).create();
        } catch (Exception e) {
            System.err.println("Failed to send SMS: " + e.getMessage());
        }
    }
}