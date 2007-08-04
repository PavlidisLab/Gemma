


//expose the main layout panels for use later. 
 var northPanel, southPanel, eastPanel, westPanel, centerPanel;


onSubmit = function() {
	
}



//The simple function will be called when the document is finished loading.  See event at bottem of file 
Simple = function() {

   return {
       init : function() {
           var mainLayout = new Ext.BorderLayout("layout", {
               north: {
                   split: false, initialSize: 80
               },
               south: {
                   split: false, initialSize: 400
               },
               east: {
                   split: true, initialSize: 0
               },
               west: {
                   split: true, initialSize: 300
               },
               center: { split: true, initialSize: 80
               }
           });
           mainLayout.beginUpdate();
           mainLayout.add('north', northPanel = new Ext.ContentPanel('north-div', { 
              fitToFrame: true, closable: false, autoScroll: true
           }));
           mainLayout.add('south', southPanel = new Ext.ContentPanel('south-div', {
               fitToFrame: true, closable: false
           }));
           mainLayout.add('east', eastPanel = new Ext.ContentPanel('east-div', {
               fitToFrame: true, closable: false, autoScroll: true
           }));
           mainLayout.add('west', westPanel = new Ext.ContentPanel('west-div', {
               fitToFrame: true, closable: false, autoScroll: true
           }));
           mainLayout.add('center', centerPanel = new Ext.ContentPanel('center-div', {
               fitToFrame: true , autoScroll: true
           }));
           mainLayout.endUpdate();
                       mainLayout.getRegion('east').hide();
                       //mainLayout.getRegion('north').hide();
                       newOntologyLoader();
                       initBioMaterialGrid( );
                       initTree( );                     
               }
   };
}();

var showbms = function( id ) {
       var ids =  id.split(",");
       // note how we pass the new array in directly, without wraping it in an object first.
       ds.load({params:[ids]});
};

var ds;
var bioMaterialList;

var initBioMaterialGrid = function(div) {
       var     recordType = Ext.data.Record.create([
                       {name:"id", type:"int"},
                       {name:"name", type:"string"},
                       {name:"description", type:"string"}
               ]);


       ds = new Ext.data.Store(
               {
                       proxy:new Ext.data.DWRProxy(BioMaterialController.getBioMaterials),
                       reader:new Ext.data.ListRangeReader({id:"id"}, recordType),
                       remoteSort:false,
                       sortInfo:{field:'name'}
               });

       var cm = new Ext.grid.ColumnModel([
                       {header: "Name", width: 50, dataIndex:"name"},
                       {header: "Description",  width: 80, dataIndex:"description"}
                       ]);
       cm.defaultSortable = true;

       grid = new Ext.grid.Grid("south-div", {autoSizeColumns: true, ds:ds,cm:cm, loadMask: true });
       
       var gridClickHandler = function(grid, rowIndex, event){
       		//Get the ids of the selected biomaterials and put them in BiomatierialList
	       	var selected = grid.getSelectionModel().getSelections();	
	   
	    	bioMaterialList = [];
	    	for(var index=0; index<selected.length; index++) {	    		
	    		bioMaterialList.push(selected[index].id);
	    	}  	
       	
       }
       
       grid.on("rowclick", gridClickHandler);
       
       grid.render();

	
       var id = dwr.util.getValue("cslist");
       showbms(id);
};
var tree;
var root;

var initTree = function(div){
	
    tree = new Ext.tree.TreePanel("west-div", {
       animate:true,
       loader: new Ext.tree.DwrTreeLoader({dataUrl:MgedOntologyService.getBioMaterialTreeNodeTerms})
   });


    
   tree.on('click', function(node){                       
                   MgedOntologyService.getTerm(node.id,displayRestrictionsPanel);                                  			   
          });


   // set the root node
   root = new Ext.tree.AsyncTreeNode({
       text: 'Top of the tree',
       draggable:false,
       allowChildre:true,
       id:'root'
   });
   tree.setRootNode(root);


   // render the tree
   tree.render();
   root.expand();


};

var vocabC; 

