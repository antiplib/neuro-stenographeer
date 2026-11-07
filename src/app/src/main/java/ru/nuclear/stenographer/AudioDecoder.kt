package ru.nuclear.stenographer

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.io.File
import java.io.BufferedOutputStream
import java.nio.ByteOrder

/** Decode using device codecs; stream mono float32 at 16 kHz to internal storage. */
object AudioDecoder {
 fun decode(context:Context, uri:Uri, target:File):Long {
  val extractor=MediaExtractor();var codec:MediaCodec?=null
  try {
   extractor.setDataSource(context,uri,null)
   val track=(0 until extractor.trackCount).firstOrNull { extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/")==true } ?: error("В файле нет аудиодорожки")
   extractor.selectTrack(track);val format=extractor.getTrackFormat(track)
   codec=MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
   codec.configure(format,null,null,0);codec.start()
   var channels=format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);var rate=format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
   var encoding=AudioFormat.ENCODING_PCM_16BIT;var inputDone=false;var outputDone=false
   val info=MediaCodec.BufferInfo();var frames=0L;var produced=0L;var resampler:BandLimitedResampler?=null;var idle=0
   BufferedOutputStream(target.outputStream()).use { out ->
    fun emit(v:Float){val bits=v.coerceIn(-1f,1f).toRawBits();out.write(bits and 255);out.write((bits ushr 8) and 255);out.write((bits ushr 16) and 255);out.write((bits ushr 24) and 255);produced++;require(produced<=16000L*600){"Лимит MVP — 10 минут аудио"}}
    while(!outputDone){
     if(Thread.currentThread().isInterrupted)error("Обработка прервана")
     var advanced=false
     if(!inputDone){val index=codec.dequeueInputBuffer(10000)
      if(index>=0){val buf=codec.getInputBuffer(index)!!;buf.clear();val n=extractor.readSampleData(buf,0)
       if(n<0){codec.queueInputBuffer(index,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);inputDone=true}
       else {codec.queueInputBuffer(index,0,n,extractor.sampleTime,0);extractor.advance()};advanced=true}}
     val index=codec.dequeueOutputBuffer(info,10000)
     if(index==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){val f=codec.outputFormat
      val newRate=f.getInteger(MediaFormat.KEY_SAMPLE_RATE);val newChannels=f.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
      require(frames==0L||(newRate==rate&&newChannels==channels)){"Смена частоты внутри записи не поддержана"}
      rate=newRate;channels=newChannels;require(rate>0&&channels in 1..8)
      encoding=if(f.containsKey(MediaFormat.KEY_PCM_ENCODING))f.getInteger(MediaFormat.KEY_PCM_ENCODING) else AudioFormat.ENCODING_PCM_16BIT
      require(encoding==AudioFormat.ENCODING_PCM_16BIT||encoding==AudioFormat.ENCODING_PCM_FLOAT){"Неподдерживаемый PCM формат"};advanced=true
     }else if(index>=0){try{
      val buf=codec.getOutputBuffer(index)!!.order(ByteOrder.LITTLE_ENDIAN);buf.position(info.offset);buf.limit(info.offset+info.size)
      val bytes=if(encoding==AudioFormat.ENCODING_PCM_FLOAT)4 else 2
      require(info.size%(bytes*channels)==0){"Некорректная граница PCM кадра"}
      if((info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG)!=0)buf.position(buf.limit())
      while(buf.remaining()>=bytes*channels){var mono=0f;repeat(channels){mono+=if(bytes==4)buf.float else buf.short/32768f};mono/=channels;require(mono.isFinite())
       if(resampler==null)resampler=BandLimitedResampler(rate)
       resampler!!.accept(mono,::emit);frames++
      }
      outputDone=(info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0;advanced=true
     }finally{codec.releaseOutputBuffer(index,false)}}
     idle=if(advanced)0 else idle+1;require(idle<3000){"Декодер не отвечает"}
    }
    resampler?.finish(::emit)
   }
   require(produced>=1600){"Запись слишком короткая или пустая"};return produced/16
  }catch(e:Throwable){target.delete();throw e}
  finally {try{codec?.stop()}catch(_:Exception){};codec?.release();extractor.release()}
 }
}
