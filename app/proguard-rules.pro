# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\ADMIN\AppData\Local\Android\Sdk\tools\proguard\proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
r
# Keep our data classes
-keep class com.example.canetracker.HistoryEntry { *; }
-keep class com.example.canetracker.CustomerSummary { *; }

# Apache POI rules for Excel management
-keep class org.apache.poi.** { *; }
-keep class org.apache.xmlbeans.** { *; }
-keep class javax.xml.** { *; }
-keep class com.microsoft.schemas.** { *; }
-keep class org.openxmlformats.schemas.** { *; }

# General Android rules
-dontwarn android.util.Property
-dontwarn androidx.core.view.ViewHierarchyEncoder
-dontwarn androidx.core.view.ViewHierarchyDecoder
# Add navigation rules and specific rules