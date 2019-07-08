# Sample android app
Sample app demonstrating video playback using the vdocipher android sdk

## Integrating the vdocipher sdk into your android app
### Add VdoCipher maven repo to app module's `build.gradle`

```
repositories {
    // other repo, e.g. jcenter()
    maven {
        url "https://github.com/VdoCipher/maven-repo/raw/master/repo"
    }
}
```

### Add dependency

```
// use the latest available version
implementation 'com.vdocipher.aegis:vdocipher-android:1.0.0-beta7'
```

### Add cast plugin dependency

Download the cast plugin sdk (aar file) and integrate into your Android Studio project using the following steps:

In Android Studio -> File -> New Module -> Import JAR/AAR package -> locate the downloaded aar file -> Finish.

Then add a dependency to the newly added cast plugin module in your cast app module's build.gradle file.

```
implementation project(':vdocipher-cast')
implementation 'com.google.android.gms:play-services-cast-framework:16.2.0'
```

## Issues

Please send all issues and feedback to support@vdocipher.com
