package com.incheon.notice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * 이메일 발송 서비스
 * SMTP를 통한 이메일 전송
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    /**
     * 이메일 인증 메일 발송
     */
    public void sendVerificationEmail(String toEmail, String token) {
        log.debug("이메일 인증 메일 발송: toEmail={}", toEmail);

        String subject = "[인천대 공지사항] 이메일 인증";
        String verificationUrl = frontendUrl + "/verify-email?token=" + token;

        String content = String.format(
                "인천대학교 공지사항 앱 회원가입을 환영합니다!\n\n" +
                "아래 링크를 클릭하여 이메일 인증을 완료해주세요.\n\n" +
                "%s\n\n" +
                "이 링크는 24시간 동안 유효합니다.\n\n" +
                "본인이 요청하지 않은 경우 이 이메일을 무시해주세요.",
                verificationUrl
        );

        sendEmail(toEmail, subject, content);
    }

    /**
     * 아이디 찾기 메일 발송
     */
    public void sendFindIdEmail(String toEmail) {
        log.debug("아이디 찾기 메일 발송: toEmail={}", toEmail);

        String subject = "[인천대 공지사항] 아이디 찾기";

        String content = String.format(
                "아이디 찾기 요청이 접수되었습니다.\n\n" +
                "회원가입 시 사용하신 이메일 주소는 다음과 같습니다:\n\n" +
                "%s\n\n" +
                "이 이메일을 사용하여 로그인하실 수 있습니다.\n\n" +
                "본인이 요청하지 않은 경우 이 이메일을 무시해주세요.",
                toEmail
        );

        sendEmail(toEmail, subject, content);
    }

    /**
     * 비밀번호 재설정 메일 발송
     */
    public void sendPasswordResetEmail(String toEmail, String token) {
        log.debug("비밀번호 재설정 메일 발송: toEmail={}", toEmail);

        String subject = "[인천대 공지사항] 비밀번호 재설정";
        String resetUrl = frontendUrl + "/reset-password?token=" + token;

        String content = String.format(
                "비밀번호 재설정 요청이 접수되었습니다.\n\n" +
                "아래 링크를 클릭하여 비밀번호를 재설정해주세요.\n\n" +
                "%s\n\n" +
                "이 링크는 1시간 동안 유효합니다.\n\n" +
                "본인이 요청하지 않은 경우 이 이메일을 무시해주세요.",
                resetUrl
        );

        sendEmail(toEmail, subject, content);
    }

    /**
     * Firebase 이메일 인증 메일 발송
     */
    public void sendFirebaseVerificationEmail(String toEmail, String verificationLink) {
        log.debug("Firebase 이메일 인증 메일 발송: toEmail={}", toEmail);

        String subject = "[인천대 공지사항] 이메일 인증";

        String content = String.format(
                "인천대학교 공지사항 앱 회원가입을 환영합니다!\n\n" +
                "아래 링크를 클릭하여 이메일 인증을 완료해주세요.\n\n" +
                "%s\n\n" +
                "이메일 인증을 완료하면 모든 기능을 이용하실 수 있습니다.\n\n" +
                "본인이 요청하지 않은 경우 이 이메일을 무시해주세요.\n\n" +
                "감사합니다.\n" +
                "인천대학교 공지사항 앱 팀",
                verificationLink
        );

        sendEmail(toEmail, subject, content);
    }

    /**
     * Firebase 비밀번호 재설정 메일 발송
     */
    public void sendFirebasePasswordResetEmail(String toEmail, String userName, String resetLink) {
        log.debug("Firebase 비밀번호 재설정 메일 발송: toEmail={}", toEmail);

        String subject = "[인천대 공지사항] 비밀번호 재설정";

        String content = String.format(
                "%s님, 안녕하세요.\n\n" +
                "비밀번호 재설정 요청이 접수되었습니다.\n\n" +
                "아래 링크를 클릭하여 새로운 비밀번호를 설정해주세요.\n\n" +
                "%s\n\n" +
                "이 링크는 1시간 동안 유효합니다.\n\n" +
                "본인이 요청하지 않은 경우 이 이메일을 무시해주세요.\n" +
                "비밀번호는 변경되지 않습니다.\n\n" +
                "감사합니다.\n" +
                "인천대학교 공지사항 앱 팀",
                userName,
                resetLink
        );

        sendEmail(toEmail, subject, content);
    }

    /**
     * 일반 이메일 발송
     */
    private void sendEmail(String to, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);

            mailSender.send(message);
            log.info("이메일 발송 성공: to={}, subject={}", to, subject);

        } catch (Exception e) {
            log.error("이메일 발송 실패: to={}, error={}", to, e.getMessage(), e);
            throw new RuntimeException("이메일 발송에 실패했습니다", e);
        }
    }
}
