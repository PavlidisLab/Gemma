Ext.Gemma.GeneImportPanel = function ( config ) {
	
	if (!this.textBox) {
		this.textBox = new Ext.form.TextArea({
			fieldLabel:"Paste in gene symbols, one per line" 
		});
	}
	
	var superConfig=  { 
		title : "Import multiple genes (one symbol per row)",
		modal : true,
		layout : 'fit',
		autoHeight : true, 
		width : 500,
		closeAction:'hide',
		easing : 3, 
		items: [  
			this.textBox ],

        buttons: [{ 
        	text: 'Cancel',
        	handler: function(){ 
            	this.hide();  
        	},
        	scope : this
       	},{ 
        	text: 'OK',
        	handler: config.handler,
        	scope : config.scope
       	},{ 
        	text: 'Clear',
        	handler: function(){ 
            	this.textBox.setValue("");  
        	},
        	scope : this
       	}]
	};
	
	for ( property in config ) {
		superConfig[property] = config[property];
	}
	
	Ext.Gemma.GeneImportPanel.superclass.constructor.call( this, superConfig );
};

Ext.extend( Ext.Gemma.GeneImportPanel, Ext.Window, {
	
	getGeneNames : function() {
		return this.textBox.getValue();
	}
	
});