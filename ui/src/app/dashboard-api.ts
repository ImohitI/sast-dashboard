import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

// Mirrors dev.sast.dashboard.Views in dashboard-api.
export interface Ai {
  verdict: 'likely_tp' | 'likely_fp';
  confidence: number;
  reason: string | null;
  suggestedFix: string | null;
}

export interface Finding {
  fingerprint: string;
  cwe: string | null;
  ruleId: string;
  foundBy: string[];
  severity: string;
  file: string;
  line: number;
  message: string | null;
  ai: Ai | null;
}

export interface BuildRef {
  buildId: number;
  commitSha: string | null;
  scannedAt: string;
  findingCount: number;
}

export interface Build extends BuildRef {
  repoId: string;
  countsBySeverity: Record<string, number>;
  findings: Finding[];
}

export interface Diff {
  repoId: string;
  buildId: number;
  baseBuildId: number | null;
  newFindings: Finding[];
  fixedFindings: Finding[];
}

/** All calls go to /api, proxied to dashboard-api (:8082) by proxy.conf.json during `ng serve`. */
@Injectable({ providedIn: 'root' })
export class DashboardApi {
  private readonly http = inject(HttpClient);

  repos(): Observable<string[]> {
    return this.http.get<string[]>('/api/repos');
  }

  latest(repoId: string): Observable<Build> {
    return this.http.get<Build>(`/api/repos/${encodeURIComponent(repoId)}/latest`);
  }

  builds(repoId: string): Observable<BuildRef[]> {
    return this.http.get<BuildRef[]>(`/api/repos/${encodeURIComponent(repoId)}/builds`);
  }

  diff(repoId: string, buildId: number, base: number | null): Observable<Diff> {
    const query = base == null ? '' : `?base=${base}`;
    return this.http.get<Diff>(`/api/repos/${encodeURIComponent(repoId)}/builds/${buildId}/diff${query}`);
  }
}
