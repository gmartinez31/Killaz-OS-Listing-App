package com.crashcity.killaz

data class KillaListing(
        val token_id: String,
        val owner: Owner,
        val orders: List<Order>,
)

data class Owner(
        val address: String,
)

data class Order(
        val created_date: String,
        val closing_date: String?,
        val current_price: String,
)