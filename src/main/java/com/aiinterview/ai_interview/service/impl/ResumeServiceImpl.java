package com.aiinterview.ai_interview.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.aiinterview.ai_interview.dto.resume.ResumeResponse;
import com.aiinterview.ai_interview.entity.InterviewSession;
import com.aiinterview.ai_interview.entity.Resume;
import com.aiinterview.ai_interview.entity.User;
import com.aiinterview.ai_interview.enums.SessionStatus;
import com.aiinterview.ai_interview.error.BadRequestException;
import com.aiinterview.ai_interview.error.ResourceNotFoundException;
import com.aiinterview.ai_interview.repository.InterviewSessionRepository;
import com.aiinterview.ai_interview.repository.ResumeRepository;
import com.aiinterview.ai_interview.repository.UserRepository;
import com.aiinterview.ai_interview.security.AuthUtil;
import com.aiinterview.ai_interview.service.ResumeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {

    final ResumeRepository resumeRepository;
    final UserRepository userRepository;
    final InterviewSessionRepository sessionRepository;
    final AuthUtil authUtil;

    @Value("${app.file.upload-dir}")
    String uploadDir;

    private ResumeResponse uploadResume(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.endsWith(".pdf")) {
            throw new BadRequestException("Only PDF files are accepted");
        }

        Long userId = authUtil.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        try {
            // Create uploads directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Save file with unique name to avoid conflicts
            String safeFilename = Paths.get(originalFilename).getFileName().toString();
            String uniqueFileName = UUID.randomUUID() + "_" + safeFilename;
            Path filePath = uploadPath.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Extract text from PDF
            String parsedText = extractTextFromPdf(filePath);
            log.info("Extracted {} characters from resume for user {}", parsedText.length(), userId);

            // Save to DB
            Resume resume = Resume.builder()
                    .user(user)
                    .fileName(originalFilename)
                    .filePath(filePath.toString())
                    .parsedText(parsedText)
                    .build();

            Resume saved = resumeRepository.save(resume);

            return new ResumeResponse(
                    saved.getId(),
                    saved.getFileName(),
                    saved.getParsedText(),
                    saved.getUploadedAt()
            );

        } catch (IOException e) {
            log.error("Failed to process resume file", e);
            throw new BadRequestException("Failed to process file: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public ResumeResponse uploadResumeForSession(Long sessionId, MultipartFile file) {
        Long userId = authUtil.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId.toString()));

        // Cleaned up the repeated duplicates here
        if (!session.getCandidateEmail().equalsIgnoreCase(user.getEmail())) {
            throw new BadRequestException("This session does not belong to you");
        }

        if (session.getStatus() != SessionStatus.SCHEDULED) {
            throw new BadRequestException("Resume can only be uploaded for SCHEDULED sessions. Current status: " + session.getStatus());
        }

        // Upload the new resume using the standard logic
        ResumeResponse resumeResponse = uploadResume(file);

        // Fetch the newly created Resume
        Resume newResume = resumeRepository.findById(resumeResponse.id())
                .orElseThrow(() -> new ResourceNotFoundException("Resume", resumeResponse.id().toString()));

        Resume oldResume = session.getResume();
        
        session.setResume(newResume);
        sessionRepository.save(session);

        if (oldResume != null) {
            try {
                Files.deleteIfExists(Paths.get(oldResume.getFilePath()));
            } catch (IOException e) {
                log.warn("Could not delete old resume file: {}", oldResume.getFilePath(), e);
            }
            resumeRepository.delete(oldResume);
        }

        log.info("Associated new resume ID {} with session ID {}. Old resume (if any) was deleted.", newResume.getId(), sessionId);

        return resumeResponse;
    }

    private String extractTextFromPdf(Path pdfPath) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            return new PDFTextStripper().getText(document);
        }
    }
}
