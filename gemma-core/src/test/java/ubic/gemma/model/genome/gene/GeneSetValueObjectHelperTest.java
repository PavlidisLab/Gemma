/*
 * The Gemma project
 * 
 * Copyright (c) 2012 University of British Columbia
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

package ubic.gemma.model.genome.gene;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.genome.gene.GeneSetValueObjectHelper;
import ubic.gemma.genome.gene.service.GeneSetService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.security.authentication.UserManager;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * TODO Document Me
 * 
 * @author tvrossum
 * @version $Id$
 */
public class GeneSetValueObjectHelperTest extends BaseSpringContextTest {

    private Gene g1 = null;
    private Taxon tax1 = null;
    private GeneSet gset = null;

    @Autowired
    GeneSetService geneSetService;

    @Autowired
    UserManager userManager;

    @Autowired
    GeneSetValueObjectHelper geneSetValueObjectHelper;

    @Before
    public void setUp() {

        g1 = this.getTestPeristentGene();
        tax1 = this.getTaxon( "human" );
        g1.setTaxon( tax1 );

        GeneSetMember gmember = GeneSetMember.Factory.newInstance();
        gmember.setGene( g1 );
        gmember.setScore( 0.22 );

        gset = GeneSet.Factory.newInstance();
        gset.setName( "CreateTest" );
        gset.getMembers().add( gmember );

        gset = geneSetService.create( gset );
    }

    @Test
    public void testConvertToValueObject() {

        Long id = gset.getId();
        assertNotNull( id );

        GeneSetValueObject gsvo = geneSetValueObjectHelper.convertToValueObject( gset );

        assertEquals( gset.getId(), gsvo.getId() );
        assertEquals( gset.getMembers().size(), gsvo.getSize().intValue() );
        assertEquals( gset.getName(), gsvo.getName() );
        // the method for setting the taxon Id uses a db call with the geneSet's id
        // assertEquals( gmember.getGene().getTaxon().getId(), gsvo.getTaxonId() );

        /*
         * geneSetService.remove( gset );
         * 
         * assertNull( geneSetService.load( id ) );
         */

    }

    @Test
    public void testConvertToLightValueObject() {

        Long id = gset.getId();
        assertNotNull( id );

        GeneSetValueObject gsvo = geneSetValueObjectHelper.convertToLightValueObject( gset );

        assertNull( gsvo.getGeneIds() );

        assertEquals( gset.getId(), gsvo.getId() );
        assertEquals( gset.getMembers().size(), gsvo.getSize().intValue() );
        assertEquals( gset.getName(), gsvo.getName() );

        // the method for setting the taxon Id uses a db call with the geneSet's id
        // assertEquals( gmember.getGene().getTaxon().getId(), gsvo.getTaxonId() );

        /*
         * geneSetService.remove( gset ); assertNull( geneSetService.load( id ) );
         */

    }
}
