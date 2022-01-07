package com.crashcity.killaz

import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.net.URL
import java.time.LocalDate

@SpringBootApplication
class KillazApplication

val log = LoggerFactory.getLogger(KillazApplication::class.java)

fun main(args: Array<String>) {
	runApplication<KillazApplication>(*args)

	/*
	 * Date refers to cutoff date you want to look at. AKA earliest date you care for when a listing should be delisted
	 * or above the minEth value. If you wanna modify, keep the YYYY-MM-DD format. Only modify the variable. Nowhere else.
	 *
	 * minEth refers to the minimum threshold listings should be listed at. If you wanna modify, just be sure to keep
	 * to 2nd decimal place (i.e. 3eth == 3.00, 1.9999 == 1.99, etc.). Only modify the variable. Nowhere else.
	 *
	 * The wallet addresses you see below are named appropriately. When retrieving valid users for either, just be sure
	 * to update the first parameter in the getValidUsers() call. Feel free to switch between Ladiez & Killaz. Just don't
	 * forget to also update cutoffDate and minEth as well.
	 *
	 * Lastly, you can see invalidUsers call is commented out. You can uncomment that one and comment out validUsers call
	 * to look at wallet addresses that instead do have Killaz listed on or past a certain date OR where the listing is
	 * below the minEth value.
	 */

	val cutoffDate = LocalDate.parse("2021-10-07")
	val minEth = 2.00

	val killazWalletAddress = "0x21850dCFe24874382B12d05c5B189F5A2ACF0E5b"
	val ladyKillazWalletAddress = "0xE4D0E33021476Ca05aB22C8BF992D3b013752B80"

	val validUsers = getValidUsers(killazWalletAddress, cutoffDate, minEth)
	println(validUsers)
	println("\n")
	println(validUsers.size)

//	val invalidUsers = getInvalidUsers(killazWalletAddress, cutoffDate, minEth)
//	println(invalidUsers)
//	println("\n")
//	println(invalidUsers.size)
}

fun getValidUsers(walletAddress: String, cutoffDate: LocalDate, minEth: Double): Set<String> {
	val validUsers = mutableSetOf<String>()
	var id = 1
	while (id <= 500) {
		val parsedKillaListing = parseJson(getKillaJson(walletAddress, id))
		val validListing = parsedKillaListing?.let { isValidListing(it, cutoffDate, minEth) }
		validListing?.let { if (validListing) validUsers.add(parsedKillaListing.owner.address) }
		id++
	}
	log.info("Valid Wallet Addresses:")
	return validUsers
}

fun getInvalidUsers(walletAddress: String, cutoffDate: LocalDate, minEth: Double): Set<String> {
	val invalidUsers = mutableSetOf<String>()
	var id = 1
	while (id <= 9971) {
		val parsedKillaListing = parseJson(getKillaJson(walletAddress, id))
		val validListing = parsedKillaListing?.let { isValidListing(it, cutoffDate, minEth) }
		validListing?.let { if (!validListing) invalidUsers.add(parsedKillaListing.owner.address) }
		id++
	}
	log.info("Invalid Wallet Addresses:")
	return invalidUsers
}

fun getKillaJson(walletAddress: String, id: Int): String {
	val url = URL("https://api.opensea.io/api/v1/asset/${walletAddress}/${id}")
	val client = OkHttpClient()
	val request = Request.Builder()
			.url(url)
			.get()
			.build()
	val response = client.newCall(request).execute()
	return response.body!!.string()
}

fun parseJson(jsonString: String): KillaListing? {
	return Gson().fromJson(jsonString, KillaListing::class.java)
}

fun isValidListing(parsedKillaListing: KillaListing, cutoffDate: LocalDate, minEth: Double): Boolean {
	log.info("Validating Listing with ID: ${parsedKillaListing.token_id}")
	val listings = parsedKillaListing.orders

	// if this Killa has no listings, then it's never been listed
	if (listings.isEmpty()) return true

	// loop through listings and find first invalid one
	for (listing in listings) {
		val date = listing.created_date.substring(0..9)
		val listingDate = LocalDate.parse(date)

		val endDate = listing.closing_date?.substring(0..9)
		if (endDate == null || endDate.isEmpty()) {
			// then only look at listing date
			if (isListedDuringOrAfterCuttOffDate(listingDate, cutoffDate)) {
				val price = convertToETHDouble(listing.current_price)
				if (price < minEth) return false
			}
		} else {
			// look at both listing and closing dates
			val endListingDate = LocalDate.parse(endDate)
			if (isListedBeforeButNotClosedTillAfterCuttOffDate(listingDate, endListingDate, cutoffDate)
					|| isListedDuringOrAfterCuttOffDate(listingDate, cutoffDate)) {

				val price = convertToETHDouble(listing.current_price)
				if (price < minEth) return false
			}
		}
//		if (listingDate.isEqual(cutoffDate) || listingDate.isAfter(cutoffDate)) {
//			val price = convertToETHDouble(listing.current_price)
//			log.info("Listing Price = $price")
//			log.info("minEth = $minEth")
//			if (price < minEth) {
//				return false
//			}
//		}
	}
	return true
}

fun isListedDuringOrAfterCuttOffDate(listingDate: LocalDate, cutoffDate: LocalDate): Boolean {
	return listingDate.isEqual(cutoffDate) || listingDate.isAfter(cutoffDate)
}

fun isListedBeforeButNotClosedTillAfterCuttOffDate(listingDate: LocalDate, endListingDate: LocalDate, cutoffDate: LocalDate): Boolean {
	return listingDate.isBefore(cutoffDate) && endListingDate.isAfter(cutoffDate)
}

fun convertToETHDouble(currentPrice: String): Double {
//	log.info("Converting to Double. current_price == $currentPrice")
    return currentPrice.toFloat() * .000000000000000001
}