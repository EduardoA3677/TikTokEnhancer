# TikTok Smali Code Analysis Results

## Analysis Date: December 24, 2025

### Repository Analyzed
- **Source:** https://github.com/Eduardob3677/com_zhiliaoapp_musically_6
- **Package:** com.zhiliaoapp.musically (TikTok)
- **Total Files:** 384,323 files

## Key Classes and Methods Found

### 1. Video Download (No Watermark)

#### Class Location
```
./smali_classes25/com/ss/android/ugc/aweme/feed/model/Video.smali
```

#### Key Fields
```java
.field public downloadAddr:Lcom/ss/android/ugc/aweme/base/model/UrlModel;
    .annotation runtime LX/0VjZ;
        value = "download_addr"
    .end annotation
.end field

.field public downloadNoWatermarkAddr:Lcom/ss/android/ugc/aweme/base/model/UrlModel;
    .annotation runtime LX/0VjZ;
        value = "download_no_watermark_addr"
    .end annotation
.end field
```

#### Key Methods
- `getDownloadAddr()Lcom/ss/android/ugc/aweme/base/model/UrlModel;` - Line 1141
- `getDownloadNoWatermarkAddr()Lcom/ss/android/ugc/aweme/base/model/UrlModel;` - Line 1149

**Implementation Target:**
```java
Class: com.ss.android.ugc.aweme.feed.model.Video
Field: downloadNoWatermarkAddr
Method: getDownloadNoWatermarkAddr()
```

### 2. Ad Detection

#### Class Location
```
./smali_classes23/com/ss/android/ugc/aweme/feed/model/Aweme.smali
```

#### Key Fields
```java
.field public isAd:Z

.field public commerceRerankInfo:Lcom/ss/android/ugc/aweme/feed/model/commercialize/CommerceRerankInfo;

.field public commerceStickerInfo:Lcom/ss/android/ugc/aweme/commercialize/model/CommerceStickerInfo;

.field public linkAdData:Lcom/ss/android/ugc/aweme/commercialize/model/LinkData;

.field public promoteModel:Lcom/ss/android/ugc/aweme/commercialize/model/promote/PromoteModel;
```

#### Key Methods
- `isAd()Z` - Line 11936
- `isAdDescHandle()Z` - Line 11957
- `isAdTraffic()Z` - Line 11965
- `getAdCommentStruct()Lcom/ss/android/ugc/aweme/commercialize/model/CommentStruct;` - Line 5789
- `getCommerceRerankInfo()` - Line 6570
- `getCommerceStickerInfo()` - Line 6578
- `getLinkAdData()` - Line 8451
- `getPromoteModel()` - Line 9811

**Implementation Target:**
```java
Class: com.ss.android.ugc.aweme.feed.model.Aweme
Field: isAd (boolean)
Methods: isAd(), isAdTraffic()
Commercial fields: commerceRerankInfo, commerceStickerInfo, linkAdData, promoteModel
```

### 3. Story Model

#### Class Location
```
./smali/com/ss/android/ugc/aweme/story/model/Story.smali
```

#### Key Fields
```java
.field public awemes:Ljava/util/List;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/util/List<",
            "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
            ">;"
        }
    .end annotation
.end field

.field public userInfo:Lcom/ss/android/ugc/aweme/profile/model/User;
```

**Implementation Target:**
```java
Class: com.ss.android.ugc.aweme.story.model.Story
Field: awemes (List<Aweme>)
Note: Story contains a list of Aweme objects, each with video data
```

### 4. Video Quality / Bitrate

#### Class Locations
```
./smali_classes25/com/ss/android/ugc/aweme/video/bitrate/RateSettingCombineModel.smali
./smali_classes25/com/ss/android/ugc/aweme/video/bitrate/GearSet.smali
./smali_classes25/com/ss/android/ugc/aweme/simkit/model/bitrateselect/AutoBitrateSet.smali
./smali_classes25/com/ss/android/ugc/aweme/simkit/model/bitrateselect/RateSettingsResponse.smali
./smali_classes25/com/ss/android/ugc/aweme/simkit/model/bitrateselect/AutoBitrateCurve.smali
./smali_classes25/com/ss/android/ugc/aweme/bitrateselector/impl/DTBitrateSelectorServiceImpl.smali
```

**Implementation Target:**
```java
Classes:
- com.ss.android.ugc.aweme.video.bitrate.RateSettingCombineModel
- com.ss.android.ugc.aweme.video.bitrate.GearSet
- com.ss.android.ugc.aweme.bitrateselector.impl.DTBitrateSelectorServiceImpl
```

### 5. Analytics / Tracking

#### Class Patterns Found
```
com.ss.android.ugc.aweme.analytics.*
com.ss.android.ugc.aweme.im.service.analytics.*
com.google.firebase.analytics.FirebaseAnalytics
```

**Implementation Target:**
```java
Classes:
- com.ss.android.ugc.aweme.analytics.* (various analytics services)
- Firebase Analytics for Google tracking
```

## BitRate Model in Video

From the Video class analysis:
```java
.field public bitRate:Ljava/util/List;
    .annotation runtime LX/0VjZ;
        value = "bit_rate"
    .end annotation
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/util/List<",
            "Lcom/ss/android/ugc/aweme/feed/model/BitRate;",
            ">;"
        }
    .end annotation
.end field
```

## Recommended Hook Strategy

### 1. VideoDownload Hook
**Target:** `com.ss.android.ugc.aweme.feed.model.Video.getDownloadNoWatermarkAddr()`
- Hook this method to intercept and store no-watermark URLs
- Can also access field directly: `downloadNoWatermarkAddr`

### 2. AdBlocker Hook
**Target:** `com.ss.android.ugc.aweme.feed.model.Aweme.isAd()`
- Hook this method to detect ads
- Also check fields: `isAd`, `commerceStickerInfo`, `linkAdData`, `promoteModel`
- Filter feed items where `isAd == true`

### 3. StoryDownload Hook
**Target:** `com.ss.android.ugc.aweme.story.model.Story`
- Access `awemes` field to get list of story videos
- Each Aweme contains Video object with download URLs

### 4. VideoQuality Hook
**Target:** `com.ss.android.ugc.aweme.bitrateselector.impl.DTBitrateSelectorServiceImpl`
- Hook bitrate selection methods
- Force selection of highest quality bitrate from `bitRate` list in Video

### 5. Privacy Hook
**Target:** Analytics classes
- Hook all methods in `com.ss.android.ugc.aweme.analytics.*`
- Block method execution to prevent tracking

## Implementation Priority

1. ✅ **VideoDownload** - Exact class and method identified
2. ✅ **AdBlocker** - Exact class, field and methods identified
3. ⚠️ **StoryDownload** - Story model found, needs viewer class
4. ⚠️ **VideoQuality** - Bitrate classes found, need selector logic
5. ⚠️ **Privacy** - Multiple analytics classes, need comprehensive hooks

## Next Steps

1. Update hook implementations to use exact class names from smali
2. Target specific methods identified in analysis
3. Test hooks with actual TikTok app
4. Refine based on runtime behavior

---

**Analysis Complete**
