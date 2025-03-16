package com.example.bikes
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import java.text.NumberFormat
import java.util.Locale


class BikeAdapter(private val onItemClick: (Bike) -> Unit) : ListAdapter<Bike, BikeAdapter.BikeViewHolder>(BikeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BikeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.bike_item, parent, false)
        return BikeViewHolder(view)
    }

    override fun onBindViewHolder(holder: BikeViewHolder, position: Int) {
        val bike = getItem(position)
        holder.bind(bike)
    }

    inner class BikeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.bike_name)
        private val priceTextView: TextView = itemView.findViewById(R.id.bike_price)
        private val imageView: ImageView = itemView.findViewById(R.id.bike_image) 

        private val ratingBar: RatingBar = itemView.findViewById(R.id.bike_rating) 

        fun bind(bike: Bike) {
            nameTextView.text = bike.name

            
            val price = bike.price
            val belarusianFormat = NumberFormat.getCurrencyInstance(Locale("be", "BY"))
            val formattedPrice = belarusianFormat.format(price)
            priceTextView.text = formattedPrice

            ratingBar.rating = bike.averageRating


            
            if (bike.images.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(bike.images[0]) 
                    .diskCacheStrategy(DiskCacheStrategy.ALL) 
                    .into(imageView) 
            } else {
                
                imageView.setImageResource(R.drawable.bike) 
            }



            
            itemView.setOnClickListener {
                onItemClick(bike)
            }
        }
    }
}

class BikeDiffCallback : DiffUtil.ItemCallback<Bike>() {
    override fun areItemsTheSame(oldItem: Bike, newItem: Bike): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Bike, newItem: Bike): Boolean {
        return oldItem == newItem
    }
}
