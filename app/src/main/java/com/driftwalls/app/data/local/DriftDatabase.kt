package com.driftwalls.app.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ArtworkEntity::class,
        MetRejectedEntity::class,
        FavoriteEntity::class,
        RotationPickEntity::class,
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class DriftDatabase : RoomDatabase() {
    abstract fun artworkDao(): ArtworkDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun rotationPickDao(): RotationPickDao
}
