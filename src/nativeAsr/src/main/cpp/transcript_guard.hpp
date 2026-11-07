#pragma once
#include <vector>
#include <string>
#include <algorithm>
#include <cstdint>
#include <cmath>
#include <cctype>
struct VoiceTurn {int64_t start,end;int speaker;};
inline int speakerAt(int64_t a,int64_t b,const std::vector<VoiceTurn>& turns){
 int speaker=0;int64_t best=0;for(auto t:turns){auto overlap=std::max<int64_t>(0,std::min(b,t.end)-std::max(a,t.start));if(overlap>best){best=overlap;speaker=t.speaker;}}return speaker;
}
inline bool hasSpeech(int64_t a,int64_t b,const std::vector<VoiceTurn>& turns){return speakerAt(a,b,turns)!=0;}
inline bool digitalSilence(const float* audio,int64_t n){double energy=0;for(int64_t i=0;i<n;++i)energy+=audio[i]*audio[i];return !n||energy/n<1e-10;}
inline bool annotationOnly(std::string text){
 auto a=text.find_first_not_of(" \r\n\t"),b=text.find_last_not_of(" \r\n\t");if(a==std::string::npos)return true;text=text.substr(a,b-a+1);
 // Do not delete ordinary sentences containing the word music/музыка.
 if(text=="♪"||text=="♫")return true;
 return text.size()>1&&((text.front()=='['&&text.back()==']')||(text.front()=='('&&text.back()==')'));
}
inline bool decodingLoop(const std::vector<int>& tokens){
 if(tokens.size()<12)return false;
 for(size_t period=1;period<=tokens.size()/4;++period){if(period*4<12)continue;
  for(size_t base=0;base+period*4<=tokens.size();++base){bool equal=true;for(size_t j=period;j<period*4;++j)if(tokens[base+j]!=tokens[base+j%period]){equal=false;break;}if(equal)return true;}}
 return false;
}
