package com.codejudgex.evaluation;

/**
 * Maps Judge0 CE status IDs to internal submission status strings.
 *
 * Judge0 status IDs:
 *   1  = In Queue
 *   2  = Processing
 *   3  = Accepted
 *   4  = Wrong Answer
 *   5  = Time Limit Exceeded
 *   6  = Compilation Error
 *   7–12 = Runtime Errors (various)
 *   13 = Internal Error
 *   14 = Exec Format Error
 */
public final class Judge0StatusMapper {

    public static final String RUNNING              = "RUNNING";
    public static final String ACCEPTED             = "ACCEPTED";
    public static final String WRONG_ANSWER         = "WRONG_ANSWER";
    public static final String TIME_LIMIT_EXCEEDED  = "TIME_LIMIT_EXCEEDED";
    public static final String COMPILATION_ERROR    = "COMPILATION_ERROR";
    public static final String RUNTIME_ERROR        = "RUNTIME_ERROR";
    public static final String INTERNAL_ERROR       = "INTERNAL_ERROR";

    private Judge0StatusMapper() {}

    public static String map(int judge0StatusId) {
        return switch (judge0StatusId) {
            case 1, 2         -> RUNNING;
            case 3            -> ACCEPTED;
            case 4            -> WRONG_ANSWER;
            case 5            -> TIME_LIMIT_EXCEEDED;
            case 6            -> COMPILATION_ERROR;
            case 7, 8, 9, 10, 11, 12 -> RUNTIME_ERROR;
            default           -> INTERNAL_ERROR;
        };
    }

    public static boolean isTerminal(int judge0StatusId) {
        return judge0StatusId >= 3;
    }
}
