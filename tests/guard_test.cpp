#include "transcript_guard.hpp"
#include <cassert>
#include <iostream>
int main(){std::vector<VoiceTurn> turns{{0,2000,1},{1000,3000,2},{4000,5000,1}};
assert(speakerAt(0,500,turns)==1);assert(speakerAt(2100,2500,turns)==2);assert(!hasSpeech(3100,3900,turns));
float silence[100]{};assert(digitalSilence(silence,100));silence[0]=.1;assert(!digitalSilence(silence,100));
assert(annotationOnly(" [МУЗЫКА] "));assert(annotationOnly("(Music)"));assert(!annotationOnly("Мне нравится музыка"));
assert(decodingLoop({1,2,3,1,2,3,1,2,3,1,2,3}));assert(!decodingLoop({1,2,3,1,2,3}));
int calls=0;for(int a=0;a<5000;a+=2000)if(hasSpeech(a,std::min(a+2000,5000),turns))++calls;assert(calls==3); // overlap does not duplicate windows
std::cout<<"PASS speech gates, annotations, overlap and repetition detection\n";}
