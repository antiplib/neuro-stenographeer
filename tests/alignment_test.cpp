#include "transcript_segment.hpp"
#include <cassert>
#include <iostream>
int main(){std::vector<VoiceTurn> turns{{0,16000,1},{16000,32000,2}};
 std::vector<TimedTextPiece> p{{"Привет",0,600},{",",600,650},{" как",650,950},{" дела?",1100,1900}};
 auto r=splitPreserving("Привет, как дела?",p,0,2000,0,24000,turns);
 std::string joined;for(auto s:r)joined+=s.text;assert(joined=="Привет, как дела?");assert(r.size()==2&&r[0].speaker==1&&r[1].speaker==2);
 auto bad=p;bad[2].text=" wrong";auto f=splitPreserving("Привет, как дела?",bad,0,2000,0,24000,turns);assert(f.size()==1&&f[0].text=="Привет, как дела?");
 auto unknown=splitPreserving("Привет, как дела?",p,0,2000,0,24000,{});joined.clear();for(auto s:unknown){joined+=s.text;assert(!s.speaker);}assert(joined=="Привет, как дела?");
 assert(!confidentSpeaker(0,16000,{{0,16000,1},{0,16000,2}}));
 assert(confidentSpeaker(0,16000,{{0,8000,1},{0,8000,1},{0,12000,2}})==2); // duplicate spans must not inflate support
 bad=p;bad[0].end=-1;f=splitPreserving("Привет, как дела?",bad,0,2000,0,24000,turns);joined.clear();for(auto line:f)joined+=line.text;assert(joined=="Привет, как дела?");
 std::cout<<"PASS speaker change, ambiguity, overlapping intervals, missing timestamps and byte-exact text preservation\n";
}
