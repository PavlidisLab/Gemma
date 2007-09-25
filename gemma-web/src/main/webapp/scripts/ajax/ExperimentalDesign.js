//===============================
//
//Tool bar for selecting available Factors and assigning Factor values to selected BioMaterials
//================================

//member variables
var assignFactorValueToBioMaterialButton, expFactorsCB, factorValueCB, bmGrid, saveNewFactorButton, removeFactorButton, descriptionField;	//gui components
var eeID,edID, clazz;
var factorValueComboDS, bmDS, factorDS;							//Datastores behind gui components
var bioMaterialList, selectedFactorId, selectedFactorValueId, selectedFactorsInGrid;	//what is selected by user
var bmGridRefresh;									//methods
var vocabC = {};

var createMgedComboBox = function(terms){				
				
			
	var     recordType = Ext.data.Record.create([
					   {name:"id", type:"int"},
                       {name:"term", type:"string"},
                       {name:"uri", type:"string"},
               ]);
									
                    	
                var ds = new Ext.data.Store(
               {
                       proxy:new Ext.data.DWRProxy(MgedOntologyService.getUsefulMgedTerms),
                       reader:new Ext.data.ListRangeReader({id:"id"}, recordType),
                       remoteSort:false,
                       sortInfo:{field:'term'}
               });
               
				ds.load();
						
					    var combo = new Ext.form.ComboBox({	
					    	width: 200,				    	
					        store: ds,
					        fieldLabel: 'Mged',
					        displayField:'term',
					        typeAhead: true,
					        mode: 'local',
					        triggerAction: 'all',
					        emptyText:'Select a term',
					        selectOnFocus:true
					    });	
					    					   
	
		    			var comboHandler = function(field,record,index){
					    	
					    	vocabC.categoryUri = record.data.uri;
							vocabC.category = record.data.term;				    	
							
							if (vocabC.category == null)	//need to check if we should enable the save button.
								saveNewFactorButton.disable();
							else
								saveNewFactorButton.enable();
					    								      						 					    	                        
    	                };
    	                	
	
				  	 	combo.on('select', comboHandler);
					    
					    return combo;	
	
}

var createFactorDescriptionField = function(){
	
	descriptionField = new Ext.form.TextField({allowBlank : false, invalidText : "Enter a discription", blankText : "Add a simple description", value : "Description"});
	return descriptionField;
}

var saveExperimentalFactor = function(){

	//Use for debugging. 
	//console.log(dwr.util.toDescriptiveString(vocabC,10))
	//Make a copy, then send it over the wire. 
	//If there is no valueUri then it is plain text and we don't want dwr to instantiate a
	//VocabCharacteritic but a Characteritic. 
	
	var description = descriptionField.getValue();
	if ((description === undefined) || (description.length === 0)){
		alert("Please add a description");
		return;
	}
	var newVocabC = {};
	
	newVocabC.value = vocabC.value;
	newVocabC.category = vocabC.category;
	
	if (vocabC.valueUri){
		newVocabC.categoryUri = vocabC.categoryUri ;
		newVocabC.valueUri = vocabC.valueUri;
	}else if (vocabC.categoryUri){
		newVocabC.categoryUri = vocabC.categoryUri ;
		newVocabC.valueUri = vocabC.categoryUri;
	}
	
	var factor = { description: description, 
    categoryCharacteritic: newVocabC};
	
	ExperimentalDesignController.createNewFactor(factor, {id: edID, classDelegatingFor:"long"}, factorGridRefresh);
	
}

var deleteExperimentalFactor = function(){
	
	ExperimentalDesignController.deleteFactor(selectedFactorsInGrid, {id:eeID, classDelegatingFor:"long"}, factorGridRefresh);
	
}


//===================================
//
//===================================
var createFactorComboBox = function(terms){				
				
			
	var     recordType = Ext.data.Record.create([
					   {name:"id", type:"int"},
                       {name:"factorValue", type:"string"},
                       {name:"description", type:"string"},
                       {name:"category", type:"string"}
               ]);
									
                    	
                var factorComboDS = new Ext.data.Store(
               {
                       proxy:new Ext.data.DWRProxy(ExpressionExperimentController.getExperimentalFactors),
                       reader:new Ext.data.ListRangeReader({id:"id"}, recordType),
                       remoteSort:false,
                       sortInfo:{field:'factorValue'}
               });
               
				factorComboDS.load({params:[{id:eeID, classDelegatingFor:"FactorValueObject"}]});
						
					    var combo = new Ext.form.ComboBox({	
					    	width: 200,				    	
					        store: factorComboDS,
					        fieldLabel: 'Factors',
					        displayField:'factorValue',
					        typeAhead: true,
					        mode: 'local',
					        triggerAction: 'all',
					        emptyText:'Available Factors',
					        selectOnFocus:true,
					        editable: false
					    });	
					    					   
	
		    			var comboHandler = function(field,record,index){
					    	
							//TODO: When a factor is selected refresh the bm table to display the selected factor
							//update the factor value combo box with the factor values associated with the selected factor

							selectedFactorId = record.id;
							factorValueComboDS.reload({params:[{id:selectedFactorId, classDelegatingFor:"FactorValueObject"}]});
							factorValueCB.reset();
							bmGridRefresh(selectedFactorId);
							assignFactorValueToBioMaterialButton.disable();					    								      						 					    	                        
    	                };
    	                	
	
				  	 	combo.on('select', comboHandler);
					    
					    return combo;	
	
}

