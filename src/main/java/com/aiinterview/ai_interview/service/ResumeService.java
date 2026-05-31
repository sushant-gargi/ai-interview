package com.aiinterview.ai_interview.service;

import com.aiinterview.ai_interview.dto.resume.ResumeResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ResumeService {
    ResumeResponse uploadResume(MultipartFile file);
}
