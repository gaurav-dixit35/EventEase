# Step 1 — complete file contents

Every project text file is reproduced below. Binary assets are listed with SHA-256 hashes; their actual files are bundled in the project. Empty .gitkeep files preserve the prescribed package structure.

## .gitignore

``
.gradle/
**/build/
local.properties
.idea/
*.iml
.DS_Store

``

## app/build.gradle

``
plugins {
    id 'com.android.application'
}

android {
    namespace 'com.timscdr.eventease'
    compileSdk 35

    defaultConfig {
        applicationId 'com.timscdr.eventease'
        minSdk 24
        targetSdk 34
        versionCode 1
        versionName '1.0'
    }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
}

dependencies {
    implementation 'androidx.appcompat:appcompat:1.7.0'
    implementation 'com.google.android.material:material:1.12.0'
    implementation 'androidx.core:core:1.13.1'
    implementation 'androidx.lifecycle:lifecycle-viewmodel:2.8.7'
    implementation 'androidx.lifecycle:lifecycle-livedata:2.8.7'
    implementation 'androidx.room:room-runtime:2.6.1'
    annotationProcessor 'androidx.room:room-compiler:2.6.1'
    implementation 'androidx.recyclerview:recyclerview:1.3.2'
    implementation 'androidx.swiperefreshlayout:swiperefreshlayout:1.1.0'
    implementation 'com.google.zxing:core:3.5.3'
    implementation 'com.github.bumptech.glide:glide:4.16.0'
    annotationProcessor 'com.github.bumptech.glide:compiler:4.16.0'
}

``

## app/src/main/AndroidManifest.xml

``
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:allowBackup="false"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.EventEase">
        <activity
            android:name=".ui.splash.SplashActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>

``

## app/src/main/java/com/timscdr/eventease/data/dao/.gitkeep

``

``

## app/src/main/java/com/timscdr/eventease/data/database/.gitkeep

``

``

## app/src/main/java/com/timscdr/eventease/data/entity/.gitkeep

``

``

## app/src/main/java/com/timscdr/eventease/receiver/.gitkeep

``

``

## app/src/main/java/com/timscdr/eventease/repository/.gitkeep

``

``

## app/src/main/java/com/timscdr/eventease/ui/auth/.gitkeep

``

``

## app/src/main/java/com/timscdr/eventease/ui/dashboard/.gitkeep

``

``

## app/src/main/java/com/timscdr/eventease/ui/event/.gitkeep

``

``

## app/src/main/java/com/timscdr/eventease/ui/myevents/.gitkeep

``

``

## app/src/main/java/com/timscdr/eventease/ui/organizer/.gitkeep

``

``

## app/src/main/java/com/timscdr/eventease/ui/profile/.gitkeep

``

``

## app/src/main/java/com/timscdr/eventease/ui/registration/.gitkeep

``

``

## app/src/main/java/com/timscdr/eventease/ui/splash/SplashActivity.java

``
package com.timscdr.eventease.ui.splash;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.timscdr.eventease.R;

/** Step 1 theme preview; session routing is added with the authentication slice. */
public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
    }
}

``

## app/src/main/java/com/timscdr/eventease/util/.gitkeep

``

``

## app/src/main/java/com/timscdr/eventease/viewmodel/.gitkeep

``

``

## app/src/main/res/drawable/gold_divider.xml

``
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="@color/accent" />
    <size android:height="1dp" />
</shape>

``

## app/src/main/res/font/inter.ttf

Binary file, 341396 bytes. SHA-256: 8727419cb3945518c0e5954051875eabfee5d01316d6f0abdec0ed8fe760b67e

## app/src/main/res/font/playfair_display.ttf

Binary file, 194556 bytes. SHA-256: a8081b8d2f9b06dcaabc2e91ec22b1f3b586ed44d6c96d47138558b299f3a150

## app/src/main/res/layout/activity_splash.xml

``
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/background"
    android:gravity="center"
    android:orientation="vertical"
    android:padding="32dp">
    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/institution_short"
        android:textAppearance="@style/TextAppearance.EventEase.Body" />
    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="12dp"
        android:text="@string/app_name"
        android:textAppearance="@style/TextAppearance.EventEase.Heading" />
    <View
        android:layout_width="120dp"
        android:layout_height="1dp"
        android:layout_marginTop="12dp"
        android:background="@drawable/gold_divider" />
    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="24dp"
        android:gravity="center"
        android:text="@string/preview_description"
        android:textAppearance="@style/TextAppearance.EventEase.Body" />
    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="12dp"
        android:gravity="center"
        android:text="@string/preview_status"
        android:textAppearance="@style/TextAppearance.EventEase.Body" />
