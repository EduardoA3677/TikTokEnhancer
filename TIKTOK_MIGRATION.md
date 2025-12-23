# TikTok Enhancer Migration Documentation

## Overview
This document describes the migration from WhatsApp Enhancer to TikTok Enhancer, detailing the changes made and potential features that could be implemented.

## Migration Summary

### 1. Package Name Changes
- **From:** `com.whatsapp` and `com.whatsapp.w4b`
- **To:** `com.zhiliaoapp.musically` (TikTok)

### 2. Build Configuration
- **Flavors Changed:**
  - `whatsapp` → `tiktok`
  - `business` → `tiktok_lite`
- **Application ID:** `com.wmods.wppenhacer` (kept same for continuity)

### 3. Core Infrastructure Changes
- Updated `FeatureLoader.java` to use TikTok package names
- Commented out 40+ WhatsApp-specific features
- Updated broadcast receiver actions (WHATSAPP → TIKTOK)
- Modified `WppCore.java` class lookups to use placeholders

### 4. Removed Features (WhatsApp-Specific)
All the following features have been removed as they don't apply to TikTok:
- **Messaging Features:** AntiRevoke, ShowEditMessage, TagMessage, NewChat
- **Privacy Features:** HideReceipt, HideSeen, TypingPrivacy, FreezeLastSeen, DndMode
- **Call Features:** CallPrivacy, CallType
- **Status Features:** StatusDownload, ViewOnce, DownloadViewOnce, MenuStatus, DeleteStatus
- **Chat Features:** ChatLimit, ShareLimit, PinnedLimit, HideChat, SeparateGroup
- **Contact Features:** ShowOnline, HideSeenView
- **Group Features:** GroupAdmin, FilterGroups, ChatFilters
- **Other Features:** BubbleColors, SeenTick, IGStatus, Channels, Stickers, CopyStatus, TextStatusComposer, AudioTranscript, GoogleTranslate, AntiWa, CustomPrivacy, Tasker

### 5. Kept Features (Generic)
- **DebugFeature:** Generic debugging functionality
- **CustomThemeV2, CustomToolbar, CustomView:** UI customization (commented for now)
- **LiteMode, Others:** Generic features (commented for now)
- **ToastViewer:** Generic toast viewing (commented for now)

## TikTok Smali Analysis

### Key TikTok Classes Identified

#### Main Activity
```
com.ss.android.ugc.aweme.main.MainActivity
```

#### Video/Feed Classes
```
com.ss.android.ugc.aweme.feed.*
com.ss.android.ugc.aweme.video.*
com.ss.android.ugc.aweme.detail.ui.DetailActivity
```

#### Video Model
```
com.ss.android.ugc.aweme.feed.model.Video
- Contains: download_no_watermark_addr (URL to video without watermark)
```

#### Download Services
```
com.ss.android.ugc.aweme.download.component_api.DownloadServiceManager
com.ss.android.ugc.aweme.download.component_api.service.IDownloadService
com.ss.android.ugc.aweme.download.DownloadAwemeVideoServiceImpl
```

#### Ad/Commercialize
```
com.bytedance.ies.ugc.aweme.commercialize.*
com.ss.android.ugc.aweme.commercialize.*
com.ss.android.ugc.aweme.ad.*
```

### Potential TikTok Features to Implement

#### 1. Video Download Without Watermark ⭐⭐⭐
**Priority: High**
- Hook into Video model to access `download_no_watermark_addr`
- Provide download button to save videos without TikTok watermark
- Implementation: Hook Video.getDownloadAddr() and replace with no-watermark URL

#### 2. Ad Removal/Blocking ⭐⭐⭐
**Priority: High**
- Hook commercialize/ad classes to filter sponsored content
- Block in-feed advertisements
- Implementation: Filter feed items that are marked as ads

#### 3. Video Download Quality Selection ⭐⭐
**Priority: Medium**
- Allow users to select video quality before download
- Hook into video bitrate selection
- Implementation: Intercept download calls and allow quality choice

#### 4. Remove Forced Updates ⭐⭐
**Priority: Medium**
- Bypass TikTok's forced update mechanism
- Similar to WhatsApp's expiration bypass
- Implementation: Hook version check methods

#### 5. UI Customization ⭐
**Priority: Low**
- Custom themes for TikTok
- Color customization
- Implementation: Hook UI rendering and apply custom themes

#### 6. Auto-play Control ⭐
**Priority: Low**
- Control video auto-play behavior
- Save data by preventing auto-play
- Implementation: Hook video player initialization

#### 7. Download History ⭐
**Priority: Low**
- Keep track of downloaded videos
- Provide easy access to previously downloaded content
- Implementation: Database to store download metadata

## Technical Implementation Notes

### Class Lookup Strategy
Since TikTok's codebase is heavily obfuscated (unlike WhatsApp), the current approach is:
1. Use DexKit to search for classes by characteristics (methods, fields, strings)
2. Create a TikTok-specific Unobfuscator module
3. Cache discovered class mappings for each TikTok version

### Example Hook Implementation

```java
// Example: Hook Video model to expose no-watermark URL
public class VideoDownloadFeature extends Feature {
    public VideoDownloadFeature(ClassLoader loader, XSharedPreferences pref) {
        super(loader, pref);
    }
    
    @Override
    public void doHook() throws Exception {
        // Find Video class
        Class<?> videoClass = XposedHelpers.findClass(
            "com.ss.android.ugc.aweme.feed.model.Video", classLoader);
        
        // Hook getDownloadAddr to add menu option
        XposedHelpers.findAndHookMethod(videoClass, "getDownloadAddr", 
            new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    // Get no-watermark URL from field
                    Object videoObj = param.thisObject;
                    // Implementation details...
                }
            });
    }
}
```

## Current Build Status
✅ **Project builds successfully**
- All syntax errors fixed
- Minimal feature set compiles
- Ready for TikTok-specific feature implementation

## Next Steps
1. Implement TikTok-specific Unobfuscator
2. Add Video Download Without Watermark feature
3. Add Ad Blocker feature
4. Test on actual TikTok app
5. Update UI to reflect TikTok-specific features
6. Remove/update preference fragments to match available features

## Notes for Developers
- The package name `com.wmods.wppenhacer` is kept for continuity
- Most WhatsApp-specific code is commented out, not deleted, for reference
- TikTok uses heavy obfuscation, so class names will change between versions
- Focus on robust class discovery using DexKit rather than hardcoded class names
- Always test on multiple TikTok versions for compatibility

## Resources
- TikTok Smali Repository: https://github.com/Eduardob3677/com_zhiliaoapp_musically_6
- TikTok Package: `com.zhiliaoapp.musically`
- TikTok Main Activity: `com.ss.android.ugc.aweme.main.MainActivity`
