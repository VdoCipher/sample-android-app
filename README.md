# Sample android app
Sample app demonstrating video playback using the vdocipher android sdk

## Integrating the vdocipher sdk into your android app
### Add VdoCipher maven repo to app module's `build.gradle`

```gradle
repositories {
    // other repo, e.g. mavenCentral()
    maven {
        url "https://github.com/VdoCipher/maven-repo/raw/master/repo"
    }
}
```

### Add dependency

```gradle
// use the latest available version
def vdocipher_sdk_version = '1.28.10'
implementation 'com.vdocipher.aegis:vdocipher-android:' + vdocipher_sdk_version
```

VdoCipher SDK also provides Google Cast integration for your app.

### Enable Java 8 support

You also need to ensure Java 8 support is enabled by adding the following block to each of your app module's `build.gradle` file inside the `android` block:

```gradle
compileOptions {
    sourceCompatibility JavaVersion.VERSION_1_8
    targetCompatibility JavaVersion.VERSION_1_8
}
```

## Issues

Please send all issues and feedback to support@vdocipher.com
