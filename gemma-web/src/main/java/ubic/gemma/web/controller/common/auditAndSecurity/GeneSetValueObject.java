/*
 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
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
package ubic.gemma.web.controller.common.auditAndSecurity;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;

import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.model.genome.gene.GeneSetMember;

/**
 * Represents a Gene group gene set.
 * 
 * @author kelsey
 * @version $Id$
 */
public class GeneSetValueObject implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 6212231006289412683L;

    public static Collection<GeneSetValueObject> convert2ValueObjects( Collection<GeneSet> genesets ) {
        Collection<GeneSetValueObject> results = new HashSet<GeneSetValueObject>();

        for ( GeneSet gs : genesets ) {
            results.add( new GeneSetValueObject( gs ) );
        }

        return results;
    }

    private boolean currentUserHasWritePermission = false;
    private String description;
    private Collection<GeneSetMember> geneMembers;
    private Long id;

    private String name;

    private SidValueObject owner;

    private boolean publik;

    private boolean shared;
    private Integer size;
    private Taxon taxon;

    /**
     * Null constructor to satisfy java bean contract
     */
    public GeneSetValueObject() {
        super();
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
        this.setSize( members.size() );

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
     * @return
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return
     */
    public Collection<GeneSetMember> getGeneMembers() {
        return geneMembers;
    }

    /**
     * @return
     */
    public Long getId() {
        return id;
    }

    /**
     * @return
     */
    public String getName() {
        return name;
    }

    public SidValueObject getOwner() {
        return owner;
    }

    /**
     * returns the number of members in the group
     * 
     * @return
     */
    public Integer getSize() {
        return this.size;
    }

    /**
     * @return the currentUserHasWritePermission
     */
    public boolean isCurrentUserHasWritePermission() {
        return currentUserHasWritePermission;
    }

    public boolean isPublik() {
        return this.publik;
    }

    public boolean isShared() {
        return this.shared;
    }

    /**
     * @param currentUserHasWritePermission the currentUserHasWritePermission to set
     */
    public void setCurrentUserHasWritePermission( boolean currentUserHasWritePermission ) {
        this.currentUserHasWritePermission = currentUserHasWritePermission;
    }

    /**
     * @param description
     */
    public void setDescription( String description ) {
        this.description = description;
    }

    /**
     * @param geneMembers
     */
    public void setGeneMembers( Collection<GeneSetMember> geneMembers ) {
        this.geneMembers = geneMembers;
    }

    /**
     * @param id
     */
    public void setId( Long id ) {
        this.id = id;
    }

    /**
     * @param name
     */
    public void setName( String name ) {
        this.name = name;
    }

    public void setOwner( SidValueObject owner ) {
        this.owner = owner;
    }

    public void setPublik( boolean isPublic ) {
        this.publik = isPublic;
    }

    public void setShared( boolean isShared ) {
        this.shared = isShared;
    }

    /**
     * @param size
     */
    public void setSize( Integer size ) {
        this.size = size;
    }

}
