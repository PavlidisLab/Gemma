Ext.ns('Ext.ux');
Ext.ux.RadioFieldset = Ext.extend(Ext.form.FieldSet, {
    // private
    onRender : function(ct, position){
        if(!this.el){
            this.el = document.createElement('fieldset');
            this.el.id = this.id;
            if (this.title || this.header || this.radioToggle) {
                this.el.appendChild(document.createElement('legend')).className = this.baseCls + '-header';
            }
        }

        Ext.form.FieldSet.superclass.onRender.call(this, ct, position);

        if(this.radioToggle){
            var o = typeof this.radioToggle == 'object' ? this.radioToggle : {
				tag: 'input',
				type: 'radio',
				name: this.radioName || this.id + '-radio',
				id: this.radioId || this.id +'radioId'
				// this will not work, always causes it to be disabled
				// disabled: this.disableRadio
			};
			if(this.disableRadio){
				o.disabled = true;
			}
            this.radio = this.header.insertFirst(o);
            this.radio.dom.checked = !this.collapsed;
            this.mon(this.radio, 'click', this.onCheckClick, this);
        }
    },
	
    // private
    onCollapse : function(doAnim, animArg){
        if(this.radio){
            this.radio.dom.checked = false;
        }
        Ext.form.FieldSet.superclass.onCollapse.call(this, doAnim, animArg);

    },

    // private
    onExpand : function(doAnim, animArg){
        if(this.radio){
            this.radio.dom.checked = true;
        }
        Ext.form.FieldSet.superclass.onExpand.call(this, doAnim, animArg);
    },
	    
/**
     * This function is called by the fieldset's radio when it is toggled (only applies when
     * radioToggle = true).  This method should never be called externally, but can be
     * overridden to provide custom behavior when the radio is toggled if needed.
     */
    onCheckClick : function(){
        this[this.radio.dom.checked ? 'expand' : 'collapse']();
    }
});
Ext.reg('radiofieldset', Ext.ux.RadioFieldset);  