-dontwarn sun.misc.**
-dontwarn sun.util.calendar.**
-keep class com.google.android.material.theme.MaterialComponentsViewInflater {
  public <init>(...);
}
-keep class com.wireguard.android.fragment.TunnelDetailFragment {
  public <init>(...);
}
-keepattributes SourceFile,LineNumberTable
-dontobfuscate
