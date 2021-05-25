package downloader

import java.util.*

enum class Contract {
    Buy,
    Rent
}

interface Downloader {
    fun download(plz: String, contract: Contract): MutableList<Home>
}