</LinearLayout>

``

## app/src/main/res/menu/.gitkeep

``

``

## app/src/main/res/values/colors.xml

``
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="primary">#0D2545</color>
    <color name="accent">#C9A227</color>
    <color name="background">#FAF7F0</color>
    <color name="surface">#FFFFFF</color>
    <color name="text_primary">#1C1C1C</color>
    <color name="text_secondary">#5A5A5A</color>
    <color name="success">#2E7D32</color>
    <color name="error">#B3261E</color>
    <color name="on_primary">#FFFFFF</color>
    <color name="on_accent">#0D2545</color>
    <color name="on_error">#FFFFFF</color>
</resources>

``

## app/src/main/res/values/strings.xml

``
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">EventEase</string>
    <string name="institution_short">TIMSCDR</string>
    <string name="preview_description">College Event Management &amp; Registration</string>
    <string name="preview_status">Project foundation ready</string>
</resources>

``

## app/src/main/res/values/styles.xml

``
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="TextAppearance.EventEase.Heading" parent="TextAppearance.MaterialComponents.Headline6">
        <item name="fontFamily">@font/playfair_display</item>
        <item name="android:fontFamily">@font/playfair_display</item>
        <item name="android:textColor">@color/text_primary</item>
        <item name="android:textSize">28sp</item>
    </style>
    <style name="TextAppearance.EventEase.Body" parent="TextAppearance.MaterialComponents.Body1">
        <item name="fontFamily">@font/inter</item>
        <item name="android:fontFamily">@font/inter</item>
        <item name="android:textColor">@color/text_secondary</item>
        <item name="android:textSize">16sp</item>
    </style>
    <style name="ShapeAppearance.EventEase" parent="ShapeAppearance.MaterialComponents.MediumComponent">
        <item name="cornerFamily">rounded</item>
        <item name="cornerSize">10dp</item>
    </style>
    <style name="Widget.EventEase.Button" parent="Widget.MaterialComponents.Button">
        <item name="backgroundTint">@color/primary</item>
        <item name="android:textColor">@color/on_primary</item>
        <item name="android:textAllCaps">false</item>
        <item name="cornerRadius">10dp</item>
    </style>
    <style name="Widget.EventEase.Card" parent="Widget.MaterialComponents.CardView">
        <item name="cardBackgroundColor">@color/surface</item>
        <item name="cardCornerRadius">10dp</item>
        <item name="cardElevation">0dp</item>
    </style>
</resources>

``

## app/src/main/res/values/themes.xml

``
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.EventEase" parent="Theme.MaterialComponents.DayNight.NoActionBar">
        <item name="colorPrimary">@color/primary</item>
        <item name="colorPrimaryVariant">@color/primary</item>
        <item name="colorOnPrimary">@color/on_primary</item>
        <item name="colorSecondary">@color/accent</item>
        <item name="colorSecondaryVariant">@color/accent</item>
        <item name="colorOnSecondary">@color/on_accent</item>
        <item name="colorAccent">@color/accent</item>
        <item name="android:colorBackground">@color/background</item>
        <item name="android:windowBackground">@color/background</item>
        <item name="colorOnBackground">@color/text_primary</item>
        <item name="colorSurface">@color/surface</item>
        <item name="colorOnSurface">@color/text_primary</item>
        <item name="colorError">@color/error</item>
        <item name="colorOnError">@color/on_error</item>
        <item name="android:textColorPrimary">@color/text_primary</item>
        <item name="android:textColorSecondary">@color/text_secondary</item>
        <item name="fontFamily">@font/inter</item>
        <item name="android:fontFamily">@font/inter</item>
        <item name="textAppearanceHeadline4">@style/TextAppearance.EventEase.Heading</item>
        <item name="textAppearanceHeadline6">@style/TextAppearance.EventEase.Heading</item>
        <item name="textAppearanceBody1">@style/TextAppearance.EventEase.Body</item>
        <item name="shapeAppearanceSmallComponent">@style/ShapeAppearance.EventEase</item>
        <item name="shapeAppearanceMediumComponent">@style/ShapeAppearance.EventEase</item>
        <item name="materialButtonStyle">@style/Widget.EventEase.Button</item>
        <item name="materialCardViewStyle">@style/Widget.EventEase.Card</item>
        <item name="android:statusBarColor">@color/primary</item>
        <item name="android:navigationBarColor">@color/primary</item>
        <item name="android:windowLightStatusBar">false</item>
    </style>