//The call back method for the dwr call
var displayRestrictionsPanel = function(node){
	console.log(dwr.util.toDescriptiveString(node, 10));

	vocabC = { termUri : node.uri, classUri : node.uri, properties : [] };		
	createRestrictionGui(node, vocabC);
	
	var saveButton = new Ext.Button("center-div", {text : 'save'});
	saveButton.on("click", saveHandler);

};

var saveHandler = function(event){
	console.log(dwr.util.toDescriptiveString(vocabC,10))
	OntologyService.saveBioMaterialStatement(vocabC, bioMaterialList);
		
}

//Recursive function that walks the node given to it and creates a coresponding form to fill in
var createRestrictionGui = function(node, vc, indent, parentDivId) {

		var dh = Ext.DomHelper;  //allows html output

		if (indent === undefined){
			dh.overwrite("center-div", {html : ""});
			indent = 0;			
		}
		if (parentDivId === undefined) {
			parentDivId = "center-div";
			
		}
	
		

		dh.append(parentDivId, {html : node.term+ ": " + node.comment});
		

      
        var res = node.restrictions;
        if ( (res !== undefined) && (res !== null) && (res.size() > 0) ) {	//Has restrictions (not a leaf node)
        	
        	//TODO: Facotor out abstract class detection
        	//		Grey out rest of form
        	//		VC can get messed up quickly.  Need flag to be set so we know what mode we are in. (abstact or not mode)
        	//		
        	if (node.annotations !== undefined) {
				var annots = 	node.annotations;
				var isAbstract = false;
				for(var i = 0, len = annots.length; i<len; i++ ) {
			
					var ann = annots[i];
					if ( ann.property == "class_role" && ann.contents == "abstract") {
					isAbstract = true;
					break;
					}
				}
		
		        	if (isAbstract) {
					// make a text box so an instance can be created
						var simple = createAbstractForm(node, vc, parentDivId);
						simple.render(parentDivId);
					}
        	}
        	//End of abstract class code
        	
            for ( var i = 0, len = res.length; i < len; i++ ) {
            	
            	var restrictedOn = res[i].restrictionOn;
            	var restrictedTo = res[i].restrictedTo;
            	//make a nested div for adding to.
            	var divId = (Math.random() * 100000).toFixed();   

								
				if (specialCase(restrictedOn)){
					continue;	            	
				}
				     
            	            	
            	dh.append(parentDivId, {tag: 'div', id: divId, style : "border-width:thin;border-style:dotted;padding:5px;margin:5px;"});            
       	
            	if (restrictedOn.type !== undefined ) {	//Primitive Type
 					
                    	var simple = new Ext.form.Form({
					        labelWidth: 75, // label settings here cascade unless overridden
					        url:'save-form.php'
					    });
					    
					    var handler = createPrimitiveTypeHandler(restrictedOn, vc);

					    var valueField = new Ext.form.TextField({
					            fieldLabel: restrictedOn.label , // remove "has_"
					            name: 'hasValue',
					            width:175,
					            allowBlank:false,					           
					        });
					   
					   valueField.vocabId = divId;
					   							       
					   valueField.on('valid', handler);
					        
					   simple.add(valueField);
                     //  dh.append(divId, {tag: 'h3', html : 'Value:' });
 
					   simple.render(divId);
					   
					   
					   
            	} else if ( (restrictedTo !== undefined) && (restrictedTo !== null)) {	//Class restriction
                                       
                    if (restrictedTo.restrictions === undefined || restrictedTo.restrictions === null || restrictedTo.restrictions.size() === 0){	// ie) we are at a leaf node so display gui
                        dh.append(divId, {tag: 'h3', html : "Create an instance of: " +restrictedTo.term });
 					 
	                    var simple = createForm(restrictedOn,vc, divId);	
					                       
    	                if (restrictedTo.individuals !== undefined && restrictedTo.individuals !== null && restrictedTo.individuals.size() > 0){  //are there examples?
    	                		var combo = createComboBox(restrictedOn, restrictedTo.individuals, vc, divId);    	                
        		            	simple.column({width:285},combo);                                 	                    
                	    }
                    
	                    simple.render(divId);
                    } else{        //Not a leaf node. recurse down another level
                    	//do i need to use propertiesPush here? ie do i want a field id associated with this?
                        var vcChild = { termUri : restrictedOn.uri, classUri : restrictedOn.uri, object : { termUri : restrictedTo.uri }, properties : []};
                        propertiesPush(vc, divId,vcChild);
                        //vc.properties.push(vcChild);
    	            	createRestrictionGui( restrictedTo, vcChild.object, indent + 3, divId );
                   }                
                    
                } else if ( res[i].cardinality !== undefined  ) { //Cardinality Type
                    // this will be rare.                  
                    var cardinality = res[i].cardinality;
                    var cardinalityType = res[i].cardinalityType;
                    dh.append(parentDivId,{ html: " Cardinality Slot to fill in: " + restrictedOn.label + " with " + cardinalityType.term + " "
                            + cardinality + " things" });
                    // todo check range of the property (what 'things' should be) if specified.
                } else{
                	  dh.append(parentDivId,{ html: " Error: Not a typical type.  " + restrictedOn.label});                	
                }
            }
        }
        else{//No restrictions must be a leaf node. 
			
			//Create new div
        	var divId = (Math.random() * 100000).toFixed();   
           	dh.append(parentDivId, {tag: 'div', id: divId, style : "border-width:thin;border-style:dotted;padding:5px;margin:5px;"});
           	dh.append(divId, {tag: 'h3', html :  "Create an instance of: " + node.term });      
           	var simple = createLeafForm(node,vc, divId);	
           	
           	//If there already exisit individuals display them in a drop down box so the user can select one.
            if ((node.individuals !== undefined) && (node.individuals !== null) && (node.individuals.size() > 0))
            	simple.column({width:285},createLeafComboBox(node, node.individuals, vc, divId)); 	   	    
    		
        	simple.render(divId);
        }
	};

