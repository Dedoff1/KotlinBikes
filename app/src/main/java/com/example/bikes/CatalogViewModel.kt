package com.example.bikes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.*

class CatalogViewModel : ViewModel() {

    private val _bikes = MutableLiveData<List<Bike>>()
    val bikes: LiveData<List<Bike>> = _bikes

    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val bikesRef: DatabaseReference = database.getReference("bikes") 

    init {
        bikesRef.keepSynced(true) 
        loadBikes()
    }

    private fun loadBikes() {
        bikesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val bikeList = mutableListOf<Bike>()
                for (childSnapshot in snapshot.children) {
                    val bike = childSnapshot.getValue(Bike::class.java)
                    bike?.let {
                        
                        val averageRating = it.calculateAverageRating()
                        bikeList.add(it.copy(id = childSnapshot.key ?: "", averageRating = averageRating)) 
                    }
                }
                _bikes.value = bikeList
            }

            override fun onCancelled(error: DatabaseError) {
                
                println("Ошибка загрузки данных: ${error.message}")
            }
        })
    }

    
    fun addBike(bike: Bike) {
        val newBikeRef = bikesRef.push() 
        newBikeRef.setValue(bike)
    }

    fun updateBike(bike: Bike) {
        if (bike.id.isNotEmpty()) {
            bikesRef.child(bike.id).setValue(bike)
        }
    }
}