</resources>

``

## app/src/main/res/values-night/colors.xml

``
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="primary">#1B3A63</color>
    <color name="accent">#D4AF37</color>
    <color name="background">#0F1420</color>
    <color name="surface">#1A2236</color>
    <color name="text_primary">#EDEAE0</color>
    <color name="text_secondary">#A8ADBB</color>
    <color name="success">#66BB6A</color>
    <color name="error">#E4685D</color>
    <color name="on_primary">#EDEAE0</color>
    <color name="on_accent">#0D2545</color>
    <color name="on_error">#0F1420</color>
</resources>

``

## build.gradle

``
plugins {
    id 'com.android.application' version '8.7.3' apply false
}

``

## gradle/wrapper/gradle-wrapper.jar

Binary file, 43504 bytes. SHA-256: 498495120a03b9a6ab5d155f5de3c8f0d986a449153702fb80fc80e134484f17

## gradle/wrapper/gradle-wrapper.properties

``
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.9-bin.zip
distributionSha256Sum=d725d707bfabd4dfdc958c624003b3c80accc03f7037b5122c4b1d0ef15cecab
networkTimeout=60000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists

``

## gradle.properties

``
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
android.nonTransitiveRClass=true

``

## gradlew

``
#!/bin/sh

#
# Copyright © 2015-2021 the original authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# SPDX-License-Identifier: Apache-2.0
#

##############################################################################
#
#   Gradle start up script for POSIX generated by Gradle.
#
#   Important for running:
#
#   (1) You need a POSIX-compliant shell to run this script. If your /bin/sh is
#       noncompliant, but you have some other compliant shell such as ksh or
#       bash, then to run this script, type that shell name before the whole
#       command line, like:
#
#           ksh Gradle
#
#       Busybox and similar reduced shells will NOT work, because this script
#       requires all of these POSIX shell features:
#         * functions;
#         * expansions «$var», «${var}», «${var:-default}», «${var+SET}»,
#           «${var#prefix}», «${var%suffix}», and «$( cmd )»;
#         * compound commands having a testable exit status, especially «case»;
#         * various built-in commands including «command», «set», and «ulimit».
#
#   Important for patching:
#
#   (2) This script targets any POSIX shell, so it avoids extensions provided
#       by Bash, Ksh, etc; in particular arrays are avoided.
#
#       The "traditional" practice of packing multiple parameters into a
#       space-separated string is a well documented source of bugs and security
#       problems, so this is (mostly) avoided, by progressively accumulating
#       options in "$@", and eventually passing that to Java.
#
#       Where the inherited environment variables (DEFAULT_JVM_OPTS, JAVA_OPTS,
#       and GRADLE_OPTS) rely on word-splitting, this is performed explicitly;
#       see the in-line comments for details.
#
#       There are tweaks for specific operating systems such as AIX, CygWin,
#       Darwin, MinGW, and NonStop.
#
#   (3) This script is generated from the Groovy template
#       https://github.com/gradle/gradle/blob/HEAD/platforms/jvm/plugins-application/src/main/resources/org/gradle/api/internal/plugins/unixStartScript.txt
#       within the Gradle project.
#
#       You can find Gradle at https://github.com/gradle/gradle/.
#
##############################################################################

# Attempt to set APP_HOME

# Resolve links: $0 may be a link
app_path=$0

# Need this for daisy-chained symlinks.
while
    APP_HOME=${app_path%"${app_path##*/}"}  # leaves a trailing /; empty if no leading path
    [ -h "$app_path" ]
