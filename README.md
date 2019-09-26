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
implementation 'com.vdocipher.aegis:vdocipher-android:1.1.0'
```

### Add cast plugin dependency

If you also need Google Cast integration for your app, add a dependency to the cast plugin as well.

Add the dependency in your cast app module's `build.gradle` file.

```
implementation 'com.vdocipher.aegis:vdocipher-android:1.1.0'
implementation 'com.vdocipher.aegis:vdocipher-cast:1.1.0'
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