var createFactorValueComboBox = function(){
	
	
		var     recordType = Ext.data.Record.create([
				   		{name:"id", type:"int"},
                       	{name:"factorValue", type:"string"},
                       	{name:"description", type:"string"},
                       	{name:"category", type:"string"}
               ]);
									
                    	
                factorValueComboDS = new Ext.data.Store(
               {
                       proxy:new Ext.data.DWRProxy(ExpressionExperimentController.getFactorValues),
                       reader:new Ext.data.ListRangeReader({id:"id"}, recordType),
                       remoteSort:false,
                       sortInfo:{field:'factorValue'}
               });
               
				
						
					    var combo = new Ext.form.ComboBox({	
					    	width: 200,				    	
					        store: factorValueComboDS,
					        fieldLabel: 'Factor Values',
					        displayField:'factorValue',
					        typeAhead: true,
					        mode: 'local',
					        triggerAction: 'all',
					        emptyText:'Factor Values',
					        selectOnFocus:true,
					        editable: false
					    });	
					    			
					    var comboHandler = function(field,record,index){
					    	
							//TODO: When a factor is selected refresh the bm table to display the selected factor
							//update the factor value combo box with the factor values associated with the selected factor

							selectedFactorValueId = record.id;
							assignFactorValueToBioMaterialButton.enable();									    								      						 					    	                        
    	                };
    	                	
	
				  	 	combo.on('select', comboHandler);		   
	    	                						    
					    return combo;
}

var saveFactorValueToBMHandler = function(){

	//Use for debugging. 
	//console.log(dwr.util.toDescriptiveString(vocabC,10))

	if ((bioMaterialList === undefined) || (bioMaterialList.length === 0)){
		alert("Please select a biomaterial");
		return;
	}
	
	BioMaterialController.addFactorValueTo(bioMaterialList, {id:selectedFactorValueId, classDelegatingFor:"FactorValueObject"}, bmGridRefresh );
	
	
}


//=================================================
//
//	Biomaterial table with a single experimental factor
//=================================================

bmGridRefresh = function(){
	
	bmDS.reload({params:[{id:eeID, classDelegatingFor:"expressionExperimentID"},{id:selectedFactorId, classDelegatingFor: "FactorID"}]});	
	bmGrid.getView().refresh(true);	
	
}

var initBioMaterialGrid = function(div) {
       var     recordType = Ext.data.Record.create([
                       {name:"id", type:"int"},
                       {name:"name", type:"string"},
                       {name:"description", type:"string"},
                       {name:"factorValue", type:"string"}
                       
                       
               ]);


       bmDS = new Ext.data.Store(
               {
                       proxy:new Ext.data.DWRProxy(BioMaterialController.getBioMaterialsForEEWithFactor),
                       reader:new Ext.data.ListRangeReader({id:"id"}, recordType),
                       remoteSort:false,
                       sortInfo:{field:'name'}
               });

       var cm = new Ext.grid.ColumnModel([
                       {header: "Name", width: 50, dataIndex:"name"},
                       {header: "Description",  width: 100, dataIndex:"description"}, 
                       {header: "Factor Value",  width: 50, dataIndex:"factorValue"}
                       
                       ]);
       cm.defaultSortable = true;

       bmGrid = new Ext.grid.Grid(div, {autoSizeColumns: true,
       							 ds:bmDS,
       							 cm:cm,
       							 loadMask: true });
       
       var gridClickHandler = function(bmGrid, rowIndex, event){
       		//Get the ids of the selected biomaterials and put them in BiomatierialList
	       	var selected = bmGrid.getSelectionModel().getSelections();	
	   
	    	bioMaterialList = [];
	    	for(var index=0; index<selected.length; index++) {	    		
	    		bioMaterialList.push(selected[index].id);
	    	}  	
       	
       }
       
       bmGrid.on("rowclick", gridClickHandler);
       
       bmGrid.render();
	
};


//=================================================
//
//	Experimental Factor table
//=================================================

factorGridRefresh = function(){
	
	factorDS.reload({params:[{id:eeID, classDelegatingFor:"expressionExperimentID"}]});	
	factorGrid.getView().refresh(true);	
	
}

