#include "transcript_segment.hpp"
#include <cassert>
#include <iostream>
std::string text(const std::vector<PreservedSegment>& a){std::string s;for(auto x:a)s+=x.text;return s;}
int main(){std::vector<PreservedSegment> x{{0,500,1,"Привет"},{550,800,0,", да"},{850,1200,1," ещё"},{1250,1500,2," ответ"}};auto before=text(x);stabilizeDialogue(x,{});assert(text(x)==before&&x.size()==2&&x[0].speaker==1);
 x={{0,500,1,"A"},{550,800,0,"?"},{850,1200,1,"A"}};stabilizeDialogue(x,{{550*16,800*16,2}});assert(x.size()==3&&x[1].speaker==0);
 x={{0,500,1,"A"},{550,600,2,"Да"},{650,900,1,"A"}};stabilizeDialogue(x,{});assert(x.size()==3&&x[1].speaker==2);
 x={{0,500,1,"A"},{4000,5000,1,"B"}};stabilizeDialogue(x,{});assert(x.size()==2);
 std::cout<<"PASS paragraph merging, brief real interjections, conflicting voice evidence and text preservation\n";
}
