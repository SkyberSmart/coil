package coil.memory

import android.graphics.Bitmap
import coil.bitmap.BitmapReferenceCounter
import coil.memory.MemoryCache.Key

//import uk.co.skyber.gardenmap.imageLoader.cache.ImageCache.Key

//SOURCE: https://www.lvguowei.me/post/android-design-pattern-imageloader/

internal class DoubleCache(
    private val memCache: SingleMemoryCache,
    private val diskCache: DiskCache,
    private val referenceCounter: BitmapReferenceCounter
) : MemoryCache {

    override val size get() = memCache.size

    override val maxSize get() = memCache.maxSize


    override fun get(key: Key): Bitmap? {
        val value =  memCache.get(key)?.bitmap ?: diskCache.get(key)
        return value?.also { referenceCounter.setValid(it, false) }
    }

    override fun set(key: Key, bitmap: Bitmap) {
        memCache.set(key, bitmap, false)
        diskCache.set(key, bitmap)
    }

    override fun remove(key: Key): Boolean {
        // Do not short circuit.
        val removedMemory = memCache.remove(key)
        val removedDisk = diskCache.remove(key)
        return removedMemory || removedDisk
    }

    override fun clear() {
        memCache.clearMemory()
        diskCache.clearCache()
    }

    interface Value {
        val bitmap: Bitmap
        val isSampled: Boolean
    }
}
