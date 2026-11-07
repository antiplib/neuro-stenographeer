import ru.nuclear.stenographer.BandLimitedResampler
import kotlin.math.*
fun resample(rate:Int,hz:Double):List<Float>{val r=BandLimitedResampler(rate);val out=mutableListOf<Float>();repeat(rate){r.accept(sin(2*Math.PI*hz*it/rate).toFloat()){out.add(it)}};r.finish{out.add(it)};check(out.size==16000);return out}
// A DFT bin projection measures pass-band and aliased stop-band power.
fun amplitude(a:List<Float>,hz:Double):Double{var re=0.0;var im=0.0;val from=1000;val until=15000;for(i in from until until){val x=2*Math.PI*hz*i/16000;re+=a[i]*cos(x);im+=a[i]*sin(x)};return 2*hypot(re,im)/(until-from)}
fun main(){
 val pass=amplitude(resample(48000,4000.0),4000.0);val alias=amplitude(resample(48000,10000.0),6000.0)
 check(pass>.95){"Pass band attenuated: $pass"};check(alias<.02){"Aliasing: $alias"}
 for(rate in listOf(8000,16000,44100,96000,192000))check(resample(rate,1000.0).size==16000)
 val r=BandLimitedResampler(48000);val output=mutableListOf<Float>();for(i in 0..4)r.accept(.25f){output.add(it)};r.finish{output.add(it)};check(output.size==2)
 println("PASS DFT: 4kHz pass=$pass; 10kHz alias at 6kHz=$alias; duration, tail, 8–192kHz")
}
