package ru.nuclear.stenographer

data class SpeechPart(val speaker:String,val start:Long,val end:Long,val text:String,val first:Int,val last:Int)
fun groupDialogue(parts:List<SpeechPart>):List<SpeechPart>{
 val out=mutableListOf<SpeechPart>()
 for(part in parts){val prev=out.lastOrNull()
  if(prev!=null&&prev.speaker==part.speaker&&part.start>=prev.start&&part.start-prev.end<=2500){
   val gap=if(prev.text.lastOrNull()?.isWhitespace()==true||part.text.firstOrNull()?.isWhitespace()==true||part.text.firstOrNull() in listOf('.',',','!','?',':',';'))"" else " "
   out[out.lastIndex]=prev.copy(end=maxOf(prev.end,part.end),text=prev.text+gap+part.text,last=part.last)
  }else out.add(part)
 };return out
}
fun speakerLabel(id:String)=if(id=="speaker_unknown")"Голос не определён" else "Участник ${id.removePrefix("speaker_")}"
