# AGENTS.md

## Purpose

This file defines the working roles for software development in this repository. It is written for both AI agents and humans so work can be routed consistently, reviewed with the right context, and handed off without guesswork.

Use this file as an operating contract:

- choose one lead role for each task
- consult required resources before changing code or behavior
- involve required reviewers when work crosses boundaries
- surface documentation gaps or conflicts instead of silently ignoring them

## Repo Context

This repository is a multilayer web application with clear ownership seams:

- Frontend: Next.js 15, React 19, TypeScript, Tailwind CSS, DaisyUI in `frontend/`
- Backend: Spring Boot 4, Kotlin, Flyway, PostgreSQL in `backend/`
- Authentication and IAM: Keycloak in `keycloak/`
- Local runtime and service wiring: Docker Compose in the repo root
- Project guidance and planning resources: `docs/`

Important repo areas:

- `frontend/app`, `frontend/components`, `frontend/lib`
- `backend/src/main`, `backend/src/test`, `backend/src/main/resources/db/migration`
- `keycloak/realm`
- `docker-compose.yml`, `.env`, `README.md`
- `docs/`, including `docs/styleguide.md` and `docs/plans/`

## Shared Rules

### Documentation First

`docs/` is a first-class project resource. Agents and humans must inspect relevant files in `docs/` before proposing or implementing changes when documentation exists for the affected area.

Rules:

- treat `docs/` as authoritative project guidance unless the task is explicitly updating documentation
- consult `docs/styleguide.md` before changing UI, styling, typography, color, layout, or other frontend presentation behavior
- consult relevant files in `docs/plans/` before starting cross-cutting or planned feature work
- use `README.md` for stack, setup, and runtime expectations
- if no relevant document exists, proceed using repo evidence and note the missing documentation gap
- if code and docs conflict, do not silently choose one; flag the mismatch and route it to the owning role
- if a task changes intended behavior, the lead role must decide whether related docs also need updating

### Ownership and Handoffs

- every task must have exactly one lead role
- other roles may review or support, but they do not replace lead ownership
- when work crosses a trust boundary, runtime boundary, or major architecture boundary, escalate to the matching role
- do not claim completion without running the validation expected for your role

## Required Resources

Known resources to consult when relevant:

- `docs/styleguide.md` for visual identity, UI constraints, typography, color, and layout guidance
- `README.md` for stack, setup, local runtime, and environment expectations
- `docs/plans/` for feature plans, implementation references, and cross-cutting change context
- `backend/src/main/resources/application.yml` for backend runtime and integration expectations
- `keycloak/realm/heuermannplus-realm.json` for local IAM configuration shape

If a task cites no resources and relevant docs exist, the task intake is incomplete.

## Task Intake

Every task should be framed with:

- goal
- impacted area
- relevant docs or resources consulted
- lead role
- required reviewers
- done criteria

Example intake:

```text
Goal: Adjust registration form spacing and button hierarchy
Impacted area: frontend/app, frontend/components
Relevant resources: docs/styleguide.md
Lead role: Frontend Developer
Required reviewers: QA
Done criteria: UI matches style guidance, existing behavior preserved, lint/typecheck pass
```

## Roles

### Architect

**Mission**

Own system shape, cross-cutting design decisions, major tradeoffs, and boundaries between frontend, backend, auth, and infrastructure.

**Owns**

- cross-cutting technical direction
- API and domain boundaries
- feature decomposition across subsystems
- major design decisions that affect multiple roles

**Required Resources**

- `README.md`
- relevant files in `docs/plans/`
- any impacted docs in `docs/`

**Inputs**

- ambiguous or cross-cutting feature requests
- conflicting implementation proposals
- documentation or code mismatches with system-wide impact

**Outputs**

- implementation direction
- subsystem boundaries
- reviewer requirements
- handoff guidance to the owning implementation role

**Allowed Actions**

- define architecture direction
- decide lead role for cross-cutting work
- specify interfaces, responsibilities, and acceptance criteria

**Must Not**

- bypass Security on trust-boundary changes
- bypass Ops on runtime-impacting changes
- bypass QA on release-risking work

**Validation**

- design is consistent with repo structure and existing plans
- ownership and reviewer expectations are explicit

**Escalate When**

- auth, token flow, or permissions are involved
- infrastructure or deployment behavior changes
- documentation is missing for a high-risk cross-cutting feature

### Frontend Developer

**Mission**

Own user-facing UI, presentation behavior, frontend interactions, and BFF-style frontend API routes.

**Owns**

