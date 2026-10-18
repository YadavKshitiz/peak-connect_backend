# PeakConnect Design System & UI Specification

PeakConnect is an adventure booking and trekking companion app.
**Mood**: Outdoors, calm, adventurous, clean cards, big imagery.

---

## 1. Color Palette

### Light Mode (`LightColorScheme`)
| Token | Hex Value | Description / Usage |
| :--- | :--- | :--- |
| `primary` | `#1F6B45` | Forest green, primary brand actions |
| `onPrimary` | `#FFFFFF` | Text/icons on primary |
| `primaryContainer` | `#CDEBD6` | Light green badge / container |
| `onPrimaryContainer` | `#08301C` | Text on primary container |
| `secondary` | `#2A7FA8` | Glacier blue, secondary accents |
| `onSecondary` | `#FFFFFF` | Text on secondary |
| `secondaryContainer`| `#D2E9F5` | Ice blue container |
| `tertiary` | `#F28C28` | Sunrise orange, prices & highlights |
| `onTertiary` | `#FFFFFF` | Text on tertiary |
| `background` | `#F7F9F7` | Warm light outdoor background |
| `onBackground` | `#17201A` | High-contrast body text |
| `surface` | `#FFFFFF` | Card & sheet surfaces |
| `onSurface` | `#17201A` | Content on surface |
| `surfaceVariant` | `#E4EBE5` | Shimmers, dividers, text field containers |
| `onSurfaceVariant`| `#4A574E` | Supporting captions, inactive icons |
| `outline` | `#8A978E` | Borders & outlines |
| `error` | `#BA1A1A` | Destructive actions, validation errors |
| `onError` | `#FFFFFF` | Content on error |

### Dark Mode (`DarkColorScheme`)
| Token | Hex Value | Description / Usage |
| :--- | :--- | :--- |
| `primary` | `#7FD1A0` | Light forest green |
| `onPrimary` | `#003822` | Text on dark primary |
| `primaryContainer` | `#0F4D31` | Deep forest container |
| `onPrimaryContainer` | `#CDEBD6` | Text on deep container |
| `secondary` | `#8CC8E6` | Soft glacier blue |
| `onSecondary` | `#00344A` | Text on dark secondary |
| `secondaryContainer`| `#1C566F` | Dark blue container |
| `tertiary` | `#FFB066` | Warm sunrise highlight |
| `onTertiary` | `#4A2800` | Text on dark tertiary |
| `background` | `#0F1511` | Deep nocturnal green-grey |
| `onBackground` | `#E1E8E2` | Light reading text |
| `surface` | `#171E19` | Dark card surface |
| `onSurface` | `#E1E8E2` | Content on dark surface |
| `surfaceVariant` | `#2A332D` | Dark shimmer & borders |
| `onSurfaceVariant`| `#BFC9C1` | Secondary muted text |
| `outline` | `#89938B` | Subtle outlines |
| `error` | `#FFB4AB` | Dark error tint |
| `onError` | `#690005` | Text on dark error |

### Semantic Colors
Accessed via `MaterialTheme.semanticColors`:
- **Risk Levels**:
  - `LOW`: `#2E9E5B` (Green)
  - `MODERATE`: `#E0A100` (Amber)
  - `HIGH`: `#D64545` (Red)
  - `UNKNOWN`: `#7A8580` (Muted Grey)
- **Booking Status**:
  - `PENDING_GUIDE_RESPONSE`: `#E0A100` (Amber)
  - `AWAITING_PAYMENT`: `#2A7FA8` (Blue)
  - `CONFIRMED`: `#2E9E5B` (Green)
  - `CANCELLED`: `#7A8580` (Grey)
  - `NO_SHOW`: `#D64545` (Red)
  - `REMATCH_FAILED`: `#E8590C` (Orange)
  - `PENDING`: `#7A8580` (Grey)

---

## 2. Typography

Headlines and titles use **Poppins** (via Google Fonts downloadable provider with `FontFamily.SansSerif` fallback). Body and labels use the platform default sans-serif font.

| Style | Font Family | Size | Weight | Line Height |
| :--- | :--- | :--- | :--- | :--- |
| `headlineLarge` | Poppins | 28sp | SemiBold | 39sp |
| `headlineMedium`| Poppins | 24sp | SemiBold | 34sp |
| `headlineSmall` | Poppins | 20sp | SemiBold | 28sp |
| `titleLarge` | Poppins | 18sp | SemiBold | 25sp |
| `titleMedium` | Poppins | 16sp | Medium | 22sp |
| `titleSmall` | Poppins | 14sp | Medium | 20sp |
| `bodyLarge` | System | 16sp | Normal | 22sp |
| `bodyMedium` | System | 14sp | Normal | 20sp |
| `bodySmall` | System | 12sp | Normal | 17sp |
| `labelLarge` | System | 14sp | Medium | 20sp |
| `labelMedium` | System | 12sp | Medium | 17sp |
| `labelSmall` | System | 11sp | Medium | 15sp |

