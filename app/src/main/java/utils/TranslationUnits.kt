package utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.sprint_2_kotlin.R

/**
 * Maps a category name from the database to a translatable string resource.
 *
 * @param categoryName The English category name from Supabase (e.g., "Technology").
 * @return The translated string from your string resources.
 */
@Composable
fun getTranslatedCategoryName(categoryName: String): String {
    val resourceId = when (categoryName.lowercase()) {
        "economics" -> R.string.category_economics
        "politics" -> R.string.category_politics
        "science" -> R.string.category_science
        "health" -> R.string.category_health
        "sports" -> R.string.category_sports
        "climate" -> R.string.category_climate
        "business" -> R.string.category_business
        "technology" -> R.string.category_technology
        // Add other categories here
        else -> -1 // Default case for unknown categories
    }

    // If a mapping is found, return the string resource. Otherwise, return the original name.
    return if (resourceId != -1) {
        stringResource(id = resourceId)
    } else {
        categoryName
    }
}

@Composable
fun getTranslatedCountryName(countryName: String): String {
    val resourceId = when (countryName.lowercase()) {
        "afghanistan" -> R.string.country_afghanistan
        "albania" -> R.string.country_albania
        "algeria" -> R.string.country_algeria
        "andorra" -> R.string.country_andorra
        "angola" -> R.string.country_angola
        "antigua and barbuda" -> R.string.country_antigua_and_barbuda
        "argentina" -> R.string.country_argentina
        "armenia" -> R.string.country_armenia
        "australia" -> R.string.country_australia
        "austria" -> R.string.country_austria
        "azerbaijan" -> R.string.country_azerbaijan
        "bahamas" -> R.string.country_bahamas
        "bahrain" -> R.string.country_bahrain
        "bangladesh" -> R.string.country_bangladesh
        "barbados" -> R.string.country_barbados
        "belarus" -> R.string.country_belarus
        "belgium" -> R.string.country_belgium
        "belize" -> R.string.country_belize
        "benin" -> R.string.country_benin
        "bhutan" -> R.string.country_bhutan
        "bolivia" -> R.string.country_bolivia
        "bosnia and herzegovina" -> R.string.country_bosnia_and_herzegovina
        "botswana" -> R.string.country_botswana
        "brazil" -> R.string.country_brazil
        "brunei" -> R.string.country_brunei
        "bulgaria" -> R.string.country_bulgaria
        "burkina faso" -> R.string.country_burkina_faso
        "burundi" -> R.string.country_burundi
        "cabo verde" -> R.string.country_cabo_verde
        "cambodia" -> R.string.country_cambodia
        "cameroon" -> R.string.country_cameroon
        "canada" -> R.string.country_canada
        "central african republic" -> R.string.country_central_african_republic
        "chad" -> R.string.country_chad
        "chile" -> R.string.country_chile
        "china" -> R.string.country_china
        "colombia" -> R.string.country_colombia
        "comoros" -> R.string.country_comoros
        "congo (drc)" -> R.string.country_congo_dr
        "congo (republic)" -> R.string.country_congo_republic
        "costa rica" -> R.string.country_costa_rica
        "côte d'ivoire" -> R.string.country_cote_divoire
        "croatia" -> R.string.country_croatia
        "cuba" -> R.string.country_cuba
        "cyprus" -> R.string.country_cyprus
        "czech republic" -> R.string.country_czech_republic
        "denmark" -> R.string.country_denmark
        "djibouti" -> R.string.country_djibouti
        "dominica" -> R.string.country_dominica
        "dominican republic" -> R.string.country_dominican_republic
        "ecuador" -> R.string.country_ecuador
        "egypt" -> R.string.country_egypt
        "el salvador" -> R.string.country_el_salvador
        "equatorial guinea" -> R.string.country_equatorial_guinea
        "eritrea" -> R.string.country_eritrea
        "estonia" -> R.string.country_estonia
        "eswatini" -> R.string.country_eswatini
        "ethiopia" -> R.string.country_ethiopia
        "fiji" -> R.string.country_fiji
        "finland" -> R.string.country_finland
        "france" -> R.string.country_france
        "gabon" -> R.string.country_gabon
        "gambia" -> R.string.country_gambia
        "georgia" -> R.string.country_georgia
        "germany" -> R.string.country_germany
        "ghana" -> R.string.country_ghana
        "greece" -> R.string.country_greece
        "grenada" -> R.string.country_grenada
        "guatemala" -> R.string.country_guatemala
        "guinea" -> R.string.country_guinea
        "guinea-bissau" -> R.string.country_guinea_bissau
        "guyana" -> R.string.country_guyana
        "haiti" -> R.string.country_haiti
        "honduras" -> R.string.country_honduras
        "hungary" -> R.string.country_hungary
        "iceland" -> R.string.country_iceland
        "india" -> R.string.country_india
        "indonesia" -> R.string.country_indonesia
        "iran" -> R.string.country_iran
        "iraq" -> R.string.country_iraq
        "ireland" -> R.string.country_ireland
        "israel" -> R.string.country_israel
        "italy" -> R.string.country_italy
        "jamaica" -> R.string.country_jamaica
        "japan" -> R.string.country_japan
        "jordan" -> R.string.country_jordan
        "kazakhstan" -> R.string.country_kazakhstan
        "kenya" -> R.string.country_kenya
        "kiribati" -> R.string.country_kiribati
        "kuwait" -> R.string.country_kuwait
        "kyrgyzstan" -> R.string.country_kyrgyzstan
        "laos" -> R.string.country_laos
        "latvia" -> R.string.country_latvia
        "lebanon" -> R.string.country_lebanon
        "lesotho" -> R.string.country_lesotho
        "liberia" -> R.string.country_liberia
        "libya" -> R.string.country_libya
        "liechtenstein" -> R.string.country_liechtenstein
        "lithuania" -> R.string.country_lithuania
        "luxembourg" -> R.string.country_luxembourg
        "madagascar" -> R.string.country_madagascar
        "malawi" -> R.string.country_malawi
        "malaysia" -> R.string.country_malaysia
        "maldives" -> R.string.country_maldives
        "mali" -> R.string.country_mali
        "malta" -> R.string.country_malta
        "marshall islands" -> R.string.country_marshall_islands
        "mauritania" -> R.string.country_mauritania
        "mauritius" -> R.string.country_mauritius
        "mexico" -> R.string.country_mexico
        "micronesia" -> R.string.country_micronesia
        "moldova" -> R.string.country_moldova
        "monaco" -> R.string.country_monaco
        "mongolia" -> R.string.country_mongolia
        "montenegro" -> R.string.country_montenegro
        "morocco" -> R.string.country_morocco
        "mozambique" -> R.string.country_mozambique
        "myanmar" -> R.string.country_myanmar
        "namibia" -> R.string.country_namibia
        "nauru" -> R.string.country_nauru
        "nepal" -> R.string.country_nepal
        "netherlands" -> R.string.country_netherlands
        "new zealand" -> R.string.country_new_zealand
        "nicaragua" -> R.string.country_nicaragua
        "niger" -> R.string.country_niger
        "nigeria" -> R.string.country_nigeria
        "north korea" -> R.string.country_north_korea
        "north macedonia" -> R.string.country_north_macedonia
        "norway" -> R.string.country_norway
        "oman" -> R.string.country_oman
        "pakistan" -> R.string.country_pakistan
        "palau" -> R.string.country_palau
        "palestine state" -> R.string.country_palestine_state
        "panama" -> R.string.country_panama
        "papua new guinea" -> R.string.country_papua_new_guinea
        "paraguay" -> R.string.country_paraguay
        "peru" -> R.string.country_peru
        "philippines" -> R.string.country_philippines
        "poland" -> R.string.country_poland
        "portugal" -> R.string.country_portugal
        "qatar" -> R.string.country_qatar
        "romania" -> R.string.country_romania
        "russia" -> R.string.country_russia
        "rwanda" -> R.string.country_rwanda
        "saint kitts and nevis" -> R.string.country_saint_kitts_and_nevis
        "saint lucia" -> R.string.country_saint_lucia
        "saint vincent and the grenadines" -> R.string.country_saint_vincent_and_the_grenadines
        "samoa" -> R.string.country_samoa
        "san marino" -> R.string.country_san_marino
        "sao tome and principe" -> R.string.country_sao_tome_and_principe
        "saudi arabia" -> R.string.country_saudi_arabia
        "senegal" -> R.string.country_senegal
        "serbia" -> R.string.country_serbia
        "seychelles" -> R.string.country_seychelles
        "sierra leone" -> R.string.country_sierra_leone
        "singapore" -> R.string.country_singapore
        "slovakia" -> R.string.country_slovakia
        "slovenia" -> R.string.country_slovenia
        "solomon islands" -> R.string.country_solomon_islands
        "somalia" -> R.string.country_somalia
        "south africa" -> R.string.country_south_africa
        "south korea" -> R.string.country_south_korea
        "south sudan" -> R.string.country_south_sudan
        "spain" -> R.string.country_spain
        "sri lanka" -> R.string.country_sri_lanka
        "sudan" -> R.string.country_sudan
        "suriname" -> R.string.country_suriname
        "sweden" -> R.string.country_sweden
        "switzerland" -> R.string.country_switzerland
        "syria" -> R.string.country_syria
        "taiwan" -> R.string.country_taiwan
        "tajikistan" -> R.string.country_tajikistan
        "tanzania" -> R.string.country_tanzania
        "thailand" -> R.string.country_thailand
        "timor-leste" -> R.string.country_timor_leste
        "togo" -> R.string.country_togo
        "tonga" -> R.string.country_tonga
        "trinidad and tobago" -> R.string.country_trinidad_and_tobago
        "tunisia" -> R.string.country_tunisia
        "turkey" -> R.string.country_turkey
        "turkmenistan" -> R.string.country_turkmenistan
        "tuvalu" -> R.string.country_tuvalu
        "uganda" -> R.string.country_uganda
        "ukraine" -> R.string.country_ukraine
        "united arab emirates" -> R.string.country_united_arab_emirates
        "united kingdom" -> R.string.country_united_kingdom
        "united states" -> R.string.country_united_states
        "uruguay" -> R.string.country_uruguay
        "uzbekistan" -> R.string.country_uzbekistan
        "vanuatu" -> R.string.country_vanuatu
        "vatican city" -> R.string.country_vatican_city
        "venezuela" -> R.string.country_venezuela
        "vietnam" -> R.string.country_vietnam
        "yemen" -> R.string.country_yemen
        "zambia" -> R.string.country_zambia
        "zimbabwe" -> R.string.country_zimbabwe
        else -> -1 // Default case for unknown countries
    }

    // If a mapping is found, return the string resource. Otherwise, return the original name.
    return if (resourceId != -1) {
        stringResource(id = resourceId)
    } else {
        countryName
    }
}

@Composable
fun getTranslatedPQRStypeame(pqrstypename: String): String {
    val resourceId = when (pqrstypename.lowercase()) {
        "petition" -> R.string.petition
        "suggestion" -> R.string.suggestion
        "claim" -> R.string.claim
        "complaint" -> R.string.complaint
        // Add other categories here
        else -> -1 // Default case for unknown categories
    }

    // If a mapping is found, return the string resource. Otherwise, return the original name.
    return if (resourceId != -1) {
        stringResource(id = resourceId)
    } else {
        pqrstypename
    }
}
