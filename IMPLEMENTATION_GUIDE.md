# TikTok Smali Analysis & Hook Implementation Guide

## Overview

This document provides a comprehensive guide to the TikTok smali code analysis and hook implementations in TikTok Enhancer. It explains how each feature works, what it hooks, and how to extend or modify the functionality.

## TikTok App Architecture

### Package Structure

TikTok (com.zhiliaoapp.musically) is organized into several key packages:

```
com.zhiliaoapp.musically/
com.ss.android.ugc.aweme/
├── feed/                    # Feed and video content
│   ├── model/
│   │   ├── Aweme           # Feed item model
│   │   └── Video           # Video metadata
│   └── adapter/            # Feed adapters
├── video/                   # Video playback
│   └── VideoBitmapManager
├── download/                # Download services
│   ├── component_api/
│   └── DownloadAwemeVideoServiceImpl
├── commercialize/           # Ads and sponsored content
│   └── media/
├── ad/                      # Advertisement services
│   └── feed/
├── player/                  # Video player
│   └── sdk/
└── setting/                 # App settings
    └── services/
```

## Feature Implementations

### 1. VideoDownload Feature

#### Purpose
Enables downloading TikTok videos without the watermark by accessing the clean video URL.

#### Key Classes Hooked

**Video Model Class:**
```java
com.ss.android.ugc.aweme.feed.model.Video
```

**Important Fields:**
- `download_addr` - Standard download URL (with watermark)
- `download_no_watermark_addr` - Clean download URL (without watermark)
- `play_addr` - Playback URL
- `origin_cover` - Video thumbnail

#### Implementation Strategy

1. **Class Discovery:**
```java
public Class<?> loadTikTokVideoClass(ClassLoader classLoader) {
    // Try standard class name
    try {
        return XposedHelpers.findClass(
            "com.ss.android.ugc.aweme.feed.model.Video", 
            classLoader
        );
    } catch (Throwable e) {
        // Fallback to DexKit search
        return findFirstClassUsingStrings(
            classLoader, 
            StringMatchType.Contains, 
            "download_no_watermark_addr", 
            "download_addr"
        );
    }
}
```

2. **Method Hooking:**
```java
private void hookVideoModel() {
    Class<?> videoClass = loadTikTokVideoClass(classLoader);
    
    // Hook download URL access methods
    for (Method method : videoClass.getDeclaredMethods()) {
        if (method.getName().toLowerCase().contains("download") ||
            method.getName().toLowerCase().contains("url")) {
            
            XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    Object result = param.getResult();
                    // Access no-watermark URL
                    storeDownloadUrl(param.thisObject, result);
                }
            });
        }
    }
}
```

3. **Download Service Integration:**
```java
private void hookDownloadService() {
    Class<?> downloadServiceClass = XposedHelpers.findClass(
        "com.ss.android.ugc.aweme.download.component_api.DownloadServiceManager",
        classLoader
    );
    
    // Hook download methods to modify behavior
    // Force no-watermark downloads
}
```

#### Usage

To download a video without watermark:
1. Enable `video_download` preference
2. Video URLs are intercepted when accessed
3. No-watermark URLs are made available
4. Download proceeds with clean video

### 2. AdBlocker Feature

#### Purpose
Removes advertisements and sponsored content from the TikTok feed.

#### Key Classes Hooked

**Feed Item List:**
```java
com.ss.android.ugc.aweme.feed.model.FeedItemList
com.ss.android.ugc.aweme.feed.adapter.FeedAdapter
```

**Commercialize Services:**
```java
com.ss.android.ugc.aweme.commercialize.media.impl.utils.CommercializeMediaServiceImpl
com.bytedance.ies.ugc.aweme.commercialize.splash.service.CommercializeSplashServiceImpl
```

**Ad Services:**
```java
com.ss.android.ugc.aweme.ad.feed.FeedAdServiceImpl
com.bytedance.ies.ad.base.AdBaseServiceImpl
```

#### Implementation Strategy

1. **Feed Filtering:**
```java
private void hookFeedMethods(Class<?> feedClass) {
    for (Method method : feedClass.getDeclaredMethods()) {
        if (method.getReturnType() == List.class) {
            XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    List<?> feedItems = (List<?>) param.getResult();
                    filterAdsFromList(feedItems);
                }
            });
        }
    }
}
```

2. **Ad Detection:**
```java
private boolean isAdItem(Object item) {
    // Check for ad-related fields
    try {
        Object isAd = XposedHelpers.getObjectField(item, "isAd");
        if (isAd instanceof Boolean && (Boolean) isAd) {
            return true;
        }
    } catch (Throwable ignored) {}
    
    // Check for commercialize field
    try {
        Object commercialize = XposedHelpers.getObjectField(item, "commercialize");
        if (commercialize != null) {
            return true;
        }
    } catch (Throwable ignored) {}
    
    return false;
}
```

3. **Blocking Ad Services:**
```java
private void hookCommercializeMethods(Class<?> commercializeClass) {
    for (Method method : commercializeClass.getDeclaredMethods()) {
        if (method.getName().toLowerCase().contains("show") ||
            method.getName().toLowerCase().contains("load")) {
            
            XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    // Block ad display
                    param.setResult(null);
                }
            });
        }
    }
}
```

