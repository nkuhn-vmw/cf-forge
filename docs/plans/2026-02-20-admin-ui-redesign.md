# Admin UI Full Redesign — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Redesign the CF Forge admin dashboard to pixel-match the main UI's visual style using pure CSS + Thymeleaf with inline Lucide SVG icons.

**Architecture:** Fix two bugs (missing layout dialect dep, security blocking static assets) then rewrite all CSS and templates. The layout uses a header bar + sidebar + scrollable content area matching the main UI's ProjectDashboard pattern. All design tokens are unified with the main UI's `index.css`.

**Tech Stack:** Thymeleaf Layout Dialect 3.x, pure CSS with custom properties, Chart.js (already present), inline Lucide SVG icons (no build step).

---

### Task 1: Add thymeleaf-layout-dialect dependency

The Thymeleaf templates use `layout:decorate` and `layout:fragment` attributes for template inheritance, but the required `thymeleaf-layout-dialect` library is missing from the classpath. Without it, the layout is silently ignored and pages render without sidebar/CSS.

**Files:**
- Modify: `cf-forge-admin/pom.xml`

**Step 1: Add the dependency**

Add to the `<dependencies>` section of `cf-forge-admin/pom.xml`, after the existing `spring-boot-starter-thymeleaf` dependency:

```xml
        <dependency>
            <groupId>nz.net.ultraq.thymeleaf</groupId>
            <artifactId>thymeleaf-layout-dialect</artifactId>
        </dependency>
```

Note: No `<version>` needed — Spring Boot's dependency management provides it.

**Step 2: Verify the build compiles**

Run: `cd /home/vibe-coder/claude/cf-forge && mvn package -DskipTests -pl cf-forge-admin -am`
Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add cf-forge-admin/pom.xml
git commit -m "fix: add thymeleaf-layout-dialect dependency for template inheritance"
```

---

### Task 2: Permit static resources in Spring Security

The `AdminSecurityConfig` has `anyRequest().denyAll()` which blocks `/css/admin.css` and `/js/charts.js` since they don't match any `permitAll()` rule.

**Files:**
- Modify: `cf-forge-admin/src/main/java/com/cfforge/admin/config/AdminSecurityConfig.java`

**Step 1: Add static resource matchers**

In the `adminFilterChain` method, add a `permitAll()` rule for static resources before the existing `denyAll()`. Change this section:

```java
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/admin/**").authenticated()
                .requestMatchers("/api/v1/admin/**").authenticated()
                .anyRequest().denyAll()
            )
```

To:

```java
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/admin/**").authenticated()
                .requestMatchers("/api/v1/admin/**").authenticated()
                .anyRequest().denyAll()
            )
```

**Step 2: Verify the build compiles**

Run: `cd /home/vibe-coder/claude/cf-forge && mvn package -DskipTests -pl cf-forge-admin -am`
Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add cf-forge-admin/src/main/java/com/cfforge/admin/config/AdminSecurityConfig.java
git commit -m "fix: permit static resources (css/js) through Spring Security"
```

---

### Task 3: Rewrite admin.css

Full CSS rewrite using the exact same design token names as the main UI's `index.css`. The current file uses `--bg`, `--surface-1`, `--text-1` etc. The main UI uses `--bg-primary`, `--bg-secondary`, `--text-primary`. Rename everything for unity. Add the new header bar styles and all component patterns matching the React SPA.

**Files:**
- Rewrite: `cf-forge-admin/src/main/resources/static/css/admin.css`

**Step 1: Rewrite the complete CSS file**

Write the entire file content. Key sections:

1. **CSS reset & design tokens** — `:root` with exact same variable names as main UI `index.css`
2. **Body & global layout** — `display: flex; flex-direction: column;` for header+body-row pattern
3. **Header bar** — 48px, `--bg-secondary`, bottom border, flex row, logo left / user right
4. **Sidebar** — 220px, `--bg-secondary`, right border, nav items with Lucide icon gap
5. **Content area** — `flex: 1`, `overflow-y: auto`, 32px padding
6. **Metric cards** — matching main UI's project card pattern: `--bg-secondary`, `1px solid var(--border)`, `border-radius: 10px`, hover `border-color: var(--accent)`
7. **Health cards** — same card base + status pill with colored bg
8. **Data tables** — `--code-bg` header, mono uppercase, `--accent-glow` row hover
9. **Audit filters & pagination** — filter inputs with accent focus glow
10. **Buttons** — mono font, surface bg, accent variants
11. **Chart cards** — same card pattern
12. **Animations** — `fadeIn`, `pulse`
13. **Responsive** — 768px tablet, 480px mobile breakpoints

Complete CSS (write all ~500 lines):

```css
/* CF Forge Admin — unified with main UI design system */
@import url('https://fonts.googleapis.com/css2?family=IBM+Plex+Mono:wght@400;500;600&family=Outfit:wght@300;400;500;600&display=swap');

