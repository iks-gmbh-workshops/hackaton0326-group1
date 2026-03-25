# 📘 User Frontend Application – Design Reference (Extracted from Styleguide)

## 1. Purpose & Design Principles

* Ensure **consistent visual identity** across all applications (internal & external) 
* Design goals:

  * Clarity
  * Reliability
  * Consistency
  * Flexibility for future extensions 

### Key UI Principles

* Strong **visual hierarchy**
* High **readability**
* Clear **content structuring**
* Use of **contrast (color + typography)**
* Maintain **whitespace for clarity** 

---

## 2. Typography System

### 2.1 Font Families

| Role      | Font                                 | Usage              |
| --------- | ------------------------------------ | ------------------ |
| Primary   | Segoe UI                             | Body text, UI text |
| Secondary | Merriweather (Italic / Light Italic) | Headlines only     |

* **Roboto** allowed as fallback for web applications 

### 2.2 Rules

* ❌ Do NOT use Merriweather for body text
* ✅ Use Segoe UI for all readable content
* ✅ Headlines must use Merriweather Italic variants 

---

### 2.3 Typography Hierarchy (Design Tokens)

```yaml
typography:
  headline:
    font: Merriweather Light Italic
    color: "#005578"
    size_ratio: 100%
    example_size: 24pt

  subheadline:
    font: Segoe UI Regular
    color: "#575756"
    size_ratio: 60%
    example_size: 14pt

  section_title:
    font: Segoe UI Semibold
    color: "#005578"
    size_ratio: 50%
    example_size: 12pt

  subsection_title:
    font: Segoe UI Semibold
    color: "#575756"
    size_ratio: 40%
    example_size: 9pt

  body:
    font: Segoe UI Light
    color: "#575756"
    size_ratio: 40%
    example_size: 9pt

  caption:
    font: Segoe UI Semibold
    color: "#575756"
    size_ratio: 30%
    example_size: 7.5pt
```



---

### 2.4 Layout & Text Behavior

* Default alignment: **left-aligned (ragged right)**
* Avoid:

  * excessive justified text (blocksatz)
  * centered body text (reduces readability) 

---

## 3. Color System

### 3.1 General Rules

* Use only **defined primary + secondary colors**
* Ensure **sufficient contrast** at all times
* Avoid arbitrary colors outside palette 

---

### 3.2 Color Usage

* **Primary colors**:

  * Used for:

    * logos
    * important UI elements
    * headings

* **Secondary colors**:

  * Used for:

    * backgrounds
    * decorative elements
    * contrast accents 

---

### 3.3 UI Constraints

```yaml
color_rules:
  allowed_colors: [primary_palette, secondary_palette]
  text_default: "#575756" (80% black)
  contrast_required: true
  forbidden:
    - colors outside palette
    - low contrast combinations
```

---

## 4. Layout & Composition

### 4.1 Grid System

* Use **column-based layout**
* Maintain consistent **spacing (gutter / margins)**
* Possible:

  * asymmetric layouts
  * unused columns for emphasis 

---

### 4.2 Whitespace

* Mandatory for:

  * readability
  * visual structure
* Treat whitespace as **active design element** 

---

### 4.3 Structural Elements

UI should clearly separate:

* Logo
* Headline
* Content blocks
* Footer
* Graphics 

---

## 5. Logo System

### 5.1 Concept

* Logo represents:

  * **“iceberg model” of software development**
  * visible vs hidden complexity 

---

### 5.2 Placement Rules

* Align logo to a **vertical reference line**
* No fixed size → scale based on:

  * medium
  * layout proportions 

---

### 5.3 Variants

Available logo variants:

* CMYK (print)
* Negative (dark backgrounds)
* Grayscale
* Line versions (small scale / technical limitations) 

---

### 5.4 Constraints

```yaml
logo_rules:
  must:
    - maintain spacing (protection area)
    - use approved variants
  must_not:
    - recolor arbitrarily
    - distort proportions
    - place on non-approved backgrounds
```

---

## 6. Decorative Elements

### 6.1 Polygon Graphics

* Two variants:

  * Polygon grid (bottom/background)
  * Polygon cloud (centered) 

### Usage Rules

* Use as **background elements**
* Must maintain:

  * proportional scaling
  * consistent line thickness
  * color contrast 

---

## 7. UI/Frontend-Specific Considerations

### 7.1 Web Constraints

* Typography rendering varies by:

  * browser
  * device
* Colors may differ slightly across displays 

---

### 7.2 Recommended Adaptations

```yaml
frontend_guidelines:
  fonts:
    primary: "Segoe UI, Roboto, sans-serif"
    headline: "Merriweather, serif"

  layout:
    use_responsive_grid: true
    preserve_spacing_ratios: true

  accessibility:
    enforce_contrast: WCAG-compatible
    avoid_centered_body_text: true

  theming:
    restrict_to_design_tokens: true
```

---

## 8. Content Structuring

* Divide UI into:

  * thematic blocks
  * hierarchical sections
* Use:

  * headings
  * subheadings
  * visual grouping 

---

## 9. Anti-Patterns (Important for AI Enforcement)

### ❌ Forbidden

* Using non-brand colors
* Using Merriweather for body text
* Low contrast UI
* Overcrowded layouts (no whitespace)
* Arbitrary logo modifications 

---

## 10. Summary for Coding AI

### Core Design Tokens

```yaml
design_system:
  fonts:
    primary: Segoe UI
    secondary: Merriweather

  colors:
    primary_palette: defined
    secondary_palette: defined

  layout:
    grid: column_based
    spacing: mandatory_whitespace

  components:
    headline: serif (Merriweather)
    body: sans-serif (Segoe UI)

  rules:
    enforce_contrast: true
    enforce_consistency: true
    avoid_freeform_design: true
```

---

## ⚠️ Important Note

This document is **NOT a functional frontend specification**, but a **visual design system (corporate design manual)**.

If you're building a *User Frontend Application*, you will still need:

* Functional requirements (features, flows)
* Component architecture
* State management rules
* API contracts

---

## If you want next step

I can transform this into:

* ✅ React / Tailwind design system
* ✅ Figma token system
* ✅ Component library spec (buttons, forms, etc.)
* ✅ Prompt-ready system for coding AI (e.g., Cursor / Copilot)

Just tell me.
