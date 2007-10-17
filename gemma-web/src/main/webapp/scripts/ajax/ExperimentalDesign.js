//===============================
//
//Tool bar for selecting available Factors and assigning Factor values to selected BioMaterials
//================================

//member variables
var assignFactorValueToBioMaterialButton, expFactorsCB, factorValueCB, bmGrid, saveNewFactorButton, removeFactorButton, factorDescriptionField,factorValueDescriptionField, saveNewFactorValueButton, removeFactorValueButton;	//gui components
var eeID,edID, clazz;
var factorValueComboDS, bmDS, factorDS, factorComboDS;							//Datastores behind gui components
var bioMaterialList, selectedFactorId, selectedFactorValueId, selectedFactorsInGrid, selectedFactorValuesInGrid;	//what is selected by user
var bmGridRefresh;									//methods
var mgedSelectedVocabC = {};
var ontologySearchVocabC = {};

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
					    	
					    	mgedSelectedVocabC.categoryUri = record.data.uri;
							mgedSelectedVocabC.category = record.data.term;				    	
							
							if (mgedSelectedVocabC.category == null)	//need to check if we should enable the save button.
								saveNewFactorButton.disable();
							else
								saveNewFactorButton.enable();
					    								      						 					    	                        
    	                };
    	                	
	
				  	 	combo.on('select', comboHandler);
					    
					    return combo;	
	
}

var createFactorDescriptionField = function(){
	
	factorDescriptionField = new Ext.form.TextField({allowBlank : false, invalidText : "Enter a discription", blankText : "Add a simple description", value : "Description"});
	return factorDescriptionField;
}

var createFactorValueDescriptionField = function(){
	
	factorValueDescriptionField = new Ext.form.TextField({allowBlank : false, invalidText : "Enter a discription", blankText : "Add a simple description", value : "Description"});
	return factorValueDescriptionField;
}


var saveExperimentalFactor = function(){

	//Use for debugging. 
	//console.log(dwr.util.toDescriptiveString(vocabC,10))
	//Make a copy, then send it over the wire. 
	//If there is no valueUri then it is plain text and we don't want dwr to instantiate a
	//VocabCharacteritic but a Characteritic. 
	
	var description = factorDescriptionField.getValue();
	if ((description === undefined) || (description.length === 0)){
		alert("Please add a description");
		return;
	}
	var newVocabC = {};
	
	newVocabC.value = mgedSelectedVocabC.value;
	newVocabC.category = mgedSelectedVocabC.category;
	
	if (mgedSelectedVocabC.valueUri){
		newVocabC.categoryUri = mgedSelectedVocabC.categoryUri ;
		newVocabC.valueUri = mgedSelectedVocabC.valueUri;
	}else if (mgedSelectedVocabC.categoryUri){  //Possible to have a categoryURI and no valueUri
		newVocabC.categoryUri = mgedSelectedVocabC.categoryUri ;
		newVocabC.valueUri = mgedSelectedVocabC.categoryUri;
	}
	
	var factor = { description: description, 
    categoryCharacteritic: newVocabC};
	
	ExperimentalDesignController.createNewFactor(factor, {id: edID, classDelegatingFor:"long"}, factorGridRefresh);
	
}

var deleteExperimentalFactor = function(){
	
	ExperimentalDesignController.deleteFactor(selectedFactorsInGrid, {id:eeID, classDelegatingFor:"long"}, factorGridRefresh);
		
}

var saveExperimentalFactorValue = function(){
	if(selectedFactorsInGrid.length === 0){
		alert("No factor is selected");
		return;
	}
	
	var newVocabC = {};
	newVocabC.value = ontologySearchVocabC.value;
	newVocabC.valueUri = ontologySearchVocabC.valueUri;
	newVocabC.category = mgedSelectedVocabC.category;
	newVocabC.categoryUri = mgedSelectedVocabC.categoryUri ;
	

	
	var description = factorValueDescriptionField.getValue();
	if ((description === undefined) || (description.length === 0)){
		alert("Please add a description");
		return;
	}
	newVocabC.description = description;
	
//	if (ontologySearchVocabC.valueUri){
//		newVocabC.categoryUri = ontologySearchVocabC.categoryUri ;
//		newVocabC.valueUri = ontologySearchVocabC.valueUri;
//	}else if (mgedSelectedVocabC.categoryUri){
//		newVocabC.categoryUri = mgedSelectedVocabC.categoryUri ;
//		newVocabC.valueUri = mgedSelectedVocabC.categoryUri;
//	}
	
	ExperimentalDesignController.createNewFactorValue({id:selectedFactorsInGrid[0], classDelegatingFor:"long"},[newVocabC], factorValueGridRefresh);
	
}

var deleteExperimentalFactorValue = function(){
	
		ExperimentalDesignController.deleteFactorValue(selectedFactorValuesInGrid, {id:selectedFactorsInGrid[0], classDelegatingFor:"long"},{id:eeID, classDelegatingFor:"long"}, factorValueGridRefresh);
		
}

