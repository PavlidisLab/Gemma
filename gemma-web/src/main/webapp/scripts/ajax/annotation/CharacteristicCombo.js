Ext.namespace("Gemma");
Gemma.CharacteristicCombo = Ext.extend(Ext.form.ComboBox, {

	loadingText : "Searching...",
	minChars : 2,
	selectOnFocus : true,
	listWidth : 350,
	taxonId : null,
	name : 'characteristicCombo',

	initComponent : function() {

		Ext.apply(this, {

					record : Ext.data.Record.create([{
								name : "id",
								type : "int"
							}, {
								name : "value",
								type : "string"
							}, {
								name : "valueUri",
								type : "string"
							}, {
								name : "categoryUri",
								type : "string"
							}, {
								name : "category",
								type : "string"
							}, {
								name : "hover",
								mapping : "this",
								convert : this.getHover.createDelegate(this)
							}, {
								name : "style",
								mapping : "this",
								convert : this.getStyle.createDelegate(this)
							}])
				});

		Ext.apply(this, {
					store : new Ext.data.Store({
								proxy : new Ext.data.DWRProxy(OntologyService.findExactTerm),
								reader : new Ext.data.ListRangeReader({
											id : "id"
										}, this.record),
								remoteSort : true
							})
				});

		Gemma.CharacteristicCombo.superclass.initComponent.call(this);

		this.tpl = new Ext.XTemplate('<tpl for="."><div ext:qtip="{hover}"  style="font-size:11px" class="x-combo-list-item {style}">{value}</div></tpl>');
		this.tpl.compile();

		this.characteristic = {
			category : null,
			categoryUri : null,
			value : null,
			valueUri : null
		};

		this.on("select", function(combo, record, index) {
					this.characteristic.value = record.data.value;
					this.characteristic.valueUri = record.data.valueUri;
					/*
					 * The addition of '.' is a complete hack to workaround an extjs limitation. It's to make sure extjs
					 * knows we want it to detect a change. See bug 1811
					 */
					combo.setValue(record.data.value + ".");
				});

	},

	getParams : function(query) {
		return [query, this.characteristic.categoryUri, this.taxonId];
	},

	getCharacteristic : function() {

		/*
		 * check to see if the user has typed anything in the combo box (rather than selecting something); if they have,
		 * remove the URI from the characteristic and update its value, so we end up with a plain text. See note about hack '.' above.
		 */
		if (this.getValue() != this.characteristic.value + ".") {
			this.characteristic.value = this.getValue();
			this.characteristic.valueUri = null;
		}
		/*
		 * if we don't have a valueUri or categoryUri set, don't return URI fields or a VocabCharacteristic will be
		 * created when we only want a Characteristic...
		 */
		return (this.characteristic.valueUri !== null || this.characteristic.categoryUri !== null)
				? this.characteristic
				: {
					category : this.characteristic.category,
					value : this.characteristic.value
				};
	},

	setCharacteristic : function(value, valueUri, category, categoryUri) {
		this.characteristic.value = value;
		this.characteristic.valueUri = valueUri;
		this.characteristic.category = category;
		this.characteristic.categoryUri = categoryUri;
		this.setValue(value);
	},

	setCategory : function(category, categoryUri) {
		this.characteristic.category = category;
		this.characteristic.categoryUri = categoryUri;
	},

	/*
	 * if the characteristic has a URI, use that as the description; if not, strip the " -USED- " string (added in
	 * OntologyService) if present.
	 */
	getHover : function(record) {
		if (record.valueUri) {
			return record.valueUri;
		} else {
			return (record.description.substring(0, 8) == " -USED- ")
					? record.description.substring(8)
					: record.description;
		}
	},
	getStyle : function(record) {
		if (record.description && record.description.substring(0, 8) == " -USED- ") {
			return record.valueUri ? "usedWithUri" : "usedNoUri";
		} else {
			return record.valueUri ? "unusedWithUri" : "unusedNoUri";
		}
	}

});
