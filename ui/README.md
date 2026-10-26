# ui

Angular 22 page for the SAST dashboard: repo picker, latest build with counts by severity, new vs fixed against a base build, and every finding with its AI triage badge (hover for the reason and suggested fix).

```bash
npm install
npm start          # http://localhost:4200, /api proxied to dashboard-api on :8082 (proxy.conf.json)
npm run build      # dist/ui/browser
```
