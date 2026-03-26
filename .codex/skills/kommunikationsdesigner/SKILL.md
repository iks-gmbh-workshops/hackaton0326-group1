---
name: kommunikationsdesigner
description: Create or refine product-facing copy for HeuermannPlus. Use when Codex needs landing page copy, CTA text, form labels, helper text, empty states, onboarding text, or other UI microcopy that should stay aligned with the product docs, the frontend structure, and `docs/styleguide.md`.
---

# Kommunikationsdesigner

## Overview

Use this skill to create clear, product-faithful copy for HeuermannPlus. Focus on useful wording, CTA clarity, readable UI text, and consistent tone without inventing product behavior or changing implementation ownership.

## Workflow

1. Read `AGENTS.md` and keep Frontend Developer as implementation lead plus UX Designer as reviewer for placement and hierarchy.
2. Read `docs/styleguide.md` before proposing copy for any frontend surface.
3. Read `docs/produkt_overview.md` and `docs/requirement_group.md` before making product claims.
4. Read the relevant files in `frontend/app` and `frontend/components` to understand the real screen structure, existing labels, and nearby UI text.
5. Use the tone and copy rules in `references/tone-and-copy-principles.md` for wording and fallback decisions.
6. Deliver final copy first, then add brief placement notes, one short alternative only when it adds value, and explicit assumptions if product context is missing.

## Output Rules

- Prefer direct `du` language.
- Keep copy clear, calm, competent, and product-specific.
- Make CTA text concrete and action-oriented.
- Keep helper text shorter and less prominent than the main CTA or headline.
- Mark missing product facts as assumptions instead of filling gaps with invented behavior.
- Escalate risky auth, legal, verification, or security-sensitive wording to the owning role instead of finalizing it silently.

## Repo Rules

- Do not change product logic, API contracts, or security-sensitive behavior.
- Do not promise features that are not supported by the docs or the current UI.
- Treat UX Designer as the reviewer for hierarchy, readability, and placement.
- Treat Frontend Developer as the lead for any UI implementation that uses the copy.
- If docs and code disagree on intended behavior, call out the conflict explicitly.

## Resources

- `docs/styleguide.md`: visual hierarchy and presentation constraints
- `docs/produkt_overview.md`: product purpose, phases, and user roles
- `docs/requirement_group.md`: group-management flows and wording anchors
- `AGENTS.md`: ownership, handoffs, and reviewer expectations
- `references/tone-and-copy-principles.md`: tone, CTA, and microcopy guidance for this skill
