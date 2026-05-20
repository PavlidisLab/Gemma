/*
 * The baseCode project
 *
 * Copyright (c) 2008-2019 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.web.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Convenience Map that has a count as the value for each key. Calling increment(K key) increases the count for
 * <em>key</em>.
 * <p>
 * This replaces the idiom of map.containsKey(k) ? map.put(k, map.get(k) + 1) : map.put(k, 0);
 * <p>
 * Ported in-tree from {@code ubic.basecode.dataStructure.CountingMap} as part of the baseCode in-tree migration.
 *
 * @author luke
 *
 * @param <K>
 */
public class CountingMap<K> implements Map<K, Integer> {
    private class AscendingCountComparator extends CountComparator {
        @Override
        public int compare( K key1, K key2 ) {
            return map.get( key1 ).compareTo( map.get( key2 ) );
        }
    }

    private abstract class CountComparator implements Comparator<K> {
    }

    private class DescendingCountComparator extends CountComparator {
        @Override
        public int compare( K key1, K key2 ) {
            return map.get( key2 ).compareTo( map.get( key1 ) );
        }
    }

    private Map<K, Integer> map;

    /**
     * Constructs a CountingMap backed by a simple HashMap.
     */
    public CountingMap() {
        this( new HashMap<K, Integer>() );
    }

    /**
     * Constructs a CountingMap backed by the specified Map.
     *
     * @param map the backing Map
     */
    public CountingMap( Map<K, Integer> map ) {
        this.map = map;
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public boolean containsKey( Object key ) {
        return map.containsKey( key );
    }

    @Override
    public boolean containsValue( Object value ) {
        return map.containsValue( value );
    }

    /**
     * Returns the count associated with the specified key, or zero if the key has never been incremented.
     *
     * @param key the key whose associated count is to be returned
     * @return the count associated with the specified key, or zero if the key has never been incremented
     */
    public int count( K key ) {
        Integer i = map.get( key );
        return i == null ? 0 : i;
    }

    @Override
    public Set<java.util.Map.Entry<K, Integer>> entrySet() {
        return map.entrySet();
    }

    @Override
    public boolean equals( Object o ) {
        return map.equals( o );
    }

    @Override
    public Integer get( Object key ) {
        return map.containsKey( key ) ? map.get( key ) : 0;
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    /**
     * Increments the count associated with the specified key and returns the incremented count. If the key doesn't
     * already exist in the map, it will be added.
     *
     * @param key the key whose associated count is to be incremented
     * @return the incremented value associated with the specified key
     */
    public int increment( K key ) {
        Integer i = map.get( key );
        if ( i == null ) i = 0;
        map.put( key, ++i );
        return i;
    }

    /**
     * Increments the count associated with the specified keys. If a key doesn't already exist in the map, it will be
     * added.
     *
     * @param keys the keys whose associated count is to be incremented
     */
    public void incrementAll( Collection<K> keys ) {
        for ( K key : keys ) {
            increment( key );
        }
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public Set<K> keySet() {
        return map.keySet();
    }

    public int max() {
        int r = 0;
        for ( Integer i : map.values() ) {
            if ( i > r ) {
                r = i;
            }
        }
        return r;
    }

    @Override
    public Integer put( K key, Integer value ) {
        return map.put( key, value );
    }

    @Override
    public void putAll( Map<? extends K, ? extends Integer> t ) {
        map.putAll( t );
    }

    @Override
    public Integer remove( Object key ) {
        return map.remove( key );
    }

    /**
     * Returns true if the specified key has ever been incremented, false otherwise.
     *
     * @param key the key whose presence is to be tested
     * @return true if the specified key has ever been incremented, false otherwise
     */
    public boolean seen( K key ) {
        return map.containsKey( key );
    }

    @Override
    public int size() {
        return map.size();
    }

    /**
     * Returns a list of the keys in this map, sorted by ascending count.
     *
     * @return a list of the keys in this map, sorted by ascending count
     */
    public List<K> sortedKeyList() {
        return sortedKeyList( false );
    }

    /**
     * Returns a list of the keys in this map, sorted as specified.
     *
     * @param sortDescending true to sort by descending count, false to sort by ascending count
     * @return a list of the keys in this map, sorted as specified.
     */
    public List<K> sortedKeyList( boolean sortDescending ) {
        List<K> keys = new ArrayList<K>( keySet() );
        Collections.sort( keys, sortDescending ? new DescendingCountComparator() : new AscendingCountComparator() );
        return keys;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder( "[" );
        boolean first = true;
        for ( K key : keySet() ) {
            if ( !first ) sb.append( ", " );
            sb.append( key.toString() + "=" + map.get( key ) );
            first = false;
        }
        return sb.toString() + "]";
    }

    /**
     * Returns the sum of all counts in the map.
     */
    public int total() {
        int summation = 0;
        for ( int value : map.values() ) {
            summation += value;
        }
        return summation;
    }

    @Override
    public Collection<Integer> values() {
        return map.values();
    }

}
