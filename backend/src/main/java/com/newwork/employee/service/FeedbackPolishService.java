package com.newwork.employee.service;

import com.newwork.employee.dto.response.PolishFeedbackResponse;

/**
 * AI-assisted feedback utilities.
 */
public interface FeedbackPolishService {

    PolishFeedbackResponse polish(String text);
}
