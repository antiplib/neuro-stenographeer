package ru.nuclear.stenographer
import kotlin.math.*
/** Streaming windowed-sinc low-pass interpolation. Delay is removed by final flushing. */
class BandLimitedResampler(private val rate:Int){
 init {require(rate in 8000..192000)}
 private val radius=ceil(32*max(1.0,rate/48000.0)).toInt()
 private val ring=FloatArray(1024)
 private var input=0L;private var output=0L;private var finished=false
 private val phases=Array(1024){phase->
  val frac=phase/1024.0;val cutoff=.90*min(1.0,16000.0/rate)
  val taps=DoubleArray(radius*2);var sum=0.0
  for(k in taps.indices){val x=k-radius+1-frac;val z=cutoff*x
   val sinc=if(abs(z)<1e-12)1.0 else sin(Math.PI*z)/(Math.PI*z)
   val window=if(abs(x)<radius).5+.5*cos(Math.PI*x/radius) else 0.0
   taps[k]=cutoff*sinc*window;sum+=taps[k]
  };FloatArray(taps.size){(taps[it]/sum).toFloat()}
 }
 fun accept(sample:Float,emit:(Float)->Unit){check(!finished);require(sample.isFinite());if(rate==16000){input++;output++;emit(sample);return};ring[(input%ring.size).toInt()]=sample;input++;drain(Long.MAX_VALUE,emit)}
 private fun drain(limit:Long,emit:(Float)->Unit){
  while(output<limit){val position=output*rate/16000.0;val center=floor(position).toLong();if(center+radius>=input)return
   val phase=((position-center)*1024).toInt().coerceIn(0,1023);val taps=phases[phase];var value=0.0
   for(k in taps.indices){val ix=center-radius+1+k;if(ix>=0&&ix<input)value+=ring[(ix%ring.size).toInt()]*taps[k]}
   emit(value.toFloat());output++
  }
 }
 fun finish(emit:(Float)->Unit){check(!finished);finished=true;if(rate==16000)return
  val count=input;val limit=(count*16000+rate-1)/rate
  repeat(radius+2){ring[(input%ring.size).toInt()]=0f;input++}
  drain(limit,emit);check(output==limit)
 }
}
