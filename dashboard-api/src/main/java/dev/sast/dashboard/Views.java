package dev.sast.dashboard;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Response shapes of dashboard-api. Read models, built for the UI; they are not the Kafka contract. */
public final class Views {

    private Views() {
    }

    public record Ai(String verdict, Double confidence, String reason, String suggestedFix) {
    }

    public record FindingView(String fingerprint, String cwe, String ruleId, List<String> foundBy, String severity,
                              String file, int line, String message, Ai ai) {
    }

    public record BuildRef(long buildId, String commitSha, Instant scannedAt, int findingCount) {
    }

    public record BuildView(String repoId, long buildId, String commitSha, Instant scannedAt, int findingCount,
                            Map<String, Integer> countsBySeverity, List<FindingView> findings) {
    }

    /** baseBuildId is null for the first build of a repo: then every finding is new. */
    public record DiffView(String repoId, long buildId, Long baseBuildId,
                           List<FindingView> newFindings, List<FindingView> fixedFindings) {
    }
}
