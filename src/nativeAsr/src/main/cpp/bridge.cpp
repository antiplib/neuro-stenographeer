#include <jni.h>
#include "whisper.h"
#include "sherpa-onnx/c-api/c-api.h"
#include "transcript_segment.hpp"
#include <map>
#include <cmath>
#include <fstream>
#include <sstream>
#include <memory>
#include <thread>
#include <algorithm>
struct Utf {JNIEnv* e;jstring s;const char* p;Utf(JNIEnv* e,jstring s):e(e),s(s),p(e->GetStringUTFChars(s,nullptr)){}~Utf(){if(p)e->ReleaseStringUTFChars(s,p);}};
std::string escape(const std::string& s){std::ostringstream o;for(unsigned char c:s){if(c=='"'||c=='\\')o<<'\\'<<c;else if(c<32){const char* h="0123456789abcdef";o<<"\\u00"<<h[c>>4]<<h[c&15];}else o<<c;}return o.str();}
extern "C" JNIEXPORT jbyteArray JNICALL Java_ru_nuclear_stenographer_NativeAsr_transcribe(JNIEnv* e,jobject,jstring pcm,jstring model,jstring segmentation,jstring embedding,jstring language,jint speakers){try{
 Utf p(e,pcm),m(e,model);std::ifstream f(p.p,std::ios::binary|std::ios::ate);if(!f)throw std::runtime_error("Cannot open PCM");auto bytes=f.tellg();if(bytes<=0||bytes%4||bytes>16000LL*600*4)throw std::runtime_error("PCM must be nonempty float32, <=10 minutes");std::vector<float> audio(size_t(bytes)/4);f.seekg(0);if(!f.read(reinterpret_cast<char*>(audio.data()),bytes))throw std::runtime_error("PCM read failed");
 for(auto v:audio)if(!std::isfinite(v))throw std::runtime_error("Invalid PCM sample");

 Utf seg(e,segmentation),emb(e,embedding),lang(e,language);
 std::vector<VoiceTurn> turns;bool diarizationFailed=false;
 try {
 SherpaOnnxOfflineSpeakerDiarizationConfig cfg{};
 cfg.segmentation.pyannote.model=seg.p;cfg.segmentation.num_threads=2;cfg.segmentation.provider="cpu";
 cfg.embedding.model=emb.p;cfg.embedding.num_threads=2;cfg.embedding.provider="cpu";
 cfg.clustering.num_clusters=speakers>0?speakers:-1;cfg.clustering.threshold=.5f;
 cfg.min_duration_on=.2f;cfg.min_duration_off=.3f;
 std::unique_ptr<const SherpaOnnxOfflineSpeakerDiarization,decltype(&SherpaOnnxDestroyOfflineSpeakerDiarization)> diar(SherpaOnnxCreateOfflineSpeakerDiarization(&cfg),SherpaOnnxDestroyOfflineSpeakerDiarization);
 if(!diar)throw std::runtime_error("Cannot load neural speaker models");
 if(SherpaOnnxOfflineSpeakerDiarizationGetSampleRate(diar.get())!=16000)throw std::runtime_error("Expected 16kHz diarization model");
 std::unique_ptr<const SherpaOnnxOfflineSpeakerDiarizationResult,decltype(&SherpaOnnxOfflineSpeakerDiarizationDestroyResult)> result(SherpaOnnxOfflineSpeakerDiarizationProcess(diar.get(),audio.data(),audio.size()),SherpaOnnxOfflineSpeakerDiarizationDestroyResult);
 if(!result)throw std::runtime_error("Neural diarization failed");
 int n=SherpaOnnxOfflineSpeakerDiarizationResultGetNumSegments(result.get());
 std::unique_ptr<const SherpaOnnxOfflineSpeakerDiarizationSegment,decltype(&SherpaOnnxOfflineSpeakerDiarizationDestroySegment)> segments(SherpaOnnxOfflineSpeakerDiarizationResultSortByStartTime(result.get()),SherpaOnnxOfflineSpeakerDiarizationDestroySegment);
 if(n&&!segments)throw std::runtime_error("Missing diarization segments");
 std::map<int,int> labels;
 for(int i=0;i<n;++i){auto t=segments.get()[i];if(!std::isfinite(t.start)||!std::isfinite(t.end))continue;
 int64_t a=std::max<int64_t>(0,std::llround(t.start*16000)),b=std::min<int64_t>(audio.size(),std::llround(t.end*16000));if(b<=a)continue;
 if(!labels.count(t.speaker))labels[t.speaker]=labels.size()+1;
 turns.push_back({a,b,labels[t.speaker]});
 }
 }catch(const std::exception&){turns.clear();diarizationFailed=true;} // Keep ASR available when diarization fails.
 std::ostringstream out;out<<"{\"transcript\":[";bool first=true;
 {
 auto cp=whisper_context_default_params();cp.use_gpu=false;
 std::unique_ptr<whisper_context,decltype(&whisper_free)> ctx(whisper_init_from_file_with_params(m.p,cp),whisper_free);if(!ctx)throw std::runtime_error("Cannot load Whisper model");
 auto params=whisper_full_default_params(WHISPER_SAMPLING_BEAM_SEARCH);params.beam_search.beam_size=5;params.n_threads=std::max(1,std::min(4,int(std::thread::hardware_concurrency())));
 params.language=lang.p;params.translate=false;params.print_progress=false;params.print_realtime=false;params.print_timestamps=false;params.print_special=false;params.no_context=true;params.token_timestamps=true;params.split_on_word=true;params.max_len=80;params.temperature=0;params.temperature_inc=.2f;params.suppress_blank=true;params.suppress_nst=true;params.no_speech_thold=1.01f;params.logprob_thold=-1.f;params.entropy_thold=2.4f;params.max_tokens=0;


 std::vector<PreservedSegment> lines;
 // ASR sees every sample, including regions missed by speaker segmentation.
 for(int64_t a=0;a<int64_t(audio.size());a+=24*16000){
 int64_t b=std::min<int64_t>(audio.size(),a+24*16000);
 if(whisper_full(ctx.get(),params,audio.data()+a,int(b-a)))throw std::runtime_error("Whisper inference failed");
 for(int i=0;i<whisper_full_n_segments(ctx.get());++i){
 std::string original=whisper_full_get_segment_text(ctx.get(),i);
 if(original.find_first_not_of(" \r\n\t")==std::string::npos)continue;
 // Preserve the complete segment text. Confidence, music labels and lack of a
 // speaker match are not grounds for silently deleting a recognized utterance.
 int64_t start=a/16+whisper_full_get_segment_t0(ctx.get(),i)*10,end=a/16+whisper_full_get_segment_t1(ctx.get(),i)*10;
 std::vector<TimedTextPiece> pieces;
 for(int j=0;j<whisper_full_n_tokens(ctx.get(),i);++j){auto td=whisper_full_get_token_data(ctx.get(),i,j);if(td.id>=whisper_token_eot(ctx.get()))continue;
 pieces.push_back({whisper_full_get_token_text(ctx.get(),i,j),td.t0>=0?a/16+td.t0*10:-1,td.t1>=0?a/16+td.t1*10:-1});}
 auto split=splitPreserving(original,pieces,start,end,a/16,b/16,turns);lines.insert(lines.end(),split.begin(),split.end());
 }
 }
 stabilizeDialogue(lines,turns);
 for(auto& line:lines){if(!first)out<<',';first=false;out<<"{\"speaker_id\":\""<<(line.speaker?"speaker_"+std::to_string(line.speaker):"speaker_unknown")<<"\",\"start_ms\":"<<line.start<<",\"end_ms\":"<<line.end<<",\"text\":\""<<escape(line.text)<<"\"}";}

 }
 out<<"],\"diagnostics\":{\"diarization_failed\":"<<(diarizationFailed?"true":"false")<<"}}";auto s=out.str();jbyteArray result=e->NewByteArray(s.size());if(result)e->SetByteArrayRegion(result,0,s.size(),reinterpret_cast<const jbyte*>(s.data()));return result;
 }catch(const std::exception& ex){e->ThrowNew(e->FindClass("java/lang/RuntimeException"),ex.what());return nullptr;}}
