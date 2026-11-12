package com.manikanta.countryexplorer.model

import java.io.Serializable
import java.text.NumberFormat
import java.util.Locale

data class CountryDto(
    val name: NameDto? = null,
    val flags: FlagsDto? = null,
    val capital: List<String>? = null,
    val region: String? = null,
    val population: Long? = null,
    val currencies: Map<String, CurrencyDto>? = null,
    val languages: Map<String, String>? = null,
    val cca2: String? = null,
    val cca3: String? = null
) : Serializable {

    val displayName: String
        get() = name?.common ?: "Unknown"

    val displayOfficialName: String
        get() = name?.official ?: displayName

    val displayCapital: String
        get() = if (!capital.isNullOrEmpty()) capital.joinToString(", ") else "N/A"

    val displayRegion: String
        get() = region ?: "N/A"

    val displayPopulation: String
        get() = population?.let { NumberFormat.getInstance(Locale.US).format(it) } ?: "N/A"

    val displayCurrencies: String
        get() {
            if (currencies.isNullOrEmpty()) return "N/A"
            return currencies.values.joinToString(", ") { currency ->
                val nameStr = currency.name ?: ""
                val symbolStr = if (!currency.symbol.isNullOrEmpty()) " (${currency.symbol})" else ""
                "$nameStr$symbolStr".trim()
            }
        }

    val displayLanguages: String
        get() {
            if (languages.isNullOrEmpty()) return "N/A"
            return languages.values.joinToString(", ")
        }

    val flagUrl: String
        get() {
            if (!flags?.png.isNullOrEmpty()) return flags!!.png!!
            if (!flags?.svg.isNullOrEmpty()) return flags!!.svg!!
            val code = (cca2 ?: "").lowercase(Locale.ROOT)
            return if (code.isNotEmpty()) "https://flagcdn.com/w320/$code.png" else ""
        }

    val countryCode: String
        get() = (cca2 ?: cca3 ?: displayName).uppercase(Locale.ROOT)
}

data class NameDto(
    val common: String? = null,
    val official: String? = null
) : Serializable

data class FlagsDto(
    val png: String? = null,
    val svg: String? = null,
    val alt: String? = null
) : Serializable

data class CurrencyDto(
    val name: String? = null,
    val symbol: String? = null
) : Serializable
