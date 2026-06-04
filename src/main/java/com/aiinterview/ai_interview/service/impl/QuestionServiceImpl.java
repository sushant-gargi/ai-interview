package com.aiinterview.ai_interview.service.impl;

import com.aiinterview.ai_interview.dto.question.AnswerRequest;
import com.aiinterview.ai_interview.dto.question.QuestionResponse;
import com.aiinterview.ai_interview.entity.InterviewQuestion;
import com.aiinterview.ai_interview.entity.InterviewSession;
import com.aiinterview.ai_interview.enums.SessionStatus;
import com.aiinterview.ai_interview.error.BadRequestException;
import com.aiinterview.ai_interview.error.ResourceNotFoundException;
import com.aiinterview.ai_interview.repository.InterviewQuestionRepository;
import com.aiinterview.ai_interview.repository.InterviewSessionRepository;
import com.aiinterview.ai_interview.security.AuthUtil;
import com.aiinterview.ai_interview.service.QuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    final InterviewQuestionRepository questionRepository;
    final InterviewSessionRepository sessionRepository;
    final AuthUtil authUtil;

    @Override
    public List<QuestionResponse> getSessionQuestions(Long sessionId) {
        Long userId = authUtil.getCurrentUserId();
        InterviewSession session = getSessionAndValidateOwner(sessionId, userId);

        return questionRepository
                .findBySessionIdOrderBySequenceOrderAsc(sessionId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public QuestionResponse submitAnswer(Long sessionId, Long questionId,
                                         AnswerRequest request) {
        Long userId = authUtil.getCurrentUserId();
        InterviewSession session = getSessionAndValidateOwner(sessionId, userId);

        // Session must be IN_PROGRESS
        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new BadRequestException(
                    "Session is not in progress. Current status: " + session.getStatus());
        }

        InterviewQuestion question = questionRepository
                .findByIdAndSessionId(questionId, sessionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Question", questionId.toString()));

        // Can't answer twice
        if (question.getCandidateAnswer() != null) {
            throw new BadRequestException("This question has already been answered");
        }

        question.setCandidateAnswer(request.answer());
        question.setAnsweredAt(Instant.now());

        // aiFeedback and score are null for now — Phase 2 will fill these
        InterviewQuestion saved = questionRepository.save(question);
        log.info("Answer saved for question {} in session {}", questionId, sessionId);

        return toResponse(saved);
    }

    @Override
    public QuestionResponse getCurrentQuestion(Long sessionId) {
        Long userId = authUtil.getCurrentUserId();
        InterviewSession session = getSessionAndValidateOwner(sessionId, userId);

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new BadRequestException(
                    "Session is not in progress. Current status: " + session.getStatus());
        }

        return questionRepository
                .findFirstBySessionIdAndCandidateAnswerIsNullOrderBySequenceOrderAsc(sessionId)
                .map(this::toResponse)
                .orElseThrow(() -> new BadRequestException(
                        "All questions have been answered"));
    }

    private InterviewSession getSessionAndValidateOwner(Long sessionId, Long userId) {
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Session", sessionId.toString()));

        if (!session.getUser().getId().equals(userId)) {
            throw new BadRequestException("This session does not belong to you");
        }

        return session;
    }

    private QuestionResponse toResponse(InterviewQuestion q) {
        return new QuestionResponse(
                q.getId(),
                q.getSequenceOrder(),
                q.getQuestionText(),
                q.getCandidateAnswer(),
                q.getAiFeedback(),
                q.getIdealAnswer(),
                q.getScore(),
                q.getAnsweredAt()
        );
    }
}