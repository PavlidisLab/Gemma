

//member variables
var vocabC = {};
var eeid;


var createMgedComboBox = function(terms){				
				
			
	var     recordType = Ext.data.Record.create([
					   {name:"id", type:"int"},
                       {name:"term", type:"string"},
                       {name:"uri", type:"string"},
               ]);
									
                    	
                var ds = new Ext.data.Store(
               {
                       proxy:new Ext.data.DWRProxy(MgedOntologyService.getBioMaterialTerms),
                       reader:new Ext.data.ListRangeReader({id:"id"}, recordType),
                       remoteSort:false,
                       sortInfo:{field:'id'}
               });
               
				ds.load();
						
					    var combo = new Ext.form.ComboBox({					    	
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
					    	
					    	vocabC.classUri = record.data.uri;					    	
					    								      						 					    	                        
    	                };
    	                	
	
				  	 	combo.on('select', comboHandler);
					    
					    return combo;	
	
}

var createSearchComponent = function(){
	
	
	 var searchHandler = function(record, index){
        	
        	 
            if(this.fireEvent('beforeselect', this, record, index) !== false){
        	    this.setValue(record.data.term);
            	this.collapse();
            	this.fireEvent('select', this, record, index);
							
				vocabC.termUri = record.data.uri;
				vocabC.value = record.data.term;
            }           	
    	                	
        }
        
        
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
        width: 270,
        pageSize:10,
        tpl: resultTpl,
        hideTrigger:true,    
        onSelect: searchHandler,  
        getParams: function (q) {	//Need to overide this so that the query data makes it to the client side. Otherwise its not included. 
    		var p = [q]; 
   		 	return p;
		}
        
    });
	
	return search;
	
}

var saveHandler = function(){
	
	console.log(dwr.util.toDescriptiveString(vocabC,10))
	OntologyService.saveExpressionExperimentStatment(vocabC, [eeid]);
	
	
}



Ext.onReady(function() {
		
	eeid = dwr.util.getValue("auditableId"); // turns out to be the EE id
	
	// this will be the case if we're not admins.
	if (!id) {
		return;
	}

	
	//the mged combo box 
	var mgedComboBox = createMgedComboBox();   
	
	//the lookup combobox
	var lookup = createSearchComponent();			

	
	var simpleForm = new Ext.form.Form({
		labelWidth: 50, // label settings here cascade unless overridden
    });
								
    simpleForm.column({width:220, labelWidth: 30}, mgedComboBox );

	simpleForm.column( {width:360},lookup );
    
    var saveColumn = simpleForm.column({width: 50});
    //The save button
	var save = new 	Ext.Button	(saveColumn.getEl(),{text: 'save',
								  tooltip: 'Saves the desired annotation',
								  minWidth: 50,
								  handler: saveHandler
								});

	simpleForm.column(save);
								   
    simpleForm.render("eeAnnotator");	
	
});