:root {
  --bg-primary: #080b12;
  --bg-secondary: #0f1420;
  --bg-tertiary: #161d2e;
  --bg-hover: #1c2538;
  --border: #1e2a3f;
  --text-primary: #e4e8f1;
  --text-secondary: #6b7a94;
  --text-muted: #4a5568;
  --accent: #00d4aa;
  --accent-hover: #00f0c0;
  --accent-glow: rgba(0,212,170,0.15);
  --accent-dim: rgba(0,212,170,0.08);
  --success: #2ed573;
  --warning: #ffa502;
  --danger: #ff4757;
  --info: #58a6ff;
  --code-bg: #0a0e18;
  --font-body: "Outfit", system-ui, -apple-system, "Segoe UI", sans-serif;
  --font-mono: "IBM Plex Mono", "SF Mono", "Cascadia Code", "Fira Code", "Consolas", monospace;
}

/* — Reset — */
*, *::before, *::after { margin: 0; padding: 0; box-sizing: border-box; }

html, body { height: 100%; overflow: hidden; }

body {
  font-family: var(--font-body);
  background: radial-gradient(ellipse at 50% 0%, #0f1a2e 0%, var(--bg-primary) 50%);
  color: var(--text-primary);
  font-size: 14px;
  line-height: 1.5;
  display: flex;
  flex-direction: column;
}

a { color: var(--accent); text-decoration: none; }
a:hover { color: var(--accent-hover); }

/* — Scrollbars — */
::-webkit-scrollbar { width: 6px; height: 6px; }
::-webkit-scrollbar-track { background: var(--bg-secondary); }
::-webkit-scrollbar-thumb { background: var(--bg-tertiary); border-radius: 3px; }
::-webkit-scrollbar-thumb:hover { background: var(--bg-hover); }

/* — Header Bar — */
.header {
  height: 48px;
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  flex-shrink: 0;
}

.header-brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-brand svg { flex-shrink: 0; }

.header-title {
  font-family: var(--font-mono);
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.header-title span { color: var(--accent); }

.header-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 2px 10px;
  border-radius: 20px;
  background: var(--accent-dim);
  color: var(--accent);
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 500;
}

.header-badge::before {
  content: '';
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--accent);
  animation: pulse 2s ease-in-out infinite;
}

.header-user {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text-secondary);
}

.header-user svg { color: var(--text-muted); }

/* — App Layout (sidebar + content) — */
.app-body {
  display: flex;
  flex: 1;
  overflow: hidden;
}

/* — Sidebar — */
.sidebar {
  width: 220px;
  background: var(--bg-secondary);
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  overflow-y: auto;
}

.sidebar-nav { list-style: none; padding: 8px 0; flex: 1; }

.sidebar-nav li a {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  color: var(--text-secondary);
  text-decoration: none;
  font-size: 13px;
  font-weight: 400;
  transition: all 0.2s ease;
  border-left: 3px solid transparent;
}

.sidebar-nav li a svg {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
  opacity: 0.7;
}

.sidebar-nav li a:hover {
  background: var(--accent-dim);
  color: var(--text-primary);
}

.sidebar-nav li a:hover svg { opacity: 1; }

.sidebar-nav li a.active {
  background: var(--accent-glow);
  color: var(--accent);
  border-left-color: var(--accent);
  font-weight: 500;
}

.sidebar-nav li a.active svg { opacity: 1; color: var(--accent); }

/* — Content — */
.content {
  flex: 1;
  padding: 32px;
  overflow-y: auto;
}

