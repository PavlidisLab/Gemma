/**
 * A Json reader that accepts a raw list from the server OR can act as a regular JsonReader. In the former case, the
 * 'success' and 'total' fields are filled in with reasonable values.
 * 
 * @param {Object}
 *            meta - id and totalRecords.
 * @param {Object}
 *            recordType
 * @see Ext.data.JsonReader
 * @version $Id$
 * @author Paul (revised from older versions)
 * 
 */
Ext.data.ListRangeReader = function(meta, recordType) {
	this.recordType = meta.record || recordType;

	Ext.data.ListRangeReader.superclass.constructor.call(this, meta, this.recordType);

};

Ext.extend(Ext.data.ListRangeReader, Ext.data.JsonReader, {

			readResponse : function(action, response) {
				var o = (response.responseText !== undefined) ? Ext.decode(response.responseText) : response;
				if (!o) {
					throw new Ext.data.JsonReader.Error('response');
				}
				var res;
				var root = this.getRoot(o); // this will return our plain list.

				if (action === Ext.data.Api.actions.create) {
					var def = Ext.isDefined(root);
					if (def && Ext.isEmpty(root)) {
						throw new Ext.data.JsonReader.Error('root-empty', this.meta.root);
					} else if (!def) {
						throw new Ext.data.JsonReader.Error('root-undefined-response', this.meta.root);
					}
				}

				// instantiate response object via the regular JsonReader method, slightly changed from native.
				res = new Ext.data.Response({
							action : action,
							success : this.getSuccess(o) == null ? true : this.getSuccess(o),
							data : (root) ? this.extractData(root, false) : [],
							message : this.getMessage(o) == null ? "" : this.getMessage(o),
							raw : o
						});

				if (Ext.isEmpty(res.success)) {
					throw new Ext.data.JsonReader.Error('successProperty-response', this.meta.successProperty);
				}
				return res;
			},

			readRecords : function(o) {
				if (!o) {
					return; // FIXME happens in Exp. Design editor on edit/add factor.
				}
				this.jsonData = o;
				if (o.metaData) {
					this.onMetaChange(o.metaData);
				}
				var s = this.meta, Record = this.recordType, f = Record.prototype.fields, fi = f.items, fl = f.length, v;

				// if no root, just use o.
				var root = this.getRoot(o) || o, c = root.length, totalRecords = c, success = true;
				if (s.totalProperty) {
					v = parseInt(this.getTotal(o), 10);
					if (!isNaN(v)) {
						totalRecords = v;
					}
				}

				if (s.successProperty) {
					v = this.getSuccess(o);
					if (v === false || v === 'false') {
						success = false;
					}
				}

				return {
					success : success,
					records : this.extractData(root, true),
					totalRecords : totalRecords
				};
			}

		});
