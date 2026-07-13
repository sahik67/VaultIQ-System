---
name: Executive Management Dashboard
colors:
  surface: '#faf8ff'
  surface-dim: '#d9d9e5'
  surface-bright: '#faf8ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f3f3fe'
  surface-container: '#ededf9'
  surface-container-high: '#e7e7f3'
  surface-container-highest: '#e1e2ed'
  on-surface: '#191b23'
  on-surface-variant: '#434655'
  inverse-surface: '#2e3039'
  inverse-on-surface: '#f0f0fb'
  outline: '#737686'
  outline-variant: '#c3c6d7'
  surface-tint: '#0053db'
  primary: '#004ac6'
  on-primary: '#ffffff'
  primary-container: '#2563eb'
  on-primary-container: '#eeefff'
  inverse-primary: '#b4c5ff'
  secondary: '#565e74'
  on-secondary: '#ffffff'
  secondary-container: '#dae2fd'
  on-secondary-container: '#5c647a'
  tertiary: '#943700'
  on-tertiary: '#ffffff'
  tertiary-container: '#bc4800'
  on-tertiary-container: '#ffede6'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#dbe1ff'
  primary-fixed-dim: '#b4c5ff'
  on-primary-fixed: '#00174b'
  on-primary-fixed-variant: '#003ea8'
  secondary-fixed: '#dae2fd'
  secondary-fixed-dim: '#bec6e0'
  on-secondary-fixed: '#131b2e'
  on-secondary-fixed-variant: '#3f465c'
  tertiary-fixed: '#ffdbcd'
  tertiary-fixed-dim: '#ffb596'
  on-tertiary-fixed: '#360f00'
  on-tertiary-fixed-variant: '#7d2d00'
  background: '#faf8ff'
  on-background: '#191b23'
  surface-variant: '#e1e2ed'
typography:
  display:
    fontFamily: Inter
    fontSize: 40px
    fontWeight: '700'
    lineHeight: 48px
    letterSpacing: -0.02em
  display-mobile:
    fontFamily: Inter
    fontSize: 36px
    fontWeight: '700'
    lineHeight: 44px
    letterSpacing: -0.02em
  h1:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '600'
    lineHeight: 40px
    letterSpacing: -0.01em
  h1-mobile:
    fontFamily: Inter
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 36px
  h2:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  body-lg:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 28px
  body-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-sm:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
    letterSpacing: 0.05em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  base: 4px
  xs: 8px
  sm: 12px
  md: 16px
  lg: 24px
  xl: 32px
  xxl: 48px
  sidebar_width: 260px
---

## Brand & Style
The design system is engineered for clarity, efficiency, and high-stakes monitoring. The brand personality is **Professional, Systematic, and Precise**, reflecting the reliability required for device management. 

The aesthetic follows a **Modern Corporate Minimalism** approach. It prioritizes data density without visual clutter, utilizing generous whitespace to separate distinct functional areas. The interface utilizes a "Sidebar-First" architecture to establish a clear structural hierarchy. Visual weight is used sparingly to highlight critical system statuses (Success, Warning, Error), ensuring that the user's attention is always directed toward actionable insights.

## Colors
The palette is rooted in a "Trust Blue" primary, supported by a deep slate neutral scale that provides high contrast for legibility.

- **Primary**: Used for primary actions, active navigation states, and focus indicators.
- **Success/Warning/Error**: Reserved strictly for system status, health indicators, and alerts. These must always be paired with their "Light" variants for background tints in banners or badges to ensure WCAG 2.1 AA compliance.
- **Neutrals**: `#0F172A` is the anchor for the sidebar and primary headings. Use `#475569` for secondary information and `#94A3B8` for placeholder or disabled states.
- **Backgrounds**: The main application canvas uses `#F8FAFC` to provide a soft contrast against the pure white (`#FFFFFF`) component cards.

## Typography
The design system utilizes **Inter** exclusively to maintain a utilitarian, highly readable environment. 

The scale is optimized for data-heavy dashboards. **Display** styles are reserved for high-level metric overviews. **Heading** styles establish section hierarchy within cards. **Body-sm** (14px) is the workhorse for table data and metadata, while **Body-md** (16px) is the standard for form labels and general content. Use tight letter-spacing on larger headings to maintain a modern, "tight" feel.

## Layout & Spacing
The layout employs a **Fixed-Fluid hybrid grid** based on a 4px stepping system.

- **Sidebar**: Fixed at `260px`. It uses `#0F172A` as its base to visually "lock" the navigation to the left.
- **Main Canvas**: A fluid container with a maximum content width of `1440px` for optimal readability, centered on larger displays.
- **Gaps**: Use `24px` (lg) for margins between cards and `16px` (md) for spacing within card elements.
- **Mobile Adaptation**: On devices below `768px`, the sidebar collapses into a hamburger menu, and horizontal page padding reduces from `32px` to `16px`.

## Elevation & Depth
Depth is created through **Tonal Layering** and a single, consistent shadow definition for interactive containers.

- **Level 0 (Surface)**: The app background (`#F8FAFC`).
- **Level 1 (Cards)**: White background (`#FFFFFF`) with a subtle ambient shadow: `0 4px 20px rgba(15, 23, 42, 0.06)`. This elevation is used for all primary content modules.
- **Level 2 (Overlays)**: Modals and dropdowns use the same shadow but with a `1px` solid border in `#E2E8F0` to ensure separation against white cards.
- **Sidebar**: No shadow; depth is achieved via the high-contrast color shift between the dark navigation and light content area.

## Shapes
The design system uses a **Mixed-Radius Logic** to create a sophisticated hierarchy:

- **Cards**: `16px` radius for a friendly, modern container feel.
- **Buttons**: `10px` radius to distinguish interactive elements from structural containers.
- **Inputs**: `8px` radius for a precise, "tool-like" appearance.
- **Badges/Chips**: Fully rounded (pill-shaped) to represent status tags and categories.

## Components

### Buttons
- **Primary**: Background `#2563EB`, text `#FFFFFF`. 12px 24px padding. 
- **Secondary**: Transparent background, border `1px` solid `#E2E8F0`, text `#1E293B`.
- **States**: Hover states should involve a brightness shift (Primary becomes `#1D4ED8`).

### Inputs
- **Text Fields**: `48px` height. Border `#E2E8F0`. On focus, the border shifts to Primary Blue with a `2px` outer glow (Primary Blue at 10% opacity).
- **Checkboxes**: `16px` square, `4px` radius. Primary Blue fill when checked.

### Cards
- Standardized `24px` internal padding. 
- Headers within cards should use a bottom border of `1px` solid `#F1F5F9` to separate titles from content.

### Chips/Badges
- Status indicators (e.g., "Online", "Warning") use a light background tint (e.g., `#DCFCE7`) with high-contrast text (e.g., `#10B981`) and a 12px horizontal padding.

### Sidebar Items
- Active state: Left-aligned `4px` vertical border in Primary Blue, with a background tint of white at 5% opacity. Text color remains white for high contrast against `#0F172A`.