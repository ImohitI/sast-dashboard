package dev.sast.dashboard;

import dev.sast.dashboard.Views.Ai;
import dev.sast.dashboard.Views.BuildRef;
import dev.sast.dashboard.Views.BuildView;
import dev.sast.dashboard.Views.FindingView;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Read-only queries against the consumer's schema (user dashboard_ro). Every query is served by a primary key:
 * scan_runs (repo_id, build_id) and findings (repo_id, build_id, fingerprint).
 */
@Repository
public class ScanRepository {

    private static final List<String> SEVERITIES = List.of("CRITICAL", "HIGH", "MEDIUM", "LOW", "INFO");

    private final JdbcClient jdbc;

    public ScanRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<String> repos() {
        return jdbc.sql("SELECT DISTINCT repo_id FROM scan_runs ORDER BY repo_id").query(String.class).list();
    }

    public List<BuildRef> builds(String repoId, int limit) {
        return jdbc.sql("""
                        SELECT build_id, commit_sha, scanned_at, finding_count FROM scan_runs
                        WHERE repo_id = ? ORDER BY build_id DESC LIMIT ?
                        """)
                .params(repoId, limit)
                .query((rs, n) -> new BuildRef(rs.getLong("build_id"), rs.getString("commit_sha"),
                        rs.getTimestamp("scanned_at").toInstant(), rs.getInt("finding_count")))
                .list();
    }

    /** Backward scan of the primary key index: newest build first, stop after one row. */
    public Optional<Long> latestBuildId(String repoId) {
        return jdbc.sql("SELECT build_id FROM scan_runs WHERE repo_id = ? ORDER BY build_id DESC LIMIT 1")
                .param(repoId).query(Long.class).optional();
    }

    public Optional<Long> previousBuildId(String repoId, long buildId) {
        return jdbc.sql("SELECT build_id FROM scan_runs WHERE repo_id = ? AND build_id < ? ORDER BY build_id DESC LIMIT 1")
                .params(repoId, buildId).query(Long.class).optional();
    }

    public Optional<BuildView> build(String repoId, long buildId) {
        return jdbc.sql("SELECT build_id, commit_sha, scanned_at, finding_count FROM scan_runs WHERE repo_id = ? AND build_id = ?")
                .params(repoId, buildId)
                .query((rs, n) -> new BuildRef(rs.getLong("build_id"), rs.getString("commit_sha"),
                        rs.getTimestamp("scanned_at").toInstant(), rs.getInt("finding_count")))
                .optional()
                .map(run -> {
                    List<FindingView> findings = findings(repoId, buildId);
                    return new BuildView(repoId, buildId, run.commitSha(), run.scannedAt(), run.findingCount(),
                            countBySeverity(findings), findings);
                });
    }

    public List<FindingView> findings(String repoId, long buildId) {
        return jdbc.sql("""
                        SELECT fingerprint, cwe, rule_id, found_by, severity, file_path, line, message,
                               ai_verdict, ai_confidence, ai_reason, ai_suggested_fix
                        FROM findings WHERE repo_id = ? AND build_id = ?
                        ORDER BY file_path, line
                        """)
                .params(repoId, buildId)
                .query((rs, n) -> toFinding(rs))
                .list();
    }

    private static FindingView toFinding(ResultSet rs) throws SQLException {
        String verdict = rs.getString("ai_verdict");
        Ai ai = verdict == null ? null : new Ai(verdict, rs.getObject("ai_confidence", Double.class),
                rs.getString("ai_reason"), rs.getString("ai_suggested_fix"));
        Array foundBy = rs.getArray("found_by");
        return new FindingView(rs.getString("fingerprint"), rs.getString("cwe"), rs.getString("rule_id"),
                Arrays.asList((String[]) foundBy.getArray()), rs.getString("severity"), rs.getString("file_path"),
                rs.getInt("line"), rs.getString("message"), ai);
    }

    private static Map<String, Integer> countBySeverity(List<FindingView> findings) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        SEVERITIES.forEach(s -> counts.put(s, 0));
        findings.forEach(f -> counts.merge(f.severity(), 1, Integer::sum));
        return counts;
    }
}