var initFactorGrid = function(div) {
	
	
				
	var     recordType = Ext.data.Record.create([
					   {name:"id", type:"int"},
                       {name:"factorValue", type:"string"},
                       {name:"description", type:"string"},
                       {name:"category", type:"string"}
               ]);
									
                    	
     factorDS = new Ext.data.Store(
     { 		           proxy:new Ext.data.DWRProxy(ExpressionExperimentController.getExperimentalFactors),
                       reader:new Ext.data.ListRangeReader({id:"id"}, recordType),
                       remoteSort:false,
                       sortInfo:{field:'factorValue'}
      });
               
	   factorDS.load({params:[{id:eeID, classDelegatingFor:"FactorValueObject"}]});
	

       var cm = new Ext.grid.ColumnModel([
                       {header: "Factor", width: 50, dataIndex:"factorValue"},
                       {header: "Description",  width: 100, dataIndex:"description"}, 
                       {header: "Category",  width: 50, dataIndex:"category"}
                       
                       ]);
       cm.defaultSortable = true;

       factorGrid = new Ext.grid.Grid(div, {autoSizeColumns: true,
       							 ds:factorDS,
       							 cm:cm,
       							 loadMask: true });
       //todo: change the selection model to just one instead of multiple. factorGrid.getSelectionModel().
       
       var gridClickHandler = function(factorGrid, rowIndex, event){

			
       		var selections =  factorGrid.getSelectionModel().getSelections();
       		
       		if (selections.length === 0){
       			removeFactorButton.disable();
       			return;
       		}
       		
       		selectedFactorsInGrid = [];
	    	for(var index=0; index<selections.length; index++) {	    		
	    		selectedFactorsInGrid.push(selections[index].id);
	    	}  	
       		factorValueGridRefresh(selectedFactorsInGrid[0]); //just show the 1st one in the factor value table
       		removeFactorButton.enable();
       		
       	
       }
       
       factorGrid.on("rowclick", gridClickHandler);
       
       factorGrid.render();
	
};


//=================================================
//
//	Factor Value table
//=================================================

factorValueGridRefresh = function(selectedFactorId){
	
	factorValueGridDS.reload({params:[{id:selectedFactorId, classDelegatingFor:"expressionExperimentID"}]});	
	factorValueGrid.getView().refresh(true);	
	
}

var initFactorValueGrid = function(div) {
	
	
				
		var     recordType = Ext.data.Record.create([
				   		{name:"id", type:"int"},
                       	{name:"factorValue", type:"string"},
                       	{name:"description", type:"string"},
                       	{name:"category", type:"string"}
               ]);
									
                    	
                factorValueGridDS = new Ext.data.Store(
               {
                       proxy:new Ext.data.DWRProxy(ExpressionExperimentController.getFactorValues),
                       reader:new Ext.data.ListRangeReader({id:"id"}, recordType),
                       remoteSort:false,
                       sortInfo:{field:'factorValue'}
               });
                          
	  
	

       var cm = new Ext.grid.ColumnModel([
                       {header: "Factor Value", width: 50, dataIndex:"factorValue"},
                       {header: "Description",  width: 100, dataIndex:"description"}, 
                       {header: "Category",  width: 50, dataIndex:"category"}
                       
                       ]);
       cm.defaultSortable = true;

       factorValueGrid = new Ext.grid.Grid(div, {autoSizeColumns: true,
       							 ds:factorValueGridDS,
       							 cm:cm,
       							 loadMask: true, 
       							 editable: true });   
      factorValueGrid.render();
	
};


Ext.onReady(function() {

	eeID = dwr.util.getValue("expressionExperimentID");
	edID = dwr.util.getValue("experimentalDesignID");
	
	//the mged combo box 
	expFactorsCB = createFactorComboBox();   
	
	//the lookup combobox
	factorValueCB = createFactorValueComboBox();		

	//The tool bar. 
	var simpleTB = new Ext.Toolbar("eDesign");
	
	simpleTB.addField(expFactorsCB);
	simpleTB.addSpacer();
	simpleTB.addField(factorValueCB);
	simpleTB.addSpacer();
	assignFactorValueToBioMaterialButton = simpleTB.addButton({text: 'assign',
						tooltip: 'assigns the selected Factor Value to the selected BioMaterials',								  
						handler: saveFactorValueToBMHandler,
						disabled: true
					});
					
					
	initBioMaterialGrid("bmGrid");			
	
	initFactorGrid("factorGrid");
 	initFactorValueGrid("factorValueGrid");	
 	
 	var factorTB = new Ext.Toolbar("factorGridTB");
	
	factorTB.addField(createMgedComboBox());
	factorTB.addSpacer();
	factorTB.addField(createFactorDescriptionField());
	factorTB.addSpacer();
	saveNewFactorButton = factorTB.addButton({text: 'create',
						tooltip: 'creates a new Experimental Factor',								  
						handler: saveExperimentalFactor,
						disabled: true
					});
 	factorTB.addSpacer();
	removeFactorButton = factorTB.addButton({text: 'remove',
						tooltip: 'removes the selected Experimental Factor',								  
						handler: deleteExperimentalFactor,
						disabled: true
					});
 		
	
});