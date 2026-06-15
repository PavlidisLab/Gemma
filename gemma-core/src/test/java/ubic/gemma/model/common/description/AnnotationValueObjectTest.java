/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.model.common.description;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.expression.experiment.Statement;

/**
 * Unit tests for {@link AnnotationValueObject}'s discrimination between plain
 * {@link Characteristic} input and the {@link Statement} subclass. Pins the
 * read-side wire shape that the Gemma 2.0 EE Statement support emits.
 */
public class AnnotationValueObjectTest {

    @Test
    public void plainCharacteristic_leavesStatementFieldsNull() {
        Characteristic c = Characteristic.Factory.newInstance();
        c.setCategory( "organism part" );
        c.setCategoryUri( "http://purl.obolibrary.org/obo/UBERON_0000479" );
        c.setValue( "liver" );
        c.setValueUri( "http://purl.obolibrary.org/obo/UBERON_0002107" );
        c.setEvidenceCode( GOEvidenceCode.IEA );

        AnnotationValueObject vo = new AnnotationValueObject( c );

        assertThat( vo.getClassName() ).isEqualTo( "organism part" );
        assertThat( vo.getTermName() ).isEqualTo( "liver" );
        assertThat( vo.getEvidenceCode() ).isEqualTo( "IEA" );
        assertThat( vo.getPredicate() ).isNull();
        assertThat( vo.getPredicateUri() ).isNull();
        assertThat( vo.getObject() ).isNull();
        assertThat( vo.getObjectUri() ).isNull();
        assertThat( vo.getSecondPredicate() ).isNull();
        assertThat( vo.getSecondPredicateUri() ).isNull();
        assertThat( vo.getSecondObject() ).isNull();
        assertThat( vo.getSecondObjectUri() ).isNull();
    }

    @Test
    public void statement_populatesAllRelationalFields() {
        Statement s = Statement.Factory.newInstance();
        s.setCategory( "treatment" );
        s.setCategoryUri( "http://www.ebi.ac.uk/efo/EFO_0000727" );
        s.setSubject( "high fat diet" );
        s.setSubjectUri( "http://purl.obolibrary.org/obo/EFO_0002091" );
        s.setPredicate( "has_dose" );
        s.setPredicateUri( "http://purl.obolibrary.org/obo/RO_0002211" );
        s.setObject( "30%" );
        s.setObjectUri( "http://example.com/dose/30pct" );
        s.setSecondPredicate( "for" );
        s.setSecondPredicateUri( "http://purl.obolibrary.org/obo/RO_0002092" );
        s.setSecondObject( "12 weeks" );
        s.setSecondObjectUri( "http://example.com/duration/12w" );

        AnnotationValueObject vo = new AnnotationValueObject( s );

        // Shared Characteristic-shape fields stay populated for back-compat with consumers
        // that don't know about Statement — Statement aliases value → subject internally.
        assertThat( vo.getClassName() ).isEqualTo( "treatment" );
        assertThat( vo.getTermName() ).isEqualTo( "high fat diet" );
        // The new Statement-only fields surface for consumers that DO know:
        assertThat( vo.getPredicate() ).isEqualTo( "has_dose" );
        assertThat( vo.getPredicateUri() ).isEqualTo( "http://purl.obolibrary.org/obo/RO_0002211" );
        assertThat( vo.getObject() ).isEqualTo( "30%" );
        assertThat( vo.getObjectUri() ).isEqualTo( "http://example.com/dose/30pct" );
        assertThat( vo.getSecondPredicate() ).isEqualTo( "for" );
        assertThat( vo.getSecondPredicateUri() ).isEqualTo( "http://purl.obolibrary.org/obo/RO_0002092" );
        assertThat( vo.getSecondObject() ).isEqualTo( "12 weeks" );
        assertThat( vo.getSecondObjectUri() ).isEqualTo( "http://example.com/duration/12w" );
    }

    @Test
    public void statementWithOnlyFirstPair_leavesSecondPairNull() {
        Statement s = Statement.Factory.newInstance();
        s.setCategory( "treatment" );
        s.setSubject( "HFD" );
        s.setPredicate( "has_dose" );
        s.setObject( "30%" );

        AnnotationValueObject vo = new AnnotationValueObject( s );

        assertThat( vo.getPredicate() ).isEqualTo( "has_dose" );
        assertThat( vo.getObject() ).isEqualTo( "30%" );
        assertThat( vo.getSecondPredicate() ).isNull();
        assertThat( vo.getSecondObject() ).isNull();
    }
}
