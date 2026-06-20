package com.aiinterview.ai_interview.service.impl;

import com.aiinterview.ai_interview.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.mail.enabled:true}")
    private boolean emailEnabled;

    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a z", Locale.ENGLISH)
                             .withZone(ZoneId.of("Asia/Kolkata"));

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void sendInterviewInvite(String candidateEmail, String recruiterName,
                                    String jobRole, Instant scheduledStart,
                                    Instant scheduledEnd, Long sessionId) {
        if (!emailEnabled) {
            log.info("[EmailService] Email disabled — skipping invite to {}", candidateEmail);
            return;
        }

        String subject = "Your Interview has been Scheduled — " + jobRole;
        String startFormatted = DISPLAY_FMT.format(scheduledStart);
        String endFormatted   = DISPLAY_FMT.format(scheduledEnd);

        String html = """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="UTF-8">
                  <style>
                    body { font-family: 'Segoe UI', Arial, sans-serif; background: #0d0e15; margin: 0; padding: 0; }
                    .wrapper { max-width: 600px; margin: 40px auto; background: #14162b; border-radius: 16px;
                               border: 1px solid rgba(255,255,255,0.08); overflow: hidden; }
                    .header { background: linear-gradient(135deg, #8b5cf6, #06b6d4); padding: 36px 40px; text-align: center; }
                    .header h1 { color: #fff; font-size: 24px; font-weight: 700; margin: 0; letter-spacing: -0.5px; }
                    .header p  { color: rgba(255,255,255,0.8); margin: 8px 0 0; font-size: 14px; }
                    .body { padding: 36px 40px; color: #f3f4f6; }
                    .body p  { line-height: 1.7; margin: 0 0 16px; color: #d1d5db; font-size: 15px; }
                    .card { background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.08);
                            border-radius: 12px; padding: 20px 24px; margin: 24px 0; }
                    .card-row { display: flex; justify-content: space-between; padding: 8px 0;
                                border-bottom: 1px solid rgba(255,255,255,0.05); font-size: 14px; }
                    .card-row:last-child { border-bottom: none; }
                    .card-label { color: #9ca3af; }
                    .card-value { color: #f3f4f6; font-weight: 600; text-align: right; }
                    .cta { display: block; text-align: center; margin: 28px 0; }
                    .cta a { background: linear-gradient(135deg, #8b5cf6, #7c3aed); color: #fff; padding: 14px 36px;
                             border-radius: 8px; text-decoration: none; font-weight: 700; font-size: 15px;
                             box-shadow: 0 4px 20px rgba(139,92,246,0.4); }
                    .tip { background: rgba(6,182,212,0.08); border-left: 3px solid #06b6d4;
                           border-radius: 6px; padding: 14px 18px; margin: 20px 0; font-size: 14px; color: #a5f3fc; }
                    .footer { text-align: center; padding: 20px 40px; font-size: 12px; color: #6b7280;
                              border-top: 1px solid rgba(255,255,255,0.06); }
                  </style>
                </head>
                <body>
                  <div class="wrapper">
                    <div class="header">
                      <h1>🎯 Interview Scheduled</h1>
                      <p>You have been invited to a technical interview</p>
                    </div>
                    <div class="body">
                      <p>Hi there,</p>
                      <p><strong>%s</strong> has scheduled a technical interview for you. Please review the details below and make sure you are ready to join on time.</p>
                      <div class="card">
                        <div class="card-row">
                          <span class="card-label">Position</span>
                          <span class="card-value">%s</span>
                        </div>
                        <div class="card-row">
                          <span class="card-label">Starts At</span>
                          <span class="card-value">%s</span>
                        </div>
                        <div class="card-row">
                          <span class="card-label">Ends At</span>
                          <span class="card-value">%s</span>
                        </div>
                        <div class="card-row">
                          <span class="card-label">Session ID</span>
                          <span class="card-value">#%d</span>
                        </div>
                      </div>
                      <div class="tip">
                        💡 <strong>Pro tip:</strong> Upload your resume in the interview lobby before the session starts.
                        The AI interviewer will tailor its questions based on your background.
                      </div>
                      <p>Log in to the platform to access the interview lobby and prepare your resume upload.</p>
                    </div>
                    <div class="footer">
                      This email was sent by AI Interviewer Platform. If you believe this is a mistake, please ignore it.
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(recruiterName, jobRole, startFormatted, endFormatted, sessionId);

        send(candidateEmail, subject, html);
        log.info("[EmailService] Interview invite sent to {} for session {}", candidateEmail, sessionId);
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void sendReportReady(String recruiterEmail, String candidateEmail,
                                String jobRole, int overallScore,
                                String recommendation, Long sessionId) {
        if (!emailEnabled) {
            log.info("[EmailService] Email disabled — skipping report notification to {}", recruiterEmail);
            return;
        }

        String subject = "Interview Report Ready — " + jobRole + " | Session #" + sessionId;

        String scoreColor = overallScore >= 7 ? "#10b981" : overallScore >= 5 ? "#f59e0b" : "#ef4444";
        String recBadgeColor = "YES".equals(recommendation) ? "#10b981"
                : "MAYBE".equals(recommendation) ? "#f59e0b" : "#ef4444";

        String html = """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="UTF-8">
                  <style>
                    body { font-family: 'Segoe UI', Arial, sans-serif; background: #0d0e15; margin: 0; padding: 0; }
                    .wrapper { max-width: 600px; margin: 40px auto; background: #14162b; border-radius: 16px;
                               border: 1px solid rgba(255,255,255,0.08); overflow: hidden; }
                    .header { background: linear-gradient(135deg, #8b5cf6, #06b6d4); padding: 36px 40px; text-align: center; }
                    .header h1 { color: #fff; font-size: 24px; font-weight: 700; margin: 0; }
                    .header p  { color: rgba(255,255,255,0.8); margin: 8px 0 0; font-size: 14px; }
                    .body { padding: 36px 40px; color: #f3f4f6; }
                    .body p  { line-height: 1.7; margin: 0 0 16px; color: #d1d5db; font-size: 15px; }
                    .card { background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.08);
                            border-radius: 12px; padding: 20px 24px; margin: 24px 0; }
                    .card-row { display: flex; justify-content: space-between; padding: 8px 0;
                                border-bottom: 1px solid rgba(255,255,255,0.05); font-size: 14px; }
                    .card-row:last-child { border-bottom: none; }
                    .card-label { color: #9ca3af; }
                    .card-value { color: #f3f4f6; font-weight: 600; text-align: right; }
                    .score-ring { text-align: center; padding: 24px 0; }
                    .score-circle { display: inline-block; width: 90px; height: 90px; border-radius: 50%;
                                    border: 4px solid %s; line-height: 82px; font-size: 28px;
                                    font-weight: 800; color: %s; }
                    .rec-badge { display: inline-block; padding: 6px 20px; border-radius: 20px;
                                 background: %s; color: #fff; font-weight: 700; font-size: 14px; margin-top: 10px; }
                    .footer { text-align: center; padding: 20px 40px; font-size: 12px; color: #6b7280;
                              border-top: 1px solid rgba(255,255,255,0.06); }
                  </style>
                </head>
                <body>
                  <div class="wrapper">
                    <div class="header">
                      <h1>📊 Evaluation Report Ready</h1>
                      <p>The AI evaluation report for your candidate is now available</p>
                    </div>
                    <div class="body">
                      <p>Hi,</p>
                      <p>The interview for session <strong>#%d</strong> has been evaluated by our AI system. Here is a summary:</p>
                      <div class="card">
                        <div class="card-row">
                          <span class="card-label">Candidate</span>
                          <span class="card-value">%s</span>
                        </div>
                        <div class="card-row">
                          <span class="card-label">Position</span>
                          <span class="card-value">%s</span>
                        </div>
                        <div class="card-row">
                          <span class="card-label">Session ID</span>
                          <span class="card-value">#%d</span>
                        </div>
                      </div>
                      <div class="score-ring">
                        <div class="score-circle">%d</div>
                        <div style="color: #9ca3af; font-size: 13px; margin-top: 6px;">/ 10</div>
                        <div><span class="rec-badge">%s</span></div>
                      </div>
                      <p>Log in to the platform to view the full evaluation report including detailed skill breakdown, question-by-question scoring, and the complete interview transcript.</p>
                    </div>
                    <div class="footer">
                      This email was sent by AI Interviewer Platform. You are receiving this because you created this interview session.
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(scoreColor, scoreColor, recBadgeColor,
                              sessionId, candidateEmail, jobRole, sessionId,
                              overallScore, recommendation);

        send(recruiterEmail, subject, html);
        log.info("[EmailService] Report-ready email sent to {} for session {}", recruiterEmail, sessionId);
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void send(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("[EmailService] Failed to send email to {}: {}", to, e.getMessage());
            // Intentionally not re-throwing — email failures must never crash the business flow
        }
    }
}
