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

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * @author pavlidis
 * @version $Id$
 */
public abstract class ArrayDesignSequenceManipulatingCli extends AbstractSpringAwareCLI {

    ArrayDesignService arrayDesignService;
    String arrayDesignName = null;
    String commonName;
    TaxonService taxonService;

    @Override
    @SuppressWarnings("static-access")
    protected void buildOptions() {
        Option taxonOption = OptionBuilder.hasArg().isRequired().withArgName( "Taxon name" ).withDescription(
                "Taxon common name, e.g., 'rat'" ).withLongOpt( "taxon" ).create( 't' );

        addOption( taxonOption );

        Option arrayDesignOption = OptionBuilder.hasArg().isRequired().withArgName( "Array design" ).withDescription(
                "Array design name (or short name)" ).withLongOpt( "array" ).create( 'a' );

        addOption( arrayDesignOption );

    }

    protected void unlazifyArrayDesign( final ArrayDesign arrayDesign ) {
        arrayDesignService.thaw( arrayDesign );
    }

    protected ArrayDesign locateArrayDesign( String arrayDesignName ) {
        ArrayDesign arrayDesign = arrayDesignService.findArrayDesignByName( arrayDesignName );

        if ( arrayDesign == null ) {
            arrayDesign = arrayDesignService.findByShortName( arrayDesignName );
        }

        if ( arrayDesign == null ) {
            log.error( "No arrayDesign " + arrayDesignName + " found" );
            bail( ErrorCode.INVALID_OPTION );
        }
        return arrayDesign;
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( this.hasOption( 't' ) ) {
            commonName = this.getOptionValue( 't' );
        }

        if ( this.hasOption( 'a' ) ) {
            this.arrayDesignName = this.getOptionValue( 'a' );
        }
        taxonService = ( TaxonService ) this.getBean( "taxonService" );
        arrayDesignService = ( ArrayDesignService ) this.getBean( "arrayDesignService" );
    }

}
