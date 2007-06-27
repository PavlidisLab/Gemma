package ubic.gemma.analysis.linkAnalysis;

import cern.colt.list.LongArrayList;
import cern.colt.list.ObjectArrayList;
import cern.colt.map.AbstractLongObjectMap;
import cern.colt.map.OpenLongObjectHashMap;

public class GenePair implements Comparable<GenePair> {
    private long firstId = 0;

    private long secondId = 0;

    private Double effectSize = 0.0;

    private AbstractLongObjectMap eeCorrelationMap;

    private double maxCorrelation;

    /**
     * Construct a gene pair with the specified pair of IDs and count
     * 
     * @param id1 - ID of first gene
     * @param id2 - ID of second gene
     */
    public GenePair( long id1, long id2 ) {
        this.firstId = id1;
        this.secondId = id2;
        eeCorrelationMap = new OpenLongObjectHashMap();
    }

    /**
     * Add a correlation
     * 
     * @param eeID - expression experiment ID
     * @param correlation
     */
    public void addCorrelation( long eeID, double correlation ) {
        eeCorrelationMap.put( eeID, new Double( correlation ) );
        if ( correlation > maxCorrelation ) {
            maxCorrelation = correlation;
        }
    }

    /**
     * Get a correlation for a specified expression experiment
     * 
     * @param eeID - expression experiment ID
     * @return correlation of the expression experiment
     */
    public Double getCorrelation( long eeID ) {
        return ( Double ) eeCorrelationMap.get( eeID );
    }

    public double getMaxCorrelation() {
        return maxCorrelation;
    }

    /**
     * Get the of correlations
     * 
     * @return list of correlations (Double)
     */
    public ObjectArrayList getCorrelations() {
        return eeCorrelationMap.values();
    }

    /**
     * Get the list of expression experiment IDs
     * 
     * @return list of expression experiment IDs
     */
    public LongArrayList getEEIDs() {
        return eeCorrelationMap.keys();
    }

    public int compareTo( GenePair o ) {
        return -effectSize.compareTo( o.effectSize );
    }

    public Double getEffectSize() {
        return effectSize;
    }

    public void setEffectSize( Double effectSize ) {
        this.effectSize = effectSize;
    }

    public long getFirstId() {
        return firstId;
    }

    public void setFirstId( long firstId ) {
        this.firstId = firstId;
    }

    public long getSecondId() {
        return secondId;
    }

    public void setSecondId( long secondId ) {
        this.secondId = secondId;
    }
}