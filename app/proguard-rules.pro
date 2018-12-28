-dontobfuscate
-dontwarn sun.misc.**
-keep class com.google.android.material.theme.MaterialComponentsViewInflater {
  public <init>(...);
}
-dontwarn sun.util.calendar.*
