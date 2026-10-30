package com.driftwalls.app.domain.model

/** Browse categories. Each museum source maps these to its own curated query. */
enum class Category(val displayName: String, val isGenerated: Boolean = false) {
    IMPRESSIONISM("Impressionism"),
    LANDSCAPES("Landscapes"),
    JAPANESE_PRINTS("Japanese prints"),
    BOTANICAL("Botanical"),
    GENERATED("Abstract / Generated", isGenerated = true),
}

sealed interface SearchQuery {
    data class InCategory(val category: Category) : SearchQuery
    data class Text(val text: String) : SearchQuery
}
