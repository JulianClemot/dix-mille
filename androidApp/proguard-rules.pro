# R8 rules for the release build.
#
# Most libraries ship their own consumer rules inside their AAR/JAR, so nothing
# extra is needed for them here:
#   - kotlinx.serialization (@Serializable DTOs, Navigation3 routes)
#   - Room 3 (keeps RoomDatabase subclasses for AppDatabase_Impl lookup)
#   - Compose, Lifecycle, Navigation3, Koin, coroutines
# Activities and the Application class are kept automatically via the manifest.

# Keep line numbers so Play Console crash reports can be deobfuscated with mapping.txt
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
