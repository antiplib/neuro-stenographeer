package ru.nuclear.stenographer
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject

data class Saved(val id:Long,val title:String,val duration:Long,val processing:Long,val summary:String,val json:String)
class Store(private val ctx:Context):SQLiteOpenHelper(ctx,"meetings.db",null,1){
 override fun onConfigure(db:SQLiteDatabase){db.setForeignKeyConstraintsEnabled(true)}
 override fun onCreate(db:SQLiteDatabase){ctx.assets.open("schema.sql").bufferedReader().use{it.readText()}.split(';').filter{it.isNotBlank()}.forEach{db.execSQL(it)}}
 override fun onUpgrade(db:SQLiteDatabase,old:Int,new:Int){error("Неизвестная версия БД")}
 fun save(title:String,duration:Long,processing:Long,summary:String,json:String):Long {
  val array=JSONObject(json).getJSONArray("transcript");val db=writableDatabase;db.beginTransaction()
  try {val values=android.content.ContentValues().apply{put("created_ms",System.currentTimeMillis());put("title",title);put("duration_ms",duration);put("processing_ms",processing);put("summary",summary)}
   val id=db.insertOrThrow("recordings",null,values)
   for(i in 0 until array.length()){val s=array.getJSONObject(i);val start=s.getLong("start_ms");val end=s.getLong("end_ms");require(start>=0&&end>=start&&end<=duration)
    val v=android.content.ContentValues().apply{put("recording_id",id);put("ordinal",i);put("speaker_id",s.getString("speaker_id"));put("start_ms",start);put("end_ms",end);put("text",s.getString("text"))};db.insertOrThrow("utterances",null,v)}
   db.setTransactionSuccessful();return id
  }finally{db.endTransaction()}
 }
 fun assignSpeaker(recording:Long,first:Int,last:Int,speaker:String){
  require(first>=0&&last>=first);require(speaker=="speaker_unknown"||speaker.matches(Regex("speaker_[1-6]")))
  writableDatabase.execSQL("UPDATE utterances SET speaker_id=? WHERE recording_id=? AND ordinal BETWEEN ? AND ?",arrayOf(speaker,recording,first,last))
 }
 fun list():List<Pair<Long,String>> {val out=mutableListOf<Pair<Long,String>>();readableDatabase.rawQuery("SELECT id,title FROM recordings ORDER BY id DESC",null).use{c->while(c.moveToNext())out+=c.getLong(0) to c.getString(1)};return out}
 fun load(id:Long):Saved {
  val a=JSONArray();readableDatabase.rawQuery("SELECT speaker_id,start_ms,end_ms,text FROM utterances WHERE recording_id=? ORDER BY ordinal",arrayOf(id.toString())).use{c->while(c.moveToNext())a.put(JSONObject().put("speaker_id",c.getString(0)).put("start_ms",c.getLong(1)).put("end_ms",c.getLong(2)).put("text",c.getString(3)))}
  readableDatabase.rawQuery("SELECT title,duration_ms,processing_ms,summary FROM recordings WHERE id=?",arrayOf(id.toString())).use{c->check(c.moveToFirst());return Saved(id,c.getString(0),c.getLong(1),c.getLong(2),c.getString(3),JSONObject().put("transcript",a).toString(2))}
 }
}