do
    ls=$( ls -ld "$app_path" )
    link=${ls#*' -> '}
    case $link in             #(
      /*)   app_path=$link ;; #(
      *)    app_path=$APP_HOME$link ;;
    esac
done

# This is normally unused
# shellcheck disable=SC2034
APP_BASE_NAME=${0##*/}
# Discard cd standard output in case $CDPATH is set (https://github.com/gradle/gradle/issues/25036)
APP_HOME=$( cd -P "${APP_HOME:-./}" > /dev/null && printf '%s
' "$PWD" ) || exit

# Use the maximum available, or set MAX_FD != -1 to use that value.
MAX_FD=maximum

warn () {
    echo "$*"
} >&2

die () {
    echo
    echo "$*"
    echo
    exit 1
} >&2

# OS specific support (must be 'true' or 'false').
cygwin=false
msys=false
darwin=false
nonstop=false
case "$( uname )" in                #(
  CYGWIN* )         cygwin=true  ;; #(
  Darwin* )         darwin=true  ;; #(
  MSYS* | MINGW* )  msys=true    ;; #(
  NONSTOP* )        nonstop=true ;;
esac

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar


# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        # IBM's JDK on AIX uses strange locations for the executables
        JAVACMD=$JAVA_HOME/jre/sh/java
    else
        JAVACMD=$JAVA_HOME/bin/java
    fi
    if [ ! -x "$JAVACMD" ] ; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
    fi
else
    JAVACMD=java
    if ! command -v java >/dev/null 2>&1
    then
        die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
    fi
fi

# Increase the maximum file descriptors if we can.
if ! "$cygwin" && ! "$darwin" && ! "$nonstop" ; then
    case $MAX_FD in #(
      max*)
        # In POSIX sh, ulimit -H is undefined. That's why the result is checked to see if it worked.
        # shellcheck disable=SC2039,SC3045
        MAX_FD=$( ulimit -H -n ) ||
            warn "Could not query maximum file descriptor limit"
    esac
    case $MAX_FD in  #(
      '' | soft) :;; #(
      *)
        # In POSIX sh, ulimit -n is undefined. That's why the result is checked to see if it worked.
        # shellcheck disable=SC2039,SC3045
        ulimit -n "$MAX_FD" ||
            warn "Could not set maximum file descriptor limit to $MAX_FD"
    esac
fi

# Collect all arguments for the java command, stacking in reverse order:
#   * args from the command line
#   * the main class name
#   * -classpath
#   * -D...appname settings
#   * --module-path (only if needed)
#   * DEFAULT_JVM_OPTS, JAVA_OPTS, and GRADLE_OPTS environment variables.

# For Cygwin or MSYS, switch paths to Windows format before running java
if "$cygwin" || "$msys" ; then
    APP_HOME=$( cygpath --path --mixed "$APP_HOME" )
    CLASSPATH=$( cygpath --path --mixed "$CLASSPATH" )

    JAVACMD=$( cygpath --unix "$JAVACMD" )

    # Now convert the arguments - kludge to limit ourselves to /bin/sh
    for arg do
        if
            case $arg in                                #(
              -*)   false ;;                            # don't mess with options #(
              /?*)  t=${arg#/} t=/${t%%/*}              # looks like a POSIX filepath
                    [ -e "$t" ] ;;                      #(
              *)    false ;;
            esac
        then
            arg=$( cygpath --path --ignore --mixed "$arg" )
        fi
        # Roll the args list around exactly as many times as the number of
        # args, so each arg winds up back in the position where it started, but
        # possibly modified.
        #
        # NB: a `for` loop captures its iteration list before it begins, so
        # changing the positional parameters here affects neither the number of
        # iterations, nor the values presented in `arg`.
        shift                   # remove old arg
        set -- "$@" "$arg"      # push replacement arg
    done
fi


# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
DEFAULT_JVM_OPTS='-Dfile.encoding=UTF-8 "-Xmx64m" "-Xms64m"'

# Collect all arguments for the java command:
#   * DEFAULT_JVM_OPTS, JAVA_OPTS, JAVA_OPTS, and optsEnvironmentVar are not allowed to contain shell fragments,
#     and any embedded shellness will be escaped.
#   * For example: A user cannot expect ${Hostname} to be expanded, as it is an environment variable and will be
#     treated as '${Hostname}' itself on the command line.

set -- \
        "-Dorg.gradle.appname=$APP_BASE_NAME" \
        -classpath "$CLASSPATH" \
        org.gradle.wrapper.GradleWrapperMain \
        "$@"

# Stop when "xargs" is not available.
if ! command -v xargs >/dev/null 2>&1
then
    die "xargs is not available"
fi