var createOntologySearchComponent = function(){
	
	
	 var searchHandler = function(record, index){
        	
        	 
            if(this.fireEvent('beforeselect', this, record, index) !== false){
        	    this.setValue(record.data.value);
            	this.collapse();
            	this.fireEvent('select', this, record, index);
							
				ontologySearchVocabC.valueUri = record.data.valueUri;
				ontologySearchVocabC.value = record.data.value;
				saveNewFactorValueButton.enable();
            }           	
    	                	
        }
    
    
        
    var getStyle = function(record) {
    	if ( record.description.substring(0, 8) == " -USED- ")
    		return record.valueUri ? "usedWithUri" : "usedNoUri";
    	else
    		return record.valueUri ? "unusedWithUri" : "unusedNoUri";
    }
    
    var getDescription = function(record) {
    	if ( record.valueUri )
    		return record.valueUri;
    	else
    		return ( record.description.substring(0, 8) == " -USED- " ) ?
    			record.description.substring(8) : record.description;
    }    
        
	var     recordType = Ext.data.Record.create([
					   {name:"id", type:"int"},
                       {name:"value", type:"string"},
                       {name:"valueUri", type:"string"},
                       {name:"categoryUri",type:"string"},
                       {name:"category", type:"string"},
                       {name:"description", mapping:"this", convert:getDescription},
                       {name:"style", mapping:"this", convert:getStyle}
               ]);


       ds = new Ext.data.Store(
               {
                       proxy:new Ext.data.DWRProxy(OntologyService.findExactTerm),
                       reader:new Ext.data.ListRangeReader({id:"id"}, recordType),
                       remoteSort:false                     
               });

       var cm = new Ext.grid.ColumnModel([
                       {header: "term", width: 50, dataIndex:"value"},
                       {header: "uri",  width: 80, dataIndex:"valueUri"}                       
                       ]);
       cm.defaultSortable = true;	
    
     // Custom rendering Template
    var resultTpl = new Ext.Template(
        '<div class="search-item">',
            '<div class="{style}" title="{description}">{value}</div>',
        '</div>'
    );
    
    var search = new Ext.form.ComboBox({
    	width: 300,
        store: ds,
        displayField:'title',
        fieldLabel: 'Lookup',
        typeAhead: false,
        loadingText: 'Searching...',     
        pageSize:0,
        minChars: 2,
        tpl: resultTpl,
        hideTrigger:true,    
        onSelect: searchHandler,  
        getParams: function (q) {	//Need to overide this so that the query data makes it to the client side. Otherwise its not included. 
    		var p = [q]; 
    		
    		ontologySearchVocabC.value = q;	//if the user doesn't select a provided ontolgy term this will set it to be the free text. 
    		if (!mgedSelectedVocabC.categoryUri)
    			mgedSelectedVocabC.categoryUri="{}";
    			
   		 	p.push(mgedSelectedVocabC.categoryUri);
   		 		
   		 	return p;
		}
        
    });
	
	return search;
	
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
	factorComboDS.reload({params:[{id:eeID, classDelegatingFor:"long"}]}, function() {expFactorsCB.getView().refresh(true)});
	//expFactorsCB.getView().refresh(true);
	
//refresh factor value combo box
	factorValueComboDS.reload({params:[{id:selectedFactorId, classDelegatingFor:"Long"}]}, function() {factorValueCB.getView().refresh(true)} );
	//factorValueCB.getView().refresh(true);
	
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
	refreshBMFactorComboBoxes();
	
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
	    	
	    	mgedSelectedVocabC.categoryUri = selections[0].data.category;
			mgedSelectedVocabC.category = selections[0].data.factorValue;
			
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
	
//	if (!selectedFactorId){
//		selectedFactorId = selectedFactorsInGrid[0];
//	}
	
	var selections =  factorGrid.getSelectionModel().getSelections();
	
	factorValueGridDS.reload({params:[{id:selections[0].id, classDelegatingFor:"long"}]}, function() {factorValueGrid.getView().refresh(true)} );	
//	factorValueGrid.getView().refresh(true);	
	refreshBMFactorComboBoxes();	
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
                       {header: "Category",  width: 50, dataIndex:"category"},
                       {header: "Description",  width: 50, dataIndex:"description"}                       
                       ]);
       cm.defaultSortable = true;

       factorValueGrid = new Ext.grid.Grid(div, {autoSizeColumns: true,
       							 ds:factorValueGridDS,
       							 cm:cm,
       							 loadMask: true, 
       							 editable: true });   

       var gridClickHandler = function(factorGrid, rowIndex, event){

			
       		var selections =  factorGrid.getSelectionModel().getSelections();
       		
       		if (selections.length === 0){
       			if (removeFactorButton)
       				removeFactorButton.disable();
       			return;
       		}
       		
       		selectedFactorValuesInGrid = [];
	    	for(var index=0; index<selections.length; index++) {	    		
	    		selectedFactorValuesInGrid.push(selections[index].id);
	    	}  	
       		
       		if (removeFactorValueButton)
       			removeFactorValueButton.enable();
       		
       	
       }
       
       factorValueGrid.on("rowclick", gridClickHandler);
       							 
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
	 if (Ext.get("eDesign")){
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
	 }				
				
	
	initFactorGrid("factorGrid"); 	 	
	if (Ext.get("factorGridTB")){
	
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
	}
						
	initFactorValueGrid("factorValueGrid");	

	if (Ext.get("factorValueTB")){
		
		var factorValueTB = new Ext.Toolbar("factorValueTB");	
		factorValueTB.addField(createOntologySearchComponent());	
		factorValueTB.addSpacer();
		factorValueTB.addField(createFactorValueDescriptionField());
		factorValueTB.addSpacer();
		
		saveNewFactorValueButton = factorValueTB.addButton({text: 'create',
							tooltip: 'creates a new Experimental Factor Value',								  
							handler: saveExperimentalFactorValue,
							disabled: true
						});
	 	factorValueTB.addSpacer();
		removeFactorValueButton = factorValueTB.addButton({text: 'remove',
							tooltip: 'removes the selected Experimental Factor Value',								  
							handler: deleteExperimentalFactorValue,
							disabled: true
						});
	}
 		
	
});