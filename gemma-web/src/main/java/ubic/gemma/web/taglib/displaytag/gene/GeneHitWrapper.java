package ubic.gemma.web.taglib.displaytag.gene;

import org.compass.core.CompassHit;
import org.displaytag.decorator.TableDecorator;

import ubic.gemma.model.genome.Gene;

public class GeneHitWrapper extends TableDecorator {

    /**
     * @return String
     */
    public String getTaxon() {
        CompassHit hit = ( CompassHit ) getCurrentRowObject();
        return ( ( Gene ) hit.getData() ).getTaxon().getScientificName();
    }

    public String getOfficialName() {
        CompassHit hit = ( CompassHit ) getCurrentRowObject();
        return ( ( Gene ) hit.getData() ).getOfficialName();
    }

    public String getNcbiLink() {
        CompassHit hit = ( CompassHit ) getCurrentRowObject();
        String ncbiLink = "<a href='http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gene&cmd=Retrieve&dopt=full_report&list_uids="
                + ( ( Gene ) hit.getData() ).getNcbiGeneId() + "'>(ncbi)</a>";
        return ncbiLink;
    }

    public String getGemmaLink() {
        CompassHit hit = ( CompassHit ) getCurrentRowObject();
        String gemmaLink = "<a href='/Gemma/gene/showGene.html?id=" + ( ( Gene ) hit.getData() ).getId()
                + "'>(gemma)</a>";
        return gemmaLink;
    }

    public String getNameLink() {
        CompassHit hit = ( CompassHit ) getCurrentRowObject();
        String nameLink = ( ( Gene ) hit.getData() ).getName() + getGemmaLink() + getNcbiLink();
        return nameLink;
    }

    public String getScore() {
        CompassHit hit = ( CompassHit ) getCurrentRowObject();
        return String.valueOf( hit.getScore() );

    }

    public String getResource() {
        CompassHit hit = ( CompassHit ) getCurrentRowObject();
        return hit.getResource().toString();
    }

    public String getHighlightedText() {
        CompassHit hit = ( CompassHit ) getCurrentRowObject();
        if ( hit.getHighlightedText() == null ) return "";

        return hit.getHighlightedText().getHighlightedText();
    }

}