//-----------------------------------------------------------//
//the vc is the VocabCharacteristic we would like to add to.
//The fieldId = the div ID of the component that contains the gui component
//Will check to make sure that the fieldId doesn't already exisit in the properties and if it does update it
//if not create a new one.  This is to resolve the problem of duplicates and enforces that the VC that is created is 
//well formed. 

var propertiesPush = function(vc, fieldId, toAdd){
	
	//Make sure that the vc we've been given has a properties field
	if ((vc.properties === undefined) || (vc.properties === null) || (vc.properties.length === 0)){
		vc.properties = [];
		toAdd.fieldId = fieldId;
		vc.properties.push(toAdd);
		return;		
	}
	
	//Check if the field id already has a property set
	for(var i=0; i < vc.properties.length; i++){
	
		if (vc.properties[i].fieldId === undefined){
			console.log("The fieldId should be defined: " + vc.properties[i]);
			continue;
		}
		
		if (vc.properties[i].fieldId === fieldId) {
			toAdd.fieldId = fieldId;			
			vc.properties[i] = toAdd;
			return;					
		}						
	}
	
	//If we made it this far then the property hasn't been set already. 
	toAdd.fieldId = fieldId;
	vc.properties.push(toAdd);
	
};


//Template for the drop down box that contains all the individuals.  Needs a handler to know how to interpret and action
var createBaseComboBox = function(subject, individuals, vc, divId, comboHandler){
	
		     	var recordType = Ext.data.Record.create([
							{name:"label", type : "string" }, 
							{name:"uri", type: "string" } 
						]);
						
						//convert the arry of objects into an array of records
						var records = [];
						for(var i = 0, len = individuals.length; i < len; i++) {
							records.push(new recordType(individuals[i]));
						}
                    	
	                    var store = new Ext.data.Store({recordType : recordType} );
						store.add(records);
						
					    var combo = new Ext.form.ComboBox({
					    	fieldLabel: 'pick one',
					        store: store,
					        displayField:'label',
					        typeAhead: true,
					        mode: 'local',
					        triggerAction: 'all',
					        emptyText:'Select an individual',
					        selectOnFocus:true
					    });	
					    
					    combo.vocabId = divId;	
	
				  	 	combo.on('select', comboHandler);
					    
					    return combo;				
}

