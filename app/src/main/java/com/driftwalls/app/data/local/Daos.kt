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

    @Query("SELECT wallpaperId FROM favorites ORDER BY addedAt")
    suspend fun ids(): List<String>

    @Query("SELECT COUNT(*) FROM favorites")
    fun observeCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE wallpaperId = :id")
    suspend fun remove(id: String)
}

@Dao
interface RotationPickDao {
    @Query("SELECT wallpaperId FROM rotation_picks ORDER BY addedAt")
    fun observeIds(): Flow<List<String>>

    @Query("SELECT wallpaperId FROM rotation_picks ORDER BY addedAt")
    suspend fun ids(): List<String>

    @Query("SELECT COUNT(*) FROM rotation_picks")
    fun observeCount(): Flow<Int>

    @Query("SELECT EXISTS(SELECT 1 FROM rotation_picks WHERE wallpaperId = :id)")
    fun observeIsPicked(id: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(pick: RotationPickEntity)

    @Query("DELETE FROM rotation_picks WHERE wallpaperId = :id")
    suspend fun remove(id: String)
}

@Dao
interface RotationHistoryDao {
    @Insert
    suspend fun insert(entry: RotationHistoryEntity)

    /** Newest first. */
    @Query("SELECT wallpaperId FROM rotation_history ORDER BY appliedAt DESC, id DESC LIMIT :limit")
    suspend fun recentIds(limit: Int): List<String>

    @Query(
        """
        SELECT h.wallpaperId, h.appliedAt, a.title, a.thumbnailUrl
        FROM rotation_history h LEFT JOIN artworks a ON a.id = h.wallpaperId
        ORDER BY h.appliedAt DESC, h.id DESC LIMIT :limit
        """,
    )
    fun observeRecent(limit: Int): Flow<List<RotationHistoryItem>>

    /** Keeps the newest [keep] rows. */
    @Query("DELETE FROM rotation_history WHERE id NOT IN (SELECT id FROM rotation_history ORDER BY appliedAt DESC, id DESC LIMIT :keep)")
    suspend fun prune(keep: Int)
}
