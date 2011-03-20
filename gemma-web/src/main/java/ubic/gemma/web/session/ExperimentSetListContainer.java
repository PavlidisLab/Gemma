package ubic.gemma.web.session;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import ubic.gemma.model.genome.gene.GeneSetValueObject;
import ubic.gemma.web.controller.expression.experiment.ExpressionExperimentSetValueObject;

public class ExperimentSetListContainer implements Serializable {
	
	/**
	 * unfinished
	 */
	private static final long serialVersionUID = 2031936160381488016L;

	static final int MAX_SIZE = 10;
	
	ArrayList<ExpressionExperimentSetValueObject> experimentSetList;
	
	public ExperimentSetListContainer(){
		
		experimentSetList = new ArrayList<ExpressionExperimentSetValueObject>();
			
	}
	
	
	public void addExperimentSet(ExpressionExperimentSetValueObject eesvo){
		
		experimentSetList.add(eesvo);
		if (experimentSetList.size()>MAX_SIZE){
			experimentSetList.remove(0);
		}
		
	}
	
	public Collection<ExpressionExperimentSetValueObject> getRecentExperimentSets(){
		
		return experimentSetList;
		
	}
	
	

}