# Use "xargs" to parse quoted args.
#
# With -n1 it outputs one arg per line, with the quotes and backslashes removed.
#
# In Bash we could simply go:
#
#   readarray ARGS < <( xargs -n1 <<<"$var" ) &&
#   set -- "${ARGS[@]}" "$@"
#
# but POSIX shell has neither arrays nor command substitution, so instead we
# post-process each arg (as a line of input to sed) to backslash-escape any
# character that might be a shell metacharacter, then use eval to reverse
# that process (while maintaining the separation between arguments), and wrap
# the whole thing up as a single "set" statement.
#
# This will of course break if any of these variables contains a newline or
# an unmatched quote.
#

eval "set -- $(
        printf '%s\n' "$DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS" |
        xargs -n1 |
        sed ' s~[^-[:alnum:]+,./:=@_]~\\&~g; ' |
        tr '\n' ' '
    )" '"$@"'

exec "$JAVACMD" "$@"

``

## gradlew.bat

``
@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem
@rem SPDX-License-Identifier: Apache-2.0
@rem

@if "%DEBUG%"=="" @echo off
@rem ##########################################################################
@rem
@rem  Gradle startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
@rem This is normally unused
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

@rem Resolve any "." and ".." in APP_HOME to make it shorter.
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

@rem Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=-Dfile.encoding=UTF-8 "-Xmx64m" "-Xms64m"

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if %ERRORLEVEL% equ 0 goto execute

echo. 1>&2
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH. 1>&2
echo. 1>&2
echo Please set the JAVA_HOME variable in your environment to match the 1>&2
echo location of your Java installation. 1>&2

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto execute

echo. 1>&2
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME% 1>&2
echo. 1>&2
echo Please set the JAVA_HOME variable in your environment to match the 1>&2
echo location of your Java installation. 1>&2

goto fail

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar


@rem Execute Gradle
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*

:end
@rem End local scope for the variables with windows NT shell
if %ERRORLEVEL% equ 0 goto mainEnd

:fail
rem Set variable GRADLE_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
set EXIT_CODE=%ERRORLEVEL%
if %EXIT_CODE% equ 0 set EXIT_CODE=1
if not ""=="%GRADLE_EXIT_CONSOLE%" exit %EXIT_CODE%
exit /b %EXIT_CODE%

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega

``

## licenses/Inter-OFL.txt

``
Copyright 2020 The Inter Project Authors (https://github.com/rsms/inter)

This Font Software is licensed under the SIL Open Font License, Version 1.1.
This license is copied below, and is also available with a FAQ at:
https://scripts.sil.org/OFL


-----------------------------------------------------------
SIL OPEN FONT LICENSE Version 1.1 - 26 February 2007
-----------------------------------------------------------

PREAMBLE
The goals of the Open Font License (OFL) are to stimulate worldwide
development of collaborative font projects, to support the font creation
efforts of academic and linguistic communities, and to provide a free and
open framework in which fonts may be shared and improved in partnership
with others.

The OFL allows the licensed fonts to be used, studied, modified and
redistributed freely as long as they are not sold by themselves. The
fonts, including any derivative works, can be bundled, embedded, 
redistributed and/or sold with any software provided that any reserved
names are not used by derivative works. The fonts and derivatives,
however, cannot be released under any other type of license. The
requirement for fonts to remain under this license does not apply
to any document created using the fonts or their derivatives.

DEFINITIONS
"Font Software" refers to the set of files released by the Copyright
Holder(s) under this license and clearly marked as such. This may
include source files, build scripts and documentation.

"Reserved Font Name" refers to any names specified as such after the
copyright statement(s).

"Original Version" refers to the collection of Font Software components as
distributed by the Copyright Holder(s).

"Modified Version" refers to any derivative made by adding to, deleting,
or substituting -- in part or in whole -- any of the components of the
Original Version, by changing formats or by porting the Font Software to a
new environment.

"Author" refers to any designer, engineer, programmer, technical
writer or other person who contributed to the Font Software.

PERMISSION & CONDITIONS
Permission is hereby granted, free of charge, to any person obtaining
a copy of the Font Software, to use, study, copy, merge, embed, modify,
redistribute, and sell modified and unmodified copies of the Font
Software, subject to the following conditions:

1) Neither the Font Software nor any of its individual components,
in Original or Modified Versions, may be sold by itself.

2) Original or Modified Versions of the Font Software may be bundled,
redistributed and/or sold with any software, provided that each copy
contains the above copyright notice and this license. These can be
included either as stand-alone text files, human-readable headers or
in the appropriate machine-readable metadata fields within text or
binary files as long as those fields can be easily viewed by the user.

