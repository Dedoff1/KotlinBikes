package com.example.bikes.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.bikes.R
import android.app.AlertDialog
import android.app.Dialog
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.bikes.Bike
import com.example.bikes.CatalogViewModel
import com.example.bikes.databinding.DialogAddBikeBinding


class AddBikeDialogFragment(private val catalogViewModel: CatalogViewModel,
                            private val bike: Bike? = null) : DialogFragment() {

        private lateinit var binding: DialogAddBikeBinding

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            binding = DialogAddBikeBinding.inflate(inflater, container, false)


            bike?.let {
                binding.editTextName.setText(it.name)
                binding.editTextPrice.setText(it.price.toString())
                binding.editTextDescription.setText(it.description)
                it.images.forEachIndexed { index, url ->
                    when (index) {
                        0 -> binding.editTextImageUrl1.setText(url)
                        1 -> binding.editTextImageUrl2.setText(url)
                        2 -> binding.editTextImageUrl3.setText(url)
                        3 -> binding.editTextImageUrl4.setText(url)
                        4 -> binding.editTextImageUrl5.setText(url)
                        5 -> binding.editTextImageUrl6.setText(url)
                        6 -> binding.editTextImageUrl7.setText(url)
                    }
                }
            }

            binding.buttonSave.setOnClickListener {
                val name = binding.editTextName.text.toString()
                val price = binding.editTextPrice.text.toString().toDoubleOrNull() ?: 0.0
                val description = binding.editTextDescription.text.toString()
                val imageUrls = listOf(
                    binding.editTextImageUrl1.text.toString(),
                    binding.editTextImageUrl2.text.toString(),
                    binding.editTextImageUrl3.text.toString(),
                    binding.editTextImageUrl4.text.toString(),
                    binding.editTextImageUrl5.text.toString(),
                    binding.editTextImageUrl6.text.toString(),
                    binding.editTextImageUrl7.text.toString()
                ).filter { it.isNotEmpty() }

                if (name.isNotEmpty() && price > 0 && imageUrls.isNotEmpty()) {
                    val updatedBike = Bike(
                        id = bike?.id ?: "", 
                        name = name,
                        price = price,
                        description = description,
                        images = imageUrls
                    )

                    if (bike != null) {
                        
                        catalogViewModel.updateBike(updatedBike)
                    } else {
                        
                        catalogViewModel.addBike(updatedBike)
                    }
                    dismiss()
                } else {
                    Toast.makeText(context, "Заполните все поля", Toast.LENGTH_SHORT).show()
                }
            }

            return binding.root
        }
    }

