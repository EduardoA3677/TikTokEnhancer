# TikTok Enhancer

![TikTok Enhancer](https://img.shields.io/badge/TikTok-Enhancer-blue)
![Version](https://img.shields.io/badge/version-1.5.2--DEV-green)
![License](https://img.shields.io/badge/license-GPL--3.0-orange)

An Xposed module that enhances TikTok with additional features including ad blocking, watermark-free video downloads, auto-play control, live stream support, and comprehensive privacy controls.

## 🌟 Features

### 1. Video Download (No Watermark) 🎥
Download TikTok videos without the TikTok watermark. This feature hooks into TikTok's internal video model to access the clean video URL.

**How it works:**
- Intercepts TikTok's video model to find the `download_no_watermark_addr` field
- Provides access to download videos in their original quality without branding
- Hooks into TikTok's download service for seamless integration
- Story video download support included

### 2. Ad Blocker 🚫
Block advertisements and sponsored content from your TikTok feed.

**What it blocks:**
- In-feed advertisements
- Sponsored posts
- Commercial content (commercialize API)
- Ad splash screens
- Ad traffic tracking

**How it works:**
- Filters feed items marked as ads
- Blocks commercialize service calls
- Intercepts ad loading mechanisms
- Removes ad-related content before display
- Direct field access for improved detection

### 3. Auto-Play Control ⏯️
Control video auto-play behavior to save data and battery.

**Features:**
- Disable automatic video playback
- Override TikTok's auto-play settings
- Save mobile data by preventing unwanted video loading
- Battery optimization

**How it works:**
- Hooks into video player initialization
- Intercepts auto-play method calls
- Overrides app-level auto-play settings

### 4. Live Stream Download 📡 NEW
Download and capture live stream content.

**Features:**
- Intercept live stream URLs
- Capture streaming content
- Access live stream metadata

**How it works:**
- Hooks into live stream playback methods
- Captures stream URLs for download
- Monitors live stream activity

### 5. Comment Enhancer 💬 NEW
Enhanced comment functionality and viewing.

**Features:**
- View deleted or hidden comments
- Enhanced comment filtering
- Comment history tracking

**How it works:**
- Bypasses comment deletion checks
- Intercepts comment loading
- Provides access to hidden content

### 6. Profile Enhancer 👤 NEW
Enhanced profile viewing and interaction.

**Features:**
- Enhanced profile information access
- View additional profile data
- Bypass some profile restrictions

**How it works:**
- Hooks profile viewing methods
- Intercepts privacy checks
- Provides enhanced profile data

### 7. Feed Filter 🔍 NEW
Custom filtering options for your feed.

**Features:**
- Filter by keywords
- Filter by hashtags
- Filter by users
- Custom content filtering

**How it works:**
- Intercepts feed loading
- Applies custom filters
- Removes unwanted content

### 8. Analytics Blocker 🔒 NEW
Block TikTok analytics and tracking.

**Features:**
- Block view tracking
- Block interaction analytics
- Prevent telemetry collection
- Block data gathering

**How it works:**
- Hooks analytics methods
- Blocks tracking calls
- Prevents data collection APIs

### 9. Video Quality Control 🎬
Control video quality and bitrate selection.

**Features:**
- Force high quality playback
- Adjust bitrate settings
- Enhanced video quality

### 10. Feed Scroll Customizer 📜 NEW
Customize feed scrolling behavior when browsing videos.

**Features:**
- Adjust scroll speed (slower, normal, faster)
- Control smooth scrolling
- Custom paging behavior
- Enhanced feed navigation

**How it works:**
- Hooks RecyclerView scroll methods
- Applies configurable speed multiplier
- Intercepts smooth scroll animations
- Customizes paging behavior

### 11. Rewards Icon Hider 🎁 NEW
Hide the TikTok Rewards floating icon from the main page.

**Features:**
- Automatically hides rewards/incentive buttons
- Detects reward-related views
- Prevents rewards icon display
- Clean main feed interface

**How it works:**
- Intercepts FloatingActionButton visibility
- Pattern-matching for reward elements
- Recursively searches and hides reward views
- Blocks reward icon initialization

### 12. UI Enhancer ✨ NEW
Additional UI enhancements discovered through smali analysis.

**Features:**
- Hide live stream badges
- Hide shop/mall tab
- Hide sponsored/promoted badges
- Remove watermarks from UI
- Hide suggestion overlays

**How it works:**
- Hooks UI component visibility
- Filters sponsored keywords
- Blocks unwanted UI elements
- Provides cleaner interface

### 13. Debug Feature 🐛
Advanced logging and debugging capabilities for development and troubleshooting.

## 📋 Requirements

- Android 9.0 (API 28) or higher
- LSPosed or EdXposed framework installed
- TikTok app (version 43.x or higher recommended)

## 🔧 Installation

1. **Install an Xposed Framework:**
   - LSPosed (recommended): [GitHub](https://github.com/LSPosed/LSPosed)
   - EdXposed: [GitHub](https://github.com/ElderDrivers/EdXposed)

2. **Install TikTok Enhancer:**
   - Download the latest APK from [Releases](../../releases)
   - Install the APK on your device
   - Enable the module in LSPosed/EdXposed
   - Reboot your device

3. **Configure the module:**
   - Open TikTok Enhancer app
   - Enable desired features
   - Reboot TikTok app

## ⚙️ Configuration

### Enabling Features

Open the TikTok Enhancer app and navigate to settings:

- **Video Download**: Toggle `video_download` preference
- **Ad Blocker**: Toggle `ad_blocker` preference
- **Auto-Play Control**: Toggle `autoplay_control` preference
- **Disable Auto-Play**: Toggle `disable_autoplay` preference (requires Auto-Play Control enabled)
- **Debug Logging**: Toggle `enablelogs` preference

## 🏗️ Architecture

### TikTok-Specific Hooks

The module uses DexKit for dynamic class discovery to handle TikTok's obfuscation:

```java
// Example: Video class discovery
Class<?> videoClass = loadTikTokVideoClass(classLoader);
```

### Unobfuscator Methods

Custom methods for finding obfuscated TikTok classes:
- `loadTikTokVideoClass()` - Finds the Video model class
- `loadTikTokFeedItemClass()` - Finds feed item (Aweme) class
- `loadTikTokDownloadServiceClass()` - Finds download service
- `loadTikTokAdClass()` - Finds ad/commercialize classes
- `loadTikTokVideoPlayerClass()` - Finds video player class

### Feature Implementation

Each feature extends the `Feature` base class:

```java
public class VideoDownload extends Feature {
    @Override
    public void doHook() throws Throwable {
        // Implementation
    }
}
```

## 🔍 Technical Details

### TikTok Package Structure

The module targets the following TikTok packages:
- Main package: `com.zhiliaoapp.musically`
- Video model: `com.ss.android.ugc.aweme.feed.model.Video`
- Feed items: `com.ss.android.ugc.aweme.feed.model.Aweme`
- Download service: `com.ss.android.ugc.aweme.download.*`
- Ads: `com.ss.android.ugc.aweme.commercialize.*`

### Obfuscation Handling

TikTok uses heavy code obfuscation. The module:
1. Attempts to find classes by standard names first
2. Falls back to DexKit-based discovery using characteristics
3. Caches discovered classes per TikTok version
4. Uses reflection to access obfuscated methods

### Version Compatibility

The module checks for supported TikTok versions and can:
- Bypass TikTok's forced update mechanism
- Work across multiple TikTok versions (43.x, 44.x, 45.x)
- Cache class mappings per version for performance

## 🛠️ Development

### Building from Source

```bash
git clone https://github.com/EduardoA3677/TikTokEnhancer.git
cd TikTokEnhancer
./gradlew assembleTiktokDebug
```

The APK will be in `app/build/outputs/apk/tiktok/debug/`

### Project Structure

```
app/src/main/java/com/wmods/tkkenhancer/
├── xposed/
│   ├── core/
│   │   ├── Feature.java          # Base class for features
│   │   ├── FeatureLoader.java    # Loads and initializes features
│   │   └── devkit/
│   │       └── Unobfuscator.java # TikTok class discovery
│   └── features/
│       └── media/
│           ├── VideoDownload.java    # No-watermark downloads
│           ├── AdBlocker.java        # Ad blocking
│           └── AutoPlayControl.java  # Auto-play control
└── TkkXposed.java               # Main Xposed entry point
```

### Adding New Features

1. Create a new class extending `Feature`
2. Implement the `doHook()` method
3. Add the class to `FeatureLoader.plugins()`
4. Add preference toggle in settings

Example:
```java
public class MyFeature extends Feature {
    public MyFeature(@NonNull ClassLoader classLoader, 
                     @NonNull XSharedPreferences preferences) {
        super(classLoader, preferences);
    }

    @Override
    public void doHook() throws Throwable {
        if (!prefs.getBoolean("my_feature", false)) return;
        // Your implementation
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "My Feature";
    }
}
```

## 📖 Documentation

- [Theme.md](Theme.md) - Theme customization guide
- [BUILD_CONFIGURATION.md](.github/BUILD_CONFIGURATION.md) - Build configuration guide

## 🐛 Troubleshooting

### Module not working?

1. Check that LSPosed/EdXposed is properly installed
2. Verify the module is enabled in LSPosed
3. Make sure TikTok is selected in module scope
4. Reboot after enabling the module
5. Enable debug logs to see what's happening

### Features not activating?

1. Open TikTok Enhancer app
2. Enable the specific feature toggle
3. Force stop TikTok
4. Clear TikTok cache (optional)
5. Restart TikTok

### Checking logs

Enable debug logging in settings, then check Xposed logs:
```bash
adb logcat -s Xposed
```

## 🔒 Privacy & Security

- The module runs entirely on your device
- No data is sent to external servers
- Downloads are stored locally on your device
- No tracking or analytics

## ⚠️ Disclaimer

This module is for educational purposes only. Use at your own risk. The developers are not responsible for:
- Any damage to your device
- Account bans or restrictions
- Violation of TikTok's Terms of Service
- Any illegal use of the module

## 📄 License

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details.

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

### Guidelines

1. Follow the existing code style
2. Test your changes thoroughly
3. Update documentation as needed
4. One feature per pull request

## 📞 Support

- Issues: [GitHub Issues](../../issues)
- Discussions: [GitHub Discussions](../../discussions)

## 🌟 Acknowledgments

- Original project based on WppEnhancer
- LSPosed team for the Xposed framework
- DexKit for obfuscation handling
- TikTok reverse engineering community

## 📚 Related Projects

- [LSPosed](https://github.com/LSPosed/LSPosed) - Xposed framework
- [DexKit](https://github.com/LuckyPray/DexKit) - High performance runtime parsing library

---

**Made with ❤️ for the TikTok community**
