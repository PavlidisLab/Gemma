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
package ubic.gemma.apps;

import ubic.gemma.loader.expression.arrayDesign.ArrayDesignSequenceAlignmentService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.genome.Taxon;

/**
 * Command line interface to run blat on the sequences for a microarray; the results are persisted in the DB. You must
 * start the BLAT server first before using this.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ArrayDesignBlatCli extends ArrayDesignSequenceManipulatingCli {
    ArrayDesignSequenceAlignmentService arrayDesignSequenceAlignmentService;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#buildOptions()
     */
    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        super.buildOptions();
    }

    public static void main( String[] args ) {
        ArrayDesignBlatCli p = new ArrayDesignBlatCli();
        try {
            Exception ex = p.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( "Array design sequence BLAT - only works if server is already started!",
                args );
        if ( err != null ) return err;

        Taxon taxon = taxonService.findByCommonName( commonName );

        if ( taxon == null ) {
            log.error( "No taxon " + commonName + " found" );
            bail( ErrorCode.INVALID_OPTION );
        }

        ArrayDesign arrayDesign = locateArrayDesign( arrayDesignName );

        unlazifyArrayDesign( arrayDesign );

        arrayDesignSequenceAlignmentService.processArrayDesign( arrayDesign, taxon );

        return null;
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        arrayDesignSequenceAlignmentService = ( ArrayDesignSequenceAlignmentService ) this
                .getBean( "arrayDesignSequenceAlignmentService" );

    }

}
