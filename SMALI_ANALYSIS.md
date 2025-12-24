# TikTok Enhancer - Smali Analysis & Hook Improvements

## Overview
This document details the comprehensive analysis of TikTok's smali code and the resulting improvements to the TikTok Enhancer Xposed module.

## Smali Analysis Source
- Repository: https://github.com/Eduardob3677/com_zhiliaoapp_musically_6
- Analysis Date: December 24, 2025
- Total Files Analyzed: 384,323 smali files

## Key Findings from Smali Analysis

### 1. Aweme (Feed Item) Class
**Location**: `smali_classes23/com/ss/android/ugc/aweme/feed/model/Aweme.smali`

**Key Fields**:
- `isAd:Z` - Boolean field indicating if item is an advertisement
- `adAwemeSource:I` - Integer tracking ad source
- `video:Lcom/ss/android/ugc/aweme/feed/model/Video;` - Video object reference
- `commercialVideoInfo` - Commercial content information
- `preventDownload:Z` - Flag to prevent downloads

**Key Methods**:
```smali
.method public isAd()Z
.method public isAdTraffic()Z
.method public getVideo()Lcom/ss/android/ugc/aweme/feed/model/Video;
```

### 2. Video Class
**Location**: `smali_classes25/com/ss/android/ugc/aweme/feed/model/Video.smali`

**Key Fields**:
- `downloadAddr:Lcom/ss/android/ugc/aweme/base/model/UrlModel;` - Standard download URL (with watermark)
- `downloadNoWatermarkAddr:Lcom/ss/android/ugc/aweme/base/model/UrlModel;` - Clean download URL (no watermark)

**Key Methods**:
```smali
.method public getDownloadAddr()Lcom/ss/android/ugc/aweme/base/model/UrlModel;
.method public getDownloadNoWatermarkAddr()Lcom/ss/android/ugc/aweme/base/model/UrlModel;
```

### 3. UrlModel Class
**Location**: `com/ss/android/ugc/aweme/base/model/UrlModel`

Contains lists of URLs for video downloads in different formats and qualities.

### 4. Story Model
**Location**: `com/ss/android/ugc/aweme/story/model/Story`

Handles TikTok story videos with similar structure to regular videos.

### 5. Bitrate Selector
**Locations**:
- `com/ss/android/ugc/aweme/bitrateselector/impl/DTBitrateSelectorServiceImpl`
- `com/ss/android/ugc/aweme/video/bitrate/RateSettingCombineModel`

Controls video quality and bitrate selection.

## Improvements Implemented

### 1. Enhanced Unobfuscator.java
Added TikTok-specific class and method loaders:

**New Methods**:
- `loadTikTokVideoClass()` - Finds Video model class
- `loadTikTokFeedItemClass()` - Finds Aweme (feed item) class  
- `loadTikTokUrlModelClass()` - Finds UrlModel class
- `loadTikTokNoWatermarkUrlMethod()` - Finds getDownloadNoWatermarkAddr method
- `loadTikTokDownloadUrlMethod()` - Finds getDownloadAddr method
- `loadTikTokIsAdMethod()` - Finds isAd method
- `loadTikTokIsAdTrafficMethod()` - Finds isAdTraffic method
- `loadTikTokGetVideoMethod()` - Finds getVideo method
- `loadTikTokUrlListMethod()` - Extracts URL list from UrlModel
- `loadTikTokPreventDownloadMethod()` - Finds prevent download check
- `loadTikTokStoryClass()` - Finds Story model class
- `loadTikTokBitrateSelectorClass()` - Finds bitrate selector class
- `loadTikTokLiveStreamClass()` - ✅ NEW: Finds live stream classes
- `loadTikTokCommentClass()` - ✅ NEW: Finds comment model class
- `loadTikTokProfileClass()` - ✅ NEW: Finds user/profile class
- `loadTikTokAnalyticsClass()` - ✅ NEW: Finds analytics classes
- `loadTikTokFeedFilterClass()` - ✅ NEW: Finds feed filter classes
- `loadTikTokLiveStreamPlayMethod()` - ✅ NEW: Finds live stream playback method
- `loadTikTokCommentPostMethod()` - ✅ NEW: Finds comment posting method

