package com.example.bikes.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.example.bikes.R
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.bikes.BikeViewModel
import com.example.bikes.BikeViewModelFactory
import com.example.bikes.ImagePagerAdapter
import com.example.bikes.databinding.FragmentBikeBinding
import com.example.bikes.databinding.FragmentSignInBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import androidx.navigation.Navigation
import com.example.bikes.CatalogViewModel


class BikeFragment : Fragment() {

    private lateinit var catalogViewModel: CatalogViewModel
    private lateinit var bikeViewModel: BikeViewModel
    private lateinit var nameTextView: TextView
    private lateinit var priceTextView: TextView
    private lateinit var descriptionTextView: TextView
    private lateinit var viewPager: ViewPager2
    private lateinit var imagePagerAdapter: ImagePagerAdapter 
    private lateinit var binding: FragmentBikeBinding
    private var currentBikeId: String? = null
    private var isFavorite: Boolean = false
    private lateinit var buttonAddToFavorites: Button
    private lateinit var buttonEditBike: Button 
    private lateinit var buttonDeleteBike: Button 
    private lateinit var ratingBar: RatingBar
    private lateinit var buttonSubmitRating: Button
    private lateinit var averageRatingTextView: TextView


    private val args: BikeFragmentArgs by navArgs()

    companion object {
        private const val ARG_BIKE_ID = "bike_id"

        fun newInstance(bikeId: String): Fragment {
            val fragment = BikeFragment()
            val args = Bundle()
            args.putString(ARG_BIKE_ID, bikeId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view =
            inflater.inflate(R.layout.fragment_bike, container, false) 

        nameTextView = view.findViewById(R.id.bike_name)
        priceTextView = view.findViewById(R.id.bike_price)
        descriptionTextView = view.findViewById(R.id.bike_description)

        viewPager = view.findViewById(R.id.bike_images_viewpager)
        buttonAddToFavorites = view.findViewById(R.id.button_add_to_favorites)
        buttonEditBike = view.findViewById(R.id.button_edit_bike) 
        buttonDeleteBike = view.findViewById(R.id.button_delete_bike) 
        ratingBar = view.findViewById(R.id.ratingBar)
        buttonSubmitRating = view.findViewById(R.id.button_submit_rating)
        averageRatingTextView = view.findViewById(R.id.average_rating_text_view)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        currentBikeId = args.bikeId
        val bikeId = args.bikeId 

        bikeViewModel =
            ViewModelProvider(this, BikeViewModelFactory(bikeId)).get(BikeViewModel::class.java)

        catalogViewModel = ViewModelProvider(requireActivity()).get(CatalogViewModel::class.java)

        loadAverageRating(bikeId)

        buttonEditBike = view.findViewById(R.id.button_edit_bike)

        buttonDeleteBike = view.findViewById(R.id.button_delete_bike)
        
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser?.email == "admin@gmail.com") {
            
            buttonEditBike.visibility = View.VISIBLE
            buttonDeleteBike.visibility = View.VISIBLE
        } else {
            
            buttonEditBike.visibility = View.GONE
            buttonDeleteBike.visibility = View.GONE
        }

        bikeViewModel.bike.observe(viewLifecycleOwner) { bike ->
            nameTextView.text = bike.name
            priceTextView.text = "%.2f".format(bike.price) + " Br"
            descriptionTextView.text = bike.description



            imagePagerAdapter = ImagePagerAdapter(bike.images)
            viewPager.adapter = imagePagerAdapter

            checkIfFavorite(bikeId)

            buttonEditBike.setOnClickListener {
                val editBikeDialog = AddBikeDialogFragment(catalogViewModel, bike)
                editBikeDialog.show(parentFragmentManager, "EditBikeDialog")
            }




        }
        val buttonBackToCatalog = view.findViewById<Button>(R.id.button_back_to_catalog)
        buttonBackToCatalog.setOnClickListener {
            findNavController().navigate(R.id.action_bikeFragment_to_catalogFragment)
        }

        buttonAddToFavorites.setOnClickListener {
            currentBikeId?.let { bikeId ->
                if (isFavorite) {
                    removeFromFavorites(bikeId)
                } else {
                    addToFavorites(bikeId)
                }
            } ?: run {
                Toast.makeText(context, "Bike ID is null", Toast.LENGTH_SHORT).show()
            }
        }



        
        buttonDeleteBike.setOnClickListener {
            currentBikeId?.let { bikeId ->
                deleteBike(bikeId)
            } ?: run {
                Toast.makeText(context, "Bike ID is null", Toast.LENGTH_SHORT).show()
            }
        }

        buttonSubmitRating.setOnClickListener {
            val rating = ratingBar.rating
            currentBikeId?.let { bikeId ->
                submitRating(bikeId, rating)
            } ?: run {
                Toast.makeText(context, "Bike ID is null", Toast.LENGTH_SHORT).show()
            }
        }

        
        loadUserRating()

    }

    private fun submitRating(bikeId: String, rating: Float) {
        val user = FirebaseAuth.getInstance().currentUser
        user?.let {
            val userId = user.uid
            val ratingsRef = FirebaseDatabase.getInstance().getReference("ratings").child(bikeId)
            ratingsRef.child(userId).setValue(rating)
                .addOnSuccessListener {
                    Toast.makeText(context, "Rating submitted!", Toast.LENGTH_SHORT).show()
                    loadAverageRating(bikeId)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to submit rating: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } ?: run {
            Toast.makeText(context, "Please sign in to submit a rating.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUserRating() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.let {
            val userId = user.uid
            currentBikeId?.let { bikeId ->
                val ratingsRef = FirebaseDatabase.getInstance().getReference("ratings").child(bikeId)
                ratingsRef.child(userId).get().addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        val rating = snapshot.getValue(Float::class.java)
                        rating?.let {
                            ratingBar.rating = rating
                        }
                    }
                }
            }
        }
    }

    private fun loadAverageRating(bikeId: String) {
        val ratingsRef = FirebaseDatabase.getInstance().getReference("ratings").child(bikeId)
        ratingsRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                var totalRating = 0f
                var count = 0
                for (child in snapshot.children) {
                    val rating = child.getValue(Float::class.java)
                    rating?.let {
                        totalRating += it
                        count++
                    }
                }
                if (count > 0) {
                    val averageRating = totalRating / count
                    
                    
                    val bikeRef = FirebaseDatabase.getInstance().getReference("bikes").child(bikeId)
                    bikeRef.child("averageRating").setValue(averageRating)
                    averageRatingTextView.text = "Average Rating: %.1f".format(averageRating)
                }
            }
        }
    }

    private fun checkIfFavorite(bikeId: String) {
        val user = FirebaseAuth.getInstance().currentUser
        user?.let {
            val userId = user.uid
            val favoritesRef = FirebaseDatabase.getInstance().getReference("favorites").child(userId)
            favoritesRef.child(bikeId).get().addOnSuccessListener { snapshot ->
                isFavorite = snapshot.exists()
                updateFavoriteButtonText()
            }
        }
    }

    private fun updateFavoriteButtonText() {
        if (isFavorite) {
            buttonAddToFavorites.text = "Delete from Favorites"
        } else {
            buttonAddToFavorites.text = "Add to Favorites"
        }
    }
    private fun addToFavorites(bikeId: String) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val userId = user.uid
            val favoritesRef = FirebaseDatabase.getInstance().getReference("favorites").child(userId)
            favoritesRef.child(bikeId).setValue(true)
                .addOnSuccessListener {
                    isFavorite = true
                    updateFavoriteButtonText()
                    Toast.makeText(context, "Added to favorites!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to add to favorites: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(context, "Please sign in to add to favorites.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun removeFromFavorites(bikeId: String) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val userId = user.uid
            val favoritesRef = FirebaseDatabase.getInstance().getReference("favorites").child(userId)
            favoritesRef.child(bikeId).removeValue()
                .addOnSuccessListener {
                    isFavorite = false
                    updateFavoriteButtonText()
                    Toast.makeText(context, "Removed from favorites!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to remove from favorites: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(context, "Please sign in to remove from favorites.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteBike(bikeId: String) {
        val bikesRef = FirebaseDatabase.getInstance().getReference("bikes")
        bikesRef.child(bikeId).removeValue()
            .addOnSuccessListener {
                Toast.makeText(context, "Bike deleted successfully!", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_bikeFragment_to_catalogFragment)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to delete bike: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}

