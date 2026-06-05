package com.example.faketcp.iso;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IsoLogUtilsTest {
    @Test
    void masksSensitiveIsoFields() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("2", "5061231451417639567");
        fields.put("4", "000000010000");
        fields.put("52", "37CA95B59D63B007");

        String summary = IsoLogUtils.fieldsSummary(fields);

        assertThat(summary).contains("DE2=506123...9567");
        assertThat(summary).contains("DE4=000000010000");
        assertThat(summary).contains("DE52=<present len=16>");
        assertThat(summary).doesNotContain("145141763");
        assertThat(summary).doesNotContain("37CA95B59D63B007");
    }
}
