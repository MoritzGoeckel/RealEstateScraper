package structures

enum class Type{
    None,
    Offer,
    Ask
}

enum class Contract {
    None,
    Buy,
    Rent,
}

enum class Currency{
    None,
    EUR,
    USD,
    Other,
    Ambiguous
}

data class Price(var type: Type = Type.None,
                 var currency: Currency = Currency.None,
                 var amount: Double = Double.NaN){

    enum class Type{
        None,
        Negotiable,
        Normal
    }

    fun isValid(): Boolean{
        return !isFaulty()
    }

    fun isFaulty(): Boolean{
        return type == Type.None
                || currency == Currency.None
                || currency == Currency.Ambiguous
                || amount.isNaN()
    }
}

data class Home(var title: String = "",
                var plz: String = "",
                var address: String = "",
                var squareMetres: Double = Double.NaN,
                var price: Price = Price(),
                var description: String = "",
                var images: ArrayList<String> = ArrayList(),
                var contract: Contract = Contract.None,
                var type: Type = Type.None,
                var rooms: Double = Double.NaN,
                var url: String = "") {

    fun isValid(): Boolean{
        return !isFaulty()
    }

    fun isFaulty(): Boolean{
        return price.isFaulty()
                || url.isEmpty()
                || squareMetres.isNaN()
                || faultiness() > 0.5
    }

    fun faultiness(): Double{
        val numMembers = 11.0
        var numFaulty = 0
        numFaulty += if(title.isEmpty())  1 else 0
        numFaulty += if(plz.isEmpty()) 1 else 0
        numFaulty += if(address.isEmpty()) 1 else 0
        numFaulty += if(squareMetres.isNaN()) 1 else 0
        numFaulty += if(price.isFaulty()) 1 else 0
        numFaulty += if(description.isEmpty()) 1 else 0
        numFaulty += if(images.isEmpty()) 1 else 0
        numFaulty += if(contract == Contract.None) 1 else 0
        numFaulty += if(type == Type.None) 1 else 0
        numFaulty += if(rooms.isNaN()) 1 else 0
        numFaulty += if(url.isEmpty()) 1 else 0
        return numFaulty / numMembers
    }
}