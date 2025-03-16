package com.example.bikes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.*

class BikeViewModel(private val bikeId: String) : ViewModel() {

    private val _bike = MutableLiveData<Bike>()
    val bike: LiveData<Bike> = _bike

    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val bikeRef: DatabaseReference = database.getReference("bikes").child(bikeId) 

    init {
        loadBike()
    }

    private fun loadBike() {
        bikeRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val bike = snapshot.getValue(Bike::class.java)
                bike?.let {
                    _bike.value = it.copy(id = snapshot.key ?: "") 
                }
            }

            override fun onCancelled(error: DatabaseError) {
                
                println("Ошибка загрузки данных: ${error.message}")
            }
        })
    }

    
    fun updatePrice(newPrice: Double) {
        bikeRef.child("price").setValue(newPrice)
    }


}
