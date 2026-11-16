# Keep Room Database, DAO, and Entities intact under R8 optimization
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep Navigation 3 @Serializable route keys and kotlinx.serialization metadata
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keep class com.example.notepad.ui.app.** { *; }

# Keep Android AppWidget providers, configure activities, services, and broadcast receivers
-keep public class * extends android.appwidget.AppWidgetProvider
-keep public class * extends android.widget.RemoteViewsService
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.app.Activity

# Suppress benign warning from transitive GMS dependency
-dontwarn com.google.android.gms.internal.location.**
