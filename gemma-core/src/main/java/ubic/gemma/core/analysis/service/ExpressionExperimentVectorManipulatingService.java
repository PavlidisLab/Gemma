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
package ubic.gemma.core.analysis.service;

import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.bioAssayData.RawExpressionDataVectorService;

import java.util.Arrays;
import java.util.List;

/**
 * @author pavlidis
 */
public abstract class ExpressionExperimentVectorManipulatingService {

    @Autowired
    protected ProcessedExpressionDataVectorService processedExpressionDataVectorService;

    @Autowired
    protected RawExpressionDataVectorService rawExpressionDataVectorService;

    /**
     * @param data where data will be stored, starts out empty
     * @param representation of the quantitation type for the vector
     * @param oldV vector to be converted
     */
    protected void convertFromBytes( List<Object> data, PrimitiveType representation, DesignElementDataVector oldV ) {
        if ( representation.equals( PrimitiveType.BOOLEAN ) ) {
            for ( boolean b : oldV.getDataAsBooleans() ) {
                data.add( b );
            }
        } else if ( representation.equals( PrimitiveType.CHAR ) ) {
            for ( char b : oldV.getDataAsChars() ) {
                data.add( b );
            }
        } else if ( representation.equals( PrimitiveType.DOUBLE ) ) {
            for ( double b : oldV.getDataAsDoubles() ) {
                data.add( b );
            }
        } else if ( representation.equals( PrimitiveType.INT ) ) {
            for ( int b : oldV.getDataAsInts() ) {
                data.add( b );
            }
        } else if ( representation.equals( PrimitiveType.LONG ) ) {
            for ( long b : oldV.getDataAsLongs() ) {
                data.add( b );
            }
        } else if ( representation.equals( PrimitiveType.STRING ) ) {
            data.addAll( Arrays.asList( oldV.getDataAsStrings() ) );
        } else {
            throw new UnsupportedOperationException( "Don't know how to handle " + representation );
        }
    }

}
