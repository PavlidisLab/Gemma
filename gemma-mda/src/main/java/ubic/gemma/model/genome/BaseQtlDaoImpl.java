package ubic.gemma.model.genome;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * TODO Document Me
 * 
 * @author paul
 * @version $Id$
 * @param <T>
 */
public abstract class BaseQtlDaoImpl<T extends Qtl> extends HibernateDaoSupport implements BaseQtlDao<T> {

    /**
     * @see ubic.gemma.model.genome.BaseQtlDao#findByPhysicalMarkers(int, java.lang.String,
     *      ubic.gemma.model.genome.PhysicalMarker, ubic.gemma.model.genome.PhysicalMarker)
     */
    @SuppressWarnings( { "unchecked" })
    public java.util.Collection findByPhysicalMarkers( final int transform, final java.lang.String queryString,
            final ubic.gemma.model.genome.PhysicalMarker startMarker,
            final ubic.gemma.model.genome.PhysicalMarker endMarker ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( startMarker );
        argNames.add( "startMarker" );
        args.add( endMarker );
        argNames.add( "endMarker" );
        java.util.List results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.genome.BaseQtlDao#findByPhysicalMarkers(int, ubic.gemma.model.genome.PhysicalMarker,
     *      ubic.gemma.model.genome.PhysicalMarker)
     */
    @SuppressWarnings( { "unchecked" })
    public java.util.Collection findByPhysicalMarkers( final int transform,
            final ubic.gemma.model.genome.PhysicalMarker startMarker,
            final ubic.gemma.model.genome.PhysicalMarker endMarker ) {
        return this
                .findByPhysicalMarkers(
                        transform,
                        "from QtlImpl qtl where (qtl.startMaker.physicalLocation.chromosome = :n.physicalLocation.chromosome and qtl.startMaker.physicalLocation.nucleotide > :n.physicalLocation.nucleotide and qtl.endMarker.physicalLocation.nucleotide < (:n.physicalLocation.nucleotide + :n.physicalLocation.nucleotide.nucleotideLength)",
                        startMarker, endMarker );
    }

    /**
     * @see ubic.gemma.model.genome.BaseQtlDao#findByPhysicalMarkers(java.lang.String,
     *      ubic.gemma.model.genome.PhysicalMarker, ubic.gemma.model.genome.PhysicalMarker)
     */
    @SuppressWarnings( { "unchecked" })
    public java.util.Collection findByPhysicalMarkers( final java.lang.String queryString,
            final ubic.gemma.model.genome.PhysicalMarker startMarker,
            final ubic.gemma.model.genome.PhysicalMarker endMarker ) {
        return this.findByPhysicalMarkers( TRANSFORM_NONE, queryString, startMarker, endMarker );
    }

    /**
     * @see ubic.gemma.model.genome.BaseQtlDao#findByPhysicalMarkers(ubic.gemma.model.genome.PhysicalMarker,
     *      ubic.gemma.model.genome.PhysicalMarker)
     */
    public java.util.Collection<T> findByPhysicalMarkers( ubic.gemma.model.genome.PhysicalMarker startMarker,
            ubic.gemma.model.genome.PhysicalMarker endMarker ) {
        return this.findByPhysicalMarkers( TRANSFORM_NONE, startMarker, endMarker );
    }

    /**
     * Transforms a collection of entities using the {@link #transformEntity(int,ubic.gemma.model.genome.Qtl)} method.
     * This method does not instantiate a new collection.
     * <p/>
     * This method is to be used internally only.
     * 
     * @param transform one of the constants declared in <code>ubic.gemma.model.genome.QtlDao</code>
     * @param entities the collection of entities to transform
     * @return the same collection as the argument, but this time containing the transformed entities
     * @see #transformEntity(int,ubic.gemma.model.genome.Qtl)
     */

    protected void transformEntities( final int transform, final java.util.Collection entities ) {
        switch ( transform ) {
            case TRANSFORM_NONE: // fall-through
            default:
                // do nothing;
        }
    }

    /**
     * Allows transformation of entities into value objects (or something else for that matter), when the
     * <code>transform</code> flag is set to one of the constants defined in <code>ubic.gemma.model.genome.QtlDao</code>
     * , please note that the {@link #TRANSFORM_NONE} constant denotes no transformation, so the entity itself will be
     * returned. If the integer argument value is unknown {@link #TRANSFORM_NONE} is assumed.
     * 
     * @param transform one of the constants declared in {@link ubic.gemma.model.genome.BaseQtlDao}
     * @param entity an entity that was found
     * @return the transformed entity (i.e. new value object, etc)
     * @see #transformEntities(int,java.util.Collection)
     */
    protected Object transformEntity( final int transform, final ubic.gemma.model.genome.Qtl entity ) {
        Object target = null;
        if ( entity != null ) {
            switch ( transform ) {
                case TRANSFORM_NONE: // fall-through
                default:
                    target = entity;
            }
        }
        return target;
    }
}
