package dev.sast.dashboard;

import dev.sast.dashboard.Views.FindingView;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BuildDiffTest {

    private static FindingView finding(String fingerprint, int line) {
        return new FindingView(fingerprint, "CWE-89", "r", List.of("opengrep"), "HIGH", "app.py", line, "m", null);
    }

    @Test
    void newIsOnlyInCurrentAndFixedIsOnlyInBase() {
        var kept = finding("k", 10);
        var removed = finding("r", 20);
        var added = finding("a", 30);

        assertThat(BuildDiff.newFindings(List.of(kept, removed), List.of(kept, added))).containsExactly(added);
        assertThat(BuildDiff.fixedFindings(List.of(kept, removed), List.of(kept, added))).containsExactly(removed);
    }

    @Test
    void firstBuildHasEverythingNew() {
        var only = finding("x", 1);
        assertThat(BuildDiff.newFindings(List.of(), List.of(only))).containsExactly(only);
        assertThat(BuildDiff.fixedFindings(List.of(), List.of(only))).isEmpty();
    }
}
