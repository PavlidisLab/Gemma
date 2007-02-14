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

import ubic.gemma.model.common.auditAndSecurity.AuditAction;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.security.principal.UserDetailsServiceImpl;
import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * Aggregates functionality useful when writing CLIs that need to get an array design from the database and do something
 * with it.
 * 
 * @author pavlidis
 * @version $Id$
 */
public abstract class ArrayDesignSequenceManipulatingCli extends AbstractSpringAwareCLI {

    ArrayDesignService arrayDesignService;
    String arrayDesignName = null;    

    @Override
    @SuppressWarnings("static-access")
    protected void buildOptions() {
        Option arrayDesignOption = OptionBuilder.hasArg().withArgName( "Array design" ).withDescription(
                "Array design name (or short name)" ).withLongOpt( "array" ).create( 'a' );

        addOption( arrayDesignOption );

    }

    protected void unlazifyArrayDesign(  ArrayDesign arrayDesign ) {
              
        arrayDesignService.thaw( arrayDesign );
    }

    /**
     * @param name of the array design to find.
     * @return
     */
    protected ArrayDesign locateArrayDesign( String name ) {
        
        ArrayDesign arrayDesign = arrayDesignService.findArrayDesignByName( name.trim().toUpperCase() );

        if ( arrayDesign == null ) {
            arrayDesign = arrayDesignService.findByShortName( name );
        }

        if ( arrayDesign == null ) {
            log.error( "No arrayDesign " + name + " found" );
            bail( ErrorCode.INVALID_OPTION );
        }
        return arrayDesign;
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( this.hasOption( 'a' ) ) {
            this.arrayDesignName = this.getOptionValue( 'a' );
        }
        arrayDesignService = ( ArrayDesignService ) this.getBean( "arrayDesignService" );
    }

    protected void updateAudit( String note ) {
        ArrayDesign ad = this.locateArrayDesign( arrayDesignName );
        AuditEvent ae = AuditEvent.Factory.newInstance();
        ae.setNote( note );
        ae.setAction( AuditAction.UPDATE );
        ae.setPerformer( UserDetailsServiceImpl.getCurrentUser() );
        ad.getAuditTrail().addEvent( ae );
        arrayDesignService.update( ad );
    }

}
