Ext.namespace("Gemma");

/**
 * Provide a drop-down menu populated with array designs.
 * 
 * @class Gemma.ArrayDesignCombo
 * @extends Ext.form.ComboBox
 */
Gemma.ArrayDesignCombo = Ext.extend(Ext.form.ComboBox, {

	displayField : 'name',
	valueField : 'id',
	editable : false,
	loadingText : "Loading ...",
	listWidth : 450,
	forceSelection : true,
	mode : 'remote',
	triggerAction : 'all',
	emptyText : 'Select an array design',

	record : Ext.data.Record.create([{
				name : "id",
				type : "int"
			}, {
				name : "name",
				type : "string"
			}, {
				name : "description",
				type : "string"
			}, {
				name : "taxon"
			}, {
				name : "shortName",
				type : "string"
			}]),

	setState : function(v) {
		if (this.ready) {
			Gemma.ArrayDesignCombo.superclass.setValue.call(this, v);
		} else {
			this.state = v;
		}
	},

	restoreState : function() {
		if (this.state) {
			Gemma.ArrayDesignCombo.superclass.setValue.call(this, v);
			delete this.state;
		}
		this.setValue(this.state);
		delete this.state;
		this.ready = true;
		this.fireEvent('ready');
	},

	initComponent : function() {

		var templ = new Ext.XTemplate('<tpl for="."><div ext:qtip="{description}" class="x-combo-list-item">{shortName} - {name}</div></tpl>');

		Ext.apply(this, {
					store : new Ext.data.Store({
								sortInfo : {
									field : 'name',
									direction : 'ASC'
								},
								proxy : new Ext.data.DWRProxy(ArrayDesignController.getArrayDesigns, {
											baseParams : [[], true, false]
										}),
								reader : new Ext.data.ListRangeReader({
											id : "id"
										}, this.record),
								remoteSort : false
							}),
					tpl : templ
				});

		Gemma.ArrayDesignCombo.superclass.initComponent.call(this);

		this.store.load({
					params : [],
					scope : this,
					add : false
				});

		this.doQuery();

		this.addEvents('arrayDesignchanged', 'ready');
	},

	setValue : function(v) {
		var changed = false;
		if (this.getValue() != v) {
			changed = true;
		}

		// if setting to a filtered value, reset the filter.
		if (changed && this.store.isFiltered()) {
			this.store.clearFilter();
		}

		Gemma.ArrayDesignCombo.superclass.setValue.call(this, v);

		if (changed) {
			this.fireEvent('arrayDesignchanged', this.getArrayDesign());
		}
	},

	getArrayDesign : function() {
		var ArrayDesign = this.store.getById(this.getValue());
		return ArrayDesign;
	},

	taxonChanged : function(taxon) {
		if (this.getArrayDesign() && this.getArrayDesign().taxon && this.getArrayDesign().taxon.id != taxon.id) {
			this.reset();
		}
		this.applyFilter(taxon);
	},

	applyFilter : function(taxon) {

		if (taxon === undefined)
			return;

		this.store.filterBy(function(record, id) {
					if (!record.data.taxon) {
						return false;
					} else if (record.data.taxon == taxon.commonName) {
						return true;
					}
				});
	},

	/**
	 * Given the id (primary key in Gemma) of the ArrayDesign, select it.
	 * 
	 * @param {}
	 *            args
	 */
	selectById : function(args) {
		this.store.un("add", this.selectById);
		if (args.id) {
			this.selectByValue(args.id, true);
		} else {
			this.selectByValue(args, true);
		}
	},

	clearCustom : function() {
		var rec = this.store.getById(-1);
		if (rec) {
			this.store.remove(rec);
		}
	}

});

Ext.reg('ArrayDesigncombo', Gemma.ArrayDesignCombo);