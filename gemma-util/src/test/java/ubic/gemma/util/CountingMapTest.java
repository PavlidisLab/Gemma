package ubic.gemma.util;

import junit.framework.TestCase;

/**
 * @author luke
 *
 */
public class CountingMapTest extends TestCase
{
    private final String KEY_1 = "key";
    private final String KEY_2 = "key2";
    private final String KEY_3 = "key3";
    private final String KEY_4 = "key4";
    
    /**
     * Test method for {@link ca.elmonline.util.CountingMap#increment(java.lang.Object)}.
     */
    public void testIncrement()
    {
        CountingMap<String> map = new CountingMap<String>();
        assertEquals("increment of new key returns 1",
                map.increment( KEY_1 ), 1);
        assertEquals("increment of same key returns 2",
                map.increment( KEY_1 ), 2);
    }

    /**
     * Test method for {@link ca.elmonline.util.CountingMap#count(java.lang.Object)}.
     */
    public void testCount() {
        CountingMap<String> map = new CountingMap<String>();
        assertEquals("count of non-existant key returns 0",
                map.count( KEY_1 ), 0);
        map.increment( KEY_1 );
        assertEquals("count of once-incremented key returns 1",
                map.count( KEY_1 ), 1);
        map.increment( KEY_1 );
        assertEquals("count of twice-incremented key returns 2",
                map.count( KEY_1 ), 2);
    }

    /**
     * Test method for {@link ca.elmonline.util.CountingMap#seen(java.lang.Object)}.
     */
    public void testSeen() {
        CountingMap<String> map = new CountingMap<String>();
        assertEquals("seen of non-existant key returns false",
                map.seen( KEY_1 ), false);
        map.increment( KEY_1 );
        assertEquals("seen of incremented key returns true",
                map.seen( KEY_1 ), true);
    }
    
    /**
     * Test method for {@link ca.elmonline.util.CountingMap#sortedEntrySet(java.lang.Object)}.
     */
    public void testSortedKeyList() {
        CountingMap<String> map = new CountingMap<String>();
        map.increment( KEY_1 );
        map.increment( KEY_2 ); map.increment( KEY_2 );
        map.increment( KEY_3 ); map.increment( KEY_3 );
        map.increment( KEY_4 ); map.increment( KEY_4 ); map.increment( KEY_4 );
        int previous = Integer.MIN_VALUE;
        for ( String key : map.sortedKeyList() ) {
            if ( map.get( key ) < previous )
                fail("subsequent count less than previous value in supposedly sorted entry set");
            previous = map.get( key );
        }
    }
    
    /**
     * Test method for {@link ca.elmonline.util.CountingMap#sortedEntrySet(boolean)}.
     */
    public void testSortedKeyListBoolean() {
        CountingMap<String> map = new CountingMap<String>();
        map.increment( KEY_1 );
        map.increment( KEY_2 ); map.increment( KEY_2 );
        map.increment( KEY_3 ); map.increment( KEY_3 );
        map.increment( KEY_4 ); map.increment( KEY_4 ); map.increment( KEY_4 );
        int previous = Integer.MAX_VALUE;
        for ( String key : map.sortedKeyList( true ) ) {
            if ( map.get( key ) > previous )
                fail("subsequent count greater than previous value in supposedly sorted entry set");
            previous = map.get( key );
        }
    }
}
