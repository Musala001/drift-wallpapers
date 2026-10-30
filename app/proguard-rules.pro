# kotlinx.serialization: keep generated serializers for DTOs and navigation routes.
-keepclassmembers @kotlinx.serialization.Serializable class com.driftwalls.app.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.driftwalls.app.**$$serializer { *; }
