package com.example.bikes.fragments

import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.os.bundleOf
import com.example.bikes.R
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bikes.Bike
import com.example.bikes.BikeAdapter
import com.example.bikes.CatalogViewModel
import com.example.bikes.databinding.FragmentBikeBinding
import com.example.bikes.databinding.FragmentCatalogBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Collections

class CatalogFragment : Fragment() {

    private lateinit var catalogViewModel: CatalogViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BikeAdapter
    private lateinit var addButton: FloatingActionButton

    private lateinit var homeButton: Button
    private lateinit var binding: FragmentCatalogBinding
    private lateinit var bikesRef: DatabaseReference
    private lateinit var showFavoritesButton: Button
    private var isShowingFavorites: Boolean = false
    private lateinit var favoritesRef: DatabaseReference
    private lateinit var sortButton: Button
    private var currentBikeList: List<Bike> = emptyList()
    private var originalBikeList: List<Bike> = emptyList() 
    private var currentSortType: SortType = SortType.DEFAULT 
    private var sortAscending: Boolean = true 

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_catalog, container, false)
        recyclerView = view.findViewById(R.id.recycler_view_catalog)
        recyclerView.layoutManager = LinearLayoutManager(context)

        adapter = BikeAdapter { bike ->
            val bundle = bundleOf("bikeId" to bike.id)
            view?.findNavController()?.navigate(R.id.action_catalogFragment_to_bikeFragment, bundle)
        }
        recyclerView.adapter = adapter

        addButton = view.findViewById(R.id.add_bike_button)

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser?.email == "admin@gmail.com") {
            addButton.visibility = View.VISIBLE
        } else {
            addButton.visibility = View.GONE
        }
        addButton.setOnClickListener {
            val addBikeDialog = AddBikeDialogFragment(catalogViewModel)
            addBikeDialog.show(parentFragmentManager, "AddBikeDialog")
        }
        homeButton = view.findViewById(R.id.home_button)
        homeButton.setOnClickListener {
            view?.findNavController()?.navigate(R.id.action_catalogFragment_to_homeFragment)
        }

        showFavoritesButton = view.findViewById(R.id.show_favorites_button)
        showFavoritesButton.setOnClickListener {
            isShowingFavorites = !isShowingFavorites
            updateButtonText()
            if (isShowingFavorites) {
                loadFavorites()
            } else {
                loadBikes()
            }
        }

        sortButton = view.findViewById(R.id.sort_button)
        sortButton.setOnClickListener {
            showSortMenu(it)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        catalogViewModel = ViewModelProvider(this).get(CatalogViewModel::class.java)

        catalogViewModel.bikes.observe(viewLifecycleOwner) { bikes ->
            currentBikeList = bikes
            originalBikeList = bikes.toList() 
            adapter.submitList(bikes)
        }

        bikesRef = FirebaseDatabase.getInstance().getReference("bikes")
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        favoritesRef = FirebaseDatabase.getInstance().getReference("favorites").child(userId ?: "")

        loadBikes()
    }

    private fun loadBikesWithRatings() {
        val bikesRef = FirebaseDatabase.getInstance().getReference("bikes")
        val ratingsRef = FirebaseDatabase.getInstance().getReference("ratings")

        bikesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val bikeList = mutableListOf<Bike>()
                for (bikeSnapshot in snapshot.children) {
                    val bike = bikeSnapshot.getValue(Bike::class.java)
                    val bikeId = bikeSnapshot.key
                    if (bike != null && bikeId != null) {
                        ratingsRef.child(bikeId).addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(ratingSnapshot: DataSnapshot) {
                                val ratings = mutableMapOf<String, Float>()
                                for (ratingChild in ratingSnapshot.children) {
                                    val userId = ratingChild.key
                                    val rating = ratingChild.getValue(Float::class.java)
                                    if (userId != null && rating != null) {
                                        ratings[userId] = rating
                                    }
                                }
                                val updatedBike = bike.copy(ratings = ratings, averageRating = bike.calculateAverageRating())
                                bikeList.add(updatedBike)
                                adapter.submitList(bikeList)
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.w("CatalogFragment", "loadRatings:onCancelled", error.toException())
                            }
                        })
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("CatalogFragment", "loadBikes:onCancelled", error.toException())
                Toast.makeText(context, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun checkInternetConnection(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    private fun loadBikes() {
        if (!checkInternetConnection()) {
            Toast.makeText(context, "You are offline. Displaying cached data.", Toast.LENGTH_SHORT).show()
        }
        bikesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val bikeList = mutableListOf<Bike>()
                for (bikeSnapshot in snapshot.children) {
                    val bike = bikeSnapshot.getValue(Bike::class.java)
                    val bikeId = bikeSnapshot.key
                    if (bike != null && bikeId != null) {
                        bikeList.add(bike.copy(id = bikeId))
                    }
                }
                currentBikeList = bikeList
                originalBikeList = bikeList.toList() 
                adapter.submitList(bikeList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("CatalogFragment", "loadBikes:onCancelled", error.toException())
                Toast.makeText(context, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadFavorites() {
        favoritesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val favoriteBikeIds = mutableListOf<String>()
                for (favoriteSnapshot in snapshot.children) {
                    val bikeId = favoriteSnapshot.key
                    if (bikeId != null) {
                        favoriteBikeIds.add(bikeId)
                    }
                }

                bikesRef.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val bikeList = mutableListOf<Bike>()
                        for (bikeSnapshot in snapshot.children) {
                            val bike = bikeSnapshot.getValue(Bike::class.java)
                            val bikeId = bikeSnapshot.key
                            if (bike != null && bikeId != null && favoriteBikeIds.contains(bikeId)) {
                                bikeList.add(bike.copy(id = bikeId))
                            }
                        }
                        currentBikeList = bikeList
                        originalBikeList = bikeList.toList() 
                        adapter.submitList(bikeList)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.w("CatalogFragment", "loadFavorites:onCancelled", error.toException())
                        Toast.makeText(context, "Ошибка загрузки избранных", Toast.LENGTH_SHORT).show()
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("CatalogFragment", "loadFavorites:onCancelled", error.toException())
                Toast.makeText(context, "Ошибка загрузки избранных", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateButtonText() {
        if (isShowingFavorites) {
            showFavoritesButton.text = "Show All"
        } else {
            showFavoritesButton.text = "Show Favorite"
        }
    }

    private fun showSortMenu(view: View) {
        val popupMenu = PopupMenu(context, view)
        popupMenu.menuInflater.inflate(R.menu.sort_menu, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.sort_by_name -> {
                    sortBy(SortType.NAME)
                    true
                }
                R.id.sort_by_price -> {
                    sortBy(SortType.PRICE)
                    true
                }
                R.id.sort_by_rating -> {
                    sortBy(SortType.RATING)
                    true
                }
                R.id.sort_default -> {
                    sortBy(SortType.DEFAULT)
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun sortBy(sortType: SortType) {
        if (sortType == currentSortType) {
            
            sortAscending = !sortAscending
        } else {
            
            sortAscending = true
            currentSortType = sortType
        }

        val sortedList = when (sortType) {
            SortType.NAME -> {
                if (sortAscending) currentBikeList.sortedBy { it.name } else currentBikeList.sortedByDescending { it.name }
            }
            SortType.PRICE -> {
                if (sortAscending) currentBikeList.sortedBy { it.price } else currentBikeList.sortedByDescending { it.price }
            }
            SortType.RATING -> {
                if (sortAscending) currentBikeList.sortedByDescending { it.averageRating } else currentBikeList.sortedBy { it.averageRating }
            }
            SortType.DEFAULT -> {
                
                originalBikeList
            }
        }
        adapter.submitList(sortedList)
    }


    enum class SortType {
        NAME,
        PRICE,
        RATING,
        DEFAULT
    }

}
