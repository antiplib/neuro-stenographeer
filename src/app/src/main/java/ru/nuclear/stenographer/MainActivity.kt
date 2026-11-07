package ru.nuclear.stenographer

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.provider.OpenableColumns
import android.view.View
import android.view.WindowManager
import android.widget.*
import org.json.JSONObject
import java.io.File
import java.util.concurrent.Executors

class MainActivity:Activity(){
 private lateinit var status:TextView;private lateinit var output:TextView;private lateinit var audioLabel:TextView
 private lateinit var dialoguePanel:LinearLayout
 private lateinit var scrollPage:ScrollView
 private lateinit var progress:ProgressBar;private lateinit var store:Store
 private val executor=Executors.newSingleThreadExecutor();private val buttons=mutableListOf<Button>()
 private var audio:Uri?=null;private var audioName="Разговор";private var current:Saved?=null;private var busy=false
 private var exportSnapshot:String?=null
 private lateinit var language:Spinner;private lateinit var speakerCount:Spinner
 private val pink=Aero.ink
 private fun dp(n:Int)=(n*resources.displayMetrics.density).toInt()
 private fun model(name:String)=File(filesDir,name)
 private fun ui(fn:()->Unit){runOnUiThread{if(!isDestroyed)fn()}}
 private fun button(root:LinearLayout,title:String,action:()->Unit){val b=Button(this);b.text=title;b.isAllCaps=false;b.textSize=15f;b.letterSpacing=.01f;b.setTextColor(Aero.ink);b.backgroundTintList=null;b.background=Aero.button();b.elevation=dp(3).toFloat();b.setPadding(dp(12),dp(8),dp(12),dp(8));b.minHeight=dp(46)
  val lp=LinearLayout.LayoutParams(if(root.orientation==LinearLayout.HORIZONTAL)0 else -1,-2,if(root.orientation==LinearLayout.HORIZONTAL)1f else 0f);lp.setMargins(dp(3),dp(5),dp(3),dp(5));root.addView(b,lp);b.setOnClickListener{action()};buttons+=b}
 private fun label(root:LinearLayout,text:String,size:Float=14f):TextView{val t=TextView(this);t.text=text;t.textSize=size;t.setTextColor(Aero.ink);t.setPadding(dp(8),dp(8),dp(8),dp(5));root.addView(t);return t}

