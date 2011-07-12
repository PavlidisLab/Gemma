
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




