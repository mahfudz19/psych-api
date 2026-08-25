package com.psycorp.psychapi.feature.auth.service;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class EmailService {

    @Inject
    Mailer mailer;

    @Inject
    Logger log;

    @ConfigProperty(name = "app.frontend.url", defaultValue = "http://localhost:5173")
    String frontendUrl;

    public void sendVerificationEmail(String toEmail, String fullName, String plainToken) {
        String verifyUrl = frontendUrl + "?email=" + toEmail + "&token=" + plainToken;

        String htmlBody = """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                <h2>Halo, %s!</h2>
                <p>Terima kasih telah mendaftar di Psych. Silakan klik tombol di bawah ini untuk mengaktifkan akun Anda:</p>
                <div style="margin: 30px 0;">
                    <a href="%s" style="background-color: #4F46E5; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold;">
                        Verifikasi Email Saya
                    </a>
                </div>
                <p style="color: #666; font-size: 14px;">Tautan ini hanya berlaku selama 15 menit. Jika Anda tidak merasa mendaftar, abaikan email ini.</p>
            </div>
            """.formatted(fullName, verifyUrl);

        try {
            mailer.send(Mail.withHtml(toEmail, "Verifikasi Akun Psych Anda", htmlBody));
            log.infof("✅ Email verifikasi berhasil dikirim ke: %s", toEmail);
        } catch (Exception e) {
            log.error("❌ Gagal mengirim email ke: " + toEmail, e);
        }
    }
}