3) No Modified Version of the Font Software may use the Reserved Font
Name(s) unless explicit written permission is granted by the corresponding
Copyright Holder. This restriction only applies to the primary font name as
presented to the users.

4) The name(s) of the Copyright Holder(s) or the Author(s) of the Font
Software shall not be used to promote, endorse or advertise any
Modified Version, except to acknowledge the contribution(s) of the
Copyright Holder(s) and the Author(s) or with their explicit written
permission.

5) The Font Software, modified or unmodified, in part or in whole,
must be distributed entirely under this license, and must not be
distributed under any other license. The requirement for fonts to
remain under this license does not apply to any document created
using the Font Software.

TERMINATION
This license becomes null and void if any of the above conditions are
not met.

DISCLAIMER
THE FONT SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO ANY WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT
OF COPYRIGHT, PATENT, TRADEMARK, OR OTHER RIGHT. IN NO EVENT SHALL THE
COPYRIGHT HOLDER BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
INCLUDING ANY GENERAL, SPECIAL, INDIRECT, INCIDENTAL, OR CONSEQUENTIAL
DAMAGES, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
FROM, OUT OF THE USE OR INABILITY TO USE THE FONT SOFTWARE OR FROM
OTHER DEALINGS IN THE FONT SOFTWARE.

``

## licenses/PlayfairDisplay-OFL.txt

``
Copyright 2017 The Playfair Display Project Authors (https://github.com/clauseggers/Playfair-Display), with Reserved Font Name "Playfair Display"

This Font Software is licensed under the SIL Open Font License, Version 1.1.
This license is copied below, and is also available with a FAQ at:
http://scripts.sil.org/OFL


-----------------------------------------------------------
SIL OPEN FONT LICENSE Version 1.1 - 26 February 2007
-----------------------------------------------------------

PREAMBLE
The goals of the Open Font License (OFL) are to stimulate worldwide
development of collaborative font projects, to support the font creation
efforts of academic and linguistic communities, and to provide a free and
open framework in which fonts may be shared and improved in partnership
with others.

The OFL allows the licensed fonts to be used, studied, modified and
redistributed freely as long as they are not sold by themselves. The
fonts, including any derivative works, can be bundled, embedded, 
redistributed and/or sold with any software provided that any reserved
names are not used by derivative works. The fonts and derivatives,
however, cannot be released under any other type of license. The
requirement for fonts to remain under this license does not apply
to any document created using the fonts or their derivatives.

DEFINITIONS
"Font Software" refers to the set of files released by the Copyright
Holder(s) under this license and clearly marked as such. This may
include source files, build scripts and documentation.

"Reserved Font Name" refers to any names specified as such after the
copyright statement(s).

"Original Version" refers to the collection of Font Software components as
distributed by the Copyright Holder(s).

"Modified Version" refers to any derivative made by adding to, deleting,
or substituting -- in part or in whole -- any of the components of the
Original Version, by changing formats or by porting the Font Software to a
new environment.

"Author" refers to any designer, engineer, programmer, technical
writer or other person who contributed to the Font Software.

PERMISSION & CONDITIONS
Permission is hereby granted, free of charge, to any person obtaining
a copy of the Font Software, to use, study, copy, merge, embed, modify,
redistribute, and sell modified and unmodified copies of the Font
Software, subject to the following conditions:

1) Neither the Font Software nor any of its individual components,
in Original or Modified Versions, may be sold by itself.

2) Original or Modified Versions of the Font Software may be bundled,
redistributed and/or sold with any software, provided that each copy
contains the above copyright notice and this license. These can be
included either as stand-alone text files, human-readable headers or
in the appropriate machine-readable metadata fields within text or
binary files as long as those fields can be easily viewed by the user.

3) No Modified Version of the Font Software may use the Reserved Font
Name(s) unless explicit written permission is granted by the corresponding
Copyright Holder. This restriction only applies to the primary font name as
presented to the users.

4) The name(s) of the Copyright Holder(s) or the Author(s) of the Font
Software shall not be used to promote, endorse or advertise any
Modified Version, except to acknowledge the contribution(s) of the
Copyright Holder(s) and the Author(s) or with their explicit written
permission.

5) The Font Software, modified or unmodified, in part or in whole,
must be distributed entirely under this license, and must not be
distributed under any other license. The requirement for fonts to
remain under this license does not apply to any document created
using the Font Software.

