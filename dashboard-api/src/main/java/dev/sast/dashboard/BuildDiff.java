package dev.sast.dashboard;

import dev.sast.dashboard.Views.FindingView;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * new   = in this build, not in the base build
 * fixed = in the base build, not in this build
 * Matched by fingerprint = sha256(cwe|file|line). Insert a line above a bug and its fingerprint changes, so it shows
 * up once as fixed and once as new. That is the known limit of a line-based fingerprint.
 */
public final class BuildDiff {

    private BuildDiff() {
    }

    public static List<FindingView> newFindings(List<FindingView> base, List<FindingView> current) {
        return notIn(current, fingerprints(base));
    }

    public static List<FindingView> fixedFindings(List<FindingView> base, List<FindingView> current) {
        return notIn(base, fingerprints(current));
    }

    private static Set<String> fingerprints(List<FindingView> findings) {
        return findings.stream().map(FindingView::fingerprint).collect(Collectors.toSet());
    }

    private static List<FindingView> notIn(List<FindingView> findings, Set<String> other) {
        return findings.stream().filter(f -> !other.contains(f.fingerprint())).toList();
    }
}
