#pragma once
#include "transcript_guard.hpp"
#include <map>
struct PreservedSegment {int64_t start,end;int speaker;std::string text;};
inline PreservedSegment preserveSegment(std::string text,int64_t start,int64_t end,int64_t windowStart,int64_t windowEnd,const std::vector<VoiceTurn>& turns){
 // Missing or collapsed timestamps must not erase recognized text.
 start=std::clamp(start,windowStart,windowEnd);end=std::clamp(end,start,windowEnd);
 if(end<=start){start=windowStart;end=windowEnd;}
 return {start,end,speakerAt(start*16,end*16,turns),std::move(text)};
}

struct TimedTextPiece {std::string text;int64_t start,end;};
// Conservative assignment: ambiguous overlap is unknown, never a reason to discard words.
inline int confidentSpeaker(int64_t start,int64_t end,const std::vector<VoiceTurn>& turns,double coverage=.55,double margin=.20){
 std::map<int,std::vector<std::pair<int64_t,int64_t>>> regions;
 for(auto t:turns){auto a=std::max(start,t.start),b=std::min(end,t.end);if(b>a)regions[t.speaker].push_back({a,b});}
 int id=0;int64_t first=0,second=0;
 for(auto& entry:regions){auto& v=entry.second;std::sort(v.begin(),v.end());int64_t total=0,a=v[0].first,b=v[0].second;
  for(size_t i=1;i<v.size();++i){if(v[i].first<=b)b=std::max(b,v[i].second);else{total+=b-a;a=v[i].first;b=v[i].second;}}total+=b-a;
  if(total>first){second=first;first=total;id=entry.first;}else second=std::max(second,total);
 }
 auto length=end-start;if(length<=0||first<length*coverage||first-second<length*margin)return 0;return id;
}
// Repair only missing runs bounded by real token timestamps. No all-invented alignment.
inline std::vector<TimedTextPiece> repairTokenTimes(const std::vector<TimedTextPiece>& input,int64_t start,int64_t end){
 auto out=input;auto valid=[&](size_t i){return out[i].start>=start&&out[i].end>=out[i].start&&out[i].end<=end;};
 bool any=false;for(size_t i=0;i<out.size();++i)any=any||valid(i);if(!any)return out;
 for(size_t i=0;i<out.size();){if(valid(i)){++i;continue;}size_t first=i;while(i<out.size()&&!valid(i))++i;
  int64_t left=first?out[first-1].end:start,right=i<out.size()?out[i].start:end;if(right<=left)continue;
  size_t total=0;for(size_t j=first;j<i;++j)for(unsigned char c:out[j].text)if((c&0xc0)!=0x80)++total;
  if(!total)continue;size_t done=0;
  for(size_t j=first;j<i;++j){size_t size=0;for(unsigned char c:out[j].text)if((c&0xc0)!=0x80)++size;out[j].start=left+(right-left)*done/total;done+=size;out[j].end=left+(right-left)*done/total;}
 }return out;
}
inline std::vector<PreservedSegment> splitPreserving(const std::string& original,const std::vector<TimedTextPiece>& pieces,int64_t start,int64_t end,int64_t windowStart,int64_t windowEnd,const std::vector<VoiceTurn>& turns){
 auto fallback=preserveSegment(original,start,end,windowStart,windowEnd,turns);
 fallback.speaker=confidentSpeaker(fallback.start*16,fallback.end*16,turns,.85,.70);
 std::string joined;for(auto& p:pieces)joined+=p.text;
 if(joined!=original||pieces.empty())return {fallback};
 std::vector<PreservedSegment> result;std::string word;int64_t a=0,b=0;bool valid=true;
 auto flush=[&]{if(word.empty())return;
  if(a<start||b>end||b<=a){valid=false;return;}
  int speaker=confidentSpeaker(a*16,b*16,turns);
  if(!result.empty()&&result.back().speaker==speaker&&a>=result.back().start&&a-result.back().end<=800){result.back().text+=word;result.back().end=std::max(result.back().end,b);}
  else result.push_back({a,b,speaker,word});word.clear();};
 for(auto p:repairTokenTimes(pieces,start,end)){if(p.start<start||p.end<p.start||p.end>end)valid=false;if(p.text.empty())continue;if(std::isspace(static_cast<unsigned char>(p.text[0]))&&!word.empty())flush();
  if(word.empty()){a=p.start;b=p.end;}else b=std::max(b,p.end);word+=p.text;
 }flush();
 joined.clear();for(auto& r:result)joined+=r.text;
 if(!valid||joined!=original)return {fallback};
 return result;
}

inline void stabilizeDialogue(std::vector<PreservedSegment>& lines,const std::vector<VoiceTurn>& turns){
 // Fill only a short unassigned bridge between the same confirmed speaker.
 // Never relabel a confirmed interjection or overwrite evidence of another voice.
 for(size_t i=1;i+1<lines.size();++i){auto& x=lines[i];auto& prev=lines[i-1];auto& next=lines[i+1];
  if(x.speaker||!prev.speaker||prev.speaker!=next.speaker||x.end-x.start>600||x.start-prev.end>400||next.start-x.end>400)continue;
  bool conflict=false;for(auto t:turns)if(t.speaker!=prev.speaker&&std::min(x.end*16,t.end)>std::max(x.start*16,t.start))conflict=true;
  if(!conflict)x.speaker=prev.speaker;
 }
 std::vector<PreservedSegment> merged;
 for(auto x:lines){if(!merged.empty()&&merged.back().speaker==x.speaker&&x.start>=merged.back().start&&x.start-merged.back().end<=1800){merged.back().text+=x.text;merged.back().end=std::max(merged.back().end,x.end);}else merged.push_back(std::move(x));}
 lines=std::move(merged);
}
