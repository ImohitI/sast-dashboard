package dev.sast.dashboard;

import dev.sast.dashboard.Views.BuildRef;
import dev.sast.dashboard.Views.BuildView;
import dev.sast.dashboard.Views.DiffView;
import dev.sast.dashboard.Views.FindingView;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/repos")
public class DashboardController {

    private final ScanRepository repository;
    private final LatestBuildService latest;

    public DashboardController(ScanRepository repository, LatestBuildService latest) {
        this.repository = repository;
        this.latest = latest;
    }

    @GetMapping
    public List<String> repos() {
        return repository.repos();
    }

    @GetMapping("/{repoId}/latest")
    public ResponseEntity<BuildView> latest(@PathVariable String repoId) {
        return ResponseEntity.of(latest.latest(repoId));
    }

    @GetMapping("/{repoId}/builds")
    public List<BuildRef> builds(@PathVariable String repoId, @RequestParam(defaultValue = "20") int limit) {
        return repository.builds(repoId, Math.min(limit, 100));
    }

    /** base defaults to the build just before buildId. */
    @GetMapping("/{repoId}/builds/{buildId}/diff")
    public ResponseEntity<DiffView> diff(@PathVariable String repoId, @PathVariable long buildId,
                                         @RequestParam(required = false) Long base) {
        if (repository.build(repoId, buildId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Long baseId = base != null ? base : repository.previousBuildId(repoId, buildId).orElse(null);
        List<FindingView> current = repository.findings(repoId, buildId);
        List<FindingView> previous = baseId == null ? List.of() : repository.findings(repoId, baseId);
        return ResponseEntity.ok(new DiffView(repoId, buildId, baseId,
                BuildDiff.newFindings(previous, current), BuildDiff.fixedFindings(previous, current)));
    }
}
