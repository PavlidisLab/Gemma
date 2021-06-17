package ubic.gemma.persistence.persister;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.association.Gene2GOAssociation;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.association.Gene2GOAssociationDao;

@Service
public class Gene2GOAssociationPersister extends AbstractPersister<Gene2GOAssociation> {

    @Autowired
    private Gene2GOAssociationDao gene2GoAssociationDao;

    @Autowired
    private Persister<Gene> genePersister;

    @Autowired
    public Gene2GOAssociationPersister( SessionFactory sessionFactory ) {
        super( sessionFactory );
    }

    @Override
    @Transactional
    public Gene2GOAssociation persist( Gene2GOAssociation association ) {
        if ( association == null )
            return null;
        if ( !this.isTransient( association ) )
            return association;
        try {
            FieldUtils.writeField( association, "gene", genePersister.persist( association.getGene() ), true );
        } catch ( IllegalAccessException e ) {
            e.printStackTrace();
        }
        return gene2GoAssociationDao.create( association );
    }
}