 private fun pick(code:Int,type:String){startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{addCategory(Intent.CATEGORY_OPENABLE);this.type=type},code)}
 override fun onCreate(saved:Bundle?){super.onCreate(saved);store=Store(this);window.statusBarColor=Color.TRANSPARENT;window.navigationBarColor=0xFF079DCF.toInt()
  val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(18),dp(20),dp(18),dp(34))}
  val header=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;background=Aero.glass();setPadding(dp(12),dp(14),dp(12),dp(14));elevation=dp(5).toFloat()};root.addView(header)
  label(header,"◉  Нейро-стенографист",24f).setTypeface(null,Typeface.BOLD)
  label(header,"AQUA  /  тихое место для ваших разговоров",12f)
  status=label(header,"Готов к работе",13f)
  val actions=LinearLayout(this);header.addView(actions)
  button(actions,"⚙ Модели"){manageModels()};button(actions,"◷ История"){history()}
  val panel=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;background=Aero.glass(0xBBDDF8FF.toInt());setPadding(dp(10),dp(10),dp(10),dp(14))};root.addView(panel,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(16)})
  label(panel,"01  Аудиозапись",18f).setTypeface(null,Typeface.BOLD)
  button(panel,"♫  Выбрать запись"){pick(12,"audio/*")}
  audioLabel=label(panel,"WAV · MP3 · M4A  /  до 10 минут",13f)
  label(panel,"Язык речи")
  language=Spinner(this).apply{adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,listOf("Русский","Автоопределение","English"));background=Aero.glass();minimumHeight=dp(46)};language.setSelection(getPreferences(0).getInt("speech_language",0).coerceIn(0,2));panel.addView(language)
  label(panel,"Сколько человек разговаривает?")
  speakerCount=Spinner(this).apply{adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,listOf("Определить автоматически","1","2","3","4","5","6"));background=Aero.glass();minimumHeight=dp(46)};speakerCount.setSelection(getPreferences(0).getInt("participants",0).coerceIn(0,6));panel.addView(speakerCount)
  button(panel,"▶  Расшифровать разговор"){process()}
  progress=ProgressBar(this).apply{visibility=View.GONE;indeterminateTintList=ColorStateList.valueOf(0xFF008DBD.toInt())};panel.addView(progress)
  label(root,"02  Ваш диалог",21f).setTypeface(null,Typeface.BOLD)
  output=label(root,"Здесь появятся цельные реплики участников. Обработка работает на телефоне без интернета.",14f).apply{background=Aero.glass();setPadding(dp(16),dp(16),dp(16),dp(16));setTextIsSelectable(true)}
  dialoguePanel=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};root.addView(dialoguePanel)
  val tools=LinearLayout(this);root.addView(tools)
  button(tools,"⇩ JSON"){val c=current;if(c==null)message("Сначала получите расшифровку")else{exportSnapshot=c.json;startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply{addCategory(Intent.CATEGORY_OPENABLE);type="application/json";putExtra(Intent.EXTRA_TITLE,"transcript_${c.id}.json")},13)}}
  button(tools,"↑ Наверх"){scrollPage.smoothScrollTo(0,0)}
  label(root,"Локальные модели • ваши разговоры остаются у вас",12f)
  val page=ScrollView(this).apply{isFillViewport=true;isVerticalScrollBarEnabled=true;addView(root)};scrollPage=page
  page.setOnApplyWindowInsetsListener{view,insets->view.setPadding(insets.systemWindowInsetLeft,insets.systemWindowInsetTop,insets.systemWindowInsetRight,insets.systemWindowInsetBottom);insets.consumeSystemWindowInsets()}
  val scene=FrameLayout(this);scene.addView(AquaBackdrop(this),FrameLayout.LayoutParams(-1,-1));scene.addView(page,FrameLayout.LayoutParams(-1,-1));setContentView(scene);modelsStatus()
  if(getPreferences(0).getInt("models_version",0)!=2)restoreModels()
 }

 private fun manageModels(){
  AlertDialog.Builder(this).setTitle("Встроенные модели").setItems(arrayOf("Заменить Whisper (.bin)","Заменить Qwen (.gguf)","Восстановить из APK без сети","Удалить рабочие копии")){_,which->when(which){
   0->pick(10,"*/*")
   1->pick(11,"*/*")
   2->restoreModels()
   3->AlertDialog.Builder(this).setMessage("Удалить модели из внутренней памяти? Их можно восстановить из APK без сети. Встроенные файлы остаются частью установленного APK.").setPositiveButton("Удалить"){_,_->listOf("asr.bin","llm.gguf","segmentation.onnx","embedding.onnx").forEach{model(it).delete()};getPreferences(0).edit().putInt("models_version",0).apply();modelsStatus()}.setNegativeButton("Отмена",null).show()
  }}.show()
 }
 private fun restoreModels(){
  state(true,"Подготовка встроенных моделей… Интернет не нужен")
  executor.execute{try {
   for(name in listOf("asr.bin","llm.gguf","segmentation.onnx","embedding.onnx")){
    val target=model(name);val temp=File(filesDir,"$name.part")
    try {assets.open("models/$name").use{input->temp.outputStream().use{out->input.copyTo(out)}};check(temp.renameTo(target)){"Не удалось сохранить $name"}}
    finally{temp.delete()}
   }
   getPreferences(0).edit().putInt("models_version",2).apply()
   ui{state(false,"Модели готовы");modelsStatus()}
  }catch(e:Throwable){ui{state(false,"Ошибка подготовки моделей");message("${e.message}\nНужно достаточно свободного места для четырёх моделей.")}}}
 }
 private fun modelsStatus(){status.setTextColor(pink);status.text="Whisper small: ${if(model("asr.bin").exists())"готов" else "нет модели"} · Голоса: ${if(model("embedding.onnx").exists()&&model("segmentation.onnx").exists())"готовы" else "нет моделей"} · LLM: ${if(model("llm.gguf").exists())"готова" else "нет модели"}"}
 private fun message(s:String){AlertDialog.Builder(this).setMessage(s).setPositiveButton("ОК",null).show()}
 private fun state(value:Boolean,label:String){busy=value;buttons.forEach{it.isEnabled=!value};language.isEnabled=!value;speakerCount.isEnabled=!value;progress.visibility=if(value)View.VISIBLE else View.GONE;status.text=label;if(value)window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)}
 override fun onActivityResult(code:Int,result:Int,data:Intent?){super.onActivityResult(code,result,data);if(result!=RESULT_OK)return;val uri=data?.data?:return
  when(code){
   10,11->{state(true,"Копирование модели…");executor.execute{val target=model(if(code==10)"asr.bin" else "llm.gguf");val temp=File(filesDir,"model.part");try{
    contentResolver.openInputStream(uri)!!.use{input->temp.outputStream().use{out->val b=ByteArray(1024*1024);var total=0L;while(true){val n=input.read(b);if(n<0)break;total+=n;require(total<=2L*1024*1024*1024){"Модель больше 2 ГБ"};out.write(b,0,n)};require(total>1024){"Файл слишком мал"}}}
    if(code==11){val header=temp.inputStream().use{ByteArray(4).also { bytes -> require(it.read(bytes)==4) }};require(header.contentEquals(byteArrayOf(71,71,85,70))){"Ожидался файл GGUF"}}
    else {val header=temp.inputStream().use{ByteArray(4).also { bytes -> require(it.read(bytes)==4) }};require(header.contentEquals(byteArrayOf(108,109,103,103))){"Ожидалась Whisper ggml модель .bin"}}
    check(temp.renameTo(target)){"Не удалось сохранить модель"};ui{state(false,"Модель импортирована");modelsStatus()}
   }catch(e:Throwable){ui{state(false,"Ошибка импорта");message(e.message?:"Ошибка")}}finally{temp.delete()}}}
   12->{audio=uri;audioName="Разговор";contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use{if(it.moveToFirst())audioName=it.getString(0)};audioLabel.text=audioName}
   13->{val json=exportSnapshot?:return;executor.execute{try{contentResolver.openOutputStream(uri,"wt")!!.bufferedWriter(Charsets.UTF_8).use{it.write(json)};ui{message("JSON сохранён")}}catch(e:Exception){ui{message("Ошибка экспорта: ${e.message}")}}}}
  }
 }
 private fun process(){val source=audio?:run{message("Выберите аудиозапись");return};if(listOf("asr.bin","llm.gguf","segmentation.onnx","embedding.onnx").any{!model(it).exists()}){message("Восстановите все встроенные модели через меню «Модели и память»");return}
  val selectedLanguage=listOf("ru","auto","en")[language.selectedItemPosition];val selectedSpeakers=speakerCount.selectedItemPosition
  getPreferences(0).edit().putInt("speech_language",language.selectedItemPosition).putInt("participants",selectedSpeakers).apply()
  state(true,"1/3 · Декодирование аудио…");executor.execute{val pcm=File(cacheDir,"input.f32");val started=SystemClock.elapsedRealtime()
   try{
    val duration=AudioDecoder.decode(this,source,pcm)
    ui{status.text="2/3 · Распознавание и разделение голосов…"}
    val json=String(NativeAsr.transcribe(pcm.absolutePath,model("asr.bin").absolutePath,model("segmentation.onnx").absolutePath,model("embedding.onnx").absolutePath,selectedLanguage,selectedSpeakers),Charsets.UTF_8)
    val segments=JSONObject(json).getJSONArray("transcript");val text=(0 until segments.length()).joinToString("\n"){val s=segments.getJSONObject(it);"${s.getString("speaker_id")}: ${s.getString("text")}"}
    // Release Whisper before loading the LLM; both models need not reside in RAM together.
    ui{status.text="3/3 · Локальная LLM составляет итог…"}
    val llmSummary=if(text.isBlank())"Речь не обнаружена" else String(NativeLlm.summarize(model("llm.gguf").absolutePath,text),Charsets.UTF_8)
    val unknown=(0 until segments.length()).count{segments.getJSONObject(it).getString("speaker_id")=="speaker_unknown"}
    val failed=JSONObject(json).optJSONObject("diagnostics")?.optBoolean("diarization_failed",false)==true
    val notes=if(failed)"Не удалось выполнить диаризацию. Распознанный текст сохранён с неопределённым голосом." else if(unknown>0)"Реплик без определённого спикера: $unknown. Их текст сохранён." else ""
    val summary=if(notes.isBlank())llmSummary else "$notes\n\n$llmSummary"
    val elapsed=SystemClock.elapsedRealtime()-started;val id=store.save(audioName,duration,elapsed,summary,json);val saved=store.load(id)
    ui{state(false,"Готово · %.1f с · RTF %.2f".format(elapsed/1000.0,elapsed.toDouble()/duration));show(saved)}
   }catch(e:Throwable){ui{state(false,"Обработка не завершена");message("${e.message}\nПроверьте формат моделей, свободную память и аудиокодек.")}}
   finally{pcm.delete()}
  }
 }
 private fun show(saved:Saved){current=saved;dialoguePanel.removeAllViews();val array=JSONObject(saved.json).getJSONArray("transcript")
  val parts=(0 until array.length()).map{i->val x=array.getJSONObject(i);SpeechPart(x.getString("speaker_id"),x.getLong("start_ms"),x.getLong("end_ms"),x.getString("text"),i,i)}
  val turns=groupDialogue(parts);val count=parts.map{it.speaker}.filter{it!="speaker_unknown"}.distinct().size
  output.text="${saved.title}\n$count участников · ${turns.size} реплик\nНажмите на подпись голоса, чтобы исправить участника."
  if(turns.isEmpty())label(dialoguePanel,"Речь не обнаружена")
  val colors=intArrayOf(0xBBDDF8FF.toInt(),0xBBDDF5DA.toInt(),0xBBFFF0CB.toInt(),0xBBEEE6FF.toInt())
  for(turn in turns){val index=turn.speaker.removePrefix("speaker_").toIntOrNull();val card=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;background=Aero.glass(if(index==null)0xBBDDE6EA.toInt() else colors[(index-1)%colors.size]);setPadding(dp(14),dp(10),dp(14),dp(16));elevation=dp(3).toFloat()}
   dialoguePanel.addView(card,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(10);leftMargin=if(index!=null&&index%2==0)dp(16)else 0;rightMargin=if(index!=null&&index%2==1)dp(16)else 0})
   val head=label(card,"${if(index==null)"?" else "●"}  ${speakerLabel(turn.speaker)}    ✎",16f);head.setTypeface(null,Typeface.BOLD);head.contentDescription="Изменить участника реплики";head.setOnClickListener{editSpeaker(saved.id,turn)}
   label(card,"${time(turn.start)} — ${time(turn.end)}",12f)
   label(card,turn.text.trim(),18f).apply{setTextIsSelectable(true);setLineSpacing(dp(4).toFloat(),1f)}
  }
  val summary=label(dialoguePanel,"▸  Краткий итог и замечания",16f).apply{background=Aero.glass();setTypeface(null,Typeface.BOLD)}
  val summaryText=label(dialoguePanel,saved.summary,15f).apply{visibility=View.GONE;background=Aero.glass();setTextIsSelectable(true)}
  summary.setOnClickListener{summaryText.visibility=if(summaryText.visibility==View.GONE)View.VISIBLE else View.GONE}
 }
 private fun editSpeaker(recording:Long,turn:SpeechPart){
  val ids=(1..6).map{"speaker_$it"}+"speaker_unknown"
  AlertDialog.Builder(this).setTitle("Кто говорит?").setMessage(null).setSingleChoiceItems(ids.map{speakerLabel(it)}.toTypedArray(),ids.indexOf(turn.speaker)){d,i->store.assignSpeaker(recording,turn.first,turn.last,ids[i]);d.dismiss();show(store.load(recording))}.setNeutralButton("Что значит «голос не определён»?"){_,_->message("Речь распознана, но голос не удалось уверенно связать с участником. Такое бывает при коротких словах и одновременной речи. Текст сохранён. Здесь можно выбрать участника вручную; исправление попадёт в JSON.")}.setNegativeButton("Отмена",null).show()
 }

 private fun time(ms:Long)="%02d:%02d.%03d".format(ms/60000,ms/1000%60,ms%1000)
 private fun history(){val list=store.list();if(list.isEmpty()){message("Сохранённых записей нет");return};AlertDialog.Builder(this).setTitle("История").setItems(list.map{"#${it.first} ${it.second}"}.toTypedArray()){_,i->show(store.load(list[i].first))}.show()}
 override fun onDestroy(){super.onDestroy();executor.shutdown()}
}
