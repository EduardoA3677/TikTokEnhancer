# Deep Smali Analysis - Corrections and New Hooks

## Analysis Date: December 24, 2025 (Updated)

### Critical Findings

## 1. Video Download - VERIFIED CORRECT ✅

**Class:** `com.ss.android.ugc.aweme.feed.model.Video`
**Location:** `./smali_classes25/com/ss/android/ugc/aweme/feed/model/Video.smali`

### Verified Methods:
```smali
.method public getDownloadAddr()Lcom/ss/android/ugc/aweme/base/model/UrlModel;
    # Line 1141
    
.method public getDownloadNoWatermarkAddr()Lcom/ss/android/ugc/aweme/base/model/UrlModel;
    # Line 1149
    # Returns: Lcom/ss/android/ugc/aweme/base/model/UrlModel;
```

### Implementation Details:
```smali
# Line 1149-1155
.method public getDownloadNoWatermarkAddr()Lcom/ss/android/ugc/aweme/base/model/UrlModel;
    .locals 1
    iget-object v0, p0, Lcom/ss/android/ugc/aweme/feed/model/Video;->downloadNoWatermarkAddr:Lcom/ss/android/ugc/aweme/base/model/UrlModel;
    return-object v0
.end method
```

**Status:** Current implementation is CORRECT ✅

## 2. Download Service - NEW HOOK POINT

**Class:** `com.ss.android.ugc.aweme.download.DownloadAwemeVideoServiceImpl`
**Location:** `./smali_classes25/com/ss/android/ugc/aweme/download/DownloadAwemeVideoServiceImpl.smali`

### Key Method:
```java
.method public final LIZ(Landroid/content/Context;Lcom/ss/android/ugc/aweme/feed/model/Aweme;Ljava/lang/String;LX/0tk0;)V
```

This method:
- Takes Context, Aweme, filePathDir, and listener
- Calls `getVideo().getPlayAddrH264()` to get video URL
- Handles actual download process

**Recommendation:** Hook this method to intercept download calls

## 3. Ad Detection - VERIFIED CORRECT ✅

**Class:** `com.ss.android.ugc.aweme.feed.model.Aweme`
**Location:** `./smali_classes23/com/ss/android/ugc/aweme/feed/model/Aweme.smali`

### Verified Methods:
```smali
.method public isAd()Z
    # Line 11936
    
.method public isAdDescHandle()Z
    # Line 11957
    
.method public isAdTraffic()Z
    # Line 11965
```

### Fields (verified):
- `.field public isAd:Z` - Line 1125
- `.field public commerceRerankInfo:...CommerceRerankInfo;` - Line 485
- `.field public commerceStickerInfo:...CommerceStickerInfo;` - Line 491
- `.field public linkAdData:...LinkData;` - Line 1475
- `.field public promoteModel:...PromoteModel;` - Line 2160

**Status:** Current implementation is CORRECT ✅

## 4. NEW HOOK: Share/Save Video Functionality

**Class Pattern:** `com.ss.android.ugc.aweme.share.utils.ShareHelper`
**Related Methods:**
- `shareVideo$2` - Handles video sharing
- `downloadVideoAsync$2` - Handles async video download

**Classes Found:**
- `X/0Ic2.smali` - ShareHelper$shareVideo$2
- `X/0IrB.smali` - ShareHelper$downloadVideoAsync$2  
- `X/0Ic1.smali` - ShareHelper$shareVideo$2$1

These classes contain the actual share and download logic that users trigger.

**Recommendation:** Hook these to provide custom download options

## 5. NEW HOOK: Video Player Auto-Play

**Classes Found:**
```
./smali_classes17/com/ss/videoarch/liveplayer/VideoLiveManager.smali
./smali_classes2/com/ss/android/ugc/playerkit/injector/InjectedConfigManager.smali
```

**Recommendation:** Hook VideoLiveManager and InjectedConfigManager for play control

