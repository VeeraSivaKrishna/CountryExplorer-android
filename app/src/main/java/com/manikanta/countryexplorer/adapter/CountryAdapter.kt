package com.manikanta.countryexplorer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.manikanta.countryexplorer.R
import com.manikanta.countryexplorer.model.CountryDto
import com.manikanta.countryexplorer.utils.WishlistManager

class CountryAdapter(
    private var countries: List<CountryDto>,
    private val wishlistManager: WishlistManager,
    private val onItemClick: (CountryDto) -> Unit,
    private val onWishlistClick: (CountryDto) -> Unit
) : RecyclerView.Adapter<CountryAdapter.CountryViewHolder>() {

    class CountryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgFlag: ImageView = itemView.findViewById(R.id.imgFlag)
        val tvCountryName: TextView = itemView.findViewById(R.id.tvCountryName)
        val tvCapitalRegion: TextView = itemView.findViewById(R.id.tvCapitalRegion)
        val btnWishlist: ImageButton = itemView.findViewById(R.id.btnWishlist)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CountryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_country, parent, false)
        return CountryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CountryViewHolder, position: Int) {
        val country = countries[position]
        holder.tvCountryName.text = country.displayName
        holder.tvCapitalRegion.text = "Capital: ${country.displayCapital} | ${country.displayRegion}"

        Glide.with(holder.imgFlag.context)
            .load(country.flagUrl)
            .centerCrop()
            .into(holder.imgFlag)

        val isStarred = wishlistManager.isWishlisted(country.countryCode)
        updateStarIcon(holder.btnWishlist, isStarred)

        holder.itemView.setOnClickListener {
            onItemClick(country)
        }

        holder.btnWishlist.setOnClickListener {
            onWishlistClick(country)
            val updatedStarred = wishlistManager.isWishlisted(country.countryCode)
            updateStarIcon(holder.btnWishlist, updatedStarred)
        }
    }

    private fun updateStarIcon(btnWishlist: ImageButton, isStarred: Boolean) {
        if (isStarred) {
            btnWishlist.setImageResource(android.R.drawable.btn_star_big_on)
        } else {
            btnWishlist.setImageResource(android.R.drawable.btn_star_big_off)
        }
    }

    override fun getItemCount(): Int = countries.size

    fun updateData(newCountries: List<CountryDto>) {
        this.countries = newCountries
        notifyDataSetChanged()
    }
}
