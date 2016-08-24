/*
 * Note: this requires CSS classes. x-status-right, x-status-text, x-status-busy
 */

Ext.ux.StatusBar = Ext.extend(Ext.Toolbar, {

			cls : 'x-statusbar',

			busyIconCls : 'x-status-busy',

			busyText : 'Loading...',

			autoClear : 5000,

			// private
			activeThreadId : 0,

			// private
			initComponent : function() {
				if (this.statusAlign == 'right') {
					this.cls += ' x-status-right';
				}
				Ext.ux.StatusBar.superclass.initComponent.call(this);
			},

			// private
			afterRender : function() {
				Ext.ux.StatusBar.superclass.afterRender.call(this);
			},

			setStatus : function(o) {
				o = o || {};

				if (typeof o == 'string') {
					o = {
						text : o
					};
				}
				if (o.text !== undefined) {
					this.setText(o.text);
				}
				if (o.iconCls !== undefined) {
					this.setIcon(o.iconCls);
				}

				if (o.clear) {
					var c = o.clear, wait = this.autoClear, defaults = {
						useDefaults : true,
						anim : true
					};

					if (typeof c == 'object') {
						c = Ext.applyIf(c, defaults);
						if (c.wait) {
							wait = c.wait;
						}
					} else if (typeof c == 'number') {
						wait = c;
						c = defaults;
					} else if (typeof c == 'boolean') {
						c = defaults;
					}

					c.threadId = this.activeThreadId;
					this.clearStatus.defer(wait, this, [c]);
				}
				return this;
			},

			clearStatus : function(o) {
				o = o || {};

				if (o.threadId && o.threadId !== this.activeThreadId) {
					// this means the current call was made internally, but a newer
					// thread has set a message since this call was deferred. Since
					// we don't want to overwrite a newer message just ignore.
					return this;
				}

				var text = o.useDefaults ? this.defaultText : '', iconCls = o.useDefaults ? (this.defaultIconCls
						? this.defaultIconCls
						: '') : '';

				if (o.anim) {
					this.statusEl.fadeOut({
								remove : false,
								useDisplay : true,
								scope : this,
								callback : function() {
									this.setStatus({
												text : text,
												iconCls : iconCls
											});
									this.statusEl.show();
								}
							});
				} else {
					// hide/show the el to avoid jumpy text or icon
					this.statusEl.hide();
					this.setStatus({
								text : text,
								iconCls : iconCls
							});
					this.statusEl.show();
				}
				return this;
			},

			setText : function(text) {
				this.activeThreadId++;
				this.text = text || '';
				if (this.rendered) {
					this.statusEl.update(this.text);
				}
				return this;
			},

			getText : function() {
				return this.text;
			},

			setIcon : function(cls) {
				this.activeThreadId++;
				cls = cls || '';

				if (this.rendered) {
					if (this.currIconCls) {
						this.statusEl.removeClass(this.currIconCls);
						this.currIconCls = null;
					}
					if (cls.length > 0) {
						this.statusEl.addClass(cls);
						this.currIconCls = cls;
					}
				} else {
					this.currIconCls = cls;
				}
				return this;
			},

			showBusy : function(o) {
				if (typeof o == 'string') {
					o = {
						text : o
					};
				}
				o = Ext.applyIf(o || {}, {
							text : this.busyText,
							iconCls : this.busyIconCls
						});
				return this.setStatus(o);
			},

			nextBlock : function() {
				var td = document.createElement("td");
				this.tr.appendChild(td);
				return td;
			},

			onRender : function(ct, position) {
				if (!this.el) {
					if (!this.autoCreate) {
						this.autoCreate = {
							cls : this.toolbarCls + ' x-small-editor'
						}
					}
					this.el = ct.createChild(Ext.apply({
										id : this.id
									}, this.autoCreate), position);
				}

			},

			onLayout : function(ct, target) {
				Ext.ux.StatusBar.superclass.onLayout.call(this, ct, target);

				if (!this.tr) {
					this.tr = this.getLayout().leftTr

					var right = this.statusAlign == 'right', td = Ext.get(this.nextBlock());
					if (right) {
						this.getLayout().rightTr.appendChild(td.dom);
					} else {
						td.insertBefore(this.tr.firstChild);
					}

					this.statusEl = td.createChild({
								cls : 'x-status-text ' + (this.iconCls || this.defaultIconCls || ''),
								html : this.text || this.defaultText || ''
							});
					this.statusEl.unselectable();

					this.spacerEl = td.insertSibling({
								tag : 'td',
								style : 'width:100%',
								cn : [{
											cls : 'ytb-spacer'
										}]
							}, right ? 'before' : 'after');
				}

			}
		});
Ext.reg('statusbar', Ext.ux.StatusBar);