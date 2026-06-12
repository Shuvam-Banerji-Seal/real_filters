# Changelog

All notable changes to Real Filters will be documented in this file.

## [1.0.9] - 2026-05-23

### Fixed
- **"App fails on open"** - When an image is shared with the app (ACTION_VIEW/SEND) but fails to load (SecurityException, missing file, unsupported format), the Filter Editor would open in a broken state with a "No image loaded" placeholder. Now the app stays on the home screen and surfaces a clear error message.
- **`showEditor` no longer flips before image loads** - both the initial-image and the picker-result flows now wait for `uiState.originalBitmap != null` before navigating to the editor.
- **`pendingImageUri` properly bridges Activity to Compose** - uses `mutableStateOf` to ensure recomposition when the URI arrives from `onCreate` or `onNewIntent`.

## [1.0.8] - 2026-05-23

### Fixed
- **OutOfMemoryError no longer crashes the app** - OOM during `loadImage` and `applyFilters` is now caught with a friendly "Image is too large" message
- **Bitmap leaks on cancellation** - `applyFilterLayers` and `applyColorMatrix`/`applyConvolution` now wrap allocations in `try/finally` and recycle on exception
- **Bitmap leaks on ViewModel clear** - `onCleared` now recycles both `originalBitmap` and `processedBitmap`
- **Kernel values wiped when resizing** - resizing W/H now preserves existing values where possible and zeros only new slots
- **Missing kernel `Offset` field** - the kernel editor now exposes the `offset` field
- **European decimal separator** - matrix/kernel editors now accept `,` as decimal separator; all `String.format` calls use `Locale.US`
- **SVG viewBox-only files** - SVGs without `width`/`height` attributes no longer collapse to 1×1
- **Zero-dimension bitmaps** - `scaleBitmap` now warns instead of silently returning invalid bitmaps
- **Import without persistable permission** - `OpenDocument` URIs now take persistable read permission
- **`onNewIntent` leaked composition** - replaced `setContent` re-call with `mutableStateOf<Uri?>` to avoid re-creating the composition tree

### Added
- **`EngineError` sealed class** - structured errors for the image-processing pipeline
- **Cooperative cancellation in pixel loops** - `ensureActive()` is checked every 256K pixels (matrix) and every 64 rows (convolution)
- **Layer count cap** - `MAX_LAYERS = 32` enforced in both `addLayer` and `applyFilterLayers`
- **Import validation** - imported matrices must have exactly 20 values; kernels must have `width * height` values, with dimensions 1..9
- **`Locale.US` everywhere** - all float formatting now locale-pinned
- **Zero-dimension image rejection** - bitmap factory rejects images with `outWidth <= 0 || outHeight <= 0`
- **20 new unit tests** - `EngineDataClassesTest`, `FilterImportValidationTest`

### Performance
- **Disk I/O on `Dispatchers.IO`** - image decoding no longer blocks the main thread

## [1.0.7] - 2026-05-22

