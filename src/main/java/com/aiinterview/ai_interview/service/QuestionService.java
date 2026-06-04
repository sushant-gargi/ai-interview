package com.aiinterview.ai_interview.service;

import com.aiinterview.ai_interview.dto.question.AnswerRequest;
import com.aiinterview.ai_interview.dto.question.QuestionResponse;

import java.util.List;

public interface QuestionService {
    List<QuestionResponse> getSessionQuestions(Long sessionId);
    QuestionResponse submitAnswer(Long sessionId, Long questionId, AnswerRequest request);
    QuestionResponse getCurrentQuestion(Long sessionId);
}