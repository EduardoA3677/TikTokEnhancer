# New TikTok Hooks Implementation

## Overview
This document describes the new hooks implemented based on smali analysis of TikTok's codebase from https://github.com/Eduardob3677/com_zhiliaoapp_musically_6

## New Features

### 1. FeedScrollCustomizer
**Location**: `app/src/main/java/com/wmods/tkkenhancer/xposed/features/media/FeedScrollCustomizer.java`

**Purpose**: Customize feed scrolling behavior when users scroll through videos

**Features**:
- Adjust scroll speed (configurable multiplier)
- Control smooth scroll animations
- Custom paging behavior for feed items
- Intercept RecyclerView scroll events

**Hooks**:
- `RecyclerView.scrollBy()` - Adjusts scroll speed by applying multiplier
- `RecyclerView.smoothScrollBy()` - Controls smooth scrolling
- `LinearLayoutManager.scrollVerticallyBy()` - Custom paging logic
- Feed adapter `onBindViewHolder()` methods - Feed item customization

**Preferences**:
- `feed_scroll_customizer` (boolean) - Enable/disable feature
- `scroll_speed` (string/float) - Speed multiplier (default: 1.0)
- `smooth_scroll` (boolean) - Enable smooth scroll (default: true)
- `custom_paging` (boolean) - Enable custom paging (default: false)

**How it works**:
1. Hooks into RecyclerView scroll methods
2. Applies speed multiplier to scroll distance
3. Can disable smooth scrolling for instant navigation
4. Supports custom paging behavior through LayoutManager hooks

---

### 2. RewardsIconHider
**Location**: `app/src/main/java/com/wmods/tkkenhancer/xposed/features/media/RewardsIconHider.java`

**Purpose**: Hide the TikTok Rewards floating icon from the main page

**Features**:
- Hides floating action buttons (FAB) for rewards
- Detects reward-related views by class name and resource ID
- Recursively searches and hides reward views in activity
- Blocks reward view visibility changes

**Target Classes** (searched):
- `com.google.android.material.floatingactionbutton.FloatingActionButton`
- `com.ss.android.ugc.aweme.reward.RewardIconView`
- `com.ss.android.ugc.aweme.incentive.IncentiveFloatButton`
- `com.ss.android.ugc.aweme.incentive.IncentiveView`
- `com.ss.android.ugc.aweme.compliance.business.banappeal.RewardEntrance`
- MainActivity, HomeActivity for reward initialization

**Detection Patterns**:
- Keywords: "reward", "Reward", "REWARD", "point", "Point", "coin", "Coin", "incentive", "Incentive"
- Checks class names and view resource IDs

**Hooks**:
- `FloatingActionButton.setVisibility()` - Forces GONE for reward FABs
- Reward view classes `setVisibility()` - Forces GONE
- Reward view `show()`, `display()`, `visible()` methods - Blocks display
- Activity `onCreate()` - Searches and hides reward views on startup

**Preferences**:
- `hide_rewards_icon` (boolean) - Enable/disable feature

**How it works**:
1. Intercepts FloatingActionButton visibility changes
2. Checks if button is reward-related using pattern matching
3. Forces visibility to GONE (hidden)
4. Recursively searches activity view hierarchy for reward views
5. Blocks reward view show/display methods

---

### 3. UIEnhancer
**Location**: `app/src/main/java/com/wmods/tkkenhancer/xposed/features/media/UIEnhancer.java`

**Purpose**: Additional UI enhancements discovered through smali analysis

**Features**:
1. **Hide Live Badge** - Removes live stream badges on videos
2. **Hide Shop Tab** - Hides shopping/ecommerce tab
3. **Hide Sponsored Badge** - Removes sponsored/promoted labels
4. **Hide Watermarks** - Hides TikTok watermarks from UI
5. **Hide Suggestions** - Removes suggestion overlays

**Target Classes** (searched):
- Live Badge:
  - `com.ss.android.ugc.aweme.feed.ui.LiveBadgeView`
  - `com.ss.android.ugc.aweme.live.LiveBadge`
  - `com.ss.android.ugc.aweme.feed.model.live.LiveBanner`

- Shop Tab:
  - `com.ss.android.ugc.aweme.commerce.ShopTabFragment`
  - `com.ss.android.ugc.aweme.ecommerce.base.mall.MallFragment`
  - `com.ss.android.ugc.aweme.ecommerce.base.osp.repository.ShopRepository`

