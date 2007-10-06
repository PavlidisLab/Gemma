package ubic.gemma.web.controller.expression.experiment;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.util.CountingMap;
import ubic.gemma.util.FactorValueVector;

/**
 * @author luke
 *
 */
public class DesignMatrixRowValueObject implements Serializable {
    
    private static final long serialVersionUID = 1;
    
    private List<String> factors;
    
    private Map<String, String> factorValueMap;
    
    private int count;
    
    public DesignMatrixRowValueObject( FactorValueVector factorValues, int n ) {
        factors = new ArrayList<String>();
        factorValueMap = new HashMap<String, String>();
        for ( ExperimentalFactor factor : factorValues.getFactors() ) {
            factors.add( getFactorString( factor ) );
            factorValueMap.put( getFactorString( factor ), getFactorValueString( factorValues.getValuesForFactor( factor ) ) );
        }
        count = n;
    }
    
    private String getFactorString( ExperimentalFactor factor ) {
        return factor.getName();
    }
    
    private String getFactorValueString( List<FactorValue> factorValues ) {
        StringBuffer buf = new StringBuffer();
        for ( Iterator i = factorValues.iterator(); i.hasNext(); ) {
            buf.append( getFactorValueString( (FactorValue)i.next() ) );
            if ( i.hasNext() )
                buf.append( ", " );
        }
        return buf.toString();
    }
    
    private String getFactorValueString( FactorValue factorValue ) {
        StringBuffer buf = new StringBuffer();
        if ( !factorValue.getCharacteristics().isEmpty() ) { 
            for ( Iterator<Characteristic> i = factorValue.getCharacteristics().iterator(); i.hasNext(); ) {
                buf.append( i.next() );
                if ( i.hasNext() )
                    buf.append( ", " );
            }
        } else if ( !StringUtils.isEmpty( factorValue.getValue() ) ) {
            buf.append( factorValue.getValue() );
        }
        return buf.toString();
    }

    /**
     * @return the factors
     */
    public List<String> getFactors() {
        return factors;
    }

    /**
     * @param factors the factors to set
     */
    public void setFactors( List<String> factors ) {
        this.factors = factors;
    }

    /**
     * @return the factorValues
     */
    public Map<String, String> getFactorValueMap() {
        return factorValueMap;
    }

    /**
     * @param factorValues the factorValues to set
     */
    public void setFactorValueMap( Map<String, String> factorValueMap ) {
        this.factorValueMap = factorValueMap;
    }

    /**
     * @return the count
     */
    public int getCount() {
        return count;
    }

    /**
     * @param count the count to set
     */
    public void setCount( int count ) {
        this.count = count;
    }

    public static final class Factory {
        
        private Map<FactorValueVector, Collection<BioAssay>> assayCount;
        
        public static Collection<DesignMatrixRowValueObject> getDesignMatrix( ExpressionExperiment expressionExperiment ) {
            
            CountingMap<FactorValueVector> assayCount = new CountingMap<FactorValueVector>();
            for ( BioAssay assay : expressionExperiment.getBioAssays() ) {
                for ( BioMaterial sample : assay.getSamplesUsed() ) {
                    assayCount.increment( new FactorValueVector( sample.getFactorValues() ) );
                }
            }
            
            Collection<DesignMatrixRowValueObject> matrix = new ArrayList<DesignMatrixRowValueObject>();
            List<FactorValueVector> keys = assayCount.sortedKeyList( true );
            for ( FactorValueVector key : keys ) {
                matrix.add( new DesignMatrixRowValueObject( key, assayCount.get( key ) ) );
            }
            return matrix;
            
        }
        
    }
    
}