### Added
- **Intent handling for shared images** - App can now receive images shared from other apps (ACTION_VIEW/SEND with image/*)
- **DataStore persistence for theme** - Theme preference (Light/Dark/System) now persists across app restarts
- **77+ strings externalized to strings.xml** - Foundation for i18n/localization

### Improved
- **Performance: `remember` for matrix/kernel display strings** - avoids recomputing 20-81 `String.format` calls per recomposition
- **LazyColumn key parameters** - `key = { it.id }` for saved filters, `key = { it.name }` for presets
- **Database index on `createdAt`** - faster sorting of saved filters
- **MainActivity handles onNewIntent** - supports re-sharing while app is running

### Fixed
- **App not responding to shared images** - Manifest declared intent-filters but MainActivity didn't read intent.data

## [1.0.6] - 2026-05-22

### Fixed (Second deep audit - 12 more bugs)

**Crash Prevention**
- `ensureMutable()` no longer returns recycled bitmap when `bitmap.copy()` returns null
- `applyFilterLayers()` handles null `bitmap.copy()` with double-fallback (config → ARGB_8888 → original)
- Empty imported JSON layers no longer cause out-of-bounds `selectedLayerIndex`

**Data Integrity**
- `enabled` layer state now persists through save/load and export/import round-trips
- `FilterLayerExport` includes `enabled` field in serialization
- `exportToLayers()` restores `enabled` state on import

**Saturation Filter**
- Fixed incorrect luminance weights (row vs column index bug)
- Standardized on Rec. 709 weights (0.2126, 0.7152, 0.0722) matching grayscale filter
- Full desaturation now produces correct neutral gray

**Compose Lifecycle**
- `showEditor` and sheet booleans use `rememberSaveable` (survives config changes)
- `collectAsState()` → `collectAsStateWithLifecycle()` (pauses when backgrounded)
- `KernelEditorDialog` state keyed on `kernel` parameter
- `LaunchedEffect` → `remember(w, h)` for synchronous kernel size derivation
- `LazyColumn` for saved filters has proper height bound

**Performance**
- `@Immutable` on `ColorMatrix`, `ConvolutionKernel`, `FilterLayer` for recomposition skip
- `LazyColumn` properly bounded inside `Column`

## [1.0.5] - 2026-05-22

### Fixed (26 bugs from multi-agent audit)

**Memory Leaks (Critical)**
- `applyFilters()` now recycles old `processedBitmap` before assigning new one
- `resetFilters()` now recycles outgoing processed bitmap
- `loadImage()` now recycles old original/processed bitmaps on new load
- `applyFilterLayers()` now recycles old `current` during opacity blending
- `scaleBitmap()` now recycles original bitmap after scaling
- SVG `InputStream` now properly closed via `.use {}`
- Added `onCleared()` to cancel jobs and release resources

**Concurrency (High)**
- `loadImage()` now cancels prior `loadImageJob` and `applyJob`
- `resetFilters()` now cancels `applyJob`
- `applyFilters()` reads state inside coroutine (not stale snapshot)
- `CancellationException` properly rethrown instead of caught
- All database operations wrapped in try-catch with user-facing errors

**Data Integrity (High)**
- `selectPresetMatrix()` now clones preset (prevents shared mutable reference)
- `selectPresetKernel()` now clones preset
- `updateLayerOpacity()` now clamps to [0f, 1f]
- `selectLayer()` now validates bounds
- `moveLayer()` preserves correct selected layer index
- `importFilter()` now updates `selectedLayerIndex`

**Image Processing (High)**
- All `Bitmap.createBitmap` calls force `ARGB_8888` (prevents HARDWARE bitmap crash)
- `blendBitmaps()` now clamps color channels to [0, 255]
- `calculateInSampleSize()` guards against zero dimensions

**Data Layer (High)**
- Room database uses `fallbackToDestructiveMigration()`
- `FilterLayerExport.equals()` now compares all fields including arrays
- `FilterExport`/`FilterLayerExport` fields have default values (Gson null safety)
- `FilterSerializer.fromJson()` handles blank input

**UI (Medium)**
- Sheet state (preset/layer/theme) reset when exiting editor
- Touch targets increased from 24dp/32dp to 48dp (accessibility)
- `showEditor` set before load (shows loading indicator instead of blank)
- Error dismiss button properly sized
- Layer action buttons properly sized

## [1.0.4] - 2026-05-20

### Fixed
- **Critical: Image preview now works** - Completely rewrote app architecture to use single-screen approach. Eliminates ViewModel sharing issues across navigation by keeping everything in one screen with conditional content (home vs editor).
- **Architecture**: Replaced NavHost-based navigation with single `MainScreen` composable that conditionally shows home or editor content based on `showEditor` state flag.
- **Image composable**: Added missing `import androidx.compose.foundation.Image` and `remember(imageBitmap)` for proper bitmap caching.
- **Icon references**: Fixed `Icons.Default.Image` (not available) → `Icons.Default.PhotoLibrary`.

### Architecture Change
```
Before: MainActivity → MainNavigation (NavHost) → HomeScreen + FilterEditorScreen
        (separate ViewModels per destination - broken)

After:  MainActivity → MainScreen (single screen, conditional content)
        (one ViewModel, shared state - works)
```

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
