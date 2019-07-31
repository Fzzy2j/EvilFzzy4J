package me.fzzy.evilfzzy4j.util

enum class MediaType constructor(val formats: List<String>) {
    IMAGE(arrayListOf("png", "jpg", "jpeg", "gif")),
    VIDEO(arrayListOf("webm", "mp4")),
    IMAGE_AND_VIDEO(IMAGE.formats.plus(VIDEO.formats))
}