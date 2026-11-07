package ru.nuclear.stenographer
object NativeAsr {
 init { System.loadLibrary("steno_asr") }
 external fun transcribe(pcmPath:String, modelPath:String, segmentationPath:String, embeddingPath:String, language:String, speakers:Int):ByteArray
}
object NativeLlm {
 init { System.loadLibrary("steno_llm") }
 external fun summarize(modelPath:String, text:String):ByteArray
}
