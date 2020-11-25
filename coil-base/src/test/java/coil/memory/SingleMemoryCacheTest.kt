package coil.memory

import coil.bitmap.BitmapPool
import coil.bitmap.RealBitmapReferenceCounter
import coil.util.DEFAULT_BITMAP_SIZE
import coil.util.createBitmap
import coil.util.executeQueuedMainThreadTasks
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

@RunWith(RobolectricTestRunner::class)
class SingleMemoryCacheTest {

    @Test
    fun `can retrieve cached value`() {
        val weakCache = EmptyWeakMemoryCache
        val pool = BitmapPool(Int.MAX_VALUE)
        val counter = RealBitmapReferenceCounter(weakCache, pool, null)
        val singleCache = SingleMemoryCache(counter, (2 * DEFAULT_BITMAP_SIZE), null)

        val bitmap = createBitmap()
        singleCache.set(MemoryCache.Key("1"), bitmap, false)

        assertEquals(bitmap, singleCache.get(MemoryCache.Key("1"))?.bitmap)
    }

    @Test
    fun `can delete cached value`() {
        val weakCache = EmptyWeakMemoryCache
        val pool = BitmapPool(Int.MAX_VALUE)
        val counter = RealBitmapReferenceCounter(weakCache, pool, null)
        val singleCache = SingleMemoryCache(counter, (2 * DEFAULT_BITMAP_SIZE), null)

        val first = createBitmap(width = 101, height = 101)
        counter.setValid(first, true)
        singleCache.set(MemoryCache.Key("1"), first, false)

        val second = createBitmap(width = 102, height = 102)
        singleCache.set(MemoryCache.Key("1"), second, false)

        singleCache.remove(MemoryCache.Key("1") )

        executeQueuedMainThreadTasks()

        assertNull(singleCache.get(MemoryCache.Key("1")))
        assertSame(first, pool.getDirtyOrNull(first.width, first.height, first.config))
    }

    @Test
    fun `least recently used value is evicted`() {
        val weakCache = EmptyWeakMemoryCache
        val pool = BitmapPool(Int.MAX_VALUE)
        val counter = RealBitmapReferenceCounter(weakCache, pool, null)
        val singleCache = SingleMemoryCache(counter, (2 * DEFAULT_BITMAP_SIZE), null)

        val first = createBitmap()
        singleCache.set(MemoryCache.Key("1"), first, false)

        val second = createBitmap()
        singleCache.set(MemoryCache.Key("2"), second, false)

        val third = createBitmap()
        singleCache.set(MemoryCache.Key("3"), third, false)

        assertNull(singleCache.get(MemoryCache.Key("1")))
    }

    @Test
    fun `maxSize 0 disables memory cache`() {
        val weakCache = EmptyWeakMemoryCache
        val pool = BitmapPool(Int.MAX_VALUE)
        val counter = RealBitmapReferenceCounter(weakCache, pool, null)
        val singleCache = SingleMemoryCache(counter, 0, null)

        val bitmap = createBitmap()
        singleCache.set(MemoryCache.Key("1"), bitmap, false)

        assertNull(singleCache.get(MemoryCache.Key("1")))
    }

    @Test
    fun `value is removed after invalidate is called`() {
        val weakCache = EmptyWeakMemoryCache
        val pool = BitmapPool(Int.MAX_VALUE)
        val counter = RealBitmapReferenceCounter(weakCache, pool, null)
        val singleCache = SingleMemoryCache(counter, (2 * DEFAULT_BITMAP_SIZE), null)

        val bitmap = createBitmap()
        singleCache.set(MemoryCache.Key("1"), bitmap, false)
        singleCache.remove(MemoryCache.Key("1"))

        assertNull(singleCache.get(MemoryCache.Key("1")))
    }

    @Test
    fun `valid evicted item is added to bitmap pool`() {
        val weakCache = EmptyWeakMemoryCache
        val pool = BitmapPool(Int.MAX_VALUE)
        val counter = RealBitmapReferenceCounter(weakCache, pool, null)
        val singleCache = SingleMemoryCache(counter, DEFAULT_BITMAP_SIZE, null)

        val first = createBitmap()
        counter.setValid(first, true)
        singleCache.set(MemoryCache.Key("1"), first, false)

        assertNotNull(singleCache.get(MemoryCache.Key("1")))

        val second = createBitmap()
        counter.setValid(second, true)
        singleCache.set(MemoryCache.Key("2"), second, false)

        executeQueuedMainThreadTasks()

        assertNull(singleCache.get(MemoryCache.Key("1")))
        assertSame(first, pool.getDirtyOrNull(first.width, first.height, first.config))
    }
}
