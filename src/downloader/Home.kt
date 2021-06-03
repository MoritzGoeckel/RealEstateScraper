package downloader

import org.json.JSONObject

enum class Type{
    Offer,
    Ask
}

enum class Contract {
    Buy,
    Rent
}

enum class Currency{
    EUR,
    USD,
    None,
    Other,
    Ambiguous
}

data class Price(var type: Type = Type.None,
                 var currency: Currency = Currency.None,
                 var amount: Double? = null){

    enum class Type{
        None,
        Negotiable,
        Normal
    }
}

data class Home(var title: String? = null,
                var plz: String? = null,
                var address: String? = null,
                var squareMeters: Double? = null,
                var price: Price? = null,
                var description: String? = null,
                var images: ArrayList<String> = ArrayList<String>(),
                var contract: Contract? = null,
                var type: Type? = null,
                var rooms: Double? = null,
                var url: String? = null) {

    var raw = JSONObject();

    fun isFaulty(): Boolean{
        return price == null
                || url == null
                || faultiness() > 0.5
    }

    fun faultiness(): Double{
        val numMembers = 11.0
        var numFaulty = 0
        numFaulty += if(title == null) 1 else 0
        numFaulty += if(plz == null) 1 else 0
        numFaulty += if(address == null) 1 else 0
        numFaulty += if(squareMeters == null) 1 else 0
        numFaulty += if(price == null) 1 else 0
        numFaulty += if(description == null) 1 else 0
        numFaulty += if(images.isEmpty()) 1 else 0
        numFaulty += if(contract == null) 1 else 0
        numFaulty += if(type == null) 1 else 0
        numFaulty += if(rooms == null) 1 else 0
        numFaulty += if(url == null) 1 else 0
        return numFaulty / numMembers
    }
}