## 6. Story Model - VERIFIED CORRECT ✅

**Class:** `com.ss.android.ugc.aweme.story.model.Story`
**Location:** `./smali/com/ss/android/ugc/aweme/story/model/Story.smali`

### Fields:
```smali
.field public awemes:Ljava/util/List;
    # Signature: Ljava/util/List<Lcom/ss/android/ugc/aweme/feed/model/Aweme;>;
    
.field public userInfo:Lcom/ss/android/ugc/aweme/profile/model/User;
```

**Status:** Current implementation is CORRECT ✅

## 7. NEW HOOK: Editor Share Helper

**Class:** `com.ss.android.ugc.aweme.EditorShareHelper`
**Interface:** `com.ss.android.ugc.aweme.IEditorShareHelper`
**Location:** `./smali_classes14/`

This handles sharing from the video editor - could be used for custom save options.

## 8. Bitrate/Quality - NEEDS CORRECTION ⚠️

**Current Target:** `com.ss.android.ugc.aweme.bitrateselector.impl.DTBitrateSelectorServiceImpl`

**Verified Classes:**
```
./smali_classes25/com/ss/android/ugc/aweme/video/bitrate/RateSettingCombineModel.smali
./smali_classes25/com/ss/android/ugc/aweme/video/bitrate/GearSet.smali
./smali_classes25/com/ss/android/ugc/aweme/simkit/model/bitrateselect/AutoBitrateSet.smali
./smali_classes25/com/ss/android/ugc/aweme/simkit/model/bitrateselect/RateSettingsResponse.smali
```

**Recommendation:** 
- Hook `RateSettingCombineModel` for quality settings
- Hook `GearSet` for bitrate gear selection
- Hook Video.getBitRate() to force highest quality

## 9. Analytics/Tracking - NEEDS EXPANSION ⚠️

**Current:** Generic analytics hooking

**Better Targets:**
```
./smali_classes12/com/ss/android/ugc/aweme/analytics/*
./smali_classes12/com/ss/android/ugc/aweme/im/service/analytics/*
./smali_classes22/com/google/firebase/analytics/FirebaseAnalytics.smali
```

**Recommendation:**
- Hook FirebaseAnalytics directly
- Hook specific analytics services in aweme.analytics package
- Block telemetry upload methods

## 10. NEW HOOK: Network Download Utility

**Class:** `com.ss.android.common.util.NetworkUtils`
**Method:** `downloadVideo(...)` 
**Location:** `./smali_classes29/com/ss/android/common/util/NetworkUtils.smali:599`

This is a low-level download method that could be hooked for all video downloads.

## Summary of Corrections Needed

### Currently Correct ✅
1. VideoDownload - getDownloadNoWatermarkAddr()
2. AdBlocker - isAd() and related fields
3. StoryDownload - Story model

### Need Correction/Enhancement ⚠️
1. **VideoQuality** - Add hooks for RateSettingCombineModel and GearSet
2. **Privacy** - Add FirebaseAnalytics blocking
3. **AutoPlayControl** - Add VideoLiveManager hooks

### New Hooks to Implement 🆕
1. **DownloadAwemeVideoServiceImpl.LIZ()** - Intercept download service
2. **ShareHelper classes** - Custom download from share
3. **EditorShareHelper** - Save from editor
4. **NetworkUtils.downloadVideo()** - Low-level download hook

## Implementation Priority

1. **HIGH:** Correct VideoQuality hooks (RateSettingCombineModel, GearSet)
2. **HIGH:** Add DownloadAwemeVideoServiceImpl hook
3. **MEDIUM:** Enhance Privacy with FirebaseAnalytics
4. **MEDIUM:** Add ShareHelper hooks for custom downloads
5. **LOW:** EditorShareHelper integration
6. **LOW:** NetworkUtils low-level hook

---

**Analysis Complete - Ready for Implementation**
