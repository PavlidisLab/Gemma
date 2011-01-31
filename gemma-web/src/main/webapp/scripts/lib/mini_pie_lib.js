var MiniPieLib = {};

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
