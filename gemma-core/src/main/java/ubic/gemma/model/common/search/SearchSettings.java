/*
 * The Gemma project.
 *
 * Copyright (c) 2006-2012 University of British Columbia
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
package ubic.gemma.model.common.search;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.genome.Taxon;

import java.io.Serializable;

public abstract class SearchSettings implements Serializable {

    public static final class Factory {

        public static SearchSettings newInstance() {
            return new SearchSettingsImpl();
        }
    }

    /**
     * How many results per result type are allowed. This implies that if you search for multiple types of things, you
     * can get more than this.
     */
    static final int DEFAULT_MAX_RESULTS_PER_RESULT_TYPE = 5000;
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -982243911532743661L;
    private Integer maxResults = SearchSettings.DEFAULT_MAX_RESULTS_PER_RESULT_TYPE;
    private ArrayDesign platformConstraint;
    private String query;
    private Boolean searchBibrefs = Boolean.TRUE;
    private Boolean searchBioSequences = Boolean.TRUE;
    private Boolean searchExperiments = Boolean.TRUE;
    private Boolean searchExperimentSets = Boolean.TRUE;
    private Boolean searchGenes = Boolean.TRUE;
    private Boolean searchGeneSets = Boolean.TRUE;
    private Boolean searchPhenotypes = Boolean.TRUE;

    private Boolean searchPlatforms = Boolean.TRUE;

    private Boolean searchProbes = Boolean.TRUE;
    private Taxon taxon;
    private String termUri;
    private Boolean useCharacteristics = Boolean.TRUE;
    private Boolean useDatabase = Boolean.TRUE;
    private Boolean useGo = Boolean.TRUE;
    private Boolean useIndices = Boolean.TRUE;
    /**
     * No-arg constructor added to satisfy javabean contract
     *
     * @author Paul
     */
    public SearchSettings() {
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) {
            return true;
        }
        if ( obj == null ) {
            return false;
        }
        if ( getClass() != obj.getClass() ) {
            return false;
        }
        SearchSettings other = ( SearchSettings ) obj;
        if ( maxResults == null ) {
            if ( other.maxResults != null ) {
                return false;
            }
        } else if ( !maxResults.equals( other.maxResults ) ) {
            return false;
        }
        if ( platformConstraint == null ) {
            if ( other.platformConstraint != null ) {
                return false;
            }
        } else if ( !platformConstraint.equals( other.platformConstraint ) ) {
            return false;
        }
        if ( query == null ) {
            if ( other.query != null ) {
                return false;
            }
        } else if ( !query.equals( other.query ) ) {
            return false;
        }
        if ( searchBibrefs == null ) {
            if ( other.searchBibrefs != null ) {
                return false;
            }
        } else if ( !searchBibrefs.equals( other.searchBibrefs ) ) {
            return false;
        }
        if ( searchBioSequences == null ) {
            if ( other.searchBioSequences != null ) {
                return false;
            }
        } else if ( !searchBioSequences.equals( other.searchBioSequences ) ) {
            return false;
        }
        if ( searchExperimentSets == null ) {
            if ( other.searchExperimentSets != null ) {
                return false;
            }
        } else if ( !searchExperimentSets.equals( other.searchExperimentSets ) ) {
            return false;
        }
        if ( searchExperiments == null ) {
            if ( other.searchExperiments != null ) {
                return false;
            }
        } else if ( !searchExperiments.equals( other.searchExperiments ) ) {
            return false;
        }
        if ( searchGeneSets == null ) {
            if ( other.searchGeneSets != null ) {
                return false;
            }
        } else if ( !searchGeneSets.equals( other.searchGeneSets ) ) {
            return false;
        }
        if ( searchGenes == null ) {
            if ( other.searchGenes != null ) {
                return false;
            }
        } else if ( !searchGenes.equals( other.searchGenes ) ) {
            return false;
        }
        if ( searchPhenotypes == null ) {
            if ( other.searchPhenotypes != null ) {
                return false;
            }
        } else if ( !searchPhenotypes.equals( other.searchPhenotypes ) ) {
            return false;
        }
        if ( searchPlatforms == null ) {
            if ( other.searchPlatforms != null ) {
                return false;
            }
        } else if ( !searchPlatforms.equals( other.searchPlatforms ) ) {
            return false;
        }
        if ( searchProbes == null ) {
            if ( other.searchProbes != null ) {
                return false;
            }
        } else if ( !searchProbes.equals( other.searchProbes ) ) {
            return false;
        }
        if ( taxon == null ) {
            if ( other.taxon != null ) {
                return false;
            }
        } else if ( !taxon.equals( other.taxon ) ) {
            return false;
        }
        if ( termUri == null ) {
            if ( other.termUri != null ) {
                return false;
            }
        } else if ( !termUri.equals( other.termUri ) ) {
            return false;
        }
        if ( useCharacteristics == null ) {
            if ( other.useCharacteristics != null ) {
                return false;
            }
        } else if ( !useCharacteristics.equals( other.useCharacteristics ) ) {
            return false;
        }
        if ( useDatabase == null ) {
            if ( other.useDatabase != null ) {
                return false;
            }
        } else if ( !useDatabase.equals( other.useDatabase ) ) {
            return false;
        }
        if ( useGo == null ) {
            if ( other.useGo != null ) {
                return false;
            }
        } else if ( !useGo.equals( other.useGo ) ) {
            return false;
        }
        if ( useIndices == null ) {
            if ( other.useIndices != null ) {
                return false;
            }
        } else if ( !useIndices.equals( other.useIndices ) ) {
            return false;
        }
        return true;
    }

    /**
     * 
     * @return The maximum number of results to fetch. The default is SearchSettings.DEFAULT_MAX_RESULTS_PER_RESULT_TYPE
     */
    public Integer getMaxResults() {
        return this.maxResults;
    }

    public ubic.gemma.model.expression.arrayDesign.ArrayDesign getPlatformConstraint() {
        return this.platformConstraint;
    }

    public String getQuery() {
        return this.query;
    }

    public Boolean getSearchBibrefs() {
        return this.searchBibrefs;
    }

    public Boolean getSearchBioSequences() {
        return this.searchBioSequences;
    }

    public Boolean getSearchExperiments() {
        return this.searchExperiments;
    }

    public Boolean getSearchExperimentSets() {
        return this.searchExperimentSets;
    }

    public Boolean getSearchGenes() {
        return this.searchGenes;
    }

    public Boolean getSearchGeneSets() {
        return this.searchGeneSets;
    }

    public Boolean getSearchPhenotypes() {
        return this.searchPhenotypes;
    }

    public Boolean getSearchPlatforms() {
        return this.searchPlatforms;
    }

    public Boolean getSearchProbes() {
        return this.searchProbes;
    }

    public ubic.gemma.model.genome.Taxon getTaxon() {
        return this.taxon;
    }

    public String getTermUri() {
        return this.termUri;
    }

    public Boolean getUseCharacteristics() {
        return this.useCharacteristics;
    }

    public Boolean getUseDatabase() {
        return this.useDatabase;
    }

    public Boolean getUseGo() {
        return this.useGo;
    }

    public Boolean getUseIndices() {
        return this.useIndices;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( maxResults == null ) ? 0 : maxResults.hashCode() );
        result = prime * result + ( ( platformConstraint == null ) ? 0 : platformConstraint.hashCode() );
        result = prime * result + ( ( query == null ) ? 0 : query.hashCode() );
        result = prime * result + ( ( searchBibrefs == null ) ? 0 : searchBibrefs.hashCode() );
        result = prime * result + ( ( searchBioSequences == null ) ? 0 : searchBioSequences.hashCode() );
        result = prime * result + ( ( searchExperimentSets == null ) ? 0 : searchExperimentSets.hashCode() );
        result = prime * result + ( ( searchExperiments == null ) ? 0 : searchExperiments.hashCode() );
        result = prime * result + ( ( searchGeneSets == null ) ? 0 : searchGeneSets.hashCode() );
        result = prime * result + ( ( searchGenes == null ) ? 0 : searchGenes.hashCode() );
        result = prime * result + ( ( searchPhenotypes == null ) ? 0 : searchPhenotypes.hashCode() );
        result = prime * result + ( ( searchPlatforms == null ) ? 0 : searchPlatforms.hashCode() );
        result = prime * result + ( ( searchProbes == null ) ? 0 : searchProbes.hashCode() );
        result = prime * result + ( ( taxon == null ) ? 0 : taxon.hashCode() );
        result = prime * result + ( ( termUri == null ) ? 0 : termUri.hashCode() );
        result = prime * result + ( ( useCharacteristics == null ) ? 0 : useCharacteristics.hashCode() );
        result = prime * result + ( ( useDatabase == null ) ? 0 : useDatabase.hashCode() );
        result = prime * result + ( ( useGo == null ) ? 0 : useGo.hashCode() );
        result = prime * result + ( ( useIndices == null ) ? 0 : useIndices.hashCode() );
        return result;
    }

    public abstract void noSearches();

    public void setMaxResults( Integer maxResults ) {
        this.maxResults = maxResults;
    }

    public void setPlatformConstraint( ubic.gemma.model.expression.arrayDesign.ArrayDesign platformConstraint ) {
        this.platformConstraint = platformConstraint;
    }

    public void setQuery( String query ) {
        this.query = query;
    }

    public void setSearchBibrefs( Boolean searchBibrefs ) {
        this.searchBibrefs = searchBibrefs;
    }

    public void setSearchBioSequences( Boolean searchBioSequences ) {
        this.searchBioSequences = searchBioSequences;
    }

    public void setSearchExperiments( Boolean searchExperiments ) {
        this.searchExperiments = searchExperiments;
    }

    public void setSearchExperimentSets( Boolean searchExperimentSets ) {
        this.searchExperimentSets = searchExperimentSets;
    }

    public void setSearchGenes( Boolean searchGenes ) {
        this.searchGenes = searchGenes;
    }

    public void setSearchGeneSets( Boolean searchGeneSets ) {
        this.searchGeneSets = searchGeneSets;
    }

    public void setSearchPhenotypes( Boolean searchPhenotypes ) {
        this.searchPhenotypes = searchPhenotypes;
    }

    public void setSearchPlatforms( Boolean searchPlatforms ) {
        this.searchPlatforms = searchPlatforms;
    }

    public void setSearchProbes( Boolean searchProbes ) {
        this.searchProbes = searchProbes;
    }

    public void setTaxon( ubic.gemma.model.genome.Taxon taxon ) {
        this.taxon = taxon;
    }

    public void setTermUri( String termUri ) {
        this.termUri = termUri;
    }

    public void setUseCharacteristics( Boolean useCharacteristics ) {
        this.useCharacteristics = useCharacteristics;
    }

    public void setUseDatabase( Boolean useDatabase ) {
        this.useDatabase = useDatabase;
    }

    public void setUseGo( Boolean useGo ) {
        this.useGo = useGo;
    }

    public void setUseIndices( Boolean useIndices ) {
        this.useIndices = useIndices;
    }

}