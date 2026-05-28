package com.codejudgex.evaluation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OutputComparatorTest {

    private final OutputComparator comparator = new OutputComparator();

    @Test
    void exactMatch_returnsTrue() {
        assertThat(comparator.isCorrect("hello\n", "hello\n")).isTrue();
    }

    @Test
    void trailingWhitespaceDifference_returnsTrue() {
        assertThat(comparator.isCorrect("hello   \n", "hello\n")).isTrue();
    }

    @Test
    void crlfVsLf_returnsTrue() {
        assertThat(comparator.isCorrect("hello\r\nworld\r\n", "hello\nworld\n")).isTrue();
    }

    @Test
    void differentContent_returnsFalse() {
        assertThat(comparator.isCorrect("hello", "world")).isFalse();
    }

    @Test
    void nullActual_returnsFalse() {
        assertThat(comparator.isCorrect(null, "hello")).isFalse();
    }

    @Test
    void bothNull_returnsTrue() {
        assertThat(comparator.isCorrect(null, null)).isTrue();
    }

    @Test
    void multiline_exactMatch_returnsTrue() {
        assertThat(comparator.isCorrect("1\n2\n3\n", "1\n2\n3\n")).isTrue();
    }

    @Test
    void multiline_contentDiffers_returnsFalse() {
        assertThat(comparator.isCorrect("1\n2\n4\n", "1\n2\n3\n")).isFalse();
    }
}
