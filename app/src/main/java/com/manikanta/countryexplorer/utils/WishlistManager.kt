package com.manikanta.countryexplorer.utils

import android.content.Context
import android.content.SharedPreferences

class WishlistManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("country_wishlist_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_WISHLIST = "wishlist_codes"
    }

    fun getWishlistCodes(): Set<String> {
        return prefs.getStringSet(KEY_WISHLIST, emptySet()) ?: emptySet()
    }

    fun isWishlisted(countryCode: String): Boolean {
        return getWishlistCodes().contains(countryCode)
    }

    fun addToWishlist(countryCode: String) {
        val current = getWishlistCodes().toMutableSet()
        current.add(countryCode)
        prefs.edit().putStringSet(KEY_WISHLIST, current).apply()
    }

    fun removeFromWishlist(countryCode: String) {
        val current = getWishlistCodes().toMutableSet()
        current.remove(countryCode)
        prefs.edit().putStringSet(KEY_WISHLIST, current).apply()
    }

    fun toggleWishlist(countryCode: String): Boolean {
        return if (isWishlisted(countryCode)) {
            removeFromWishlist(countryCode)
            false
        } else {
            addToWishlist(countryCode)
            true
        }
    }
}
