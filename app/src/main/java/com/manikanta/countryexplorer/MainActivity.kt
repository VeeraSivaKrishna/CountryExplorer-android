package com.manikanta.countryexplorer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.location.LocationManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.manikanta.countryexplorer.adapter.CountryAdapter
import com.manikanta.countryexplorer.api.RetrofitClient
import com.manikanta.countryexplorer.model.CountryDto
import com.manikanta.countryexplorer.utils.WishlistManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var etSearch: EditText
    private lateinit var btnSearch: Button
    private lateinit var btnDetectLocation: Button
    private lateinit var btnWishlistFilter: Button
    private lateinit var chipGroupRegion: ChipGroup
    private lateinit var rcvCountries: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyMessage: TextView
    private lateinit var tvWelcomeUser: TextView

    private lateinit var adapter: CountryAdapter
    private lateinit var wishlistManager: WishlistManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var fullCountryList: List<CountryDto> = emptyList()
    private var isWishlistOnlyMode = false

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            detectCurrentCountry()
        } else {
            Toast.makeText(this, "Location permission denied. Detecting from device region...", Toast.LENGTH_SHORT).show()
            fallbackToLocaleCountry()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        wishlistManager = WishlistManager(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        initViews()
        setupRecyclerView()
        setupListeners()
        initFirebaseAuth()

        loadAllCountries()
    }

    private fun initViews() {
        etSearch = findViewById(R.id.etSearch)
        btnSearch = findViewById(R.id.btnSearch)
        btnDetectLocation = findViewById(R.id.btnDetectLocation)
        btnWishlistFilter = findViewById(R.id.btnWishlistFilter)
        chipGroupRegion = findViewById(R.id.chipGroupRegion)
        rcvCountries = findViewById(R.id.rcvCountries)
        progressBar = findViewById(R.id.progressBar)
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage)
        tvWelcomeUser = findViewById(R.id.tvWelcomeUser)
    }

    private fun setupRecyclerView() {
        adapter = CountryAdapter(
            countries = emptyList(),
            wishlistManager = wishlistManager,
            onItemClick = { country ->
                val intent = Intent(this, DetailActivity::class.java).apply {
                    putExtra(DetailActivity.EXTRA_COUNTRY, country)
                }
                startActivity(intent)
            },
            onWishlistClick = { country ->
                wishlistManager.toggleWishlist(country.countryCode)
                if (isWishlistOnlyMode) {
                    applyWishlistFilter()
                }
            }
        )
        rcvCountries.layoutManager = LinearLayoutManager(this)
        rcvCountries.adapter = adapter
    }

    private fun setupListeners() {
        btnSearch.setOnClickListener {
            performSearch()
        }

        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }

        btnDetectLocation.setOnClickListener {
            checkLocationPermissionAndDetect()
        }

        btnWishlistFilter.setOnClickListener {
            isWishlistOnlyMode = !isWishlistOnlyMode
            if (isWishlistOnlyMode) {
                btnWishlistFilter.text = "🌐 All Countries"
                applyWishlistFilter()
            } else {
                btnWishlistFilter.text = "⭐ Wishlist"
                updateList(fullCountryList)
            }
        }

        chipGroupRegion.setOnCheckedChangeListener { group, checkedId ->
            if (isWishlistOnlyMode) {
                isWishlistOnlyMode = false
                btnWishlistFilter.text = "⭐ Wishlist"
            }
            val chip = group.findViewById<Chip>(checkedId)
            if (chip != null) {
                val region = chip.text.toString()
                if (region.equals("All", ignoreCase = true)) {
                    updateList(fullCountryList)
                } else {
                    filterCountriesByRegion(region)
                }
            }
        }
    }

    private fun initFirebaseAuth() {
        try {
            val auth = FirebaseAuth.getInstance()
            if (auth.currentUser == null) {
                auth.signInAnonymously().addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val uid = auth.currentUser?.uid?.take(6) ?: "Guest"
                        tvWelcomeUser.text = "Welcome, Explorer ($uid)!"
                    } else {
                        tvWelcomeUser.text = "Welcome, Guest Explorer!"
                    }
                }
            } else {
                val uid = auth.currentUser?.uid?.take(6) ?: "Guest"
                tvWelcomeUser.text = "Welcome back, Explorer ($uid)!"
            }
        } catch (e: Exception) {
            tvWelcomeUser.text = "Welcome, Guest Explorer!"
        }
    }

    private fun loadAllCountries() {
        showLoading(true)
        lifecycleScope.launch {
            val networkList = try {
                val response = RetrofitClient.instance.getAllCountries()
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    response.body()
                } else null
            } catch (e: Exception) {
                null
            }

            val listToUse = if (!networkList.isNullOrEmpty()) {
                networkList
            } else {
                withContext(Dispatchers.IO) { loadFromAssets() }
            }

            showLoading(false)
            fullCountryList = listToUse.sortedBy { it.displayName }
            if (fullCountryList.isNotEmpty()) {
                updateList(fullCountryList)
            } else {
                showEmpty("Failed to load country list.")
            }
        }
    }

    private fun loadFromAssets(): List<CountryDto> {
        return try {
            val jsonString = assets.open("countries.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<CountryDto>>() {}.type
            Gson().fromJson<List<CountryDto>>(jsonString, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun performSearch() {
        val query = etSearch.text.toString().trim()
        if (query.isEmpty()) {
            updateList(fullCountryList)
            return
        }

        val filtered = fullCountryList.filter {
            it.displayName.contains(query, ignoreCase = true) ||
                    it.displayOfficialName.contains(query, ignoreCase = true)
        }

        if (filtered.isNotEmpty()) {
            updateList(filtered)
        } else {
            showEmpty("No country matching '$query'")
        }
    }

    private fun filterCountriesByRegion(region: String) {
        val filtered = fullCountryList.filter {
            it.displayRegion.equals(region, ignoreCase = true)
        }
        if (filtered.isNotEmpty()) {
            updateList(filtered)
        } else {
            showEmpty("No countries in region '$region'")
        }
    }

    private fun applyWishlistFilter() {
        val wishlistCodes = wishlistManager.getWishlistCodes()
        val wishlistedCountries = fullCountryList.filter { country ->
            wishlistCodes.contains(country.countryCode)
        }
        if (wishlistedCountries.isEmpty()) {
            showEmpty("Your Wishlist is currently empty.")
        } else {
            updateList(wishlistedCountries)
        }
    }

    private fun checkLocationPermissionAndDetect() {
        Toast.makeText(this, "Detecting your country...", Toast.LENGTH_SHORT).show()
        val fineGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            detectCurrentCountry()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun detectCurrentCountry() {
        showLoading(true)
        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        processLocation(location.latitude, location.longitude)
                    } else {
                        fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                            if (lastLoc != null) {
                                processLocation(lastLoc.latitude, lastLoc.longitude)
                            } else {
                                tryLocationManagerFallback()
                            }
                        }.addOnFailureListener {
                            tryLocationManagerFallback()
                        }
                    }
                }
                .addOnFailureListener {
                    tryLocationManagerFallback()
                }
        } catch (e: SecurityException) {
            tryLocationManagerFallback()
        }
    }

    private fun tryLocationManagerFallback() {
        try {
            val locManager = getSystemService(LOCATION_SERVICE) as? LocationManager
            val netLoc = try { locManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) } catch (e: SecurityException) { null }
            val gpsLoc = try { locManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER) } catch (e: SecurityException) { null }
            val bestLoc = netLoc ?: gpsLoc

            if (bestLoc != null) {
                processLocation(bestLoc.latitude, bestLoc.longitude)
            } else {
                fallbackToLocaleCountry()
            }
        } catch (e: Exception) {
            fallbackToLocaleCountry()
        }
    }

    private fun fallbackToLocaleCountry() {
        showLoading(false)
        val defaultCode = Locale.getDefault().country
        val codeToUse = if (defaultCode.equals("IN", ignoreCase = true) || defaultCode.isEmpty() || defaultCode.equals("US", ignoreCase = true)) "IN" else defaultCode
        showDetectedCountry(codeToUse)
    }

    private fun processLocation(latitude: Double, longitude: Double) {
        lifecycleScope.launch(Dispatchers.IO) {
            val countryCode = try {
                val geocoder = Geocoder(this@MainActivity, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                if (!addresses.isNullOrEmpty() && !addresses[0].countryCode.isNullOrEmpty()) {
                    addresses[0].countryCode
                } else {
                    "IN"
                }
            } catch (e: Exception) {
                "IN"
            }

            withContext(Dispatchers.Main) {
                showLoading(false)
                showDetectedCountry(countryCode ?: "IN")
            }
        }
    }

    private fun showDetectedCountry(code: String) {
        val targetCode = if (code.isBlank()) "IN" else code
        val match = fullCountryList.find {
            it.countryCode.equals(targetCode, ignoreCase = true) ||
                    it.cca2?.equals(targetCode, ignoreCase = true) == true ||
                    it.cca3?.equals(targetCode, ignoreCase = true) == true ||
                    it.displayName.equals("India", ignoreCase = true) && (targetCode.equals("IN", ignoreCase = true) || targetCode.equals("IND", ignoreCase = true))
        } ?: fullCountryList.find { it.displayName.equals("India", ignoreCase = true) }

        if (match != null) {
            Toast.makeText(this, "Detected Country: ${match.displayName}", Toast.LENGTH_LONG).show()
            val intent = Intent(this, DetailActivity::class.java).apply {
                putExtra(DetailActivity.EXTRA_COUNTRY, match)
            }
            startActivity(intent)
        } else {
            Toast.makeText(this, "Country detected: $targetCode", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateList(countries: List<CountryDto>) {
        adapter.updateData(countries)
        if (countries.isEmpty()) {
            showEmpty("No countries available.")
        } else {
            rcvCountries.visibility = View.VISIBLE
            tvEmptyMessage.visibility = View.GONE
        }
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        if (isLoading) {
            tvEmptyMessage.visibility = View.GONE
        }
    }

    private fun showEmpty(message: String) {
        tvEmptyMessage.text = message
        tvEmptyMessage.visibility = View.VISIBLE
        rcvCountries.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized) {
            adapter.notifyDataSetChanged()
        }
    }
}
