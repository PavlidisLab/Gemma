//===============================
//
//Tool bar for selecting available Factors and assigning Factor values to selected BioMaterials
//================================

//member variables
var assignFactorValueToBioMaterialButton, expFactorsCB, factorValueCB, bmGrid, saveNewFactorButton, removeFactorButton, factorDescriptionField;	//gui components
var eeID,edID, clazz;
var factorValueComboDS, bmDS, factorDS, factorComboDS;							//Datastores behind gui components
var bioMaterialList, selectedFactorId = 0, selectedFactorValueId, selectedFactorsInGrid	//what is selected by user
var bmGridRefresh;									//methods

var factorMgedComboBox;

var createMgedComboBox = function(comboHandler){				
				
	var mgedCombo = new Ext.Gemma.MGEDCombo( {
		emptyText : "Select a class",
		selectOnFocus : true,
		width : 125
	} );
	
	mgedCombo.on('select', comboHandler);
	
	return mgedCombo;					    
					    	
}

var createFactorDescriptionField = function(){
	
	factorDescriptionField = new Ext.form.TextField({allowBlank : false, invalidText : "Enter a description", blankText : "Add a simple description", emptyText : "Description", width: 150});
	return factorDescriptionField;
}



var saveExperimentalFactor = function(){

	//Use for debugging. 
	//console.log(dwr.util.toDescriptiveString(vocabC,10))
	//Make a copy, then send it over the wire. 
	//If there is no valueUri then it is plain text and we don't want dwr to instantiate a
	//VocabCharacteritic but a Characteritic. 
	
	var description = factorDescriptionField.getValue();
	//Don't want description to be a mandatory field	
	//	if ((description === undefined) || (description.length === 0) || description === "description"){
	//		alert("Please add a description");
	//		return;
	//	}
	
	var term = factorMgedComboBox.getTerm();	
	
	var newVocabC = {};	
	newVocabC.value = term.term;
	newVocabC.category = term.term;
	newVocabC.categoryUri = term.uri;
	newVocabC.valueUri = term.uri;
	
	var factor = { description: description, 
    categoryCharacteritic: newVocabC};
	
	ExperimentalDesignController.createNewFactor(factor, {id: edID, classDelegatingFor:"long"}, factorGridRefresh);
	
	saveNewFactorButton.disable();
	factorMgedComboBox.reset();
	factorDescriptionField.reset();
	
}

var deleteExperimentalFactor = function(){
	
	//TODO:  get the selection model explicitly and remove the intermediate variable of selectedFactorsInGrid
	ExperimentalDesignController.deleteFactor(selectedFactorsInGrid, {id:eeID, classDelegatingFor:"long"}, factorGridRefresh);
	
	removeFactorButton.disable();
		
}

var saveExperimentalFactorValue = function(characteristic, callback){
	if(selectedFactorsInGrid.length === 0){
		alert("No factor is selected");
		return;
	}
		
	ExperimentalDesignController.createNewFactorValue({id:selectedFactorsInGrid[0], classDelegatingFor:"long"},[characteristic], callback);
	
}

var deleteExperimentalFactorValue = function(characteristic, callback){
	
		ExperimentalDesignController.deleteFactorValue(factorValueGridGetSelectedIds(), {id:selectedFactorsInGrid[0], classDelegatingFor:"long"},{id:eeID, classDelegatingFor:"long"}, callback);
		
}


var createFactorComboBox = function(terms){				
				
			
	var     recordType = Ext.data.Record.create([
					   {name:"id", type:"int"},
                       {name:"factorValue", type:"string"},
                       {name:"description", type:"string"},
                       {name:"category", type:"string"}
               ]);
									
                    	
                factorComboDS = new Ext.data.Store(
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
					    				
							selectedFactorValueId = record.id;
							assignFactorValueToBioMaterialButton.enable();									    								      						 					    	                        
    	                };
    	                	
	
				  	 	combo.on('select', comboHandler);		   
	    	                						    
					    return combo;
}


var refreshBMFactorComboBoxes = function(){
	
//refresh experimental factor combo box		
	factorComboDS.reload({params:[{id:eeID, classDelegatingFor:"long"}]});
	expFactorsCB.reset();
	
//refresh factor value combo box
	factorValueComboDS.reload({params:[{id:selectedFactorId, classDelegatingFor:"Long"}]}, function() {factorValueCB.getView().refresh(true)} );
	factorValueCB.reset();
	
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
		
	bmDS.reload( {
		params : [ {id:eeID, classDelegatingFor:"expressionExperimentID"}, {id:selectedFactorId, classDelegatingFor: "FactorID"} ],
		callback : function() {
			bmGrid.getView().refresh(true);
		}
	} );		
	
}

