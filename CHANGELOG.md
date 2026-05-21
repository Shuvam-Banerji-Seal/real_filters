# Changelog

All notable changes to Real Filters will be documented in this file.

## [1.0.3] - 2026-05-20

### Fixed
- **Critical: Preset corruption** - `ColorMatrix.copy()` and `ConvolutionKernel.copy()` shared the `values` array reference. Modifying a preset (e.g., selecting Sepia then editing) would corrupt the original preset. Added `clone()` methods that deep-copy the array.
- **FilterLayer cloning** - `addLayer()` now clones the matrix/kernel to prevent shared references between the layer and the preset.
- **ViewModel sharing** - Simplified ViewModel scoping by creating it at the NavHost level instead of using `getBackStackEntry`.
- **Image loading robustness** - `ImageLoader` now ensures bitmaps are always mutable ARGB_8888 format, with fallback chain: ImageDecoder → BitmapFactory → error.
- **Loading state display** - FilterEditorScreen shows "Loading image..." while the coroutine loads the bitmap, instead of "No image loaded".

### Added
- `StableBitmap` with `@Immutable` annotation and identity-based equality
- `key(bitmap.generationId)` in Image composable for proper recomposition
- Comprehensive logging in `ImageLoader` and `FilterViewModel.loadImage()`
- 20+ unit tests covering state transitions, clone semantics, preset integrity

## [1.0.2] - 2026-05-20

### Fixed
- **Critical: Image Not Loading**: Fixed root cause where HomeScreen and FilterEditorScreen had separate ViewModel instances due to Hilt NavBackStackEntry scoping. Both screens now share a single ViewModel scoped to the "home" navigation destination.
- **ViewModel Sharing**: Refactored navigation to pass ViewModel instance from NavHost level instead of each screen creating its own via `hiltViewModel()`
- **Theme Isolation**: Extracted theme state into separate `ThemeViewModel` to avoid circular dependencies

### Architecture
- `FilterViewModel` now scoped to NavHost's "home" back stack entry, shared by both HomeScreen and FilterEditorScreen
- `ThemeViewModel` handles theme state independently at Activity level
- Screens receive ViewModel as parameter instead of creating via `hiltViewModel()`

## [1.0.1] - 2026-05-20

### Fixed
- **Image Preview**: Fixed broken image rendering - bitmaps now use stable wrapper for proper Compose recomposition
- **Memory Management**: Cancelled stale filter jobs before applying new ones to prevent race conditions
- **Bitmap Lifecycle**: Processed bitmaps properly track original reference to prevent null display

### Improved
- **Theme System**: Added proper Light/Dark/System theme switching with Material3 color tokens
- **Color Palette**: Redesigned color scheme with Indigo primary, Teal secondary, Rose tertiary
- **Typography**: Custom font weights for headings and labels
- **Bottom Bar**: Added badge counter showing active layer count
- **Layer Sheet**: Better visual distinction between selected/unselected layers with border highlight
- **Preset Sheet**: FilterChips now show selection state for current matrix/kernel
- **Matrix Editor**: Dialog resets when matrix changes via `remember(matrix)` key
- **Processing Indicator**: Compact overlay badge instead of full-screen spinner
- **Empty State**: Better placeholder when no image is loaded
- **APK Naming**: Files now named `RealFilters-v{version}-{BuildType}.apk`

### Added
- Theme selector bottom sheet (Light/Dark/System)
- Layer count display in top bar subtitle
- `StableBitmap` wrapper for reliable Compose state tracking

## [1.0.0] - 2026-05-20

### Added
- Initial release
- Image loading for JPEG, PNG, GIF, BMP, WebP, TIFF, HEIF, SVG
- 13 preset color matrix filters
- 8 preset convolution kernels
- Custom matrix/kernel editor
- Layer system with opacity and reordering
- Matrix composition (multiply)
- Filter export/import as JSON
- Room database for saved filters
- Material3 theming with dark mode
