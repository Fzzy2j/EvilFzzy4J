package me.fzzy.robofzzy4j.util

import ws.schild.jave.FFMPEGLocator

class FFMPEGLocalLocator : FFMPEGLocator() {
    override fun getFFMPEGExecutablePath(): String {
        return "ffmpeg.exe"
    }

}