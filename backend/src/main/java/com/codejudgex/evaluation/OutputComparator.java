package com.codejudgex.evaluation;

import org.springframework.stereotype.Component;

/**
 * Compares actual vs expected output for a single test case.
 *
 * Rules:
 * - Leading/trailing whitespace on each line is trimmed.
 * - Line endings are normalized (CRLF → LF).
 * - Trailing newlines at end of output are ignored.
 * - Comparison is case-sensitive.
 */
@Component
public class OutputComparator {

    /**
     * @param actual   raw stdout from Judge0 (may be base64-decoded already)
     * @param expected expected output from the test case
     * @return true if outputs are equivalent after normalization
     */
    public boolean isCorrect(String actual, String expected) {
        if (actual == null && expected == null) return true;
        if (actual == null || expected == null) return false;
        return normalize(actual).equals(normalize(expected));
    }

    private String normalize(String raw) {
        return raw.replace("\r\n", "\n")
                  .replace("\r", "\n")
                  .stripTrailing()
                  .lines()
                  .map(String::stripTrailing)
                  .collect(java.util.stream.Collectors.joining("\n"));
    }
}
