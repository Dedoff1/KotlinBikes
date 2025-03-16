
package com.example.bikes.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.example.bikes.R
import com.example.bikes.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HomeFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var navController: NavController
    private lateinit var binding: FragmentHomeBinding
    private lateinit var database: FirebaseDatabase 

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init(view)
        loadUserData() 
        registerEvents()
    }

    private fun init(view: View) {
        navController = Navigation.findNavController(view)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance() 
    }

    private fun registerEvents() {
        binding.buttonLogout.setOnClickListener {
            signOut()
        }

        binding.buttonCatalog.setOnClickListener {
            if (validateFields()) {
                navController.navigate(R.id.action_homeFragment_to_catalogFragment)
            } else {
                Toast.makeText(context, "Please fill in all fields!", Toast.LENGTH_SHORT).show()
            }
        }

        
        binding.buttonDeleteAccount.setOnClickListener {
            deleteUserAccount()
        }
    }

    
    private fun deleteUserAccount() {
        val user = auth.currentUser
        if (user != null) {
            
            val userId = user.uid
            val userRef = database.getReference("users").child(userId)
            userRef.removeValue()
                .addOnSuccessListener {
                    
                    Toast.makeText(context, "User data deleted", Toast.LENGTH_SHORT).show()

                    
                    user.delete()
                        .addOnSuccessListener {
                            
                            Toast.makeText(context, "Account deleted", Toast.LENGTH_SHORT).show()
                            navController.navigate(R.id.action_homeFragment_to_signInFragment) 
                        }
                        .addOnFailureListener { e ->
                            
                            Toast.makeText(context, "Error deleting account: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    
                    Toast.makeText(context, "Error deleting user data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            
            Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
        }
    }

    private fun signOut() {
        saveUserData() 
        auth.signOut()
        navController.navigate(R.id.action_homeFragment_to_signInFragment)
    }

    private fun validateFields(): Boolean {
        return !binding.editTextName.text.isNullOrBlank() &&
                !binding.editTextSurname.text.isNullOrBlank() &&
                !binding.editTextMiddleName.text.isNullOrBlank() &&
                !binding.editTextAge.text.isNullOrBlank() &&
                !binding.editTextPhoneNumber.text.isNullOrBlank() &&
                !binding.editTextCountry.text.isNullOrBlank() &&
                !binding.editTextCity.text.isNullOrBlank() &&
                !binding.editTextStreet.text.isNullOrBlank() &&
                !binding.editTextSex.text.isNullOrBlank()
    }

    private fun saveUserData() {
        val userId = auth.currentUser?.uid ?: return 
        val userRef = database.getReference("users").child(userId) 

        val userData = mapOf(  
            "name" to binding.editTextName.text.toString(),
            "surname" to binding.editTextSurname.text.toString(),
            "middleName" to binding.editTextMiddleName.text.toString(),
            "age" to binding.editTextAge.text.toString(),
            "phoneNumber" to binding.editTextPhoneNumber.text.toString(),
            "country" to binding.editTextCountry.text.toString(),
            "city" to binding.editTextCity.text.toString(),
            "street" to binding.editTextStreet.text.toString(),
            "sex" to binding.editTextSex.text.toString(),
            "additionalInfo" to binding.editTextAdditionalInfo.text.toString()
        )

        userRef.setValue(userData)  
            .addOnSuccessListener {
                
                Toast.makeText(context, "Data saved to Firebase", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                
                Toast.makeText(context, "Error saving data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return
        val userRef = database.getReference("users").child(userId)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    
                    binding.editTextName.setText(snapshot.child("name").getValue(String::class.java) ?: "")
                    binding.editTextSurname.setText(snapshot.child("surname").getValue(String::class.java) ?: "")
                    binding.editTextMiddleName.setText(snapshot.child("middleName").getValue(String::class.java) ?: "")
                    binding.editTextAge.setText(snapshot.child("age").getValue(String::class.java) ?: "")
                    binding.editTextPhoneNumber.setText(snapshot.child("phoneNumber").getValue(String::class.java) ?: "")
                    binding.editTextCountry.setText(snapshot.child("country").getValue(String::class.java) ?: "")
                    binding.editTextCity.setText(snapshot.child("city").getValue(String::class.java) ?: "")
                    binding.editTextStreet.setText(snapshot.child("street").getValue(String::class.java) ?: "")
                    binding.editTextSex.setText(snapshot.child("sex").getValue(String::class.java) ?: "")
                    binding.editTextAdditionalInfo.setText(snapshot.child("additionalInfo").getValue(String::class.java) ?: "")
                } else {
                    
                    Toast.makeText(context, "No data found for this user", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                
                Toast.makeText(context, "Error loading data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onPause() {
        super.onPause()
        saveUserData()
    }
}
