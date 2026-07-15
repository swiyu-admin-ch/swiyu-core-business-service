package ch.admin.bj.swiyu.core.business.common.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AddressTest {

    @Test
    void getFullAddressOneLine_shouldReturnAllParts_whenAllFieldsArePresent() {
        // Arrange
        var address = Address.builder().street("Main St 1").postalCode("3000").city("Bern").country("CH").build();

        // Act & Assert
        assertEquals("Main St 1, 3000 Bern, CH", address.getFullAddressOneLine());
    }

    @Test
    void getFullAddressOneLine_shouldSkipLeadingComma_whenStreetIsEmpty() {
        // Arrange
        var address = Address.builder().street("").postalCode("3000").city("Bern").country("CH").build();

        // Act & Assert
        assertEquals("3000 Bern, CH", address.getFullAddressOneLine());
    }

    @Test
    void getFullAddressOneLine_shouldSkipLeadingComma_whenStreetIsNull() {
        // Arrange
        var address = Address.builder().postalCode("3000").city("Bern").country("CH").build();

        // Act & Assert
        assertEquals("3000 Bern, CH", address.getFullAddressOneLine());
    }

    @Test
    void getFullAddressOneLine_shouldHandleMissingPostalCode() {
        // Arrange
        var address = Address.builder().street("Main St 1").city("Bern").country("CH").build();

        // Act & Assert
        assertEquals("Main St 1, Bern, CH", address.getFullAddressOneLine());
    }

    @Test
    void getFullAddressOneLine_shouldReturnEmptyString_whenAllFieldsAreNull() {
        // Arrange
        var address = Address.builder().build();

        // Act & Assert
        assertEquals("", address.getFullAddressOneLine());
    }
}
