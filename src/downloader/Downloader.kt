package downloader

import java.util.*

interface Downloader {
    fun download(query: String, contract: Contract, page: Int): MutableList<Home>
}