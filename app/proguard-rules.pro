-dontwarn sun.misc.**
-dontwarn sun.util.calendar.**
-keep class com.google.android.material.theme.MaterialComponentsViewInflater {
  public <init>(...);
}
-keepattributes SourceFile,LineNumberTable
-dontobfuscate
