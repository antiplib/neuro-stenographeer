import java.io.File
import java.util.Locale
fun normalized(text:String):List<String> = text.lowercase(Locale.ROOT).replace('ё','е').replace(Regex("[^\\p{L}\\p{N}\\s]")," ").trim().split(Regex("\\s+")).filter{it.isNotBlank()}
fun distance(ref:List<String>,hyp:List<String>):Int {
 var row=IntArray(hyp.size+1){it}
 for(i in ref.indices){val next=IntArray(hyp.size+1);next[0]=i+1
  for(j in hyp.indices)next[j+1]=minOf(next[j]+1,row[j+1]+1,row[j]+if(ref[i]==hyp[j])0 else 1)
  row=next
 };return row.last()
}
fun main(args:Array<String>){
 if(args.contentEquals(arrayOf("--test"))){check(distance(normalized("Ёжик, привет!"),normalized("ежик привет"))==0);check(distance(listOf("a","b"),listOf("a","c","d"))==2);check(distance(emptyList(),listOf("a"))==1);println("PASS WER normalization and edit distance");return}
 require(args.size==2){"Usage: java -jar wer.jar reference.txt hypothesis.txt"}
 val ref=normalized(File(args[0]).readText());val hyp=normalized(File(args[1]).readText());require(ref.isNotEmpty()){"WER undefined for empty reference"}
 println("WER = %.4f (%d edits / %d reference words)".format(Locale.ROOT,distance(ref,hyp).toDouble()/ref.size,distance(ref,hyp),ref.size))
}
