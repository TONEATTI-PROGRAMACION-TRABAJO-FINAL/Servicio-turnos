package com.toneatti.turnos.support;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Checks the application/problem+json errors this service answers to the app, with their stable {@code code}.
 */
public final class ProblemAssertions {

    private ProblemAssertions() {
    }

    public static ResultActions expectProblem(ResultActions result, int status, String code) throws Exception {
        return result
                .andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.code").value(code));
    }
}
