package com.web.eventsrus.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PhoneNumbersTest {

    @Test
    void acceptsNormalPhilippineMobileFormats() {
        assertThat(PhoneNumbers.isPhilippineMobile("09209099332")).isTrue();
        assertThat(PhoneNumbers.isPhilippineMobile("0920 909 9332")).isTrue();
        assertThat(PhoneNumbers.isPhilippineMobile("+63 917 123 4567")).isTrue();
        assertThat(PhoneNumbers.isPhilippineMobile("+639175040348")).isTrue();
        assertThat(PhoneNumbers.isPhilippineMobile("63-917-123-4567")).isTrue();
        assertThat(PhoneNumbers.isPhilippineMobile("(0917) 123 4567")).isTrue();
    }

    @Test
    void rejectsMadeUpForeignAndIncompleteNumbers() {
        assertThat(PhoneNumbers.isPhilippineMobile("5551234567")).isFalse();
        assertThat(PhoneNumbers.isPhilippineMobile("+1 555 123 4567")).isFalse();
        assertThat(PhoneNumbers.isPhilippineMobile("0917123456")).isFalse();
        assertThat(PhoneNumbers.isPhilippineMobile("091712345678")).isFalse();
        assertThat(PhoneNumbers.isPhilippineMobile("0287654321")).isFalse();
        assertThat(PhoneNumbers.isPhilippineMobile("")).isFalse();
        assertThat(PhoneNumbers.isPhilippineMobile(null)).isFalse();
    }
}
