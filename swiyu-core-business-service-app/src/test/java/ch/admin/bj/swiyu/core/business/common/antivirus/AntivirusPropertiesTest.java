package ch.admin.bj.swiyu.core.business.common.antivirus;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AntivirusPropertiesTest {

    @Test
    void testBaseUrlShortening() {
        var test = new AntivirusProperties("http://test/scan", "test", "test");

        assertThat(test.getBaseUrl()).isEqualTo("http://test");
    }

    @Test
    void testBaseUrlExtShortening() {
        var test = new AntivirusProperties("http://test/abc/scan", "test", "test");

        assertThat(test.getBaseUrl()).isEqualTo("http://test/abc");
    }

    @Test
    void testBaseUrlNoShortening() {
        var test = new AntivirusProperties("http://test/abc", "test", "test");

        assertThat(test.getBaseUrl()).isEqualTo("http://test/abc");
    }
}
