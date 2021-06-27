package ingest

import structures.Contract
import structures.Home

interface Downloader {
    fun download(contract: Contract, page: Int): MutableList<Home>
}