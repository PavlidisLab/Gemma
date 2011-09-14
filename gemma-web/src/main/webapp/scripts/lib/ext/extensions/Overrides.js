
// Override textArea to allow control of word wrapping
// just adds a wordWrap config field to textArea
// from here: http://www.sencha.com/forum/showthread.php?52122-preventing-word-wrap-in-textarea
// needed for download window of diff ex viz
Ext.override(Ext.form.TextArea, {
    initComponent: Ext.form.TextArea.prototype.initComponent.createSequence(function(){
        Ext.applyIf(this, {
            wordWrap: true
        });
    }),
    
    onRender: Ext.form.TextArea.prototype.onRender.createSequence(function(ct, position){
        this.el.setOverflow('auto');
        if (this.wordWrap === false) {
            if (!Ext.isIE) {
                this.el.set({
                    wrap: 'off'
                });
            }
            else {
                this.el.dom.wrap = 'off';
            }
        }
        if (this.preventScrollbars === true) {
            this.el.setStyle('overflow', 'hidden');
        }
    })
});


/**
 * Add methods to Ext.data.Store to handle multiple independent filters
 * should be able to add/remove and activate/deactivate filters by name
 * 
 * filterObjects passed to store will be js objects with 3 fields: 
 * fn : the filtering function. Takes the row's record as param and returns 'true' if the row should be shown, false otherwise
 * name : unique name of the filter so that it can be removed or deactivated
 * active : boolean indicating whether the filter should be applied or not
 * 
 * (I'm not actually overriding anything but Ext.apply doesn't work)
 */
Ext.override(Ext.data.Store, {
    multiFilters:[],
	/**
	 * add a filter to the multi-filter, must have 3 fields: fn, name, active
	 * returns true if successfully added, false if fields are missing
	 */
	addMultiFilter : function(filterObj){
		if(typeof(filterObj) !== undefined &&
			typeof(filterObj.fn) !== undefined && 
			typeof(filterObj.name) !== undefined && 
			typeof(filterObj.active) !== undefined){
			this.multiFilters.push(filterObj);
			return true;
		}
		return false;
	},
	
	removeMultiFilter : function(filterName){
		var index = this.getFilterObjIndexByName(filterName);
		if (index !== null && index > -1) {
			this.multiFilters.splice(index, 1);
		}
	},
	
	activateMultiFilter : function(filterName){
		var fo = this.getFilterObjByName(filterName);
		if(fo !== null){
			fo.active = true;
		}
	},
	
	deactivateMultiFilter : function(filterName){
		var fo = this.getFilterObjByName(filterName);
		if(fo !== null){
			fo.active = false;
		}
	},
	/**
	 * go through each filter, if any filter wants the row hidden, hide it
	 * otherwise show it
	 */
	applyMultiFilters: function(){
		this.filterBy(function(record, id){
			var j;
			for (j = 0; j < this.multiFilters.length; j++) {
				if (this.multiFilters[j].active && !this.multiFilters[j].fn(record)) {
					return false;
				}
			}
			return true;
		}, this);
		
	},
		
	clearMultiFilters : function (){
		this.multiFilters = [];
		
	},
	// could do this more efficiently with a map, but the array will probably always be small ( < 5 elements)
	getFilterObjByName : function (name){
		var i;
		for(i=0;i<this.multiFilters.length;i++){
			if(this.multiFilters[i].name && this.multiFilters[i].name === name){
				return this.multiFilters[i];
			}
		}
		return null;
	},
	// could do this more efficiently with a map, but the array will probably always be small ( < 5 elements)
	getFilterObjIndexByName : function (name){
		var i;
		for(i=0;i<this.multiFilters.length;i++){
			if(this.multiFilters[i].name && this.multiFilters[i].name === name){
				return i;
			}
		}
		return null;
	}
	
});


/* Override ColumnModel to allow adding and removing columns
 * http://www.sencha.com/forum/showthread.php?53009-Adding-removing-fields-and-columns
 * 
 * usage example: 
 * 
  var grid = new Ext.grid.GridPanel({
	store: new Ext.data.SimpleStore({
		fields: ['A', 'B'],
		data: [['ABC', 'DEF'], ['GHI', 'JKL']]
	}),
	columns: [
		{header: 'A', dataIndex: 'A'},
		{header: 'B', dataIndex: 'B'}
	]});
	new Ext.Viewport({
		layout: 'fit',
		items: grid
	});
	grid.addColumn('C');
	grid.addColumn({name: 'D', defaultValue: 'D'}, {header: 'D', dataIndex: 'D'});
	grid.removeColumn('B');
 */ 
Ext.override(Ext.data.Store,{
	addField: function(field){
		field = new Ext.data.Field(field);
		this.recordType.prototype.fields.replace(field);
		if(typeof field.defaultValue != 'undefined'){
			this.each(function(r){
				if(typeof r.data[field.name] == 'undefined'){
					r.data[field.name] = field.defaultValue;
				}
			});
		}
	},
	removeField: function(name){
		this.recordType.prototype.fields.removeKey(name);
		this.each(function(r){
			delete r.data[name];
			if(r.modified){
				delete r.modified[name];
			}
		});
	}
});
Ext.override(Ext.grid.ColumnModel,{
	addColumn: function(column, colIndex){
		if(typeof column == 'string'){
			column = {header: column, dataIndex: column};
		}
		var config = this.config;
		this.config = [];
		if(typeof colIndex == 'number'){
			config.splice(colIndex, 0, column);
		}else{
			colIndex = config.push(column);
		}
		this.setConfig(config);
		return colIndex;
	},
	removeColumn: function(colIndex){
		var config = this.config;
		this.config = [config[colIndex]];
		config.splice(colIndex, 1);
		this.setConfig(config);
	}
});
Ext.override(Ext.grid.GridPanel,{
	addColumn: function(field, column, colIndex){
		if(!column){
			if(field.dataIndex){
				column = field;
				field = field.dataIndex;
			} else{
				column = field.name || field;
			}
		}
		this.store.addField(field);
		return this.colModel.addColumn(column, colIndex);
	},
	removeColumn: function(name, colIndex){
		this.store.removeField(name);
		if(typeof colIndex != 'number'){
			colIndex = this.colModel.findColumnIndex(name);
		}
		if(colIndex >= 0){
			this.colModel.removeColumn(colIndex);
		}
	}
});