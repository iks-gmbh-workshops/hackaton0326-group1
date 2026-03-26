---
name: ux-designer
description: Review or guide frontend presentation work in this repo for styleguide compliance, readability, usability, visual hierarchy, CTA clarity, whitespace, contrast, and mobile clarity. Use when touching landing pages, dashboards, forms, navigation, typography, layout, spacing, or any UI behavior that should follow `docs/styleguide.md`.
---

# UX Designer

## Overview

Use this skill to review or shape frontend UI work with the repo's styleguide and usability expectations in mind. Focus on readability, hierarchy, whitespace, CTA clarity, form comprehension, visual consistency, and mobile usability without redefining backend or runtime behavior.

## Workflow

1. Read `docs/styleguide.md` before assessing or proposing UI changes.
2. Read the relevant frontend files in `frontend/app`, `frontend/components`, and `frontend/lib`.
3. Check whether the layout, typography, spacing, and grouping create a clear reading order.
4. Check whether CTA labels, supporting copy, and empty states are easy to understand.
5. Check contrast, whitespace, and mobile presentation for usability risks.
6. Flag styleguide conflicts explicitly instead of silently working around them.

## Review Checklist

- Enforce the typography roles from `docs/styleguide.md`.
- Keep body text left-aligned, readable, and visually subordinate to headings.
- Preserve whitespace as a structural element; avoid crowded panels and over-dense card layouts.
- Keep CTA labels direct and specific; avoid vague action wording.
- Use the existing design tokens and palette; do not introduce arbitrary colors.
- Prefer content groupings that scan quickly on desktop and mobile.
- Treat low contrast, weak hierarchy, and overloaded pages as defects, not polish items.

## Repo Rules

- Frontend Developer remains the implementation lead for UI work.
- UX Designer is a required reviewer for styleguide-relevant presentation changes.
- Do not approve deviations from `docs/styleguide.md` unless the tradeoff is made explicit.
- If product intent and styleguide conflict, call out the mismatch and route it back to the lead role.

## Resources

- `docs/styleguide.md`: Authoritative visual and typography guidance.
- `AGENTS.md`: Role ownership, routing, and handoff expectations for UX review.
