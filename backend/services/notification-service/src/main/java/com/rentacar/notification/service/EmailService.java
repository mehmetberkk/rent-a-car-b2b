package com.rentacar.notification.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    public void sendActivationEmail(String toEmail, String firstName, String token) {
        String activationUrl = frontendUrl + "/activate?token=" + token;
        log.info("Sending activation email to {} with link: {}", toEmail, activationUrl);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());

            helper.setFrom("noreply@rentacar-b2b.com");
            helper.setTo(toEmail);
            helper.setSubject("Rent A Car B2B - Hesabınızı Aktive Edin");

            String htmlContent = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e2e8f0; rounded: 8px;">
                    <h2 style="color: #0284c7;">Rent A Car B2B Platformuna Hoş Geldiniz!</h2>
                    <p>Merhaba <strong>%s</strong>,</p>
                    <p>Rent a car dükkan sahipleri ortak araç havuzu ve filo yönetim platformuna kaydınız başarıyla alındı. Sisteme giriş yapabilmek ve dükkanlarınızı yönetebilmek için lütfen aşağıdaki butona tıklayarak e-posta adresinizi doğrulayınız:</p>
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="%s" style="background-color: #0284c7; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold; display: inline-block;">Hesabımı Aktive Et</a>
                    </div>
                    <p style="color: #64748b; font-size: 13px;">Bu bağlantı 24 saat boyunca geçerlidir. Butona tıklayamıyorsanız aşağıdaki adresi tarayıcınıza kopyalayabilirsiniz:</p>
                    <p style="color: #0284c7; font-size: 13px; word-break: break-all;">%s</p>
                </div>
            """.formatted(firstName, activationUrl, activationUrl);

            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Activation email sent successfully to {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send activation email to {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("E-posta gönderiminde hata oluştu: " + e.getMessage(), e);
        }
    }

    public void sendComplianceAlertEmail(String toEmail, String plate, String alertDetails) {
        log.info("Sending compliance alert to {} for vehicle {}", toEmail, plate);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());

            helper.setFrom("alerts@rentacar-b2b.com");
            helper.setTo(toEmail);
            helper.setSubject("Acil: Araç Muayene / Sigorta Uyarısı - " + plate);

            String htmlContent = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #fecaca; background-color: #fff5f5; border-radius: 8px;">
                    <h2 style="color: #dc2626;">Araç Operasyonel Uyarı</h2>
                    <p>Dükkanınıza kayıtlı <strong>%s</strong> plakalı aracınız için kritik operasyonel süre uyarısı bulunmaktadır:</p>
                    <div style="background-color: white; padding: 15px; border-left: 4px solid #dc2626; margin: 15px 0;">
                        %s
                    </div>
                    <p>Lütfen platforma giriş yaparak gerekli yenileme işlemlerini tamamlayınız.</p>
                </div>
            """.formatted(plate, alertDetails);

            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("Failed to send compliance alert email: {}", e.getMessage(), e);
            throw new RuntimeException("Uyarı e-postası gönderilemedi", e);
        }
    }
}