- `frontend/app`
- `frontend/components`
- frontend styling and interaction behavior
- accessibility and visual consistency
- frontend routes that mediate browser-to-backend interactions

**Required Resources**

- `docs/styleguide.md`
- `README.md`
- relevant feature docs in `docs/` and `docs/plans/`

**Inputs**

- UI features
- layout, typography, color, and spacing changes
- frontend validation or interaction changes
- BFF route adjustments in the frontend app

**Outputs**

- updated UI behavior
- accessible, style-consistent components
- notes on any required backend, QA, or Security follow-up

**Allowed Actions**

- modify frontend pages, components, styles, and frontend route handlers
- align UI work to documented design guidance
- flag documentation drift when the styleguide no longer matches product needs

**Must Not**

- ignore `docs/styleguide.md` for visual changes
- redefine backend contracts without Backend alignment
- change auth-sensitive flows without Security review

**Validation**

- UI changes reference `docs/styleguide.md` when presentation is affected
- lint and typecheck expectations are met
- accessibility and behavior regressions are checked for the touched flow

**Escalate When**

- backend contract changes are needed
- auth/session/token behavior is touched
- styleguide and requested behavior conflict

### Backend Developer

**Mission**

Own application APIs, domain logic, persistence, migrations, and server-side behavior.

**Owns**

- `backend/src/main`
- backend controllers, services, config, and persistence
- Flyway migrations
- backend-side validation and business rules

**Required Resources**

- `README.md`
- relevant docs in `docs/`
- `backend/src/main/resources/application.yml`
- feature plans in `docs/plans/` when present

**Inputs**

- API changes
- data model and business logic work
- persistence and migration tasks
- server-side bug fixes

**Outputs**

- stable backend behavior
- updated endpoints or domain logic
- migration and compatibility notes

**Allowed Actions**

- modify backend application code and tests
- add or adjust migrations
- document backend-facing assumptions for QA, Ops, and Security

**Must Not**

- change frontend behavior assumptions without Frontend coordination
- change auth or trust-boundary logic without Security review
- introduce runtime-sensitive changes without Ops awareness when startup or configuration is affected

**Validation**

- tests cover changed business behavior
- migration impact is reviewed
- docs are consulted when they describe feature intent or process expectations

**Escalate When**

- API changes impact multiple clients or major flows
- security-sensitive endpoints or token handling are touched
- infrastructure or environment assumptions must change

### QA

**Mission**

Own release confidence, regression coverage, acceptance validation, and defect reporting across user-visible and integration-critical flows.

**Owns**

- test strategy and acceptance framing
- regression assessment
- defect reproduction details
- release-readiness verification

**Required Resources**

- relevant docs in `docs/`
- `docs/styleguide.md` for UI acceptance when presentation matters
- `README.md` for expected local stack behavior

**Inputs**

- completed implementation work
- acceptance criteria
- bug reports and release candidates

**Outputs**

- validation results
- defects with reproduction steps
- release signoff or explicit risk notes

**Allowed Actions**

- validate changed flows against requirements and docs
- require clarification when done criteria are incomplete
- flag stale docs when expected behavior and guidance diverge

**Must Not**

- redefine intended behavior without the owning implementation role
- sign off while known critical gaps remain unrecorded

**Validation**

- changed flows are checked against acceptance criteria and relevant docs
- regressions and unresolved risks are clearly stated

**Escalate When**

- expected behavior is unclear or undocumented
- defects cross multiple owned areas
- release risk exceeds the original task scope

### Security

**Mission**

Own trust boundaries, authentication, authorization, secrets hygiene, abuse-case review, and security-sensitive design decisions.

**Owns**

- Keycloak-related auth flow review
- token handling and permission boundaries
- security-sensitive registration and protected endpoint review
- secret and configuration hygiene expectations

**Required Resources**

- `README.md`
- relevant docs in `docs/`
- `backend/src/main/resources/application.yml`
- `keycloak/realm/heuermannplus-realm.json`

**Inputs**

- login and session changes
- token forwarding changes
- registration and verification flow changes
- permission model updates

**Outputs**

- security review findings
- approval or required remediation
- trust-boundary notes for implementers and QA

**Allowed Actions**

- review auth, session, registration, and protected-resource changes
- require explicit handling for secrets, tokens, and role mapping
- flag insecure defaults or undocumented trust assumptions

**Must Not**

- approve risky auth or secret changes without clear validation
- treat undocumented security-sensitive behavior as acceptable by default

**Validation**

- trust boundaries are explicit
- risky flows have reviewer attention
- config and realm assumptions are checked against repo resources

**Escalate When**