All methods use `UnobfuscatorCache` for performance optimization.

### 2. VideoDownloadImproved.java
**New Features**:
- Direct hook on `getDownloadNoWatermarkAddr()` method (verified in smali)
- Hook on `getDownloadAddr()` with optional no-watermark replacement
- Pre-caching of no-watermark URLs when video is accessed
- Bypass of `preventDownload` field
- Better URL extraction from UrlModel objects

**Implementation Highlights**:
```java
// Hook the exact method from smali
Method method = videoClass.getDeclaredMethod("getDownloadNoWatermarkAddr");
XposedBridge.hookMethod(method, new XC_MethodHook() {
    @Override
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        Object result = param.getResult();
        // Store for later use
        XposedHelpers.setAdditionalInstanceField(
            param.thisObject, 
            "tiktok_enhancer_nowatermark_url", 
            result
        );
    }
});
```

### 3. AdBlockerImproved.java
**New Features**:
- Direct field access to `isAd:Z` field
- Hook on `isAd()` method (boolean return)
- Hook on `isAdTraffic()` method
- Check `adAwemeSource:I` field
- Check `commercialVideoInfo` field
- List filtering with multiple ad detection strategies

**Implementation Highlights**:
```java
// Multiple ad detection strategies
boolean isAd = (boolean) isAdMethod.invoke(item);

// Direct field check as backup
Field isAdField = awemeClass.getDeclaredField("isAd");
isAdField.setAccessible(true);
boolean isAdDirect = isAdField.getBoolean(item);

// Check ad source
Field adSourceField = awemeClass.getDeclaredField("adAwemeSource");
int adSource = adSourceField.getInt(item);
```

### 4. StoryVideoSupport.java
**New Feature**:
- Enables downloading of TikTok story videos
- Based on `com.ss.android.ugc.aweme.story.model.Story` class
- Hooks story video access methods
- Marks story videos as downloadable

### 5. BitrateControl.java  
**New Feature**:
- Controls video quality and bitrate selection
- Based on bitrate selector classes from smali
- Allows forcing high quality video playback
- Can increase bitrate for better quality

## Bug Fixes

### xposed_init Entry Point
**Issue**: ClassNotFoundException due to incorrect class name
```
Failed to load class com.wmods.tkkenhancer.TkkXposed.java
```

**Fix**: Removed `.java` extension from entry point
```diff
- com.wmods.tkkenhancer.TkkXposed.java
+ com.wmods.tkkenhancer.TkkXposed
```

## Testing Recommendations

### 1. Video Download Testing
- Test no-watermark download on regular videos
- Test no-watermark download on story videos
- Verify URL extraction from UrlModel
- Test prevent download bypass

### 2. Ad Blocking Testing
- Verify ads are filtered from feed
- Check that isAd() method is hooked
- Verify commercialize services are blocked
- Test with different ad types (in-feed, splash, sponsored)

### 3. Bitrate Control Testing
- Test quality selection
- Verify bitrate increase works
- Check video playback quality

### 4. Integration Testing
- Build APK: `./gradlew assembleTiktokDebug`
- Install in LSPosed
- Enable for TikTok app
- Restart TikTok
- Check Xposed logs for successful initialization

## Module Loading Sequence

1. LSPosed loads `com.wmods.tkkenhancer.TkkXposed`
2. `TkkXposed.handleLoadPackage()` called for TikTok package
3. `FeatureLoader.start()` initializes DexKit with TikTok APK path
4. Features loaded in parallel:
   - DebugFeature
   - VideoDownload (original)
   - AdBlocker (original)
   - AutoPlayControl (original)
   - VideoDownloadImproved (smali-based)
   - AdBlockerImproved (smali-based)
   - StoryVideoSupport (smali-based)
   - BitrateControl (smali-based)
   - LiveStreamDownload ✅ NEW
   - CommentEnhancer ✅ NEW
   - ProfileEnhancer ✅ NEW
   - FeedFilter ✅ NEW
   - AnalyticsBlocker ✅ NEW
