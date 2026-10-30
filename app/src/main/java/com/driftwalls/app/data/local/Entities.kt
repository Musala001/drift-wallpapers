package com.driftwalls.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.driftwalls.app.domain.model.Artwork
import com.driftwalls.app.domain.model.Museum

/** Every artwork we have shown, so detail, favourites and rotation work without re-fetching. */
@Entity(tableName = "artworks")
data class ArtworkEntity(
    @PrimaryKey val id: String,
    val museum: Museum,
    val sourceId: String,
    val title: String,
    val artist: String?,
    val date: String?,
    val thumbnailUrl: String,
    val imageUrl: String,
    val width: Int?,
    val height: Int?,
    val altText: String?,
    val webUrl: String?,
    val cachedAt: Long,
)

/** Met objects that failed the public-domain/image check, so search pages don't re-fetch them. */
@Entity(tableName = "met_rejected")
data class MetRejectedEntity(
    @PrimaryKey val objectId: Long,
    val checkedAt: Long,
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val wallpaperId: String,
    val addedAt: Long,
)

/** Wallpapers the user added to their own rotation list from the detail screen. */
@Entity(tableName = "rotation_picks")
data class RotationPickEntity(
    @PrimaryKey val wallpaperId: String,
    val addedAt: Long,
)

fun ArtworkEntity.toDomain() = Artwork(
    museum = museum,
    sourceId = sourceId,
    title = title,
    artist = artist,
    date = date,
    thumbnailUrl = thumbnailUrl,
    imageUrl = imageUrl,
    width = width,
    height = height,
    altText = altText,
    webUrl = webUrl,
)

fun Artwork.toEntity(cachedAt: Long) = ArtworkEntity(
    id = id,
    museum = museum,
    sourceId = sourceId,
    title = title,
    artist = artist,
    date = date,
    thumbnailUrl = thumbnailUrl,
    imageUrl = imageUrl,
    width = width,
    height = height,
    altText = altText,
    webUrl = webUrl,
    cachedAt = cachedAt,
)
