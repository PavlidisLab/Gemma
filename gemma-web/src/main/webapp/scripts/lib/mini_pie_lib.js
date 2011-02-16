var MiniPieLib = {};

	/**
	 * Method to draw a two-piece pie chart in a canvas
	 * @param {Object} ctx the canvas component to draw in (here, the canvas tag)
	 * @param {int} x centre of pie on x axis relative to top right of ctx
	 * @param {int} y centre of pie on y axis relative to top right of ctx
	 * @param {int} size size of the pie chart
	 * @param {String} color colour for the slice associsted with value param 
	 * @param {int} value size of the slice in degrees

	 */
MiniPieLib.drawMiniPie = function(ctx, x, y, size, color, value)
{
    ctx.save();
    ctx.fillStyle = color;
    ctx.strokeStyle = color;
    ctx.moveTo(x, y);        
    // draw circle
    ctx.beginPath();
    ctx.arc(x,y,size/2, Math.PI*3/2 , Math.PI*3/2+(Math.PI*2),false);
    ctx.stroke();
    // draw slice
    ctx.beginPath();
    ctx.moveTo(x, y);        
    ctx.arc(x,y,size/2, Math.PI*3/2 , Math.PI*3/2+(Math.PI/180)*value,false);
    ctx.lineTo(x,y);
    ctx.fill();
    
    ctx.restore();			
}

	/**
	 * Method to draw a two-colour, two-piece pie chart in a canvas (where sum of pieces can be < total)
	 * @param {Object} ctx the canvas component to draw in (here, the canvas tag)
	 * @param {int} x centre of pie on x axis relative to top right of ctx
	 * @param {int} y centre of pie on y axis relative to top right of ctx
	 * @param {int} size size of the pie chart
	 * @param {String} colourOne colour for slice one of the pie
	 * @param {int} valueOne size of slice one in degrees
	 * @param {String} colourTwo colour for slice two of the pie
	 * @param {int} valueTwo size of slice two in degrees
	 * @param {String} outlineColour colour for the pie outline
	 */
function drawTwoColourMiniPie(ctx, x, y, size, colourOne, valueOne, colourTwo, valueTwo, outlineColour){
	    ctx.save();
	    ctx.fillStyle = '#E0E0E0';
	    ctx.moveTo(x, y);
	    // draw circle
	    ctx.beginPath();
	    ctx.arc(x,y,size/2, Math.PI*3/2 , Math.PI*3/2+(Math.PI*2),false);
	    ctx.fill();
	    // draw slice one
	    ctx.fillStyle = colourOne;
	    ctx.beginPath();
	    ctx.moveTo(x, y);        
	    ctx.arc(x,y,size/2, Math.PI*3/2 , Math.PI*3/2+(Math.PI/180)*valueOne,false);
	    ctx.lineTo(x,y);
	    ctx.fill();
	    // draw slice two
	    ctx.fillStyle = colourTwo;
	    ctx.beginPath();
	    ctx.moveTo(x, y);        
	    ctx.arc(x,y,size/2, Math.PI*3/2+(Math.PI/180)*valueOne ,
	    	Math.PI*3/2+(Math.PI/180)*valueOne+(Math.PI/180)*valueTwo,false);
	    ctx.lineTo(x,y);
	    ctx.fill();
		// draw circle outline
	    ctx.beginPath();
	    ctx.arc(x,y,size/2, Math.PI*3/2 , Math.PI*3/2+(Math.PI*2),false);
	    ctx.lineWidth = 0.75;
	    ctx.strokeStyle = outlineColour;
	    ctx.stroke();
	    
	    ctx.restore();			
	}