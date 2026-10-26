import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Build, BuildRef, DashboardApi, Diff, Finding } from './dashboard-api';

const SEVERITIES = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFO'];

@Component({
  selector: 'app-root',
  imports: [DatePipe],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App implements OnInit {
  private readonly api = inject(DashboardApi);

  protected readonly severities = SEVERITIES;
  protected readonly repos = signal<string[]>([]);
  protected readonly repo = signal<string | null>(null);
  protected readonly latest = signal<Build | null>(null);
  protected readonly builds = signal<BuildRef[]>([]);
  protected readonly diff = signal<Diff | null>(null);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.api.repos().subscribe({
      next: (repos) => {
        this.repos.set(repos);
        if (repos.length) this.selectRepo(repos[0]);
      },
      error: () => this.error.set('dashboard-api is not reachable on /api (is it running on :8082?)'),
    });
  }

  selectRepo(repoId: string): void {
    this.repo.set(repoId);
    this.latest.set(null);
    this.diff.set(null);
    this.api.latest(repoId).subscribe((build) => {
      this.latest.set(build);
      this.loadDiff(null);
    });
    this.api.builds(repoId).subscribe((builds) => this.builds.set(builds));
  }

  /** base = null lets the API pick the previous build. */
  loadDiff(base: number | null): void {
    const build = this.latest();
    if (!build) return;
    this.api.diff(build.repoId, build.buildId, base).subscribe((diff) => this.diff.set(diff));
  }

  onBaseChange(value: string): void {
    this.loadDiff(value ? Number(value) : null);
  }

  aiLabel(finding: Finding): string {
    if (!finding.ai) return 'no AI';
    const verdict = finding.ai.verdict === 'likely_tp' ? 'likely TP' : 'likely FP';
    return `AI: ${verdict} ${finding.ai.confidence.toFixed(2)}`;
  }

  aiTooltip(finding: Finding): string {
    if (!finding.ai) return 'triage was off for this scan';
    return [finding.ai.reason, finding.ai.suggestedFix && `Fix: ${finding.ai.suggestedFix}`].filter(Boolean).join('\n');
  }
}
