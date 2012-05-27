/*
 * The Gemma project
 * 
 * Copyright (c) 2007-2008 University of British Columbia
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
package ubic.gemma.web.taglib.expression.experiment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.common.Describable;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * Used to display the experimental information for a EE.
 * 
 * @jsp.tag name="eeDesignMatrix" body-content="empty"
 * @author luke
 * @deprecated use eeDesignMatrix.js (Ext table)
 * @version $Id$
 */
@Deprecated
public class ExperimentalDesignMatrixTag extends TagSupport {

    private static Log log = LogFactory.getLog( ExperimentalDesignMatrixTag.class.getName() );

    private static final long serialVersionUID = 1L;
    private ExpressionExperiment expressionExperiment;

    private Map<FactorValueVector, Collection<BioAssay>> assayCount;

    /**
     * @param design
     * @jsp.attribute required="true" rtexprvalue="true"
     */
    public void setExpressionExperiment( ExpressionExperiment expressionExperiment ) {
        this.expressionExperiment = expressionExperiment;
        assayCount = new HashMap<FactorValueVector, Collection<BioAssay>>();
    }

    @Override
    public int doEndTag() {
        return EVAL_PAGE;
    }

    /**
     * @param sample
     * @return
     */
    private Collection<BioAssay> getAssayCountEntry( BioMaterial sample ) {
        FactorValueVector key = new FactorValueVector( sample.getFactorValues() );
        Collection<BioAssay> assays = assayCount.get( key );
        if ( assays == null ) {
            assays = new ArrayList<BioAssay>();
            assayCount.put( key, assays );
        }
        return assays;
    }

    @Override
    public int doStartTag() throws JspException {
        StringBuilder buf = new StringBuilder();

        List<ExperimentalFactor> experimentalFactors = new ArrayList<ExperimentalFactor>( expressionExperiment
                .getExperimentalDesign().getExperimentalFactors() );
        Collections.sort( experimentalFactors, DescribableComparator.getInstance() );

        for ( BioAssay assay : expressionExperiment.getBioAssays() ) {
            for ( BioMaterial sample : assay.getSamplesUsed() ) {
                getAssayCountEntry( sample ).add( assay );
            }
        }
        List<FactorValueVector> vectors = new ArrayList<FactorValueVector>( assayCount.keySet() );
        Collections.sort( vectors, new FactorValueVectorComparator( experimentalFactors ) );

        buf.append( "<table>" );
        buf.append( "<tr>" );
        for ( ExperimentalFactor factor : experimentalFactors ) {
            buf.append( "<th>" );
            buf.append( factor.getName() );
            buf.append( "</th>" );
        }
        buf.append( "<th>assays</th>" );
        buf.append( "</tr>" );
        for ( FactorValueVector vector : vectors ) {
            buf.append( "<tr>" );
            for ( ExperimentalFactor factor : experimentalFactors ) {
                buf.append( "<td>" );
                buf.append( getString( vector.getValuesForFactor( factor ) ) );
                buf.append( "</td>" );
            }
            buf.append( "<td align=\"right\">" );
            buf.append( assayCount.get( vector ).size() );
            buf.append( "</td>" );
            buf.append( "</tr>" );
        }
        buf.append( "</table" );

        try {
            pageContext.getOut().print( buf.toString() );
        } catch ( Exception ex ) {
            log.error( ex, ex );
            throw new JspException( "experimental design view tag: " + ex.getMessage() );
        }
        return SKIP_BODY;
    }

    /**
     * @param values
     * @return
     */
    private String getString( Collection<FactorValue> values ) {
        if ( values == null || values.isEmpty() ) {
            return "unknown";
        }
        StringBuffer buf = new StringBuffer();
        for ( Iterator<FactorValue> it = values.iterator(); it.hasNext(); ) {
            buf.append( it.next() );
            if ( it.hasNext() ) buf.append( "<br>" );
        }
        return buf.toString();

    }

    static class FactorValueVectorComparator implements Comparator<FactorValueVector> {
        List<ExperimentalFactor> factors;

        public FactorValueVectorComparator( List<ExperimentalFactor> factors ) {
            this.factors = factors;
        }

        @Override
        public int compare( FactorValueVector o1, FactorValueVector o2 ) {
            return compare( o1, o2, 0 );
        }

        private int compare( FactorValueVector o1, FactorValueVector o2, int i ) {
            if ( i >= factors.size() ) return 0;

            String s1 = getString( o1.getValuesForFactor( factors.get( i ) ) );
            String s2 = getString( o2.getValuesForFactor( factors.get( i ) ) );
            int compare = s1.compareTo( s2 );
            if ( compare != 0 ) return compare;

            return compare( o1, o2, ++i );
        }

        private String getString( Collection<FactorValue> values ) {
            return values.toString();
        }
    }
}

class FactorValueVector {

    /**
     * An ordered list of ExperimentalFactors represented in this vector. The order must be guaranteed across all
     * FactorValueVectors. i.e.: any two FactorValueVectors containing FactorValues for the same ExperimentalFactors
     * must maintain this list in the same order.
     */
    private List<ExperimentalFactor> factors;
    private static final DescribableComparator factorComparator = DescribableComparator.getInstance();

    /**
     * A map from ExperimentalFactor to an ordered list of FactorValues. The order must be guaranteed as above.
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

        StringBuffer buf = new StringBuffer();
        for ( ExperimentalFactor factor : factors ) {
            buf.append( "; " );
            buf.append( factor.getName() );
            buf.append( " => [ " );
            for ( Iterator<FactorValue> it = getValuesForFactor( factor ).iterator(); it.hasNext(); ) {
                buf.append( it.next() );
                if ( it.hasNext() ) buf.append( ", " );
            }
            buf.append( " ] " );
        }
        key = buf.toString();
    }

    public List<FactorValue> getValuesForFactor( ExperimentalFactor factor ) {
        List<FactorValue> values = valuesForFactor.get( factor );
        if ( values == null ) {
            values = new ArrayList<FactorValue>();
            valuesForFactor.put( factor, values );
        }
        return values;
    }

    @Override
    public String toString() {
        return key;
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        if ( obj instanceof FactorValueVector ) return key.equals( ( ( FactorValueVector ) obj ).key );

        return false;
    }
}

class DescribableComparator implements Comparator<Describable> {
    private static DescribableComparator _instance = new DescribableComparator();

    public static DescribableComparator getInstance() {
        return _instance;
    }

    @Override
    public int compare( Describable d1, Describable d2 ) {
        String s1 = d1.getName();
        String s2 = d2.getName();
        if ( s1 != null ) {
            if ( s2 != null ) return s1.compareTo( s2 );

            return 1;
        }
        if ( s2 != null ) return -1;

        return 0;

    }
}

class FactorValueComparator implements Comparator<FactorValue> {
    private static FactorValueComparator _instance = new FactorValueComparator();

    public static FactorValueComparator getInstance() {
        return _instance;
    }

    @Override
    public int compare( FactorValue v1, FactorValue v2 ) {
        String s1 = v1.toString();
        String s2 = v2.toString();
        if ( s1 != null ) {
            if ( s2 != null ) return s1.compareTo( s2 );

            return 1;
        }
        if ( s2 != null ) return -1;

        return 0;

    }
}
