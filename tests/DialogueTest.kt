import ru.nuclear.stenographer.*
fun main(){
 val parts=listOf(SpeechPart("speaker_1",0,500,"Привет.",0,0),SpeechPart("speaker_1",600,900," Как дела?",1,1),SpeechPart("speaker_2",1000,1400,"Хорошо!",2,2),SpeechPart("speaker_1",1600,2000,"Рад.",3,3))
 val grouped=groupDialogue(parts);check(grouped.size==3);check(grouped[0].text=="Привет. Как дела?");check(grouped[0].first==0&&grouped[0].last==1);check(grouped[1].speaker=="speaker_2")
 check(groupDialogue(listOf(parts[0],parts[1].copy(start=5000,end=6000))).size==2)
 check(speakerLabel("speaker_unknown")=="Голос не определён");check(speakerLabel("speaker_2")=="Участник 2")
 println("PASS consecutive phrases, participant switches, long pauses and correction ranges")
}
