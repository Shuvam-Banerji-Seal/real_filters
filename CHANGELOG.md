# Changelog

All notable changes to Real Filters will be documented in this file.

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
