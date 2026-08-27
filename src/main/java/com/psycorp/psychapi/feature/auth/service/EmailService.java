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
        String verifyUrl = frontendUrl + "/register/verify-email?email=" + toEmail + "&token=" + plainToken;

        String htmlBody = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="margin: 0; padding: 0; background-color: #f8fafc; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">
                <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="background-color: #f8fafc; padding: 40px 20px;">
                    <tr>
                        <td align="center">
                            <!-- Card Container -->
                            <table width="100%%" max-width="600px" border="0" cellspacing="0" cellpadding="0" style="max-width: 600px; background-color: #ffffff; border-radius: 24px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);">
                                
                                <!-- Header / Branding -->
                                <tr>
                                    <td align="center" style="background: linear-gradient(135deg, #14b8a6 0%%, #0f766e 100%%); padding: 32px 20px;">
                                        <h1 style="color: #ffffff; margin: 0; font-size: 28px; letter-spacing: 1px;">Psych</h1>
                                    </td>
                                </tr>

                                <!-- Body Content -->
                                <tr>
                                    <td style="padding: 40px 32px;">
                                        <h2 style="color: #0f172a; font-size: 22px; margin-top: 0; margin-bottom: 16px;">Halo, %s! 👋</h2>
                                        <p style="color: #475569; font-size: 16px; line-height: 1.6; margin-bottom: 24px;">
                                            Terima kasih telah bergabung dan memilih <strong>Psych</strong> sebagai platform kolaborasi Anda. Untuk mulai menggunakan layanan kami dan memastikan keamanan akun, silakan verifikasi alamat email Anda.
                                        </p>

                                        <!-- Call to Action Button -->
                                        <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="margin: 32px 0;">
                                            <tr>
                                                <td align="center">
                                                    <a href="%s" style="background-color: #0f766e; color: #ffffff; padding: 14px 32px; text-decoration: none; border-radius: 12px; font-weight: bold; font-size: 16px; display: inline-block;">
                                                        Verifikasi Email Saya
                                                    </a>
                                                </td>
                                            </tr>
                                        </table>

                                        <!-- Fallback Link -->
                                        <p style="color: #475569; font-size: 14px; line-height: 1.5; margin-bottom: 8px;">
                                            Atau salin dan tempel tautan berikut ke browser Anda jika tombol di atas tidak berfungsi:
                                        </p>
                                        <div style="background-color: #f8fafc; border: 1px solid #e2e8f0; padding: 12px 16px; border-radius: 8px; margin-bottom: 32px;">
                                            <a href="%s" style="font-size: 13px; color: #0f766e; word-break: break-all; text-decoration: none;">
                                                %s
                                            </a>
                                        </div>

                                        <hr style="border: none; border-top: 1px solid #e2e8f0; margin: 0 0 24px 0;">

                                        <!-- Security Notice -->
                                        <p style="color: #94a3b8; font-size: 13px; line-height: 1.5; margin: 0;">
                                            Tautan verifikasi ini hanya berlaku selama <strong>15 menit</strong>. Jika Anda tidak pernah merasa mendaftar di Psych, Anda dapat mengabaikan dan menghapus email ini dengan aman.
                                        </p>
                                    </td>
                                </tr>

                                <!-- Footer -->
                                <tr>
                                    <td align="center" style="background-color: #f8fafc; padding: 24px; border-top: 1px solid #e2e8f0;">
                                        <p style="color: #94a3b8; font-size: 12px; margin: 0;">
                                            &copy; 2026 PsyCorp. Semua hak dilindungi.
                                        </p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(fullName, verifyUrl, verifyUrl, verifyUrl);

        try {
            mailer.send(Mail.withHtml(toEmail, "Verifikasi Akun Psych Anda", htmlBody));
            log.infof("✅ Email verifikasi berhasil dikirim ke: %s", toEmail);
        } catch (Exception e) {
            log.error("❌ Gagal mengirim email ke: " + toEmail, e);
        }
    }

    public void sendResetPasswordEmail(String toEmail, String fullName, String plainToken) {
        // Buat URL yang mengarah ke halaman reset password di frontend
        String resetUrl = frontendUrl + "/reset-password?email=" + toEmail + "&token=" + plainToken;

        String htmlBody = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="margin: 0; padding: 0; background-color: #f8fafc; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">
                <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="background-color: #f8fafc; padding: 40px 20px;">
                    <tr>
                        <td align="center">
                            <!-- Card Container -->
                            <table width="100%%" max-width="600px" border="0" cellspacing="0" cellpadding="0" style="max-width: 600px; background-color: #ffffff; border-radius: 24px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);">
                                
                                <!-- Header / Branding -->
                                <tr>
                                    <td align="center" style="background: linear-gradient(135deg, #14b8a6 0%%, #0f766e 100%%); padding: 32px 20px;">
                                        <h1 style="color: #ffffff; margin: 0; font-size: 28px; letter-spacing: 1px;">Psych</h1>
                                    </td>
                                </tr>

                                <!-- Body Content -->
                                <tr>
                                    <td style="padding: 40px 32px;">
                                        <h2 style="color: #0f172a; font-size: 22px; margin-top: 0; margin-bottom: 16px;">Halo, %s! 🔒</h2>
                                        <p style="color: #475569; font-size: 16px; line-height: 1.6; margin-bottom: 24px;">
                                            Kami menerima permintaan untuk mengatur ulang kata sandi akun <strong>Psych</strong> Anda. Jika Anda merasa melakukan permintaan ini, silakan klik tombol di bawah untuk membuat kata sandi baru.
                                        </p>

                                        <!-- Call to Action Button -->
                                        <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="margin: 32px 0;">
                                            <tr>
                                                <td align="center">
                                                    <a href="%s" style="background-color: #0f766e; color: #ffffff; padding: 14px 32px; text-decoration: none; border-radius: 12px; font-weight: bold; font-size: 16px; display: inline-block;">
                                                        Atur Ulang Kata Sandi
                                                    </a>
                                                </td>
                                            </tr>
                                        </table>

                                        <!-- Fallback Link -->
                                        <p style="color: #475569; font-size: 14px; line-height: 1.5; margin-bottom: 8px;">
                                            Atau salin dan tempel tautan berikut ke browser Anda:
                                        </p>
                                        <div style="background-color: #f8fafc; border: 1px solid #e2e8f0; padding: 12px 16px; border-radius: 8px; margin-bottom: 32px;">
                                            <a href="%s" style="font-size: 13px; color: #0f766e; word-break: break-all; text-decoration: none;">
                                                %s
                                            </a>
                                        </div>

                                        <hr style="border: none; border-top: 1px solid #e2e8f0; margin: 0 0 24px 0;">

                                        <!-- Security Notice -->
                                        <p style="color: #94a3b8; font-size: 13px; line-height: 1.5; margin: 0;">
                                            Tautan ini hanya berlaku selama <strong>15 menit</strong>. Jika Anda tidak pernah meminta pengaturan ulang kata sandi, <strong>abaikan email ini</strong>. Kata sandi dan akun Anda akan tetap aman.
                                        </p>
                                    </td>
                                </tr>

                                <!-- Footer -->
                                <tr>
                                    <td align="center" style="background-color: #f8fafc; padding: 24px; border-top: 1px solid #e2e8f0;">
                                        <p style="color: #94a3b8; font-size: 12px; margin: 0;">
                                            &copy; 2026 PsyCorp. Semua hak dilindungi.
                                        </p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(fullName, resetUrl, resetUrl, resetUrl);

        try {
            mailer.send(Mail.withHtml(toEmail, "Atur Ulang Kata Sandi Psych Anda", htmlBody));
            log.infof("✅ Email Lupa Password berhasil dikirim ke: %s", toEmail);
        } catch (Exception e) {
            log.error("❌ Gagal mengirim email Lupa Password ke: " + toEmail, e);
        }
    }
}