---

## 3. Shapes & Spacing

### Shapes (`AppShapes`)
- `extraSmall`: 8dp (Chips, small badges, inline tags)
- `small`: 12dp (Buttons, text fields, search bars)
- `medium`: 16dp (Cards, dialogs, media items)
- `large`: 24dp (Prominent containers, dialog containers)
- `extraLarge`: 28dp (Bottom sheet top corners, expanded hero headers)

### Spacing Tokens (`MaterialTheme.spacing`)
- `xs`: 4dp
- `sm`: 8dp
- `md`: 12dp
- `lg`: 16dp
- `xl`: 24dp
- `xxl`: 32dp
- `screenHorizontal`: 16dp (standard screen padding)
- `minTouchTarget`: 48dp (minimum accessible touch target)

---

## 4. Component Catalog (`com.peakconnect.app.core.ui`)

### 1. Buttons (`AppButton.kt`)
- `PrimaryButton`: 52dp height, 12dp corners, filled primary, supports `isLoading` spinner.
- `SecondaryButton`: 52dp height, 12dp corners, outlined secondary.
- `TertiaryTextButton`: Text-only action button with minimum 48dp touch height.
- `DestructiveButton`: 52dp height, error container/content colors for cancellations or critical actions.
- **`OnlineOnlyButton`**:
  > [!IMPORTANT]
  > **MANDATORY RULE FOR ALL TASKS**: Every network-dependent action (e.g. Book, Pay, Cancel, Submit, Accept, Check In) MUST use `OnlineOnlyButton`.
  > It automatically disables itself when `isOnline == false` and displays `"Connect to the internet to <actionLabel>"` helper text below the button.

### 2. Form Inputs (`AppTextField.kt`)
- Outlined style with 12dp corners, label, supporting text, error text, optional leading icon, and built-in password visibility toggle variant.

### 3. Navigation & App Bars (`AppTopBar.kt`)
- Centers title or left-aligns, back navigation icon with localized string, and actions slot.

### 4. Progress & Skeletons (`LoadingIndicator.kt`, `Shimmer.kt`)
- `LoadingIndicator`: Centered circular spinner with optional message.
- `Modifier.shimmer()`: Animated linear gradient shimmer.
- `ShimmerBox`: Reusable clipping box with shimmer.
- `SkeletonCard`: Card placeholder simulating image + title + subtitle lines.

### 5. State Layouts (`StateLayouts.kt`)
- `ErrorState`: Vertically centered error icon, message, and retry button.
- `EmptyState`: Vertically centered icon, title, description, and action button.

### 6. Informational Banners (`InfoBanner.kt`)
- Supports `INFO`, `WARNING`, `ERROR`, `SUCCESS` with matching icons and optional action buttons. Used for offline warnings and cold-start server notices.

### 7. Chips & Badges (`AppChips.kt`)
- `StatusChip`: Renders booking status with corresponding semantic container and text colors.
- `RiskBadge`: Renders trail risk level with mandatory icon + text + color (never color alone).
- `DifficultyChip`: Trail difficulty tag.
- `SeasonChip`: Seasonality indicator.
- `AppChip`: Generic tag composable.

### 8. Currency & Formatting (`PriceText.kt`, `CountdownText.kt`)
- `PriceText`: Formats amounts in Indian Rupees (e.g., `"₹1,710"`, `"₹1,500.50"`) using `en-IN` numbering in SemiBold tertiary color.
- `CountdownText`: Formats remaining time as `mm:ss` with optional approx prefix.

### 9. Media (`AppAsyncImage.kt`)
- Coil 3 image loader with 300ms crossfade, animated shimmer loading state, and a fail-safe green-to-blue vertical gradient fallback with landscape icon and "Image unavailable" caption when >= 120dp wide. Never crashes or shows blank white squares.

### 10. Containers (`AppCard.kt`, `SectionHeader.kt`, `AppBottomSheet.kt`, `AppPullToRefreshBox.kt`)
- `AppCard`: 16dp rounded card with outline or 2dp elevation.
- `SectionHeader`: Header row with title and optional text action.
- `AppModalBottomSheet`: Modal bottom sheet with 28dp top corners and drag handle.
- `AppPullToRefreshBox`: Material 3 pull-to-refresh wrapper.
