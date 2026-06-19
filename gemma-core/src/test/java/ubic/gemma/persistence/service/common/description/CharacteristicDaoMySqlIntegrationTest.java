/*
 * The Gemma project.
 *
 * Copyright (c) 2026 University of British Columbia
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
 */
package ubic.gemma.persistence.service.common.description;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.util.test.BaseIntegrationTest5;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.experiment.Statement;

import org.springframework.lang.Nullable;
import java.util.Collection;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real-MySQL counterpart of the H2-backed {@link CharacteristicDaoTest}, pinning the native-query
 * methods of {@link CharacteristicDaoImpl} against the production Spring context (real MySQL
 * {@code gemdtest} via {@link BaseIntegrationTest5}).
 * <p>
 * The native queries all project {@code select {C.*} from CHARACTERISTIC as C ... } with
 * {@code .addEntity( "C", Characteristic.class )}. Because {@link Characteristic} uses
 * {@code SINGLE_TABLE} inheritance with {@code @DiscriminatorColumn(name = "class")},
 * {@code @DiscriminatorValue("null")} on the root, and a {@link Statement} subclass, the
 * {@code {C.*}} alias expansion has to emit the discriminator {@code class} column so Hibernate
 * can choose the right {@code EntityPersister} per row. On MySQL this surfaces as
 * <pre>Unable to find column position by name: class [Column 'class' not found.]</pre>
 * which H2 (MODE=MYSQL, the {@link CharacteristicDaoTest} backend) does not reproduce because
 * H2 and MySQL diverge on how the {@code {alias.*}} discriminator expansion is rendered.
 * <p>
 * Each test seeds a mix of plain {@link Characteristic} rows (null discriminator) and at least
 * one {@link Statement} row (non-null discriminator) sharing the same probe URI / value / category,
 * then asserts the DAO call RETURNS (does not throw) and includes the {@link Statement} subclass
 * row. {@code findByUri} is expected to throw on MySQL today — that's the bug this test pins; do
 * NOT weaken the assertion to dodge it.
 * <p>
 * Class-level {@link Transactional} opens a per-test transaction Spring rolls back at end-of-test,
 * so the seeded rows never pollute the shared {@code gemdtest} schema — no manual cleanup needed.
 */
@Transactional
public class CharacteristicDaoMySqlIntegrationTest extends BaseIntegrationTest5 {

    @Autowired
    private CharacteristicDao characteristicDao;

    @Autowired
    private SessionFactory sessionFactory;

    /**
     * Per-run unique token so the probe URI / value / category don't collide with pre-existing
     * gemdtest rows (the @Transactional rollback keeps these out of the persistent schema, but a
     * unique token keeps the assertions exact regardless of what else is in the table).
     */
    private String token;
    private String valueUri;
    private String valuePrefix;
    private String categoryUri;
    private String category;

    @BeforeEach
    public void setUp() {
        token = UUID.randomUUID().toString();
        valueUri = "http://test/CHAR_DAO_IT/" + token;
        valuePrefix = "char_dao_it_" + token;
        categoryUri = "http://test/CHAR_DAO_IT_CAT/" + token;
        category = "char dao it cat " + token;

        // plain Characteristic (null discriminator) carrying the probe value-URI
        Characteristic plain = createCharacteristic( category, categoryUri, valuePrefix + "_plain", valueUri );
        sessionFactory.getCurrentSession().persist( plain );

        // a second plain Characteristic with the same value-URI to exercise multi-row return
        Characteristic plain2 = createCharacteristic( category, categoryUri, valuePrefix + "_plain2", valueUri );
        sessionFactory.getCurrentSession().persist( plain2 );

        // a Statement (non-null "Statement" discriminator) sharing the same value-URI / value-like
        // prefix / category — this is the row that forces the {C.*} discriminator expansion that
        // breaks on MySQL. Statement.subject == Characteristic.VALUE, subjectUri == VALUE_URI.
        Statement statement = Statement.Factory.newInstance( category, categoryUri, valuePrefix + "_stmt", valueUri );
        sessionFactory.getCurrentSession().persist( statement );

        sessionFactory.getCurrentSession().flush();
    }

    @Test
    public void testFindByUri_returnsPlainAndStatement() {
        // THE confirmed-broken path: on MySQL the {C.*} discriminator expansion renders a `class`
        // column reference the JDBC result-set lookup can't resolve. Expected to throw today.
        Collection<Characteristic> results = characteristicDao.findByUri( valueUri, null, null, true, -1 );
        assertThat( results )
                .hasSize( 3 )
                .anyMatch( c -> c instanceof Statement )
                .anyMatch( c -> !( c instanceof Statement ) );
    }

    @Test
    public void testFindByValueLike_returnsPlainAndStatement() {
        Collection<Characteristic> results = characteristicDao.findByValueLike( valuePrefix + "%", null, null, true, -1 );
        assertThat( results )
                .hasSize( 3 )
                .anyMatch( c -> c instanceof Statement )
                .anyMatch( c -> !( c instanceof Statement ) );
    }

    @Test
    public void testFindByCategoryLike_returnsPlainAndStatement() {
        Collection<Characteristic> results = characteristicDao.findByCategoryLike( category.substring( 0, 12 ) + "%", null, true, -1 );
        assertThat( results )
                .hasSize( 3 )
                .anyMatch( c -> c instanceof Statement )
                .anyMatch( c -> !( c instanceof Statement ) );
    }

    @Test
    public void testFindByCategoryUri_returnsPlainAndStatement() {
        Collection<Characteristic> results = characteristicDao.findByCategoryUri( categoryUri, null, true, -1 );
        assertThat( results )
                .hasSize( 3 )
                .anyMatch( c -> c instanceof Statement )
                .anyMatch( c -> !( c instanceof Statement ) );
    }

    @Test
    public void testFindByParentClasses_returnsPlainAndStatement() {
        // findByParentClasses runs the line-151 native query: select {C.*} from CHARACTERISTIC as C
        // where <owning-entity-constraint> [and <category-constraint>]. includeNoParents=true with
        // null parentClasses + the per-row category filter keeps the result scoped to our seeded
        // rows (all three are parent-less, so they satisfy the includeNoParents arm).
        Collection<Characteristic> results = characteristicDao.findByParentClasses( null, true, category, -1 );
        assertThat( results )
                .hasSize( 3 )
                .anyMatch( c -> c instanceof Statement )
                .anyMatch( c -> !( c instanceof Statement ) );
    }

    private Characteristic createCharacteristic( @Nullable String category, @Nullable String categoryUri, String value, @Nullable String valueUri ) {
        Characteristic c = new Characteristic();
        c.setCategory( category );
        c.setCategoryUri( categoryUri );
        c.setValue( value );
        c.setValueUri( valueUri );
        return c;
    }
}
