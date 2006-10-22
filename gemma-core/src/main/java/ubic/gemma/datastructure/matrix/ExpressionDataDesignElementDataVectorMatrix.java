/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.datastructure.matrix;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author keshav
 * @version $Id$
 */
public class ExpressionDataDesignElementDataVectorMatrix {
    private Log log = LogFactory.getLog( this.getClass() );

    private ExpressionExperiment expressionExperiment = null;

    private Collection<DesignElement> designElements = null;

    private Map<String, DesignElementDataVector> dataMap = new HashMap<String, DesignElementDataVector>();

    /**
     * @param expressionExperiment
     * @param designElements
     */
    public ExpressionDataDesignElementDataVectorMatrix( ExpressionExperiment expressionExperiment, Collection<DesignElement> designElements ) {
        if ( expressionExperiment == null || designElements == null )
            throw new RuntimeException( "Either expression experiment or collection of design elements does not exist." );

        this.expressionExperiment = expressionExperiment;
        this.designElements = designElements;

        for ( DesignElement designElement : designElements ) {
            String key = ( ( CompositeSequence ) designElement ).getName();

            // FIXME what about the quantitation type?
            Collection<DesignElementDataVector> vectors = ( ( CompositeSequence ) designElement )
                    .getDesignElementDataVectors();
            Iterator iter = vectors.iterator();
            DesignElementDataVector vector = ( DesignElementDataVector ) iter.next();

            dataMap.put( key, vector );
        }
    }

    /**
     * Log the data values
     */
    @Override
    public String toString() {
        assert designElements != null : "Design Elements not initialized";
        StringBuilder b = new StringBuilder();
        int rowsDone = 0;
        for ( DesignElement designElement : designElements ) {
            ByteArrayConverter converter = new ByteArrayConverter();
            // FIXME quantitation type
            Collection<DesignElementDataVector> vectors = ( ( CompositeSequence ) designElement )
                    .getDesignElementDataVectors();
            Iterator iter = vectors.iterator();
            DesignElementDataVector vector = ( DesignElementDataVector ) iter.next();

            String key = ( ( CompositeSequence ) designElement ).getName();

            byte[] byteData = vector.getData();
            double[] data = converter.byteArrayToDoubles( byteData );

            b.append( key );
            for ( int j = 0; j < Math.min( data.length, 10 ); j++ ) {
                b.append( " " + data[j] );
            }
            if ( data.length < 10 ) {
                b.append( "..." );
            }
            b.append( "\n" );
            rowsDone++;
            if ( rowsDone > 10 ) {
                b.append( "..." );
                break;
            }
        }
        return b.toString();
    }

    /**
     * @return Collection<DesignElement>
     */
    public Collection<DesignElement> getDesignElements() {
        return designElements;
    }

    /**
     * @return ExpressionExperiment
     */
    public ExpressionExperiment getExpressionExperiment() {
        return expressionExperiment;
    }

    /**
     * @return Returns the dataMap.
     */
    public Map<String, DesignElementDataVector> getDataMap() {
        return dataMap;
    }
}