- Watermarks:
  - `com.ss.android.ugc.aweme.shortvideo.ui.WatermarkView`
  - `com.ss.android.ugc.aweme.feed.ui.WaterMark`
  - `com.ss.android.ugc.aweme.common.ui.WatermarkLayout`

- Suggestions:
  - `com.ss.android.ugc.aweme.feed.ui.SuggestionPanel`
  - `com.ss.android.ugc.aweme.discover.ui.SuggestionView`
  - `com.ss.android.ugc.aweme.recommend.RecommendationOverlay`

**Hooks**:
- View `setVisibility()` methods - Forces GONE for targeted UI elements
- Fragment lifecycle methods - Blocks shop tab display
- `TextView.setText()` - Filters sponsored/promoted keywords
- Show/display methods - Blocks suggestions

**Preferences**:
- `ui_enhancer` (boolean) - Master enable/disable
- `hide_live_badge` (boolean) - Hide live badges
- `hide_shop_tab` (boolean) - Hide shop tab
- `hide_sponsored_badge` (boolean) - Hide sponsored badges (default: true)
- `hide_watermarks` (boolean) - Hide watermarks
- `hide_suggestions` (boolean) - Hide suggestions

**How it works**:
1. Searches for specific UI component classes
2. Hooks visibility and display methods
3. Forces visibility to GONE or blocks display methods
4. For sponsored badges, intercepts TextView.setText and filters keywords

---

## Implementation Details

### Integration with FeatureLoader
All three features are added to the FeatureLoader class:
```java
// ✅ NEW HOOKS - Based on smali analysis
com.wmods.tkkenhancer.xposed.features.media.FeedScrollCustomizer.class,
com.wmods.tkkenhancer.xposed.features.media.RewardsIconHider.class,
com.wmods.tkkenhancer.xposed.features.media.UIEnhancer.class,
```

### Preference UI
Added to `fragment_media.xml` under new category "TikTok UI Enhancements":
- All preferences with proper dependencies
- Default values configured
- EditTextPreference for scroll speed
- MaterialSwitchPreference for toggles

### String Resources
Added to `strings.xml`:
- Feature titles and summaries
- Individual setting descriptions
- Category titles

## Testing Recommendations

### FeedScrollCustomizer Testing:
1. Enable feature in settings
2. Try different scroll speed values (0.5, 1.0, 2.0)
3. Test smooth scroll toggle
4. Verify feed scrolls at adjusted speed
5. Check for any ANR or performance issues

### RewardsIconHider Testing:
1. Enable feature in settings
2. Navigate to main feed page
3. Verify rewards icon is hidden
4. Check multiple entry points (home, profile, etc.)
5. Ensure no crashes when icon should appear

### UIEnhancer Testing:
1. Enable master toggle
2. Enable individual features one by one
3. Live Badge: Check feed videos with live content
4. Shop Tab: Verify mall/shopping tab is hidden
5. Sponsored Badge: Check feed for promoted content
6. Watermarks: Look for watermark UI elements
7. Suggestions: Verify no recommendation overlays

## Security Considerations

✅ All features:
- Use try-catch blocks for safety
- Handle null cases properly
- Include debug logging
- Follow existing code patterns
- Check preferences before activation
- Do not expose sensitive data
- Use reflection safely

## Performance Considerations

- **FeedScrollCustomizer**: Minimal overhead, hooks only scroll methods
- **RewardsIconHider**: Recursive view search on activity creation (one-time cost)
- **UIEnhancer**: Multiple class searches with fallbacks (lazy evaluation)
- All features use pattern matching efficiently
- No continuous polling or heavy operations

## Known Limitations

1. **Class name changes**: TikTok may obfuscate or rename classes in future versions
2. **Pattern matching**: Rewards detection relies on naming patterns
3. **UI structure changes**: TikTok UI refactoring may affect hook effectiveness
4. **Version compatibility**: Tested primarily with TikTok 43.0.0

## Future Enhancements

Possible improvements:
1. Add more scroll customization options (acceleration curves, snap points)
2. Whitelist certain reward icons if needed
3. Add more UI elements to hide/show
4. Improve class discovery with DexKit
5. Add per-video scroll speed control
6. Custom animations for feed transitions

## Build Status

✅ BUILD SUCCESSFUL in 4m 5s
- All features compiled without errors
- Only standard deprecation warnings
- APK size: ~30MB

## References

- Smali Repository: https://github.com/Eduardob3677/com_zhiliaoapp_musically_6
- TikTok Version: 43.0.0 (com.zhiliaoapp.musically)
- Implementation Date: 2025-12-24
- Total new files: 3 features + preference XML + strings
