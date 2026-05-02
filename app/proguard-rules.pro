# OpenCC4J builds converters and segment implementations through reflection.
# R8 can otherwise remove/rewrite no-arg constructors, which crashes after
# switching to Hong Kong/Taiwan text conversion in release builds.
-keepclassmembers class com.github.houbb.** {
    public <init>();
}

# Optional desktop/server integrations referenced by the library but unused on Android.
-dontwarn com.huaban.analysis.jieba.**
-dontwarn java.beans.**
-dontwarn java.lang.management.**