//subject
var createComboBox = function(subject, individuals, vc, divId){
			    
					    
					    var comboHandler = function(field,record,index){
					    	var vcChild = { termUri : subject.uri, object : {termUri : record.data.uri}};
					    	
					    	if (vc.properties === undefined){
    	                	 	vc.properties = [];    	                	 	
    	                	}
    	                	propertiesPush(vc, field.vocabId, vcChild);    	                
                        
    	                };
    	                
    	                return createBaseComboBox(subject, individuals, vc, divId, comboHandler);
    	                
    	                		
    	              
					    
	
};

//Handling the leaf node is a little different than handaling the other nodes. 
var createLeafComboBox = function(subject, individuals, vc, divId){
					    
					    var comboHandler = function(field,record,index){
					    	
					    	vc.classUri = subject.uri;
					    	vc.termUri = record.data.uri
					    								          						 					    	                        
    	                };
    	                							    
					    return createBaseComboBox(subject, individuals, vc, divId, comboHandler);			
};



//For creating the basic forms
//Creates the lookup field and a custom field for inputing information
var createBaseForm = function(subject,vc, divId, searchHandler, customHandler){
		
 var     recordType = Ext.data.Record.create([
					   {name:"id", type:"int"},
                       {name:"term", type:"string"},
                       {name:"uri", type:"string"},
               ]);


       ds = new Ext.data.Store(
               {
                       proxy:new Ext.data.DWRProxy(OntologyService.findTerm),
                       reader:new Ext.data.ListRangeReader({id:"id"}, recordType),
                       remoteSort:false,
                       sortInfo:{field:'id'}
               });

       var cm = new Ext.grid.ColumnModel([
                       {header: "term", width: 50, dataIndex:"term"},
                       {header: "uri",  width: 80, dataIndex:"uri"}
                       ]);
       cm.defaultSortable = true;	
    
     // Custom rendering Template
    var resultTpl = new Ext.Template(
        '<div class="search-item">',
            '<h3><span>{id}</span>{term}</h3>',
            '{uri}',
        '</div>'
    );
    
    var search = new Ext.form.ComboBox({
        store: ds,
        displayField:'title',
        fieldLabel: 'Lookup',
        typeAhead: false,
        loadingText: 'Searching...',
        width: 570,
        pageSize:10,
        tpl: resultTpl,
        hideTrigger:true,    
        onSelect: searchHandler,  
        getParams: function (q) {	//Need to overide this so that the query data makes it to the client side. Otherwise its not included. 
    		var p = [q]; 
   		 	return p;
		}
        
    });
    search.vocabId = divId;

	
	var custom =   new Ext.form.TextField({
            fieldLabel: 'custom',
            name: 'custom',
            width:150 });
    
    custom.vocabId = divId;
    custom.on('valid', customHandler);

	var simple = new Ext.form.Form({
		        labelWidth: 50, // label settings here cascade unless overridden
    });
					    
    simple.column(
    	{width:300},
        search
        );

	simple.column(
		{width:250},
		custom								
    );
    	
   
   return simple;		
	
	
	
	
};
//creates the Lookup textbox and the custom textbox.  The form is just used for alignment purposes. 
//The subject = the OntologyTerm that we are trying to create the vocabulary characteristic for
//the vc = the vocabulary charactersit we are trying to create. 
var createForm = function(subject, vc, divId){
	
	
		var searchHandler = function(record, index){
        	
        	 
            if(this.fireEvent('beforeselect', this, record, index) !== false){
        	    this.setValue(record.data.term);
            	this.collapse();
            	this.fireEvent('select', this, record, index);

				var newVC = { termUri : subject.uri, classUri: subject.uri,  object : {termUri : record.data.uri}};
               	if (vc.properties === undefined){
  	                	 	vc.properties = [];    	                	 	
               	}
               	propertiesPush(vc, search.vocabId, newVC);             
            }           	
    	                	
        };
        
        var customHandler = function(field){							
							var newVC = {termUri : subject.uri,  classUri: subject.uri, object : {value: field.getValue()}};
    	                	if (vc.properties === undefined){
    	                	 	vc.properties = [];    	                	 	
    	                	}
    	                	propertiesPush(vc, field.vocabId, newVC);             
    	                };
        
        
        return createBaseForm(subject, vc, divId, searchHandler, customHandler);
        
};


