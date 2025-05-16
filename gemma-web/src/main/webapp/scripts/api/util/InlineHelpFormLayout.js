/**
 * Adds tooltip/qtip to field labels in forms
 * 
 * IE9 doesn't work with "ext:qtip" markup, so we use 'title' attribute instead --> looks like it may work now! Run more
 * checks to be sure.
 * 
 * Adds the following fields for IE: - fieldTip (no HTML markup allowed)
 * 
 * Adds the following fields for FF/Chrome: - fieldTip - fieldTipHTML (if set, this is used instead of fieldTip) -
 * fieldTipCls - fieldTipTitle
 * 
 * if you want to include markup for use in FF and Chrome, set the fieldTipHTML field
 * 
 * if you want to hide the help icon, set property hideHelpIcon = true
 * 
 * 
 */
if ( Ext.isIE ) {
   Ext
      .override(
         Ext.layout.FormLayout,
         {
            fieldTpl : new Ext.XTemplate(
               '<div class="x-form-item {itemCls}" tabIndex="-1">',
               '<label for="{id}" style="{labelStyle}" title="{labelTip}" class="x-form-item-label">{label}{labelSeparator}</label>',
               '<div class="x-form-element" id="x-form-el-{id}" style="{elementStyle}">',
               '<img src="' + Gemma.CONTEXT_PATH + '/images/icons/question_blue.png" style="float:right;display:{displayHelp}" title="{labelTip}"/>',
               '</div><div class="{clearCls}"></div>', '</div>', {
                  disableFormats : true
               } ),
            getTemplateArgs : function( field ) {

               var noLabelSep = !field.fieldLabel || field.hideLabel;
               var tipText = (typeof field.fieldTip === undefined || !field.fieldTip) ? '' : field.fieldTip;
               // escape any quotes (single and double)
               tipText = tipText.replace( "'", "&#39;" );
               tipText = tipText.replace( '"', '&quot;' );
               return {
                  id : field.id,
                  label : field.fieldLabel,
                  itemCls : (field.itemCls || this.container.itemCls || '') + (field.hideLabel ? ' x-hide-label' : ''),
                  clearCls : field.clearCls || 'x-form-clear-left',
                  labelStyle : this.getLabelStyle( field.labelStyle ),
                  elementStyle : this.elementStyle || '',
                  labelSeparator : noLabelSep ? '' : (Ext.isDefined( field.labelSeparator ) ? field.labelSeparator
                     : this.labelSeparator),

                  labelTip : tipText,
                  displayHelp : (field.hidden || field.hideHelpIcon || !tipText || typeof tipText === 'undefined' || tipText === '') ? 'none'
                     : 'inline'
               };
            }
         } );
} else {
   Ext
      .override(
         Ext.layout.FormLayout,
         {
            fieldTpl : new Ext.XTemplate(
               '<div class="x-form-item {itemCls}" tabIndex="-1">',
               '<label for="{id}" style="{labelStyle}" ext:qtip="{labelTip}" ext:qtitle="{labelTipTitle}" ext:qclass="{labelTipCls}" class="x-form-item-label">{label}{labelSeparator}</label>',
               '<div class="x-form-element" id="x-form-el-{id}" style="{elementStyle}" >',
               '<img src="' + Gemma.CONTEXT_PATH + '/images/icons/question_blue.png" style="float:right;display:{displayHelp}" ext:qtip="{labelTip}" ext:qtitle="{labelTipTitle}" ext:qclass="{labelTipCls}" "/>',
               '</div><div class="{clearCls}"></div>', '</div>', {
                  disableFormats : true
               } ),
            getTemplateArgs : function( field ) {

               var noLabelSep = !field.fieldLabel || field.hideLabel;
               var tipText = (typeof field.fieldTipHTML !== 'undefined') ? field.fieldTipHTML
                  : (typeof field.fieldTip !== 'undefined') ? field.fieldTip : '';
               // escape any quotes (single and double)
               tipText = tipText.replace( "'", "&#39;" );
               tipText = tipText.replace( '"', '&quot;' );

               return {
                  id : field.id,
                  label : field.fieldLabel,
                  itemCls : (field.itemCls || this.container.itemCls || '') + (field.hideLabel ? ' x-hide-label' : ''),
                  clearCls : field.clearCls || 'x-form-clear-left',
                  labelStyle : this.getLabelStyle( field.labelStyle ),
                  elementStyle : this.elementStyle || '',
                  labelSeparator : noLabelSep ? '' : (Ext.isDefined( field.labelSeparator ) ? field.labelSeparator
                     : this.labelSeparator),

                  labelTip : tipText,
                  labelTipCls : 'inline-help-form-q-tip '
                     + ((typeof field.fieldTipCls !== 'undefined') ? field.fieldTipCls : ''),
                  labelTipTitle : field.fieldTipTitle,
                  displayHelp : (field.hidden || field.hideHelpIcon || !tipText || typeof tipText === 'undefined' || tipText === '') ? 'none'
                     : 'inline'
               };
            }
         } );
}
