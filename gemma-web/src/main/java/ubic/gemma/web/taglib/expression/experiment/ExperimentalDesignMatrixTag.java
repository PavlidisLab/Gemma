/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
import java.util.List;
import java.util.Map;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
 * @version $Id$
 */
public class ExperimentalDesignMatrixTag extends TagSupport {

    private static Log log = LogFactory.getLog( ExperimentalDesignMatrixTag.class.getName() );

    private static final long serialVersionUID = 1L;
    private ExpressionExperiment expressionExperiment;

    private Map<FactorValueVector, Collection<BioAssay>> assayCount = new HashMap<FactorValueVector, Collection<BioAssay>>();
    
    /**
     * @param design
     * @jsp.attribute required="true" rtexprvalue="true"
     */
    public void setExpressionExperiment( ExpressionExperiment expressionExperiment ) {
        this.expressionExperiment = expressionExperiment;
    }

    @Override
    public int doEndTag() {
        return EVAL_PAGE;
    }

    private Collection<BioAssay> getAssayCountEntry( BioMaterial sample ) {
        FactorValueVector key = new FactorValueVector( sample.getFactorValues() );
        Collection<BioAssay> assays = assayCount.get( key );
        if (assays == null) {
            assays = new ArrayList<BioAssay>();
            assayCount.put( key, assays );
        }
        return assays;
    }
    
    private String getHashKey( Collection<FactorValue> factorValues ) {
        List sortedValues = new ArrayList<FactorValue>( factorValues );
        Collections.sort( sortedValues );
        return StringUtils.join( sortedValues, "," );
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public int doStartTag() throws JspException {
        StringBuilder buf = new StringBuilder();

        List<ExperimentalFactor> experimentalFactors = new ArrayList<ExperimentalFactor>( expressionExperiment.getExperimentalDesign().getExperimentalFactors() );
        Collections.sort( experimentalFactors, factorComparator );
        
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
                buf.append( getString( vector.getValueForFactor( factor ) ) );
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
    
    private String getString( FactorValue value ) {
        if ( value == null ) {
            return "null";  // shouldn't happen...
        } else if ( value.getMeasurement() != null ) {
            return value.getMeasurement().getValue();
        } else {
            return value.getValue();
        }
    }
    
    static Comparator<ExperimentalFactor> factorComparator = new Comparator() {
        public int compare( Object o1, Object o2 ) {
            String o1Name = ( (ExperimentalFactor)o1 ).getName();
            String o2Name = ( (ExperimentalFactor)o2 ).getName();
            return o1Name.compareTo( o2Name );
        }
    };
    
    static Comparator<FactorValue> factorValueComparator = new Comparator() {
        public int compare( Object o1, Object o2 ) {
            String o1Name = ( (FactorValue)o1 ).toString();
            String o2Name = ( (FactorValue)o2 ).toString();
            return o1Name.compareTo( o2Name );
        }
    };
    
    class FactorValueVector {
        private String key;
        private List<FactorValue> values;
        
        public FactorValueVector( Collection<FactorValue> values ) {
            this.values = new ArrayList<FactorValue>( values );
            Collections.sort( this.values, factorValueComparator );
            key = new String ( StringUtils.join( values, ":" ) );
        }
        
        public FactorValue getValueForFactor(ExperimentalFactor factor) {
            for ( FactorValue value : values ) {
                if ( value.getExperimentalFactor().equals( factor ) )
                    return value;
            }
            return null;
        }
        
        public int hashCode() {
            return key.hashCode();
        }
        
        public boolean equals(Object obj) {
            if ( obj instanceof FactorValueVector )
                return key.equals( ( (FactorValueVector)obj ).key );
            else
                return false;
        }
    }
    
    class FactorValueVectorComparator implements Comparator<FactorValueVector> {
        List<ExperimentalFactor> factors;
        
        public FactorValueVectorComparator(List<ExperimentalFactor> factors) {
            this.factors = factors;
        }
        
        public int compare( FactorValueVector o1, FactorValueVector o2 ) {
            return compare( o1, o2, 0 );
        }
        
        private int compare( FactorValueVector o1, FactorValueVector o2, int i ) {
            if ( i >= factors.size() )
                return 0;

            String s1 = getString ( o1.getValueForFactor( factors.get( i ) ) );
            String s2 = getString ( o2.getValueForFactor( factors.get( i ) ) );
            int compare = s1.compareTo( s2 );
            if ( compare != 0 )
                return compare;
            else
                return compare( o1, o2, ++i );
        }
    }
}
