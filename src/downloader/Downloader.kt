package downloader

import java.util.*

interface Downloader {
    fun download(from: Date,  until: Date)
}