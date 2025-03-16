package com.example.bikes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class BikeViewModelFactory(private val bikeId: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BikeViewModel::class.java)) {
            return BikeViewModel(bikeId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
