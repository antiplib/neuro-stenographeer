#include "transcript_segment.hpp"
#include <cassert>
#include <iostream>
int main(){
 std::string text=" Да, да, да. Очень тихая речь. [МУЗЫКА]";
 auto a=preserveSegment(text,0,3000,0,24000,{});assert(a.text==text&&a.speaker==0);
 std::vector<VoiceTurn> partial{{16000,32000,2}};
 auto b=preserveSegment(text,4000,7000,0,24000,partial);assert(b.text==text&&b.speaker==0);
 auto c=preserveSegment("Привет",1000,2000,0,24000,partial);assert(c.speaker==2&&c.text=="Привет");
 auto d=preserveSegment(text,24000,24000,0,24000,{});assert(d.text==text&&d.end>d.start);
 auto e=preserveSegment(text,-100,999999,24000,48000,{});assert(e.start==24000&&e.end==48000&&e.text==text);
 std::vector<std::string> input{"Первый", "второй", "третий"};std::string out;for(auto s:input)out+=preserveSegment(s,0,10,0,100,{}).text;assert(out=="Первыйвторойтретий");
 std::cout<<"PASS unknown speaker, missing diarization, complete UTF8 text, repetitions and collapsed timestamps\n";
}
