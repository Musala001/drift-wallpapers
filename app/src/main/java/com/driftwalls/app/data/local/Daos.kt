package com.driftwalls.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ArtworkDao {
    @Upsert
    suspend fun upsert(items: List<ArtworkEntity>)

    @Query("SELECT * FROM artworks WHERE id = :id")
    suspend fun byId(id: String): ArtworkEntity?

    @Query("SELECT * FROM artworks WHERE id IN (:ids)")
    suspend fun byIds(ids: List<String>): List<ArtworkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markMetRejected(items: List<MetRejectedEntity>)

    @Query("SELECT objectId FROM met_rejected WHERE objectId IN (:ids) AND checkedAt >= :since")
    suspend fun metRejectedAmong(ids: List<Long>, since: Long): List<Long>
}

@Dao
interface FavoriteDao {
    @Query(
        "SELECT a.* FROM artworks a JOIN favorites f ON f.wallpaperId = a.id ORDER BY f.addedAt DESC",
    )
    fun observeFavoriteArtworks(): Flow<List<ArtworkEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE wallpaperId = :id)")
    fun observeIsFavorite(id: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE wallpaperId = :id")
    suspend fun remove(id: String)
}
