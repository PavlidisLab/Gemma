package ubic.gemma.core.loader.expression.cellxgene.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = { "ontologyTermId" })
public class OntologyTerm {
    String ontologyTermId;
    String label;
}
