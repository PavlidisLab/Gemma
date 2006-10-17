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

import java.io.InputStream;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.StringUtils;

import ubic.basecode.util.FileTools;
import ubic.gemma.loader.expression.arrayDesign.ArrayDesignSequenceProcessingService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.biosequence.SequenceType;

/**
 * Attach sequences to array design, fetching from BLAST database if requested.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ArrayDesignSequenceAssociationCli extends ArrayDesignSequenceManipulatingCli {
    ArrayDesignSequenceProcessingService arrayDesignSequenceProcessingService;
    TaxonService taxonService;

    private String sequenceType;
    private String sequenceFile;

    public static void main( String[] args ) {
        ArrayDesignSequenceAssociationCli p = new ArrayDesignSequenceAssociationCli();
        try {
            Exception ex = p.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        super.buildOptions();

        Option fileOption = OptionBuilder.hasArg().withArgName( "Input sequence file" ).withDescription(
                "Path to file (FASTA)" ).withLongOpt( "file" ).create( 'f' );

        addOption( fileOption );

        StringBuffer buf = new StringBuffer();

        for ( String lit : SequenceType.literals() ) {
            buf.append( lit + "\n" );
        }

        String seqtypes = buf.toString();
        seqtypes = StringUtils.chop( seqtypes );

        Option sequenceTypeOption = OptionBuilder.hasArg().isRequired().withArgName( "Sequence type" ).withDescription(
                seqtypes ).withLongOpt( "type" ).create( 'y' );

        addOption( sequenceTypeOption );

    }

    @Override
    protected void processOptions() {
        super.processOptions();
        arrayDesignSequenceProcessingService = ( ArrayDesignSequenceProcessingService ) this
                .getBean( "arrayDesignSequenceProcessingService" );

        if ( this.hasOption( 'y' ) ) {
            sequenceType = this.getOptionValue( 'y' );
        }

        if ( this.hasOption( 'f' ) ) {
            this.sequenceFile = this.getOptionValue( 'f' );
        }

    }

    @Override
    protected Exception doWork( String[] args ) {
        try {
            Exception err = processCommandLine( "Sequence associator", args );
            if ( err != null ) return err;

            Taxon taxon = taxonService.findByCommonName( commonName );

            if ( taxon == null ) {
                log.error( "No taxon " + commonName + " found" );
                bail( ErrorCode.INVALID_OPTION );
            }

            ArrayDesign arrayDesign = locateArrayDesign( arrayDesignName );

            unlazifyArrayDesign( arrayDesign );

            if ( arrayDesign == null ) {
                log.error( "No arrayDesign " + arrayDesignName + " found" );
                bail( ErrorCode.INVALID_OPTION );
            }

            SequenceType sequenceTypeEn = SequenceType.fromString( sequenceType );

            if ( sequenceTypeEn == null ) {
                log.error( "No sequenceType " + sequenceType + " found" );
                bail( ErrorCode.INVALID_OPTION );
            }

            if ( this.hasOption( 'f' ) ) {
                InputStream sequenceFileIs = FileTools.getInputStreamFromPlainOrCompressedFile( sequenceFile );

                if ( sequenceFileIs == null ) {
                    log.error( "No file " + sequenceFile + " was readable" );
                    bail( ErrorCode.INVALID_OPTION );
                }

                log.info( "Processing ArrayDesign..." );
                arrayDesignSequenceProcessingService.processArrayDesign( arrayDesign, sequenceFileIs, sequenceTypeEn,
                        taxon );
                sequenceFileIs.close();
            } else {
                // FIXME - put in correctdatabases to search. Don't always want to do mouse, human etc.
                arrayDesignSequenceProcessingService.processArrayDesign( arrayDesign, new String[] { "nt",
                        "est_others", "est_human", "est_mouse" }, null );
            }

        } catch ( Exception e ) {
            log.error( e, e );
            return e;
        }
        return null;
    }

}
