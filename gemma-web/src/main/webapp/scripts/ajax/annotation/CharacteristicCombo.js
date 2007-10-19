Ext.namespace('Ext.Gemma');

/* Ext.Gemma.CharacteristicCombo constructor...
 */
Ext.Gemma.CharacteristicCombo = function (config) {

	Ext.Gemma.CharacteristicCombo.superclass.constructor.call( this, config );
	
	if ( Ext.Gemma.CharacteristicCombo.record == undefined ) {
		/* if the characteristic has a URI, use that as the description;
		 * if not, strip the " -USED- " string (added in OntologyService) if present.
		 */
		var getDescription = function (record) {
			if ( record.valueUri )
				return record.valueUri;
			else
				return ( record.description.substring(0, 8) == " -USED- " ) ?
					record.description.substring(8) : record.description;
		};
		/* return a CSS class depending on whether or not the characteristic has a URI
		 * and whether or not it exists in the database.
		 */
		var getStyle = function (record) {
			if ( record.description.substring(0, 8) == " -USED- ")
				return record.valueUri ? "usedWithUri" : "usedNoUri";
			else
				return record.valueUri ? "unusedWithUri" : "unusedNoUri";
		};
		Ext.Gemma.CharacteristicCombo.record = Ext.data.Record.create( [
			{ name:"id", type:"int" },
			{ name:"value", type:"string" },
			{ name:"valueUri", type:"string" },
			{ name:"categoryUri",type:"string" },
			{ name:"category", type:"string" },
			{ name:"description", mapping:"this", convert:getDescription },
			{ name:"style", mapping:"this", convert:getStyle }
		] );
	}
	this.store = config.store || new Ext.data.Store( {
		proxy : new Ext.data.DWRProxy( OntologyService.findExactTerm ),
		reader : new Ext.data.ListRangeReader( {id:"id"}, Ext.Gemma.CharacteristicCombo.record ),
		remoteSort : true
	} );
	
	if ( Ext.Gemma.CharacteristicCombo.template == undefined ) {
		Ext.Gemma.CharacteristicCombo.template = new Ext.Template(
			'<div class="search-item">',
				'<div class="{style}" title="{description}">{value}</div>',
			'</div>'
		);
	}
	this.tpl = config.tpl || Ext.Gemma.CharacteristicCombo.template;
	
	this.getParams = function (query) {
		return [ query, this.characteristic.categoryUri ];
	};
	
	this.hideTrigger = config.hideTrigger || false;
	this.loadingText = config.loadingText || "Searching...";
	this.minChars = config.minChars || 2;
	this.pageSize = config.pageSize || 0;
	this.typeAhead = config.typeAhead || false;
	
	this.characteristic = {
		category : null,
		categoryUri : null,
		value : null,
		valueUri : null
	}
	
	this.on( "select", function ( combo, record, index ) {
		this.characteristic.value = record.data.value;
		this.characteristic.valueUri = record.data.valueUri;
		combo.setValue( record.data.value );
	} );
}

/* other public methods...
 */
Ext.extend( Ext.Gemma.CharacteristicCombo, Ext.form.ComboBox, {

		getCharacteristic : function () {
			/* check to see if the user has typed anything in the combo box;
			 * if they have, remove the URI from the characteristic and update
			 * its value...
			 */
			if ( this.getValue() != this.characteristic.value ) {
				this.characteristic.value = this.getValue();
				this.characteristic.valueUri = null;
			}
			/* if we don't have a valueURI set, don't return URI fields or
			 * a VocabCharacteristic will be created when we only want a
			 * Characteristic...
			 */
			return this.characteristic.valueUri != null ? this.characteristic : {
				category : this.characteristic.category,
				value : this.characteristic.value
			};
		},
		
		setCategory : function ( category, categoryUri ) {
			this.characteristic.category = category;
			this.characteristic.categoryUri = categoryUri;
		}
		
} );