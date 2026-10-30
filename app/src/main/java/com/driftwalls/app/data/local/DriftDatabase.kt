package com.driftwalls.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ArtworkEntity::class, MetRejectedEntity::class, FavoriteEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class DriftDatabase : RoomDatabase() {
    abstract fun artworkDao(): ArtworkDao
    abstract fun favoriteDao(): FavoriteDao
}
