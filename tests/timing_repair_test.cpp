#include "transcript_segment.hpp"
#include <cassert>
#include <iostream>
int main(){
 std::vector<TimedTextPiece> p{{"Первый",0,600},{",",-1,-1},{" второй",800,1600},{"!",-1,-1}};
 auto repaired=repairTokenTimes(p,0,1700);assert(repaired[1].start==600&&repaired[1].end==800);assert(repaired[3].start==1600&&repaired[3].end==1700);assert(repaired[0].text==p[0].text);
 auto x=splitPreserving("Первый, второй!",p,0,1700,0,24000,{{0,800*16,1},{800*16,1700*16,2}});std::string text;for(auto r:x)text+=r.text;assert(text=="Первый, второй!"&&x.size()==2&&x[0].speaker==1&&x[1].speaker==2);
 auto missing=repairTokenTimes({{"все",-1,-1},{" слова",-1,-1}},0,1700);assert(missing[0].start==-1);
 auto full=repairTokenTimes({{"слово",0,500}},0,1700);assert(full[0].start==0&&full[0].end==500);
 std::cout<<"PASS bounded missing timestamp repair, two speakers and exact UTF8 preservation\n";
}
