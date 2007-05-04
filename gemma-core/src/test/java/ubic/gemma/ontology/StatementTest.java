/*
 * The Gemma-ONT_REV project
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
package ubic.gemma.ontology;

import java.util.Collection;
import java.util.zip.GZIPInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.ontology.OntModelSpec;

import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.ontology.OntologyCardinalityRestriction.CardinalityType;

import junit.framework.TestCase;

/**
 * @author pavlidis
 * @version $Id$
 */
public class StatementTest extends TestCase {

    private static Log log = LogFactory.getLog( StatementTest.class.getName() );

    protected void setUp() throws Exception {
        super.setUp();
        GZIPInputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/ontology/MGEDOntology.owl.gz" ) );
        OntologyTools.initOntology( is, "http://mged.sourceforge.net/ontologies/MGEDOntology.owl",
                OntModelSpec.OWL_MEM_RDFS_INF );
    }

    public void testMakeStatement() throws Exception {

        /*
         * This just illustrates how we can figure out what is needed.
         */

        /*
         * The Classes, Properties, Individuals and literals we need:
         */
        OntologyTerm age = OntologyTools
                .getOntologyTerm( "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#Age" );
        assertNotNull( age );
        
        /*
         * Illustrates how we can populate a GUI with slots.
         */
        processTerm( age, "" );

        ObjectProperty hasMeasurement = ( ObjectProperty ) OntologyTools
                .getOntologyProperty( "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#has_measurement" );
        assertNotNull( hasMeasurement );

        OntologyTerm meas = OntologyTools
                .getOntologyTerm( "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#Measurement" );
        assertNotNull( meas );

        ObjectProperty hasUnits = ( ObjectProperty ) OntologyTools
                .getOntologyProperty( "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#has_units" );
        assertNotNull( hasUnits );

        DatatypeProperty hasValue = ( DatatypeProperty ) OntologyTools
                .getOntologyProperty( "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#has_value" );
        assertNotNull( hasValue );

        OntologyIndividual months = OntologyTools
                .getOntologyIndividual( "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#months" );
        assertNotNull( months );

        String monthVal = "2";

        ObjectProperty hasInitialTimepoint = ( ObjectProperty ) OntologyTools
                .getOntologyProperty( "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#has_initial_time_point" );
        assertNotNull( hasValue );

        OntologyIndividual birth = OntologyTools
                .getOntologyIndividual( "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#birth" );
        assertNotNull( months );

        

        /*
         * Here is how we build up the VocabCharacteristic.
         */

        // start with age, leaving the chain incomplete. start with the first property that needs to be filled in.
        ChainedStatement ageStatement = new ChainedStatementImpl( age, hasMeasurement );

        /*
         * Build a statement about a measurement.
         */
        ChainedStatementObject measurementInstance = new ChainedStatementObjectImpl( meas );

        DataStatement monthsSt = new DataStatementImpl( meas, hasValue, monthVal );
        InstanceStatement unitSt = new InstanceStatementImpl( meas, hasUnits, months );

        measurementInstance.addStatement( monthsSt );
        measurementInstance.addStatement( unitSt );

        ageStatement.setObject( measurementInstance );

        VocabCharacteristic ageVc = ageStatement.toCharacteristic();

        /*
         * We do the same thing to add the initialtimepoint.
         */
        InstanceStatement ageInitialTimePoint = new InstanceStatementImpl( age, hasInitialTimepoint, birth );

        VocabCharacteristicBuilder.addStatement( ageVc, ageInitialTimePoint );

        log.info( ageVc );

    }

    /**
     * // * If the term has an objectproperty whose superclass is a Database (MGED), then they should be allowed to
     * enter a // * individual from another ontology?
     * 
     * @param startTerm
     * @param indent
     */
    private void processTerm( OntologyTerm startTerm, String indent ) {

        log.info( indent + "============ " + startTerm + " ============" );

        Collection<OntologyTerm> children = startTerm.getChildren( false );
        if ( children.size() > 0 ) {
            log.info( indent + children.size() + " available subclasses of " + startTerm );
            // for ( OntologyTerm child : children ) {
            // log.info( " " + child ); // user may pick one.
            // }
        }

        Collection<OntologyIndividual> inds = startTerm.getIndividuals( false );
        if ( inds.size() > 0 ) {
            log.info( indent + "Please select one of " + inds.size() + " available individuals of " + startTerm
                    + " or enter a new value" );
            // for ( OntologyIndividual i : inds ) {
            // log.info( " " + i ); // user must pick one
            // }
        }

        /*
         * Either fill in an individual here, OR fill in the slots.
         */

        Collection<OntologyRestriction> res = startTerm.getRestrictions();
        if ( res.size() > 0 ) {
            log.info( indent + "Please fill in the following slots " );
            for ( OntologyRestriction restriction : res ) {
                OntologyProperty restrictionOn = restriction.getRestrictionOn();
                if ( restriction instanceof OntologyClassRestriction ) {
                    OntologyClassRestriction r = ( OntologyClassRestriction ) restriction;
                    OntologyTerm to = r.getRestrictedTo();
                    log.info( indent + " Slot to fill in: " + restrictionOn + " with a " + to );
                    processTerm( to, "   " + indent );
                } else if ( restriction instanceof OntologyDatatypeRestriction ) {
                    OntologyDatatypeRestriction r = ( OntologyDatatypeRestriction ) restriction;
                    PrimitiveType restrictedTo = r.getRestrictedTo();
                    log.info( indent + " Slot to fill in: " + restrictionOn + " with a " + restrictedTo );
                } else if ( restriction instanceof OntologyCardinalityRestriction ) {
                    // this will be rare.
                    OntologyCardinalityRestriction r = ( OntologyCardinalityRestriction ) restriction;
                    int cardinality = r.getCardinality();
                    CardinalityType cardinalityType = r.getCardinalityType();
                    log.info( indent + " Slot to fill in: " + restrictionOn + " with " + cardinalityType + " "
                            + cardinality + " things" );
                    // todo check range of the property (what 'things' should be) if specified.
                }
            }
        }
        log.info( indent + "====== End of details for " + startTerm + " =======" );
    }

}
