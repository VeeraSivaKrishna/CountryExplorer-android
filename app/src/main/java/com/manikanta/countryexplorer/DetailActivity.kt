package com.manikanta.countryexplorer

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.manikanta.countryexplorer.model.CountryDto
import com.manikanta.countryexplorer.utils.WishlistManager

class DetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_COUNTRY = "extra_country"
    }

    private lateinit var wishlistManager: WishlistManager
    private var country: CountryDto? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        wishlistManager = WishlistManager(this)

        @Suppress("DEPRECATION")
        country = intent.getSerializableExtra(EXTRA_COUNTRY) as? CountryDto

        if (country == null) {
            finish()
            return
        }

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val imgFlag = findViewById<ImageView>(R.id.imgDetailFlag)
        val tvName = findViewById<TextView>(R.id.tvDetailName)
        val tvOfficialName = findViewById<TextView>(R.id.tvDetailOfficialName)
        val tvCapital = findViewById<TextView>(R.id.tvCapital)
        val tvRegion = findViewById<TextView>(R.id.tvRegion)
        val tvPopulation = findViewById<TextView>(R.id.tvPopulation)
        val tvCurrency = findViewById<TextView>(R.id.tvCurrency)
        val tvLanguages = findViewById<TextView>(R.id.tvLanguages)
        val btnWishlist = findViewById<Button>(R.id.btnDetailWishlist)

        btnBack.setOnClickListener { finish() }

        val c = country!!
        tvName.text = c.displayName
        tvOfficialName.text = c.displayOfficialName
        tvCapital.text = "🏛 Capital: ${c.displayCapital}"
        tvRegion.text = "🌍 Region: ${c.displayRegion}"
        tvPopulation.text = "👥 Population: ${c.displayPopulation}"
        tvCurrency.text = "💰 Currency: ${c.displayCurrencies}"
        tvLanguages.text = "🗣 Languages: ${c.displayLanguages}"

        Glide.with(this)
            .load(c.flagUrl)
            .into(imgFlag)

        updateWishlistButton(btnWishlist, c.countryCode)

        btnWishlist.setOnClickListener {
            wishlistManager.toggleWishlist(c.countryCode)
            updateWishlistButton(btnWishlist, c.countryCode)
        }
    }

    private fun updateWishlistButton(button: Button, countryCode: String) {
        if (wishlistManager.isWishlisted(countryCode)) {
            button.text = "⭐ Remove from Wishlist"
        } else {
            button.text = "⭐ Add to Wishlist"
        }
    }
}
