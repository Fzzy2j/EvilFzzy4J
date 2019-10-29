package me.fzzy.evilfzzy4j.voice


class LavaPlayerAudioProvider {//(val player: AudioPlayer) : AudioProvider(ByteBuffer.allocate(StandardAudioDataFormats.DISCORD_OPUS.maximumChunkSize())) {
    /*private val frame = MutableAudioFrame()

    init {
        // Set LavaPlayer's MutableAudioFrame to use the same buffer as the one we just allocated
        frame.setBuffer(buffer)
    }// Allocate a ByteBuffer for Discord4J's AudioProvider to hold audio data for Discord

    override fun provide(): Boolean {
        // AudioPlayer writes audio data to its AudioFrame
        val didProvide = player.provide(frame)
        // If audio was provided, flip from write-mode to read-mode
        if (didProvide) {
            buffer.flip()
        }
        return didProvide
    }*/
}