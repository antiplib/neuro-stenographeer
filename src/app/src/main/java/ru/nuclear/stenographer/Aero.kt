package ru.nuclear.stenographer
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.View
import kotlin.math.*

object Aero {
 val ink=Color.rgb(8,64,102)
 fun glass(accent:Int=0xCCFFFFFF.toInt()):Drawable=GlassSurface(accent)
 fun button():Drawable {
  val d=Resources.getSystem().displayMetrics.density
  val surface=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(0xFFFFFFFF.toInt(),0xFFB8F3FF.toInt(),0xFF62D1ED.toInt(),0xFF2AAAD1.toInt(),0xFF7DE2F0.toInt())).apply{cornerRadius=14*d;setStroke(max(1,(d*1.2f).toInt()),0xF5FFFFFF.toInt());if(android.os.Build.VERSION.SDK_INT>=29)setColors(colors(),floatArrayOf(0f,.42f,.45f,.8f,1f))}
  return RippleDrawable(ColorStateList.valueOf(0x4482E6FF),surface,null)
 }
 private fun colors()=intArrayOf(0xFFFFFFFF.toInt(),0xFFB8F3FF.toInt(),0xFF62D1ED.toInt(),0xFF2AAAD1.toInt(),0xFF7DE2F0.toInt())
}
private class GlassSurface(private val accent:Int):Drawable(){
 private val p=Paint(Paint.ANTI_ALIAS_FLAG);private var opacity=255
 override fun draw(c:Canvas){val r=RectF(bounds);val d=Resources.getSystem().displayMetrics.density;val radius=18*d;r.inset(d,d)
  p.style=Paint.Style.FILL;p.alpha=opacity;p.shader=LinearGradient(0f,r.top,0f,r.bottom,intArrayOf(0xF4FFFFFF.toInt(),accent,0xD9D9F6FF.toInt()),floatArrayOf(0f,.46f,1f),Shader.TileMode.CLAMP);c.drawRoundRect(r,radius,radius,p);p.shader=null
  c.save();val clip=Path().apply{addRoundRect(r,radius,radius,Path.Direction.CW)};c.clipPath(clip)
  val sheen=Path().apply{moveTo(r.left,r.top);lineTo(r.right,r.top);lineTo(r.right,r.top+r.height()*.18f);cubicTo(r.centerX(),r.top+r.height()*.08f,r.centerX(),r.top+r.height()*.39f,r.left,r.top+r.height()*.22f);close()}
  p.shader=LinearGradient(0f,r.top,0f,r.top+r.height()*.4f,0x66FFFFFF,0x00FFFFFF,Shader.TileMode.CLAMP);c.drawPath(sheen,p);p.shader=null;c.restore()
  p.style=Paint.Style.STROKE;p.strokeWidth=d*1.4f;p.color=0xF2FFFFFF.toInt();c.drawRoundRect(r,radius,radius,p)
  val inner=RectF(r);inner.inset(d*2,d*2);p.strokeWidth=d*.7f;p.color=0x5590CCDF;c.drawRoundRect(inner,radius-d,radius-d,p);p.style=Paint.Style.FILL
 }
 override fun setAlpha(alpha:Int){opacity=alpha;invalidateSelf()}
 override fun setColorFilter(filter:ColorFilter?){p.colorFilter=filter;invalidateSelf()}
 @Deprecated("Deprecated in Java") override fun getOpacity()=PixelFormat.TRANSLUCENT
}
/** Scenic viewport. Every aquatic detail shares the lake's clipping path. */
class AquaBackdrop(ctx:Context):View(ctx){
 private val p=Paint(Paint.ANTI_ALIAS_FLAG)
 override fun onDraw(c:Canvas){super.onDraw(c);val w=width.toFloat();val h=height.toFloat();if(w<=0||h<=0)return;val u=min(w,h)
  fun fill(color:Int){p.shader=null;p.style=Paint.Style.FILL;p.color=color;p.alpha=255}
  p.shader=LinearGradient(0f,0f,w*.4f,h*.7f,intArrayOf(0xFF1686CD.toInt(),0xFF74D8F4.toInt(),0xFFE3FFF5.toInt()),null,Shader.TileMode.CLAMP);c.drawRect(0f,0f,w,h,p);p.shader=null
  val sunX=w*.84f;val sunY=h*.14f
  p.shader=RadialGradient(sunX,sunY,u*.38f,intArrayOf(0xFFFFFBCD.toInt(),0x77FFFFDC,0x00FFFFDC),null,Shader.TileMode.CLAMP);c.drawCircle(sunX,sunY,u*.38f,p);p.shader=null
  fill(0xFFFFFBE0.toInt());c.drawCircle(sunX,sunY,u*.055f,p)
  p.style=Paint.Style.STROKE;p.strokeWidth=u*.003f;p.color=0x33FFFFFF
  for(i in 0..11){val a=i*Math.PI/6;c.drawLine(sunX+cos(a).toFloat()*u*.09f,sunY+sin(a).toFloat()*u*.09f,sunX+cos(a).toFloat()*u*.27f,sunY+sin(a).toFloat()*u*.27f,p)};p.style=Paint.Style.FILL
  fun cloud(x:Float,y:Float,s:Float){p.shader=LinearGradient(0f,y-s,0f,y+s,0xEFFFFFFF.toInt(),0x22FFFFFF,Shader.TileMode.CLAMP);c.drawOval(x-s*1.6f,y-s*.3f,x+s*1.6f,y+s*.4f,p);c.drawOval(x-s*.9f,y-s*.7f,x+s*.5f,y+s*.2f,p);c.drawOval(x-s*.1f,y-s*.5f,x+s*1.05f,y+s*.3f,p);p.shader=null}
  cloud(w*.18f,h*.19f,u*.10f);cloud(w*.57f,h*.29f,u*.075f);cloud(w*.95f,h*.36f,u*.11f)
  fun hill(y:Float,depth:Float,top:Int,bottom:Int){val path=Path().apply{moveTo(0f,h);lineTo(0f,y);cubicTo(w*.18f,y-depth,w*.39f,y+depth*.25f,w*.60f,y-depth*.4f);cubicTo(w*.77f,y-depth,w*.92f,y-depth*.7f,w,y-depth*.2f);lineTo(w,h);close()};p.shader=LinearGradient(0f,y-depth,0f,y+depth,top,bottom,Shader.TileMode.CLAMP);c.drawPath(path,p);p.shader=null}
  hill(h*.51f,h*.08f,0xFFB8E8A2.toInt(),0xFF8AC87C.toInt());hill(h*.57f,h*.07f,0xFF8AD95B.toInt(),0xFF46A867.toInt())
  val water=Path().apply{moveTo(0f,h*.64f);cubicTo(w*.20f,h*.58f,w*.36f,h*.65f,w*.53f,h*.61f);cubicTo(w*.72f,h*.57f,w*.89f,h*.60f,w,h*.57f);lineTo(w,h);lineTo(0f,h);close()}
  // Sandy shoreline and soft bank, following the exact water contour.
  c.save();c.translate(0f,-u*.012f);p.shader=LinearGradient(0f,h*.55f,0f,h*.67f,0xFFE8E6AA.toInt(),0xFF65AD79.toInt(),Shader.TileMode.CLAMP);c.drawPath(water,p);p.shader=null;c.restore()
  p.shader=LinearGradient(0f,h*.58f,w*.5f,h,intArrayOf(0xFFB8F3ED.toInt(),0xFF40C8DC.toInt(),0xFF087DAF.toInt()),null,Shader.TileMode.CLAMP);c.drawPath(water,p);p.shader=null
  c.save();c.clipPath(water)
  // Sun reflection, waves and underwater caustics, all clipped to water.
  p.style=Paint.Style.STROKE;p.strokeCap=Paint.Cap.ROUND
  for(i in 0..15){val y=h*(.64f+i*.016f);val x=sunX+sin(i*1.7).toFloat()*u*.024f;p.color=Color.argb(110-i*5,250,255,218);p.strokeWidth=u*.004f;c.drawLine(x-u*(.025f+i*.004f),y,x+u*(.025f+i*.004f),y,p)}
  for(i in 0..13){p.color=0x45D9FFFF;p.strokeWidth=u*.002f;val y=h*(.66f+i*.027f);c.drawArc(-w*.2f,y,w*1.15f,y+u*.10f,i%3*7f,140f,false,p)};p.style=Paint.Style.FILL
  fun fish(x:Float,y:Float,s:Float,color:Int,flip:Boolean){c.save();c.translate(x,y);if(flip)c.scale(-1f,1f)
   // Shadow beneath body, gradient scales, fins and reflective eyes.
   fill(0x220B577D);c.drawOval(-s*1.1f,s*.4f,s*.9f,s*.65f,p)
   val tail=Path().apply{moveTo(-s*.65f,0f);quadTo(-s*1.1f,-s*.22f,-s*1.5f,-s*.55f);quadTo(-s*1.32f,0f,-s*1.5f,s*.55f);close()};fill(color);c.drawPath(tail,p)
   p.shader=LinearGradient(0f,-s*.45f,0f,s*.45f,intArrayOf(Color.WHITE,color,0xFFD88C47.toInt()),null,Shader.TileMode.CLAMP);c.drawOval(-s,-s*.43f,s,s*.43f,p);p.shader=null
   fill(0x55FFFFFF);val fin=Path().apply{moveTo(-s*.1f,0f);lineTo(s*.2f,s*.5f);lineTo(s*.45f,s*.05f);close()};c.drawPath(fin,p)
   p.style=Paint.Style.STROKE;p.strokeWidth=max(1f,s*.025f);p.color=0x779B6B30;c.drawArc(s*.2f,-s*.3f,s*.7f,s*.3f,90f,180f,false,p);p.style=Paint.Style.FILL
   fill(0xFFF8FFFF.toInt());c.drawCircle(s*.66f,-s*.1f,s*.105f,p);fill(0xFF184661.toInt());c.drawCircle(s*.70f,-s*.1f,s*.048f,p);fill(Color.WHITE);c.drawCircle(s*.72f,-s*.13f,s*.018f,p);c.restore()}
  val s=u*.115f;fish(w*.30f,h*.80f,s,0xFFFFBE51.toInt(),false);fish(w*.73f,h*.91f,s*.88f,0xFFFFA875.toInt(),true)
  // Lily pads float in the lake, never on dry land.
  for(i in 0..2){val x=w*(.06f+i*.10f);val y=h*(.70f+i*.025f);fill(0x33466C70);c.drawOval(x-u*.035f,y,x+u*.04f,y+u*.022f,p);fill(0xFF52AE71.toInt());c.drawArc(x-u*.03f,y-u*.014f,x+u*.03f,y+u*.014f,15f,325f,true,p)}
  // Small rising bubbles near the fish.
  for(i in 0..7){val x=w*(.27f+(i%2)*.018f);val y=h*(.77f-i*.014f);val r=u*(.003f+(i%3)*.002f);p.style=Paint.Style.STROKE;p.strokeWidth=u*.0018f;p.color=0x99E8FFFF.toInt();c.drawCircle(x,y,r,p)};p.style=Paint.Style.FILL
  c.restore()
  // Delicate shoreline grasses are masked away from the water.
  c.save();c.clipOutPath(water);p.style=Paint.Style.STROKE;p.strokeWidth=u*.003f;p.strokeCap=Paint.Cap.ROUND
  for(i in 0..19){val x=w*(i/20f);val y=h*(.62f-.02f*sin(i.toDouble()).toFloat());p.color=if(i%2==0)0xFF429A59.toInt() else 0xFF76BF55.toInt();val blade=Path().apply{moveTo(x,y);quadTo(x+u*.016f,y-u*.024f,x-u*.007f,y-u*.055f)};c.drawPath(blade,p)};p.style=Paint.Style.FILL;c.restore()
  // Sparse foreground droplets on the glass; clear central reading area.
  for(i in 0..10){val x=if(i%2==0)w*.035f else w*.965f;val y=h*(.06f+i*.083f);val r=u*(.010f+i%3*.005f)
   p.shader=RadialGradient(x-r*.3f,y-r*.35f,r,intArrayOf(0xCCFFFFFF.toInt(),0x116AD6FF,0x667ABED7),null,Shader.TileMode.CLAMP);c.drawCircle(x,y,r,p);p.shader=null
   p.style=Paint.Style.STROKE;p.strokeWidth=u*.0015f;p.color=0xBBFFFFFF.toInt();c.drawCircle(x,y,r,p);p.style=Paint.Style.FILL;fill(0xCCFFFFFF.toInt());c.drawOval(x-r*.5f,y-r*.6f,x+r*.12f,y-r*.25f,p)
  }
 }
}
