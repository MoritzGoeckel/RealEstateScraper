package ingest

import structures.Contract
import structures.Home

interface Downloader {
    fun download(query: String, contract: Contract, page: Int): MutableList<Home>
}