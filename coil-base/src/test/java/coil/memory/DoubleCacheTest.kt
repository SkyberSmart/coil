package coil.memory

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import coil.bitmap.FakeBitmapPool
import coil.bitmap.RealBitmapReferenceCounter
import coil.util.DEFAULT_BITMAP_SIZE
import coil.util.allocationByteCountCompat
import coil.util.createBitmap
import coil.util.isValid
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class DoubleCacheTest {
    private lateinit var context: Context
    //private lateinit var weakCache: EmptyWeakMemoryCache
    private lateinit var counter: RealBitmapReferenceCounter
    private lateinit var memoryCache: SingleMemoryCache
    private lateinit var diskCache: DiskCache
    private lateinit var cache: MemoryCache

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        //weakCache = EmptyWeakMemoryCache
        counter = RealBitmapReferenceCounter(EmptyWeakMemoryCache, FakeBitmapPool(), null)
        memoryCache = SingleMemoryCache(counter, Int.MAX_VALUE, null)
        diskCache = DiskCache(context,(Long.MAX_VALUE), null)
        cache = DoubleCache(memoryCache, diskCache, counter)
    }

    @Test
    fun `can retrieve memory cached value`() {
        val key = MemoryCache.Key("memory")
        val bitmap = createBitmap(width = 101, height = 101)

        assertNull(cache[key])

        counter.setValid(bitmap, true)
        memoryCache.set(key, bitmap, false)

        assertTrue(counter.isValid(bitmap))
        assertEquals(bitmap, cache[key])
        assertFalse(counter.isValid(bitmap))
    }

    @Test
    fun `can retrieve disk cached value`() {
        val key = MemoryCache.Key("disk")
        val bitmap = createBitmap(width = 102, height = 102)

        assertNull(cache[key])

        counter.setValid(bitmap, true)
        diskCache.set(key, bitmap)

        assertTrue(counter.isValid(bitmap))
        //assertEquals(bitmap, cache[key])
        assert(cache[key]?.let {
            bitmap.width == it.width && bitmap.height == it.height
        } ?: false)
        //assertFalse(counter.isValid(bitmap))
    }

    @Test
    fun `remove removes from both caches`() {
        val key = MemoryCache.Key("key")
        val bitmap = createBitmap()

        assertNull(cache[key])

        counter.setValid(bitmap, true)
        memoryCache.set(key, bitmap, false)
        diskCache.set(key, bitmap)

        assertTrue(cache.remove(key))
        assertNull(memoryCache.get(key))
        assertNull(diskCache.get(key))
    }

    @Test
    fun `clear clears all values`() {
        assertEquals(0, cache.size)

        memoryCache.set(MemoryCache.Key("a"), createBitmap(), false)
        memoryCache.set(MemoryCache.Key("b"), createBitmap(), false)
        diskCache.set(MemoryCache.Key("c"), createBitmap())
        diskCache.set(MemoryCache.Key("d"), createBitmap())

        assertEquals(2 * DEFAULT_BITMAP_SIZE, cache.size)

        cache.clear()

        assertEquals(0, cache.size)
        assertNull(cache[MemoryCache.Key("a")])
        assertNull(cache[MemoryCache.Key("b")])
        assertNull(cache[MemoryCache.Key("c")])
        assertNull(cache[MemoryCache.Key("d")])
    }

    @Test
    fun `set can be retrieved with get`() {
        val key = MemoryCache.Key("a")
        val bitmap = createBitmap()
        cache[key] = bitmap

        assertEquals(bitmap, cache[key])
    }

    @Test
    fun `set replaces memory and disk values`() {
        val key = MemoryCache.Key("a")
        val expected = createBitmap(width = 103, height = 103)

        memoryCache.set(key, createBitmap(width = 104, height = 104), false)
        diskCache.set(key, createBitmap(width = 105, height = 105))
        cache[key] = expected

        assertFalse(counter.isValid(expected))
        assertEquals(expected, memoryCache.get(key)?.bitmap)

        assert(diskCache.get(key)?.let {
            expected.width == it.width && expected.height == it.height
        } ?: false)

    }

    @Test
    fun `setting the same bitmap multiple times can only be removed once`() {
        val key = MemoryCache.Key("a")
        val bitmap = createBitmap()

        diskCache.set(key, bitmap)
        diskCache.set(key, bitmap)

        assertTrue(diskCache.remove(key))
        diskCache.flush()
        assertFalse(diskCache.remove(key))
    }
}
