package downloader

interface Downloader {
    fun download(query: String, contract: Contract, page: Int): MutableList<Home>
}