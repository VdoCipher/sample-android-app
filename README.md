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
implementation 'com.vdocipher.aegis:vdocipher-android:1.10.1'
```

### Add cast plugin dependency

If you also need Google Cast integration for your app, add a dependency to the cast plugin as well.

Add the dependency in your cast app module's `build.gradle` file.

```
def vdocipher_sdk_version = '1.10.1'
```

```
implementation 'com.vdocipher.aegis:vdocipher-android:' + vdocipher_sdk_version
implementation 'com.vdocipher.aegis:vdocipher-cast:' + vdocipher_sdk_version
implementation 'com.google.android.gms:play-services-cast-framework:16.2.0'
```

### Enable Java 8 support

You also need to ensure Java 8 support is enabled by adding the following block to each of your app module's `build.gradle` file inside the `android` block:

```
compileOptions {
    sourceCompatibility JavaVersion.VERSION_1_8
    targetCompatibility JavaVersion.VERSION_1_8
}
```

## Issues

Please send all issues and feedback to support@vdocipher.com