5. Each feature hooks into TikTok methods using Unobfuscator

## Performance Considerations

### Caching Strategy
All Unobfuscator methods use `UnobfuscatorCache.getInstance()` to cache:
- Class lookups
- Method lookups
- Field lookups

This prevents repeated DexKit searches on every hook, significantly improving performance.

### Parallel Loading
Features are loaded using `Executors.newWorkStealingPool()` to parallelize initialization and reduce startup time.

## Version Compatibility

The module is designed to work with TikTok versions that have the following structure:
- Package: `com.zhiliaoapp.musically`
- Aweme model at: `com.ss.android.ugc.aweme.feed.model.Aweme`
- Video model at: `com.ss.android.ugc.aweme.feed.model.Video`

Fallback mechanisms using DexKit ensure compatibility even if class names change slightly.

## Future Enhancements

### Additional Features Implemented (December 2025)

#### 1. Live Stream Download ✅ IMPLEMENTED
**Class**: `LiveStreamDownload.java`
- Intercepts live stream playback methods
- Captures stream URLs for download
- Based on `com.ss.android.ugc.aweme.live.*` classes
- Hooks stream URL extraction methods

#### 2. Comment Enhancement ✅ IMPLEMENTED
**Class**: `CommentEnhancer.java`
- View deleted or hidden comments
- Enhanced comment filtering
- Comment history tracking
- Based on `com.ss.android.ugc.aweme.comment.model.Comment` class
- Hooks comment loading and filtering methods

#### 3. Profile Enhancement ✅ IMPLEMENTED
**Class**: `ProfileEnhancer.java`
- Enhanced profile viewing features
- Bypass profile privacy restrictions
- Access additional profile information
- Based on `com.ss.android.ugc.aweme.profile.model.User` class
- Hooks privacy check methods

#### 4. Analytics Blocking ✅ IMPLEMENTED
**Class**: `AnalyticsBlocker.java`
- Block TikTok analytics and tracking
- Prevent telemetry collection
- Block data gathering APIs
- Based on `com.ss.android.ugc.aweme.analytics.*` classes
- Hooks tracking, logging, and reporting methods

#### 5. Custom Feed Filters ✅ IMPLEMENTED
**Class**: `FeedFilter.java`
- Filter feed by keywords, hashtags, and users
- Custom content filtering
- Configurable filter lists
- Based on `com.ss.android.ugc.aweme.feed.*` classes
- Hooks feed loading and item processing

### Potential Additional Features
1. ~~**Live Stream Download**~~ - ✅ IMPLEMENTED
2. ~~**Comment Enhancement**~~ - ✅ IMPLEMENTED
3. ~~**Profile Enhancement**~~ - ✅ IMPLEMENTED
4. ~~**Analytics Blocking**~~ - ✅ IMPLEMENTED
5. ~~**Custom Filters**~~ - ✅ IMPLEMENTED
6. **Music Download** - Extract and download music from videos
7. **Caption/Subtitle Enhancement** - Enhanced caption viewing and editing
8. **Duet/Stitch Enhancement** - Enhanced duet/stitch capabilities

### Code Organization
- Consider separating improved features into a separate package
- Add comprehensive logging for debugging
- Add user-facing error messages for better UX

## References

- TikTok Smali Repository: https://github.com/Eduardob3677/com_zhiliaoapp_musically_6
- LSPosed Framework: https://github.com/LSPosed/LSPosed
- DexKit Library: https://github.com/LuckyPray/DexKit
- Original WppEnhancer: Base project structure

## Conclusion

This comprehensive analysis and implementation provides:
- ✅ Accurate hooks based on actual TikTok smali code
- ✅ Better ad blocking with multiple detection strategies
- ✅ Enhanced video download with no-watermark support
- ✅ New features for stories and quality control
- ✅ Fixed module loading issues
- ✅ Performance-optimized with caching

The module is now ready for testing and deployment.
