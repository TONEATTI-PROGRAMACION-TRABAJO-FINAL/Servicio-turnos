package com.toneatti.turnos.reservation.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhoneNumberTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "+5492615551234      | +5492615551234",
            "+54 9 261 555 1234  | +5492615551234",
            "+54 9 (261) 555-1234| +5492615551234",
            "261.555.1234        | 2615551234",
            "(261) 555-12-34     | 2615551234",
            "' 2615551234 '      | 2615551234",
            "'261\t555\t1234'    | 2615551234"
    })
    void removesSpacesDashesParenthesesAndDots(String raw, String expected) {
        assertThat(PhoneNumber.parse(raw)).map(PhoneNumber::value).contains(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234567", "+1234567", "123456789012345", "+123456789012345"})
    void acceptsFrom7To15Digits(String raw) {
        assertThat(PhoneNumber.parse(raw)).map(PhoneNumber::value).contains(raw);
    }

    @ParameterizedTest
    @ValueSource(strings = {"123456", "+123456", "1234567890123456", "+1234567890123456"})
    void rejectsFewerThan7OrMoreThan15Digits(String raw) {
        assertThat(PhoneNumber.parse(raw)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"54+92615551234", "++5492615551234", "261555123a", "261/555/1234", "261_555_1234",
            "+", "-- () ..", "２６１５５５１２３４"})
    void rejectsAPlusOutOfPlaceAndAnyOtherCharacter(String raw) {
        assertThat(PhoneNumber.parse(raw)).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void rejectsNothing(String raw) {
        assertThat(PhoneNumber.parse(raw)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"261 555 1234", "123"})
    void theConstructorOnlyTakesANormalizedValue(String value) {
        assertThatThrownBy(() -> new PhoneNumber(value)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void doesNotExposeTheNumberInItsText() {
        assertThat(new PhoneNumber("+5492615551234").toString()).doesNotContain("5551234");
    }
}
