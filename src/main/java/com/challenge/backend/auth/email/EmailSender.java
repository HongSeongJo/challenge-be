package com.challenge.backend.auth.email;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailSender {

    private final JavaMailSender mailSender;

    public void sendVerificationCode(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("[Challpot] 이메일 인증 코드");
        message.setText("인증 코드: " + code + "\n\n5분 이내에 입력해 주세요.");
        mailSender.send(message);
    }
}
