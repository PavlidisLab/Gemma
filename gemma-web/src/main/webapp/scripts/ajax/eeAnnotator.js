

//member variables
var vocabC = {};
var eeid;
var deleteButton;
var saveButton;

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
								saveButton.disable();
							else
								saveButton.enable();
					    								      						 					    	                        
    	                };
    	                	
	
				  	 	combo.on('select', comboHandler);
					    
					    return combo;	
	
}

var createSearchComponent = function(){
	
	
	 var searchHandler = function(record, index){
        	
        	 
            if(this.fireEvent('beforeselect', this, record, index) !== false){
        	    this.setValue(record.data.value);
            	this.collapse();
            	this.fireEvent('select', this, record, index);
							
				vocabC.valueUri = record.data.valueUri;
				vocabC.value = record.data.value;
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
        
		var recordType = Ext.data.Record.create([
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
    		
    		vocabC.value = q;	//if the user doesn't select a provided ontolgy term this will set it to be the free text. 
    		if (!vocabC.categoryUri)
    			vocabC.categoryUri="{}";
    			
   		 	p.push(vocabC.categoryUri);
   		 		
   		 	return p;
		}
        
    });
	
	return search;
	
}

var saveHandler = function(){

	//Use for debugging. 
	//console.log(dwr.util.toDescriptiveString(vocabC,10))
	//Make a copy, then send it over the wire. 
	//If there is no valueUri then it is plain text and we don't want dwr to instantiate a
	//VocabCharacteritic but a Characteritic. 
	var newVocabC = {};
	
	newVocabC.value = vocabC.value;
	newVocabC.category = vocabC.category;
	
	if (vocabC.valueUri){
		newVocabC.categoryUri = vocabC.categoryUri ;
		newVocabC.valueUri = vocabC.valueUri;
	}
	
	OntologyService.saveExpressionExperimentStatement(newVocabC, [eeid], refreshEEAnnotations);
	
}

var deleteHandler = function(){
		
	OntologyService.removeExpressionExperimentStatement(characteristicIdList, [eeid], refreshEEAnnotations)		
	
}



Ext.onReady(function() {
		
	eeid = dwr.util.getValue("auditableId"); // turns out to be the EE id
	
	// this will be the case if we're not admins.
	if (!eeid) {
		return;
	}

	
	//the mged combo box 
	var mgedComboBox = createMgedComboBox();   
	
	//the lookup combobox
	var lookup = createSearchComponent();			

	
	var simpleTB = new Ext.Toolbar("eeAnnotator");
	
	simpleTB.addField(mgedComboBox);
	simpleTB.addSpacer();
	simpleTB.addField(lookup);
	simpleTB.addSpacer();
	saveButton = simpleTB.addButton({text: 'save',
						tooltip: 'Saves the desired annotation',								  
						handler: saveHandler,
						disabled: true
					});
	simpleTB.addSeparator();
	deleteButton = simpleTB.addButton({text: 'delete',
						tooltip: 'Removes the desired annotation',								
						handler: deleteHandler,
						disabled: true						
					});
	
});