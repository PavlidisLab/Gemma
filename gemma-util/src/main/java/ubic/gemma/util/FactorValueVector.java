package ubic.gemma.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * @author luke
 *
 */
public class FactorValueVector {
    
    /**
     * An ordered list of ExperimentalFactors represented in this vector.
     * The order must be guaranteed across all FactorValueVectors.  i.e.:
     * any two FactorValueVectors containing FactorValues for the same
     * ExperimentalFactors must maintain this list in the same order.
     */
    private List<ExperimentalFactor> factors;
    private static final DescribableComparator factorComparator = DescribableComparator.getInstance();
    
    /**
     * A map from ExperimentalFactor to an ordered list of FactorValues.
     * The order must be guaranteed as above.
     */
    private Map<ExperimentalFactor, List<FactorValue>> valuesForFactor;
    private static final FactorValueComparator factorValueComparator = FactorValueComparator.getInstance();
    
    private String key;
    
    public FactorValueVector( Collection<FactorValue> values ) {
        
        valuesForFactor = new HashMap<ExperimentalFactor, List<FactorValue>>();
        for ( FactorValue value : values ) {
            if ( value.getExperimentalFactor() != null )
                getValuesForFactor( value.getExperimentalFactor() ).add( value );
        }
        for ( List<FactorValue> storedValues : valuesForFactor.values() )
            Collections.sort( storedValues, factorValueComparator );
        
        factors = new ArrayList<ExperimentalFactor>( valuesForFactor.keySet() );
        Collections.sort( factors, factorComparator );
        
        key = buildKey();
        
    }
    
    protected String buildKey() {
        StringBuffer buf = new StringBuffer();
        buf.append( "[" );
        for ( Iterator i = factors.iterator() ; i.hasNext(); ) {
            ExperimentalFactor factor = (ExperimentalFactor)i.next();
            buf.append( factor.getName() );
            buf.append( " => [ " );
            for ( Iterator it = getValuesForFactor( factor ).iterator() ; it.hasNext(); ) {
                buf.append( (FactorValue)it.next() );
                if ( it.hasNext() )
                    buf.append( ", " );
            }
            buf.append( " ] " );
            if ( i.hasNext() )
                buf.append( "; " );
        }
        buf.append( "]" );
        return buf.toString();
    }
    
    public List<FactorValue> getValuesForFactor( ExperimentalFactor factor ) {
        List<FactorValue> values = valuesForFactor.get( factor );
        if ( values == null ) {
            values = new ArrayList<FactorValue>();
            valuesForFactor.put( factor, values );
        }
        return values;
    }
    
    public List<ExperimentalFactor> getFactors() {
        return factors;
    }
    
    public String toString() {
        return key;
    }
    
    public int hashCode() {
        return key.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        if ( obj instanceof FactorValueVector )
            return key.equals( ( (FactorValueVector)obj ).key );
        else
            return false;
    }
}
