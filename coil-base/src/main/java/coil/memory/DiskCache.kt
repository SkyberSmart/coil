package coil.memory

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import coil.util.Logger
import coil.memory.MemoryCache.Key
import com.jakewharton.disklrucache.DiskLruCache
import java.io.*

/** An disk cache that holds strong references [Bitmap]s. */
internal interface DiskCache {

    companion object {
        operator fun invoke(
            context: Context,
            maxSize: Long,
            logger: Logger?
        ): DiskCache {
            return when {
                maxSize > 0 -> RealDiskCache(context, maxSize, logger)
                else -> EmptyDiskCache
            }
        }
    }

    /** The current size of the memory cache in bytes. */
    val size: Long

    /** The maximum size of the memory cache in bytes. */
    val maxSize: Long

    /** Get the value associated with [key]. */
    fun get(key: Key): Bitmap?

    /** Set the value associated with [key]. */
    fun set(key: Key, bitmap: Bitmap)

    /** Remove the value referenced by [key] from this cache. */
    fun remove(key: Key): Boolean

    /** Remove all values from this cache. */
    fun clearCache()

    /** Force buffered operations to the file system. */
    fun flush()

}

/** A [StrongMemoryCache] implementation that caches nothing. */
private object EmptyDiskCache : DiskCache {

    override val size: Long get() = 0

    override val maxSize: Long get() = 0

    override fun get(key: Key): Bitmap? = null

    override fun set(key: Key, bitmap: Bitmap) {}

    override fun remove(key: Key) = false

    override fun clearCache() {}

    override fun flush() {}

}

/** A [StrongMemoryCache] implementation backed by an [LruCache]. */
private class RealDiskCache(
    context: Context,
    maxSize: Long,
    private val logger: Logger?
) : DiskCache {

    private val cache = DiskLruCache.open(context.cacheDir, 1, 1, maxSize) //10 * 1024 * 1024

    override val size get() = cache.size()

    override val maxSize get() = cache.maxSize

    @Synchronized
    override  fun get(key: Key): Bitmap? {
        val cacheKey = key.md5
        val snapshot: DiskLruCache.Snapshot? = cache.get(cacheKey)
        return if (snapshot != null) {
            val inputStream: InputStream = snapshot.getInputStream(0)
            val buffIn = BufferedInputStream(inputStream, 8 * 1024)
            BitmapFactory.decodeStream(buffIn)
        } else {
            null
        }
    }


    @Synchronized
    override fun set(key: Key, bitmap: Bitmap) {
        val cacheKey = key.md5
        var editor: DiskLruCache.Editor? = null
        try {
            editor = cache.edit(cacheKey)
            if (editor == null) {
                return
            }
            if (writeBitmapToFile(bitmap, editor)) {
                cache.flush()
                editor.commit()
            } else {
                editor.abort()
            }
        } catch (e: IOException) {
            try {
                editor?.abort()
            } catch (ignored: IOException) {
            }
        }
    }


    private fun writeBitmapToFile(bitmap: Bitmap, editor: DiskLruCache.Editor): Boolean {
        var out: OutputStream? = null
        try {
            out = BufferedOutputStream(editor.newOutputStream(0), 8 * 1024)
            return bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        } finally {
            out?.close()
        }
    }


    @Synchronized
    override fun remove(key: Key): Boolean {
        try {
            cache.remove(key.md5)
        } catch (error: IOException){
            return false
        }
        return true
    }

    @Synchronized
    override fun clearCache() {
        //logger?.log(TAG, Log.VERBOSE) { "clearMemory" }
        cache.delete()
    }

    override fun flush() {
        cache.flush()
    }

    companion object {
        private const val TAG = "RealStrongMemoryCache"
    }
}
