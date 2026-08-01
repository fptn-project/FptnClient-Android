## Setup
### Java

JDK 17 is required (`app/build.gradle.kts` sets the Java toolchain to 17). Debian 13 has no `openjdk-17` package, so install Eclipse Temurin 17 manually from [Adoptium](https://adoptium.net/temurin/releases/):

```bash
wget -O temurin17.tar.gz "https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse?project=jdk"
mkdir -p ~/jdks
tar -xzf temurin17.tar.gz -C ~/jdks
```
Set `JAVA_HOME` to the unpacked directory and select JDK 17 as the Gradle JDK in Android Studio (`Settings -> Build, Execution, Deployment -> Build Tools -> Gradle -> Gradle JDK`). **Using a newer JDK (e.g. 21) breaks the build with class file has wrong version 65.0, should be 61.0.**

### Formatting
- Install ktlint plugin
- For automated formatting, Install Ktlint plugin to IDEA then you can go to `Settings -> Tools -> KtLint -> Distract free`.
- For manual formatting you can use `Command+Option+Enter`.

### Wildcard imports
For disabling wildcards go to `Settings -> Editor -> Code style -> Kotlin -> Imports`
Choose `Use single name import` for all cases and uncheck all checkboxes below.


### Submodules

```bash
git submodule update --init --recursive
```

### Install conan

(For Windows, refer to these [instructions](https://github.com/batchar2/fptn/tree/master/deploy/windows) to install all required dependencies.)


```bash
pip install conan numpy
```



```bash
sudo apt install clang
```

Create profile, get your conan home path

```bash
conan config home
```

```bash
conan profile detect -f
```

Go to the following path and open the profiles folder.
For example, on my system, the path is:
`~/.conan2/profiles`

Then, create a file named `android-studio` with the following content:

```bash
include(default)

[settings]
os=Android
os.api_level=28
compiler=clang
compiler.version=20
compiler.libcxx=c++_static
compiler.cppstd=17

[tool_requires]
*: android-ndk/r28b
```
[Hack] If above config doesn't help, install ndk 29.0.13599879 and point it out in 3 places:
  - app/build.gradle.kts in android section, for example
    ```android {
        ...
        ndkVersion = "28.1.13356709"
        ...
    ```
  - local.properties
    ```
        ndk.dir=/Users/<user-name>/Library/Android/sdk/ndk/28.1.13356709
    ```
  - ~/.conan2/profiles/android-studio in conf section
    ```
        ...
        [conf]
        tools.android:ndk_path=/Users/ddeviatilov/Library/Android/sdk/ndk/28.1.13356709
        ...
    ```


## Ktlint formatting
Check: `./gradlew ktlintCheck`

Format: `./gradlew ktlintFormat`

## Detekt check
Check: `./gradlew detektCheck`

## Dependency soring check
Sort: `./gradlew sortDependencies`

Check: `./gradlew checkSortDependencies`
