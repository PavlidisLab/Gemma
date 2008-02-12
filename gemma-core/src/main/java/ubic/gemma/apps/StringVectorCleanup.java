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
package ubic.gemma.apps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.StringUtils;

import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.datastructure.matrix.VectorMarshall;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Remove tabs from strings stored in the database. Can also check all vectors for correct sizes (a useful database
 * check, but slow). This is more or less a one-off, it was used to clean up errors that shouldn't happen any more (!)
 * 
 * @author pavlidis
 * @version $Id$
 */
public class StringVectorCleanup extends ExpressionExperimentManipulatingCLI {

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        super.buildOptions();
        this
                .addOption( OptionBuilder.withDescription(
                        "Examine ALL vectors for correct sizes, "
                                + "not just string types. Slow but useful check of the integrity of the system" )
                        .create( 'f' ) );
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( this.hasOption( 'f' ) ) {
            this.fullCheck = true;
            log.info( "A full check of all vectors will be done" );
        }
    }

    DesignElementDataVectorService dedvs;
    QuantitationTypeService qts;
    private boolean fullCheck = false;

    @SuppressWarnings("unchecked")
    @Override
    protected Exception doWork( String[] args ) {
        Exception e = processCommandLine( "remove tabs from strings and check vectors", args );
        if ( e != null ) return e;

        qts = ( QuantitationTypeService ) this.getBean( "quantitationTypeService" );

        dedvs = ( DesignElementDataVectorService ) this.getBean( "designElementDataVectorService" );

        for ( ExpressionExperiment ee : expressionExperiments ) {
            processExperiment( ee );
        }

        summarizeProcessing();
        return null;

    }

    /**
     * @param ee
     */
    @SuppressWarnings("unchecked")
    private void processExperiment( ExpressionExperiment ee ) {
        Collection<QuantitationType> types = this.eeService.getQuantitationTypes( ee );

        ByteArrayConverter converter = new ByteArrayConverter();

        qtype: for ( QuantitationType type : types ) {
            boolean isStringType = type.getRepresentation().equals( PrimitiveType.STRING );
            if ( !isStringType && !fullCheck ) continue;

            log.info( "Processing " + type );
            Collection<DesignElementDataVector> vecs = dedvs.find( type );
            dedvs.thaw( vecs );

            boolean changed = false;
            int count = 0;
            for ( DesignElementDataVector vector : vecs ) {

                if ( isStringType ) {
                    byte[] dat = vector.getData();

                    int numBioAssays = vector.getBioAssayDimension().getBioAssays().size();
                    String[] rawStrings = converter.byteArrayToStrings( dat );
                    List<String> updated = new ArrayList<String>();
                    for ( String string : rawStrings ) {
                        if ( string.equals( "\t" ) ) {
                            changed = true;
                        } else {
                            updated.add( string );
                        }
                    }

                    if ( updated.size() != numBioAssays ) {
                        dedvs.thaw( vector );
                        log.error( "Vector " + vector.getId()
                                + " did not have right number of values after 'tab' removal for " + type
                                + "; expected " + numBioAssays + " got " + updated.size() + "; "
                                + vector.getExpressionExperiment() );
                        continue qtype;
                    }

                    if ( changed ) {
                        byte[] newDat = converter.toBytes( updated.toArray( new String[] {} ) );
                        vector.setData( newDat );
                    }

                } else if ( fullCheck ) {
                    List<Object> vec = VectorMarshall.marshall( vector );
                    int numBioAssays = vector.getBioAssayDimension().getBioAssays().size();
                    if ( vec.size() != numBioAssays ) {
                        dedvs.thaw( vector );
                        eeService.thawLite( vector.getExpressionExperiment() );
                        log.error( "Vector " + vector.getId() + " did not have right number of values  " + type
                                + "; expected " + numBioAssays + " got " + vec.size() + "; "
                                + vector.getExpressionExperiment() );
                        log.error( "Values:\n" + StringUtils.join( vec, "," ) );
                        continue qtype;
                    }
                }
                if ( ++count % 10000 == 0 ) {
                    log.info( "Processed " + count + " vectors for " + type );
                }
            }

            if ( changed ) {
                log.info( "Updating " + vecs.size() + " vectors that may have contained 'tab'." );
                dedvs.update( vecs );
            }

        }
    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        StringVectorCleanup c = new StringVectorCleanup();
        Exception e = c.doWork( args );
        if ( e != null ) {
            log.fatal( e, e );
        }

    }
}