.content h1 {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 24px;
}

.content h2 {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 28px 0 16px;
}

/* — Metric Cards — */
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}

.metric-card {
  background: var(--bg-secondary);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 20px;
  transition: border-color 0.2s ease;
  animation: fadeIn 0.3s ease forwards;
  position: relative;
}

.metric-card:hover { border-color: var(--accent); }

.metric-card .metric-icon {
  position: absolute;
  top: 16px;
  right: 16px;
  color: var(--text-muted);
  opacity: 0.5;
}

.metric-label {
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 500;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 8px;
}

.metric-card h3 {
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 500;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin: 0 0 8px;
}

.metric-value {
  font-family: var(--font-mono);
  font-size: 28px;
  font-weight: 600;
  color: var(--accent);
  line-height: 1.2;
}

.metric-value.danger { color: var(--danger); }
.metric-value.success { color: var(--success); }
.metric-value.warning { color: var(--warning); }

/* — Health Grid — */
.health-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
}

.health-card {
  background: var(--bg-secondary);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 16px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: border-color 0.2s ease;
  animation: fadeIn 0.3s ease forwards;
}

.health-card:hover { border-color: var(--accent); }

.component-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  font-size: 14px;
}

.component-name svg { color: var(--text-muted); width: 16px; height: 16px; }