TERMINATION
This license becomes null and void if any of the above conditions are
not met.

DISCLAIMER
THE FONT SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO ANY WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT
OF COPYRIGHT, PATENT, TRADEMARK, OR OTHER RIGHT. IN NO EVENT SHALL THE
COPYRIGHT HOLDER BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
INCLUDING ANY GENERAL, SPECIAL, INDIRECT, INCIDENTAL, OR CONSEQUENTIAL
DAMAGES, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
FROM, OUT OF THE USE OR INABILITY TO USE THE FONT SOFTWARE OR FROM
OTHER DEALINGS IN THE FONT SOFTWARE.

``

## local.properties

``
sdk.dir=C:/Users/lucky/AppData/Local/Android/Sdk

``

## README.md

``
# EventEase — Step 1

Java + XML Android project for TIMSCDR. Only the project foundation is implemented.

## Terminal build

Use JDK 17 (recommended), Android SDK Platform 35, and Android SDK Build Tools
34.0.0. No Android Studio is needed. AGP 8.7.3 uses the bundled Gradle 8.9 wrapper.
Min SDK is 24; target SDK is 34 (within the requested 34/35 range).
Compile SDK 35 is independent of the target SDK. Target 34 keeps enforced Android
15 edge-to-edge behavior out of this initial theme setup.

Set ANDROID_HOME to your SDK directory, or create an untracked local.properties:

```properties
sdk.dir=C:/path/to/Android/Sdk
```

Install missing SDK packages using the Android command-line tools:

```powershell
sdkmanager "platforms;android-35" "build-tools;34.0.0" "platform-tools"
sdkmanager --licenses
.\gradlew.bat assembleDebug
.\gradlew.bat installDebug
```

On Linux/macOS:

```sh
chmod +x gradlew
./gradlew assembleDebug
```

APK: app/build/outputs/apk/debug/app-debug.apk.
The first build requires network access for Gradle and Maven dependencies.

## Verify this step

Launch EventEase on a device (USB debugging enabled). Confirm the foundation
preview shows Playfair Display headings, Inter body text, and a thin gold divider.
Switch the device between light and dark modes and reopen the app to check both
palettes. App-specific theme selection belongs to Step 8.

## Structure and next steps

Base package: com.timscdr.eventease.
The data/entity, data/dao, data/database, repository, viewmodel, ui, util, and
receiver packages follow the supplied specification. Empty directories are kept
in Git with empty .gitkeep files. No data or repository implementation exists yet.
The organizer package is reserved only; optional organizer work has not started.
SplashActivity currently hosts the foundation preview, not the finished splash.

Next steps, each requiring your confirmation: Room; repositories; authentication
and profile; dashboard and browsing; registration and QR; My Events; settings.
UI will observe ViewModels; ViewModels will depend on repository contracts.
Room access will remain behind repositories to preserve a future backend swap.

## Assets and decisions

All eight light/dark palette roles match the supplied specification. Dark colors
override values through values-night/colors.xml; the shared DayNight theme does
not need a duplicate values-night/themes.xml. Navy fills buttons; gold is an
accent, following your explicit instruction over the spec's gold button examples.
Cards have no elevation and 10dp corners. No animations are needed in this static
preview; later transitions will be subtle fades/slides.

Local static regular-weight TTFs are bundled in res/font. Sources:
- https://github.com/google/fonts/tree/main/ofl/playfairdisplay
- https://github.com/google/fonts/tree/main/ofl/inter

They were instantiated at their default axes using fontTools for API 24 support.
Their SIL Open Font Licenses are included in licenses/. Fonts never need runtime
network access. Gradle wrapper files come from Gradle v8.9.0; its distribution
checksum is pinned in gradle-wrapper.properties.

The real TIMSCDR crest has not been supplied. Provide the approved PNG/WebP asset
before completing splash/login branding; no substitute crest has been invented.
The launcher currently uses the system default icon pending that asset.

## Manual requirements

No API keys or manual dependency additions are needed. Room, lifecycle, ZXing,
Glide, RecyclerView, SwipeRefreshLayout, and Material dependencies are declared.
No Kotlin plugin or Kotlin source is used.
No app permissions are needed for Step 1. Internet permission for remote event
banners, notification permission/reminder handling, and optional scanner camera
permission will be introduced with their corresponding features.

``

## settings.gradle

``
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = 'EventEase'
include ':app'

``
