Ext.onReady(function() {
	indexForm();
});


var indexForm = function() {
	
    Ext.form.Field.prototype.msgTarget = 'side';
    var simple = new Ext.BasicForm('index-form',{ 
    });

	geneCheckBox =     new Ext.form.Checkbox({
            fieldLabel: 'Index genes',
            name: 'gene'
        });
    simple.add(geneCheckBox);     
    
	probeCheckBox =     new Ext.form.Checkbox({
            fieldLabel: 'Index probes',
            name: 'probe'
        });
    simple.add( probeCheckBox   );
	
	adCheckBox =     new Ext.form.Checkbox({
            fieldLabel: 'Index ADs',
            name: 'ad'
        });
    simple.add( adCheckBox   );
    
	bsCheckBox =     new Ext.form.Checkbox({
            fieldLabel: 'Index Biosequences',
            name: 'bs'
        });
	simple.add( bsCheckBox    );
	    
	eeCheckBox =     new Ext.form.Checkbox({
            fieldLabel: 'Index EEs',
            name: 'ee'
        });
    simple.add(  eeCheckBox  );
        
	bibRefCheckBox =     new Ext.form.Checkbox({
            fieldLabel: 'Index Bibliographic Refs',
            name: 'bibRef'
        });		
    simple.add( bibRefCheckBox );
    
    
    simple.add(new Ext.Button({text: "index", handler: function(event) {index(event);} }));
    simple.render('index-form');
};


function handleSuccess(data) {
	Ext.DomHelper.overwrite("messages", {tag : 'div', html:data });   
};

function handleFailure(data, e) {
	Ext.DomHelper.overwrite("taskId", "");
	Ext.DomHelper.overwrite("messages", {tag : 'img', src:'/Gemma/images/icons/warning.png' });  
	Ext.DomHelper.append("messages", {tag : 'span', html : "&nbsp;There was an error: " + data });  
};

function index(event) {

	var dh = Ext.DomHelper;	
	var callParams = [];
	

	var commandObj = {indexArray: adCheckBox.valueOf() , indexEE: eeCheckBox.valueOf(), indexProbe : probeCheckBox.valueOf(), indexBibliographic: bibRefCheckBox.valueOf(), indexGene: geneCheckBox.valueOf(), indexBioSequence: bsCheckBox.valueOf()};

	callParams.push(commandObj);
	
	var delegate = handleIndexSuccess.createDelegate(this, [], true);
	var errorHandler = handleFailure.createDelegate(this, [], true);
	
	callParams.push({callback : delegate, errorHandler : errorHandler  });
	
	// this should return quickly, with the task id.
	Ext.DomHelper.overwrite("messages", {tag : 'img', src:'/Gemma/images/default/tree/loading.gif' });  
	Ext.DomHelper.append("messages", "&nbsp;Submitting job...");  
	CustomCompassIndexController.run.apply(this, callParams);
	
};


function handleIndexSuccess(data) {
	try {
		taskId = data;
		Ext.DomHelper.overwrite("messages", "");  
		Ext.DomHelper.overwrite("taskId", "<input type = 'hidden' name='taskId' id='taskId' value= '" + taskId + "'/> ");
		var p = new progressbar();
	 	p.createIndeterminateProgressBar();
		p.on('fail', handleFailure);
		p.on('cancel', reset);
	 	p.startProgress();
	}
	catch (e) {
		handleFailure(data, e);
		return;
	}
};