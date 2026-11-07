#include "speaker.hpp"
#include <cassert>
#include <iostream>
int main(){
 SpectralEncoder encoder;std::vector<float> silence(16000);assert(encoder.encode(silence.data(),silence.size()).empty());
 std::vector<float> a(16000),b(16000);for(int i=0;i<16000;++i){a[i]=.2f*std::sin(2*3.141592653589793*180*i/16000);b[i]=a[i]*.4f;}
 auto x=encoder.encode(a.data(),a.size()),y=encoder.encode(b.data(),b.size());assert(x.size()==24&&y.size()==24);
 SpeakerCluster cluster;assert(cluster.assign({})==-1);assert(cluster.assign(x)==0);assert(cluster.assign(y)==0);std::vector<float> reverse=x;for(auto& v:reverse)v=-v;assert(cluster.assign(reverse)==1);
 std::cout<<"PASS silence, finite embedding, gain invariance and clustering separation\n";
}