//creates the Lookup textbox and the custom textbox.  The form is just used for alignment purposes. 
//The subject = the OntologyTerm that we are trying to create the vocabulary characteristic for
//the vc = the vocabulary charactersit we are trying to create. 
var createLeafForm = function(subject, vc, divId){

 var searchHandler = function(record, index){
        	
        	 
            if(this.fireEvent('beforeselect', this, record, index) !== false){
        	    this.setValue(record.data.term);
            	this.collapse();
            	this.fireEvent('select', this, record, index);
				
				vc.classUri = subject.uri;
				vc.termUri = record.data.uri;
            }           	
    	                	
        }

	var customHandler = function(field){
	
							vc.classUri = subject.uri;
							vc.termUri =  field.getValue()														    	                	      
    	                };


	return createBaseForm(subject, vc, divId, searchHandler, customHandler); 
    	                		
};


var createAbstractForm = function(subject, vc, divId){

 var searchHandler = function(record, index){
        	
        	 
            if(this.fireEvent('beforeselect', this, record, index) !== false){
        	    this.setValue(record.data.term);
            	this.collapse();
            	this.fireEvent('select', this, record, index);

				//var vc = {termUri : subject.uri, object : {termUri : record.data.uri}};
				vc.classUri = subject.uri;
				vc.termUri =  record.data.uri;            	      
            }           	
    	                	
        }

	var customHandler = function(field){
							vc.properties = [];
							vc.classUri = subject.uri;
							vc.termUri = field.getValue();    							    	                	      
    	                };


	return createBaseForm(subject, vc, divId, searchHandler, customHandler); 
    	                		
};



//Do this for scope reasons. 
//By creating a function on the fly the scope of the restrictions will be fixed for the handler.
var createPrimitiveTypeHandler = function(restrictedOn, vc){

	return 	function(field) {
					     	
					     	var newVc = {termUri : restrictedOn.uri, classUri : restrictedOn.uri,  data : field.getValue(), type : restrictedOn.type};
					     	
					     	if (vc.properties === undefined){
    	                	 	vc.properties = [];    	                	 	
    	                	}
					     	
					     	propertiesPush(vc, field.vocabId,newVc);
			}
};

//
//For usablitly reasons we felt the  need to remove some ontology details. 
//
var specialCase = function(restrictedOn){
	
            	 if (restrictedOn.label == "has_ID") {
  					return true;
 	 				}		
}


//Loads a new ontology for display
var newOntologyLoader = function(div){
	
	var ontologyURL =   new Ext.form.TextField({
            fieldLabel: 'Ontology URL',
            name: 'OntologyURL',
            width:150 });
    
	var ontologyStartingURL =   new Ext.form.TextField({
            fieldLabel: 'Ontology Starting URL',
            name: 'OntologyStartingURL',
            width:150 });
    

var loadHandler = function(event){
	MgedOntologyService.loadNewOntology(ontologyURL.getValue(), ontologyStartingURL.getValue());	

};

var displayLoaded = function(loaded){
	
              dialog = new Ext.BasicDialog("center-div", { 
                        autoTabs:true,
                        width:200,
                        height:75,
                        shadow:true,
                        minWidth:200,
                        minHeight:50,
                        proxyDrag: true,
                        title: "Loaded = " + loaded                        
                });
                          
            dialog.show();
                  
};

var isLoadedHandler = function(event){
	MgedOntologyService.isOntologyLoaded(displayLoaded);	
}

var refreshHandler = function(event){
	   tree.render();
   		root.expand();
}



	var simpleURLForm = new Ext.form.Form({
		        labelWidth: 75, // label settings here cascade unless overridden
    });
					    
    simpleURLForm.column(
    	{width:300},
        ontologyURL
        );

	simpleURLForm.column(
		{width:250},
		ontologyStartingURL
    );
    
    simpleURLForm.addButton('Load', loadHandler);    	    
    simpleURLForm.addButton('isLoaded',isLoadedHandler);	
    simpleURLForm.addButton('refresh',refreshHandler);
   
    simpleURLForm.render("north-div");	
	
}


Ext.EventManager.onDocumentReady(Simple.init, Simple, true);