- a task changes identity, token, or permission behavior
- external integrations alter the attack surface
- docs and implementation disagree on a security-sensitive flow

### Ops

**Mission**

Own local runtime reliability, environment wiring, container orchestration, operational troubleshooting, and service startup consistency.

**Owns**

- `docker-compose.yml`
- environment wiring through `.env`
- container startup behavior
- local service health and troubleshooting paths

**Required Resources**

- `README.md`
- relevant docs in `docs/`
- compose and environment files in the repo root
- Keycloak and backend runtime configuration when affected

**Inputs**

- startup failures
- local environment issues
- service wiring changes
- runtime configuration and dependency updates

**Outputs**

- reliable local stack behavior
- operational guidance
- runtime change notes for developers and QA

**Allowed Actions**

- modify runtime and orchestration configuration
- define expected local startup and recovery steps
- surface environment-sensitive impacts to QA and Security

**Must Not**

- redefine app behavior outside runtime concerns
- change trust boundaries without Security involvement

**Validation**

- startup flow remains coherent with `README.md`
- runtime impacts and recovery steps are documented when needed

**Escalate When**

- auth infrastructure or secret handling changes
- environment changes affect application contracts or testability
- runtime fixes expose undocumented assumptions that need product or architecture decisions

### Product/Delivery Coordinator

**Mission**

Own task clarity, scope framing, intake quality, and routing to the correct lead role.

**Owns**

- task intake completeness
- scope boundaries
- reviewer assignment
- documentation-gap escalation

**Required Resources**

- `README.md`
- relevant docs in `docs/`
- this `AGENTS.md`

**Inputs**

- incoming work requests
- unclear ownership
- incomplete acceptance criteria

**Outputs**

- a complete task intake
- clear lead role assignment
- required reviewers and done criteria

**Allowed Actions**

- route work to the correct lead role
- require cited docs or explicit note that no relevant docs were found
- flag stale or missing documentation to the owning role

**Must Not**

- override implementation ownership
- approve technical tradeoffs without the owning technical role

**Validation**

- task includes goal, impacted area, resources, lead role, reviewers, and done criteria

**Escalate When**

- ownership is ambiguous
- documentation is missing for a high-risk task
- a request spans multiple major subsystems

## Handoff Matrix

Default routing rules:

- styling, layout, typography, color, and visual consistency work -> Frontend Developer, using `docs/styleguide.md`
- backend API, domain logic, validation, persistence, and migration work -> Backend Developer
- auth, registration hardening, token forwarding, permission, or trust-boundary changes -> Security review required
- compose, startup, environment, service wiring, and local runtime problems -> Ops
- cross-cutting or unclear work -> Architect first
- release confidence, regression signoff, and acceptance validation -> QA

Required handoffs:

- Architect -> implementation role when direction is stable
- Frontend Developer -> QA after self-validation
- Backend Developer -> QA after implementation and test updates
- Frontend Developer or Backend Developer -> Security when auth, secrets, permissions, or trust boundaries are touched
- Frontend Developer or Backend Developer -> Ops when runtime or environment assumptions change
- Ops -> QA when runtime changes affect testability or acceptance flow
- QA -> owning lead role for defects or unresolved risks

## Task Routing Examples

Example: CSS/theme/layout update

- Lead role: Frontend Developer
- Required resources: `docs/styleguide.md`
- Required reviewers: QA

Example: new registration field with backend persistence

- Lead role: Backend Developer
- Required resources: relevant docs in `docs/`, `README.md`
- Required reviewers: QA
- Add Security if validation, identity, or abuse handling changes

Example: Keycloak client, token, or login flow change

- Lead role: Security or Architect, depending on scope
- Required resources: `README.md`, `keycloak/realm/heuermannplus-realm.json`, relevant docs
- Required reviewers: Backend Developer, Frontend Developer, QA, Ops as needed

Example: `docker compose up` fails or local services do not wire together

- Lead role: Ops
- Required resources: `README.md`, compose files, env files
- Required reviewers: QA if testability changes, Security if auth wiring is involved

Example: feature request with related plan in `docs/plans/`

- Lead role: Architect first if the feature is cross-cutting
- Required resources: matching plan in `docs/plans/`
- Handoff: Architect to Frontend Developer or Backend Developer once boundaries are clear

## Acceptance Checks for This File

This `AGENTS.md` is correct if:

- `docs/` is explicitly treated as an authoritative resource area
- every role includes `Required Resources`
- frontend visual work is explicitly bound to `docs/styleguide.md`
- the file explains what to do when code and docs disagree
- common task types in this repo map to one clear lead role

