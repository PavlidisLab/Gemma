//Ext.namespace('Gemma');
//
///**
// * Window for user to set diff ex search refinements
// *
// * you must implement handling for 'Re-run search' button
// * click: an event is fired with param: threshold (p value)
// *
// * pass in a "threshold" param to set value
// */
//
//Gemma.DifferentialExpressionSearchOptions = Ext.extend(Ext.Window,{
//	title: 'Differential expression search refinements',
//	layout: 'form',
//	width: 275,
//	height: 200,
//	padding: 10,
//	threshold: Gemma.DEFAULT_THRESHOLD,
//
//	initComponent: function(){
//		Ext.apply(this, {
//			buttons: [{
//				text: 'Re-run search',
//				scope: this,
//				handler: function(){
//					this.fireEvent('rerunSearch', this.thresholdfield.getValue());
//				}
//			}, {
//				text: 'Cancel',
//				scope: this,
//				handler: function(){
//					this.hide();
//				}
//			}],
//			items: [{
//				html: '<h4>Run your <u>original</u> differential expression search with: </h4><br>',
//				bodyStyle: 'background-color: transparent',
//				border: false
//			}, {
//				xtype: 'numberfield',
//				accelerate: true,
//				ref: 'thresholdfield',
//				allowBlank: false,
//				allowDecimals: true,
//				allowNegative: false,
//				minValue: Gemma.MIN_THRESHOLD,
//				maxValue: Gemma.MAX_THRESHOLD,
//				invalidText: "Minimum threshold is " + Gemma.MIN_THRESHOLD + ".  Max threshold is " +
//				Gemma.MAX_THRESHOLD,
//				msgTarget:'side',
//				value: this.threshold,
//				fieldLabel: 'Threshold ' +
//				'<img ext:qtip="Only genes with a qvalue less than this threshold are returned." ' +
//				'src="/Gemma/images/icons/question_blue.png"/>',
//				width: 60
//			}]
//		});
//		Gemma.DifferentialExpressionSearchOptions.superclass.initComponent.call(this);
//	}
//});