var initBioMaterialGrid = function(div) {
       var     recordType = Ext.data.Record.create([
                       {name:"id", type:"int"},
                       {name:"name", type:"string"},
                       {name:"description", type:"string"},
                       {name:"bioAssayDescription", type:"string"},   
                       {name:"bioAssayName", type:"string"},                                              
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
                       {header: "BioAssay Info",  width: 100, dataIndex:"bioAssayDescription"},   
                       {header: "BioAssay Name",  width: 100, dataIndex:"bioAssayName"},                                               
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
       
      bmGridRefresh();
	
};


//=================================================
//
//	Experimental Factor table
//=================================================

factorGridRefresh = function(){
	
	factorDS.reload( {
		params : [{id:eeID, classDelegatingFor:"expressionExperimentID"}],
		callback : function() {	
			factorGrid.getView().refresh(true);	
			refreshBMFactorComboBoxes();
		}
	} );
	
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
     factorDS.on( "load", function() {
       	factorGrid.getView().autoSizeColumns();
     } );
               
	   factorDS.load({params:[{id:eeID, classDelegatingFor:"FactorValueObject"}]});
	

       var cm = new Ext.grid.ColumnModel([
                       {header: "Factor", width: 50, dataIndex:"factorValue"}, 
                       {header: "Category",  width: 50, dataIndex:"category"},
                       {header: "Description",  width: 100, dataIndex:"description"}
                       
                       ]);
       cm.defaultSortable = true;

       factorGrid = new Ext.grid.Grid(div, {autoSizeColumns: true,
       							 ds:factorDS,
       							 cm:cm,
       							 loadMask: true });
       
       var gridClickHandler = function(factorGrid, rowIndex, event){

			
       		var selections =  factorGrid.getSelectionModel().getSelections();
       		
       		if (selections.length === 0){
       			if (removeFactorButton)
       			removeFactorButton.disable();
       			return;
       		}
       		
       		selectedFactorsInGrid = [];
	    	for(var index=0; index<selections.length; index++) {	    		
	    		selectedFactorsInGrid.push(selections[index].id);
	    	}  	
	    		 
			
			factorValueGridRefresh(); //just show the 1st one in the factor value table
			if (removeFactorButton)
			 		removeFactorButton.enable();
       		
       	
       }
       
       factorGrid.on("rowclick", gridClickHandler);
       
       factorGrid.render();
	
};


//=================================================
//
//	Factor Value table
//=================================================

factorValueGridRefresh = function(){
		
	var selections =  factorGrid.getSelectionModel().getSelections();
	
	factorValueGridDS.reload( {
		params : [{id:selections[0].id, classDelegatingFor:"long"}],
		callback : function() {
			factorValueGrid.getView().refresh(true);
			refreshBMFactorComboBoxes();
		}
	} );
}

factorValueGridGetSelectedIds = function() {
		    
		var selected = factorValueGrid.getSelectionModel().getSelections();
		var ids = [];
		for ( var i=0; i<selected.length; ++i ) {
			ids.push( selected[i].id );
		}
		return ids;	
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
     factorValueGridDS.on( "load", function() {
       	factorValueGrid.getView().autoSizeColumns();
     } );
                          
	  
	

       var cm = new Ext.grid.ColumnModel([
                       {header: "Factor Value", width: 50, dataIndex:"factorValue"},                      
                       {header: "Category",  width: 50, dataIndex:"category"},
                       {header: "Description",  width: 50, dataIndex:"description"}                       
                       ]);
       cm.defaultSortable = true;

		
       factorValueGrid = new Ext.grid.Grid(div, {autoSizeColumns: true,
       							 ds:factorValueGridDS,
       							 cm:cm,
       							 loadMask: true, 
       							 editable: true}
       							 );

		//Need two extra methods because annotationGrid defines refresh and getSelected      							 
       factorValueGrid.refresh = factorValueGridRefresh;
       factorValueGrid.getSelectedIds = factorValueGridGetSelectedIds;
              							 
      factorValueGrid.render();
	
};


Ext.onReady(function() {

	eeID = dwr.util.getValue("expressionExperimentID");
	edID = dwr.util.getValue("experimentalDesignID");
	

	//================================================
	//The tool bm to factor value assoction toolbar. 
	// ===============================================	

	//TODO: rename the div to be more obvious to its purpose
	 if (Ext.get("eDesign")){	//Don't show anything unless this div exists
	
		expFactorsCB = createFactorComboBox();   				//the available expimental factors  
		factorValueCB = createFactorValueComboBox();			//the availiable factor values

		var simpleTB = new Ext.Toolbar("eDesign");				//Tool bar to rule them all	
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
	 }				
				
	//===================================
	//Experimental factor grid and tool bar
	//===================================
	initFactorGrid("factorGrid"); 	 	
	if (Ext.get("factorGridTB")){
	
    	
    	factorMgedComboBox = createMgedComboBox(function(field,record,index){saveNewFactorButton.enable();});
	 	var factorTB = new Ext.Toolbar("factorGridTB");	
		factorTB.addField(factorMgedComboBox);
		factorTB.addSpacer();
		factorTB.addField(createFactorDescriptionField());
		factorTB.addSpacer();
		saveNewFactorButton = factorTB.addButton({text: 'save',
							tooltip: 'creates a new Experimental Factor',								  
							handler: saveExperimentalFactor,
							disabled: true
						});
	 	factorTB.addSeparator();
		removeFactorButton = factorTB.addButton({text: 'delete',
							tooltip: 'removes the selected Experimental Factor',								  
							handler: deleteExperimentalFactor,
							disabled: true
						});
								
	}
	
	//=============================
	//Factor Value Grid and Tool bar
	//=============================
						
	initFactorValueGrid("factorValueGrid");	

	if (Ext.get("factorValueTB")){
		
		var factorValueTB = new Ext.Gemma.AnnotationToolBar( "factorValueTB",
			factorValueGrid, saveExperimentalFactorValue, deleteExperimentalFactorValue, true, { mgedComboWidth: 125, charComboWidth: 100 } );
					
	}
 		
	
});