#### Ad Indicators

The following fields/methods indicate an item is an ad:
- `isAd` field (boolean)
- `adLabel` field (non-null)
- `commercialize` field (non-null)
- `sponsored` field (boolean)
- Class name contains "ad", "commercialize", or "sponsor"

### 3. AutoPlayControl Feature

#### Purpose
Allows users to control video auto-play behavior to save data and battery.

#### Key Classes Hooked

**Video Player:**
```java
com.ss.android.ugc.aweme.player.sdk.api.OnUIPlayListener
com.ss.android.ugc.aweme.video.VideoBitmapManager
```

**Settings Service:**
```java
com.ss.android.ugc.aweme.setting.services.SettingServiceImpl
```

#### Implementation Strategy

1. **Player Control:**
```java
private void hookPlayerMethods(Class<?> playerClass) {
    for (Method method : playerClass.getDeclaredMethods()) {
        if (method.getName().toLowerCase().contains("autoplay") ||
            method.getName().toLowerCase().contains("startplay")) {
            
            XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (prefs.getBoolean("disable_autoplay", false)) {
                        // Block auto-play
                        param.setResult(false);
                    }
                }
            });
        }
    }
}
```

2. **Settings Override:**
```java
private void hookSettingsMethods(Class<?> settingsClass) {
    for (Method method : settingsClass.getDeclaredMethods()) {
        if (method.getName().toLowerCase().contains("autoplay")) {
            XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (prefs.getBoolean("disable_autoplay", false)) {
                        param.setResult(false);
                    }
                }
            });
        }
    }
}
```

## Unobfuscator Methods

### TikTok-Specific Class Loaders

#### loadTikTokVideoClass()
Finds the Video model class that contains video metadata and download URLs.

**Search Strategy:**
1. Try standard class name: `com.ss.android.ugc.aweme.feed.model.Video`
2. Try alternate location: `com.ss.android.ugc.aweme.video.Video`
3. Use DexKit to search for classes containing: `"download_no_watermark_addr"`, `"download_addr"`

#### loadTikTokFeedItemClass()
Finds the Aweme class representing feed items.

**Search Strategy:**
1. Try standard class name: `com.ss.android.ugc.aweme.feed.model.Aweme`
2. Use DexKit to search for classes containing: `"aweme"`, `"video"`, `"author"`

#### loadTikTokDownloadServiceClass()
Finds the download service implementation.

**Search Strategy:**
1. Try: `com.ss.android.ugc.aweme.download.component_api.DownloadServiceManager`
2. Try: `com.ss.android.ugc.aweme.download.DownloadAwemeVideoServiceImpl`
3. Use DexKit to search for classes containing: `"DownloadService"`, `"aweme"`

#### loadTikTokAdClass()
Finds ad/commercialize service classes.

**Search Strategy:**
1. Try: `com.ss.android.ugc.aweme.commercialize.media.impl.utils.CommercializeMediaServiceImpl`
2. Try: `com.bytedance.ies.ugc.aweme.commercialize.splash.service.CommercializeSplashServiceImpl`
3. Use DexKit to search for classes containing: `"commercialize"`, `"ad"`

#### loadTikTokVideoPlayerClass()
Finds the video player implementation.

**Search Strategy:**
1. Try: `com.ss.android.ugc.aweme.player.sdk.api.OnUIPlayListener`
2. Try: `com.ss.android.ugc.aweme.video.VideoBitmapManager`
3. Use DexKit to search for classes containing: `"player"`, `"video"`, `"play"`

## Handling Obfuscation

### DexKit Integration

TikTok uses ProGuard/R8 obfuscation. To handle this:

1. **String-based Search:**
```java
Class<?> clazz = findFirstClassUsingStrings(
    classLoader,
    StringMatchType.Contains,
    "unique_string_in_class"
);
```

2. **Field-based Search:**
Look for classes with specific field combinations:
```java
ClassMatcher matcher = new ClassMatcher()
    .addField("fieldName1")
    .addField("fieldName2");
```

3. **Method Signature Search:**
Find methods by parameter and return types:
```java
MethodMatcher matcher = new MethodMatcher()
    .returnType(String.class)
    .paramTypes(int.class, boolean.class);
```

### Caching Strategy

UnobfuscatorCache automatically caches discovered classes:

```java
public Class<?> getClass(ClassLoader loader, String key, 
                         FunctionCall<Class<?>> functionCall) {
    // Check cache first
    String cachedClassName = sPrefsCacheHooks.getString(key, null);
    if (cachedClassName != null) {
        return XposedHelpers.findClass(cachedClassName, loader);
    }
    
    // Not cached, perform discovery
    Class<?> result = functionCall.call();
    
    // Save to cache
    saveClass(key, result);
    
    return result;
}
```

**Benefits:**
- Faster subsequent loads
- Survives app restarts
- Version-specific caching
- Automatic cache invalidation on TikTok update

