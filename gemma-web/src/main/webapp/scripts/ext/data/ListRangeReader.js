/**
 * 
 * @param {Object} meta - id and totalRecords.
 * @param {Object} recordType
 */
Ext.data.ListRangeReader = function (meta, recordType) {
	Ext.data.ListRangeReader.superclass.constructor.call(this, meta, recordType);
	this.recordType = recordType;
};

/**
 * 
 *  
 */
Ext.extend(Ext.data.ListRangeReader, Ext.data.DataReader, {

 	// private method
	getJsonAccessor:function () {
		var re = /[\[\.]/;
		return function (expr) {
			try {
				if ( expr == "this" )
					return function (obj) { return obj; };
				else if ( re.test(expr) )
					return new Function("obj", "return obj." + expr);
				else
					return function (obj) { return obj[expr]; };
			}
			catch (e) {
			}
			return Ext.emptyFn;
		};
	}(), 
	
	readRecords:function (o) {
		return this.read(o);
	},
	
	read:function (o) {
		var recordType = this.recordType, fields = recordType.prototype.fields;
	
		//Generate extraction functions for the totalProperty, the root, the id, and for each field
	 	if (!this.ef) {
			if (this.meta.totalProperty) {
				this.getTotal = this.getJsonAccessor(this.meta.totalProperty);
			}
			if (this.meta.successProperty) {
				this.getSuccess = this.getJsonAccessor(this.meta.successProperty);
			}
			if (this.meta.id) {
				var g = this.getJsonAccessor(this.meta.id);
				this.getId = function (rec) {
					var r = g(rec);
					return (r === undefined || r === "") ? null : r;
				};
			} else {
				this.getId = Ext.id; // random id generated.
			}
			this.ef = [];
			for (var i = 0; i < fields.length; i++) {
				f = fields.items[i];
				var map = (f.mapping !== undefined && f.mapping !== null) ? f.mapping : f.name;
				this.ef[i] = this.getJsonAccessor(map);
			}
		}
		 
		var records = [];
		var root;
		
		if (o && (o !== null) && (o.data !== undefined)) {
			root = o.data;
		} else {
			if (o === null){
				o = [];	
			}
			root = o;
		}
		
		var c = root.length;
		var totalRecords = c;
		var success = true;
		
		if (this.meta.totalProperty) {
			var v = parseInt(this.getTotal(o), 10);
			if (!isNaN(v)) {
				totalRecords = v;
			}
		}
		
		if (this.meta.successProperty) {
			var v = this.getSuccess(o);
			if (v === false || v === "false") {
				success = false;
			}
		}
		
		for (var i = 0; i < c; i++) {
			var n = root[i];
			var values = {};
			var id = this.getId(n);
			for (var j = 0; j < fields.length; j++) {
		 		var f = fields.items[j];
				var v = this.ef[j](n);
				if (f.convert) {
					values[f.name] = f.convert((v !== undefined) ? v : f.defaultValue);
				} else {
					values[f.name] = "Could not convert";
				}
			}
			var record = new recordType(values, id);
			records[i] = record;
		}
	
		return {success:success, records:records, totalRecords:totalRecords};
	}
});
