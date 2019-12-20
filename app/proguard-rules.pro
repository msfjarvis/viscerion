-dontwarn sun.misc.**
-dontwarn sun.util.calendar.**
-keep class com.wireguard.android.fragment.TunnelDetailFragment {
  public <init>(...);
}
-keep class androidx.camera.core.** { *; }
-keepattributes SourceFile,LineNumberTable
-dontobfuscate
