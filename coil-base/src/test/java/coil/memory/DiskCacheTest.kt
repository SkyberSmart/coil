package coil.memory

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import coil.util.DEFAULT_BITMAP_SIZE
import coil.util.DEFAULT_BITMAP_SIZE_ON_DISK
import coil.util.createBitmap
import coil.util.executeQueuedMainThreadTasks
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertNull
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
class DiskCacheTest {

    private lateinit var context: Context

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `can retrieve cached value`() {
        val diskCache = DiskCache(context,(2 * DEFAULT_BITMAP_SIZE.toLong()), null)

        val bitmap = createBitmap(width = 101, height = 101)
        diskCache.set(MemoryCache.Key("1"), bitmap)

        assert(diskCache.get(MemoryCache.Key("1"))?.let {
            bitmap.width == it.width && bitmap.height == it.height
        } ?: false)

        diskCache.clearCache()
    }

    @Test
    fun `can delete cached value`() {
        val diskCache = DiskCache(context,(2 * DEFAULT_BITMAP_SIZE.toLong()), null)

        val first = createBitmap(width = 101, height = 101)
        diskCache.set(MemoryCache.Key("1"), first)

        val second = createBitmap(width = 102, height = 102)
        diskCache.set(MemoryCache.Key("1"), second)

        diskCache.remove(MemoryCache.Key("1") )

        assertNull(diskCache.get(MemoryCache.Key("1")))
        diskCache.clearCache()
    }

    @Test
    fun `least recently used value is evicted`() {
        val diskCache = DiskCache(context,(2 * DEFAULT_BITMAP_SIZE_ON_DISK.toLong()), null)

        val first = createBitmap(width = 101, height = 101)
        diskCache.set(MemoryCache.Key("1"), first)

        val second = createBitmap(width = 102, height = 102)
        diskCache.set(MemoryCache.Key("2"), second)

        diskCache.flush()
        assertNotNull(diskCache.get(MemoryCache.Key("1")))
        assertNotNull(diskCache.get(MemoryCache.Key("2")))

        val third = createBitmap(width = 103, height = 103)
        diskCache.set(MemoryCache.Key("3"), third)

        diskCache.flush()
        assertNull(diskCache.get(MemoryCache.Key("1")))

        diskCache.clearCache()
    }

    @Test
    fun `maxSize 0 disables disk cache`() {
        val diskCache = DiskCache(context, 0, null)

        val bitmap = createBitmap()
        diskCache.set(MemoryCache.Key("1"), bitmap)

        assertNull(diskCache.get(MemoryCache.Key("1")))
        diskCache.clearCache()
    }
}