.health-status {
  padding: 4px 12px;
  border-radius: 20px;
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.status-UP, .status-up { background: rgba(46,213,115,0.15); color: var(--success); }
.status-DOWN, .status-down { background: rgba(255,71,87,0.15); color: var(--danger); }
.status-DEGRADED { background: rgba(255,165,2,0.15); color: var(--warning); }
.status-UNKNOWN { background: var(--bg-tertiary); color: var(--text-secondary); }

/* — Data Table — */
.data-table {
  width: 100%;
  border-collapse: collapse;
  background: var(--bg-secondary);
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid var(--border);
}

.data-table th {
  background: var(--code-bg);
  color: var(--text-secondary);
  padding: 12px 16px;
  text-align: left;
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.data-table td {
  padding: 10px 16px;
  border-top: 1px solid var(--border);
  font-size: 13px;
}

.data-table tbody tr:hover { background: var(--accent-dim); }

.empty-row { text-align: center; color: var(--text-muted); padding: 24px 16px; }

/* — Audit Log — */
.audit-filters {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  flex-wrap: wrap;
  align-items: center;
}

.filter-input {
  background: var(--bg-secondary);
  border: 1px solid var(--border);
  color: var(--text-primary);
  padding: 8px 14px;
  border-radius: 8px;
  font-family: var(--font-mono);
  font-size: 13px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.filter-input:focus {
  outline: none;
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-glow);
}

.filter-input::placeholder { color: var(--text-muted); }

.audit-summary {
  font-family: var(--font-mono);
  color: var(--text-secondary);
  margin-bottom: 12px;
  font-size: 13px;
}

.audit-table {
  width: 100%;
  border-collapse: collapse;
  background: var(--bg-secondary);
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid var(--border);
}

.audit-table th {
  background: var(--code-bg);
  color: var(--text-secondary);
  padding: 12px 16px;
  text-align: left;
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.audit-table td {
  padding: 10px 16px;
  border-top: 1px solid var(--border);
  font-size: 13px;
}

.audit-table tbody tr:hover { background: var(--accent-dim); }

.ts { white-space: nowrap; color: var(--text-secondary); font-family: var(--font-mono); font-size: 12px; }
.mono { font-family: var(--font-mono); font-size: 13px; color: var(--accent); }

.action-badge {
  display: inline-block;
  background: var(--accent-dim);
  color: var(--accent);
  padding: 2px 10px;
  border-radius: 20px;
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 500;
}

.details {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--text-muted);
  font-size: 13px;
}

.pagination {
  margin-top: 16px;
  display: flex;
  gap: 8px;
  align-items: center;
  color: var(--text-secondary);
  font-family: var(--font-mono);
  font-size: 13px;
}

.export-group { display: flex; gap: 6px; margin-left: auto; }

/* — Buttons — */
.btn {
  padding: 8px 16px;
  border: 1px solid var(--border);
  border-radius: 8px;
  cursor: pointer;
  font-family: var(--font-mono);
  font-size: 13px;
  font-weight: 500;
  background: var(--bg-tertiary);
  color: var(--text-primary);
  transition: all 0.2s ease;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.btn:hover { border-color: var(--accent); background: var(--accent-dim); }

.btn-primary {
  background: var(--accent);
  color: var(--bg-primary);
  border-color: var(--accent);
}

.btn-primary:hover { background: var(--accent-hover); border-color: var(--accent-hover); }

.btn-secondary { background: var(--bg-tertiary); color: var(--text-primary); }
.btn-secondary:hover { background: var(--bg-hover); border-color: var(--accent); }

.btn-sm { padding: 4px 10px; font-size: 12px; }

/* — Chart Cards — */
.chart-card {
  background: var(--bg-secondary);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 20px;
  margin-top: 16px;
}

.chart-card h3 {
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 500;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin: 0 0 16px;
}

/* — Empty State — */
.empty-state {
  text-align: center;
  padding: 48px 24px;
  color: var(--text-muted);
}

.empty-state svg { margin-bottom: 12px; opacity: 0.3; }
.empty-state p { font-size: 13px; }

/* — Animations — */
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

/* — Responsive — */
@media (max-width: 768px) {
  .sidebar { width: 180px; }
  .content { padding: 20px; }
  .metrics-grid { grid-template-columns: 1fr 1fr; }
  .health-grid { grid-template-columns: 1fr; }
  .header { padding: 0 12px; }
}

@media (max-width: 480px) {
  .app-body { flex-direction: column; }
  .sidebar { width: 100%; border-right: none; border-bottom: 1px solid var(--border); }
  .sidebar-nav { display: flex; overflow-x: auto; padding: 0; }
  .sidebar-nav li a {
    padding: 10px 14px;
    border-left: none;
    border-bottom: 3px solid transparent;
    white-space: nowrap;
  }
  .sidebar-nav li a.active { border-left-color: transparent; border-bottom-color: var(--accent); }
  .metrics-grid { grid-template-columns: 1fr; }
}
```

**Step 2: Commit**

```bash
git add cf-forge-admin/src/main/resources/static/css/admin.css
git commit -m "feat: rewrite admin CSS to match main UI design system"
```

---

### Task 4: Rewrite layout.html with header bar + sidebar

Replace the existing layout with a new structure: `header bar` at top, then `sidebar + content` below. Add inline Lucide SVG icons for each nav item. The layout uses Thymeleaf Layout Dialect's `layout:fragment="content"` for page content injection.

**Files:**
- Rewrite: `cf-forge-admin/src/main/resources/templates/admin/layout.html`

**Step 1: Write the new layout**

Key structural changes from old layout:
- Old: `<body> <nav.sidebar> <main.content>` (no header)
- New: `<body> <header.header> <div.app-body> <nav.sidebar> <main.content>`
- Header has: Code2 SVG + "CF Forge" title + "Admin" badge + authenticated user display
- Sidebar nav items each get a Lucide SVG icon inline
- Use `sec:authorize` from Spring Security Thymeleaf extras for user info (if available), otherwise keep static

SVG icons to embed (16x16, stroke-width="2", stroke="currentColor", fill="none"):
- LayoutDashboard: Overview
- Users: Users
- Bot: AI Agent
- Rocket: Deployments
- Hammer: Builds
- Sparkles: GenAI Usage
- HeartPulse: Health
- ScrollText: Audit Log

The layout should include inline `<svg>` elements copied from the Lucide icon set (the feather icon format: 24x24 viewBox, no fill, stroke currentColor).

**Step 2: Commit**

```bash
git add cf-forge-admin/src/main/resources/templates/admin/layout.html
git commit -m "feat: rewrite admin layout with header bar and icon sidebar"
```

---

### Task 5: Rewrite all 8 page templates

Rewrite each page template with the updated CSS class names and improved markup. Key changes across all pages:
- `metric-label` class on label divs (not `h3` inside metric-card)
- Add `metric-icon` SVG inside metric cards
- Use `empty-state` class for empty data
- Chart cards keep the same `chart-card` class and canvas elements
- Tables use `data-table` class with improved structure

**Files:**
- Rewrite: `cf-forge-admin/src/main/resources/templates/admin/overview.html`
- Rewrite: `cf-forge-admin/src/main/resources/templates/admin/users.html`
- Rewrite: `cf-forge-admin/src/main/resources/templates/admin/agent.html`
- Rewrite: `cf-forge-admin/src/main/resources/templates/admin/deployments.html`
- Rewrite: `cf-forge-admin/src/main/resources/templates/admin/builds.html`
- Rewrite: `cf-forge-admin/src/main/resources/templates/admin/genai.html`
- Rewrite: `cf-forge-admin/src/main/resources/templates/admin/health.html`
- Rewrite: `cf-forge-admin/src/main/resources/templates/admin/audit.html`

**Step 1: Rewrite each template**

All templates follow this pattern:
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{admin/layout}">
<head><title>PageName - CF Forge Admin</title></head>
<body>
<div layout:fragment="content">
    <!-- page-specific content -->
</div>
</body>
</html>
```

Each metric card in templates should include a relevant inline SVG icon:
```html
<div class="metric-card">
    <svg class="metric-icon" ...><!-- icon paths --></svg>
    <div class="metric-label">Label Text</div>
    <div class="metric-value" th:text="${value}">0</div>
</div>
```

Key template-specific notes:
- **overview.html**: 2 metric cards (Active Users, Audit Events) + health grid. Add Users and FileText SVGs as metric icons.
- **users.html**: 2 metric cards (Total Users, Active 7d) + data-table with top users. Add empty-state div when no users.
- **agent.html**: 4 metric cards (Prompts, Refine, Avg Response, Total) + chart canvas. Script block for chart data.
- **deployments.html**: 4 metric cards (Total, Success Rate, Rollbacks, Failed) + 2 chart canvases. Script block for data.
- **builds.html**: 4 metric cards (Total, Success Rate, Avg Duration, Failed) + chart canvas. The avgDurationMs null check already exists and should be preserved.
- **genai.html**: 3 metric cards (Volume, Avg Latency, P95 Latency) + chart canvas.
- **health.html**: health-grid only with health cards. Empty state when no data.
- **audit.html**: Filters bar + table + pagination + inline JavaScript. Preserve the existing JS logic for API calls and dynamic table rendering.

**Step 2: Commit**

```bash
git add cf-forge-admin/src/main/resources/templates/admin/
git commit -m "feat: rewrite all admin page templates with new design"
```

---

### Task 6: Update charts.js to use unified token names

The charts.js already uses the correct hex colors, but update the variable names for consistency and ensure all Chart.js defaults match the new design tokens.

**Files:**
- Modify: `cf-forge-admin/src/main/resources/static/js/charts.js`

**Step 1: Update the color references**

No functional changes needed — the hex colors are already correct. Just update the comment at the top and ensure `Chart.defaults.borderColor` and `Chart.defaults.color` reference the correct colors. The existing file is already well-structured.

**Step 2: Commit**

```bash
git add cf-forge-admin/src/main/resources/static/js/charts.js
git commit -m "chore: update charts.js comments for design system consistency"
```

---

### Task 7: Build, deploy, and verify

**Step 1: Run the existing tests**

Run: `cd /home/vibe-coder/claude/cf-forge && mvn test -pl cf-forge-admin -am`
Expected: All 26 tests pass (10 controller + 16 service)

**Step 2: Build the JAR**

Run: `cd /home/vibe-coder/claude/cf-forge && mvn package -DskipTests -pl cf-forge-admin -am`
Expected: BUILD SUCCESS

**Step 3: Deploy to CF**

Run: `cd /home/vibe-coder/claude/cf-forge/cf-forge-admin && cf push`
Expected: App starts, health check passes

**Step 4: Browser test**

Use Playwright to:
1. Navigate to `https://cf-forge-admin.apps.tas-ndc.kuhn-labs.com/admin/` (with `ignoreHTTPSErrors: true`)
2. Log in via UAA (runner1 / scale12clearz)
3. Verify: dark background renders, sidebar visible with icons, metric cards styled with accent color
4. Take screenshots of overview, health, and audit pages

**Step 5: Final commit**

```bash
git add -A
git commit -m "feat: complete admin UI redesign matching main UI style"
```
