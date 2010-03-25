package ubic.gemma.web.controller.common.auditAndSecurity;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;

import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.model.genome.gene.GeneSetMember;

public class GeneSetValueObject implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 6212231006289412683L;

    private String name;
    private Long id;
    private String description;
    private Collection<GeneSetMember> geneMembers;
    private boolean publik;
    private boolean shared;
    private SidValueObject owner;

    public static Collection<GeneSetValueObject> convert2ValueObjects( Collection<GeneSet> genesets ) {
        Collection<GeneSetValueObject> results = new HashSet<GeneSetValueObject>();

        for ( GeneSet gs : genesets ) {
            results.add( new GeneSetValueObject( gs ) );
        }

        return results;
    }

    /**
     * Null constructor to satisfy java bean contract
     */
    public GeneSetValueObject() {
        super();
    }

    /**
     * Constructor to build from listed sub components
     * 
     * @param id
     * @param name
     * @param description
     * @param members
     */
    public GeneSetValueObject( Long id, String name, String description, Collection<GeneSetMember> members ) {

        this();
        this.setName( name );
        this.setId( id );
        this.setDescription( description );
        this.setGeneMembers( members );

    }

    /**
     * Constructor to build from listed sub components
     * 
     * @param id
     * @param name
     * @param description
     * @param members
     */
    public GeneSetValueObject( Long id, String name, String description, Collection<GeneSetMember> members,
            Boolean isShared, Boolean isPublic ) {

        this( id, name, description, members );
        this.setPublik( isPublic );
        this.setShared( isShared );
    }

    /**
     * Constructor to build value object from GeneSet
     * 
     * @param gs
     */
    public GeneSetValueObject( GeneSet gs ) {
        this( gs.getId(), gs.getName(), gs.getDescription(), gs.getMembers() );
    }

    /**
     * @param name
     */
    public void setName( String name ) {
        this.name = name;
    }

    /**
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * @param id
     */
    public void setId( Long id ) {
        this.id = id;
    }

    /**
     * @return
     */
    public Long getId() {
        return id;
    }

    /**
     * @param description
     */
    public void setDescription( String description ) {
        this.description = description;
    }

    /**
     * @return
     */
    public String getDescription() {
        return description;
    }

    /**
     * returns the number of members in the group
     * 
     * @return
     */
    public Integer getSize() {

        if ( this.geneMembers == null ) return null;

        return this.geneMembers.size();
    }

    /**
     * @param geneMembers
     */
    public void setGeneMembers( Collection<GeneSetMember> geneMembers ) {
        this.geneMembers = geneMembers;
    }

    /**
     * @return
     */
    public Collection<GeneSetMember> getGeneMembers() {
        return geneMembers;
    }

    public void setPublik( boolean isPublic ) {
        this.publik = isPublic;
    }

    public boolean isPublik() {
        return this.publik;
    }

    public void setShared( boolean isShared ) {
        this.shared = isShared;
    }

    public boolean isShared() {
        return this.shared;
    }

    public void setOwner( SidValueObject owner ) {
        this.owner = owner;
    }

    public SidValueObject getOwner() {
        return owner;
    }

}