## Version Compatibility

### Version Detection

```java
PackageInfo packageInfo = packageManager.getPackageInfo(
    mApp.getPackageName(), 0
);
String currentVersion = packageInfo.versionName;
```

### Supported Versions

The module declares supported versions in `arrays.xml`:
```xml
<string-array name="supported_versions_tkk">
    <item>43.xx</item>
    <item>44.xx</item>
    <item>45.xx</item>
</string-array>
```

### Bypass Version Check

If a version is unsupported, the module can still work if:
1. `bypass_version_check` preference is enabled
2. Expiration version bypass is applied

```java
if (!isSupported) {
    disableExpirationVersion(classLoader);
    if (!pref.getBoolean("bypass_version_check", false)) {
        throw new Exception("Unsupported version");
    }
}
```

## Testing Your Hooks

### Enable Debug Logging

```java
if (!prefs.getBoolean("enablelogs", false)) return;
logDebug("Your debug message");
```

### Check Xposed Logs

```bash
adb logcat -s Xposed
```

### Verify Hooks

```java
@Override
protected void afterHookedMethod(MethodHookParam param) {
    logDebug("Method called: " + param.method.getName());
    logDebug("Parameters: " + Arrays.toString(param.args));
    logDebug("Result: " + param.getResult());
}
```

## Adding New Features

### Step-by-Step Guide

1. **Create Feature Class:**
```java
public class MyNewFeature extends Feature {
    public MyNewFeature(ClassLoader classLoader, XSharedPreferences preferences) {
        super(classLoader, preferences);
    }

    @Override
    public void doHook() throws Throwable {
        if (!prefs.getBoolean("my_new_feature", false)) return;
        logDebug("Initializing My New Feature");
        
        // Your implementation
        hookTargetClass();
        
        logDebug("My New Feature initialized");
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "My New Feature";
    }
}
```

2. **Add Unobfuscator Method (if needed):**
```java
public synchronized static Class<?> loadMyTargetClass(ClassLoader classLoader) 
        throws Exception {
    return UnobfuscatorCache.getInstance().getClass(
        classLoader, 
        "MyTargetClass", 
        () -> {
            try {
                return XposedHelpers.findClass(
                    "com.ss.android.ugc.aweme.my.TargetClass", 
                    classLoader
                );
            } catch (Throwable e) {
                return findFirstClassUsingStrings(
                    classLoader,
                    StringMatchType.Contains,
                    "unique_identifier"
                );
            }
        }
    );
}
```

3. **Register in FeatureLoader:**
```java
var classes = new Class<?>[]{
    DebugFeature.class,
    VideoDownload.class,
    AdBlocker.class,
    AutoPlayControl.class,
    MyNewFeature.class  // Add here
};
```

4. **Add Preference:**
Add to preferences XML and UI.

5. **Test:**
- Build the module
- Install and enable
- Test with debug logging
- Verify functionality

## Best Practices

### 1. Graceful Fallbacks
Always provide fallback mechanisms:
```java
try {
    // Try standard approach
} catch (Throwable e1) {
    try {
        // Try alternative approach
    } catch (Throwable e2) {
        logDebug("All approaches failed", e2);
        return;
    }
}
```

### 2. Null Checks
Always check for null:
```java
if (videoClass == null) {
    log("Video class not found");
    return;
}
```

### 3. Performance
Minimize performance impact:
- Cache discovered classes/methods
- Use early returns
- Avoid heavy operations in hot paths

### 4. Logging
Use appropriate log levels:
```java
logDebug("Detailed info");  // Only when debug enabled
log("Important info");       // Always logged
```

## Troubleshooting

### Common Issues

**1. Class Not Found:**
- Check TikTok version compatibility
- Verify class name is correct
- Use DexKit for obfuscated classes
- Check Xposed logs for errors

**2. Method Not Hooking:**
- Verify method signature
- Check parameter types
- Ensure method exists in current version
- Try broader method search

**3. Feature Not Working:**
- Check preference is enabled
- Verify TikTok is in module scope
- Check for exceptions in logs
- Test on different TikTok versions

### Debug Checklist

- [ ] LSPosed/EdXposed properly installed
- [ ] Module enabled in framework
- [ ] TikTok in module scope
- [ ] Device rebooted after enabling
- [ ] Debug logs enabled
- [ ] Checking correct log output
- [ ] TikTok version is supported
- [ ] Preferences correctly set

## Future Enhancements

Potential features to implement:

1. **Video Quality Selection**
   - Allow choosing download quality
   - Hook bitrate selection methods

2. **UI Customization**
   - Custom themes
   - Color modifications
   - Layout changes

3. **Download History**
   - Track downloaded videos
   - Database integration
   - Search and management

4. **Enhanced Privacy**
   - Tracking prevention
   - Analytics blocking
   - Data collection control

## References

- [LSPosed Documentation](https://github.com/LSPosed/LSPosed/wiki)
- [DexKit Documentation](https://github.com/LuckyPray/DexKit)
- [Xposed Framework API](https://api.xposed.info/reference/packages.html)

---

**Last Updated:** December 2025
