package com.example.bikes

data class Bike(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val description: String = "",
    val images: List<String> = emptyList(), 
    val ratings: Map<String, Float> = emptyMap(), 
    val averageRating: Float = 0.0f 
)
{
    
    fun calculateAverageRating(): Float {
        if (ratings.isEmpty()) return 0.0f
        val totalRating = ratings.values.sum()
        return totalRating / ratings.size
    }
}