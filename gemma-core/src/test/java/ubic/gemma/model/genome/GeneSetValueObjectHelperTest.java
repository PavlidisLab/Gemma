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

package ubic.gemma.model.genome;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.model.genome.gene.GeneSetMember;
import ubic.gemma.model.genome.gene.GeneSetValueObject;
import ubic.gemma.persistence.service.genome.gene.GeneSetValueObjectHelper;
import ubic.gemma.persistence.service.genome.gene.GeneSetService;
import ubic.gemma.core.util.test.BaseSpringContextTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author tvrossum
 */
public class GeneSetValueObjectHelperTest extends BaseSpringContextTest {

    @Autowired
    private GeneSetService geneSetService;
    @Autowired
    private GeneSetValueObjectHelper geneSetValueObjectHelper;
    private GeneSet gset = null;

    @Before
    public void setUp() throws Exception {

        Gene g1 = this.getTestPersistentGene();
        Taxon tax1 = this.getTaxon( "human" );
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
        assertEquals( gset.getMembers().size(), gsvo.getSize().longValue() );
        assertEquals( gset.getName(), gsvo.getName() );
    }

    @Test
    public void testConvertToLightValueObject() {

        Long id = gset.getId();
        assertNotNull( id );

        GeneSetValueObject gsvo = geneSetValueObjectHelper.convertToLightValueObject( gset );

        assertEquals( gset.getId(), gsvo.getId() );
        assertEquals( gset.getMembers().size(), gsvo.getSize().longValue() );
        assertEquals( gset.getName(), gsvo.getName() );
    }
}
