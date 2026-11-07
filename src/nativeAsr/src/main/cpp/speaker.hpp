#pragma once
#include <vector>
#include <array>
#include <cmath>
#include <algorithm>
#include <stdexcept>
// Replace this interface with a neural voice encoder; clustering stays independent.
struct SpeakerEncoder { virtual std::vector<float> encode(const float* pcm,int samples)=0; virtual ~SpeakerEncoder()=default; };
struct SpectralEncoder:SpeakerEncoder {
 std::vector<float> encode(const float* pcm,int n)override{
  // Baseline: mean log spectral bands, 25 ms Hann windows, stride 20 ms.
  static const auto hann=[] {std::array<double,400> a{};for(int i=0;i<400;++i)a[i]=.5-.5*std::cos(2*3.141592653589793*i/399);return a;}();
  std::vector<float> out(24,0);int frames=0;
  for(int at=0;at+400<=n;at+=320){double energy=0;for(int i=0;i<400;++i)energy+=pcm[at+i]*pcm[at+i];if(energy/400<1e-6)continue;
   for(int k=0;k<24;++k){double hz=100*std::pow(60.0,k/23.0),re=0,im=0;
    double omega=2*3.141592653589793*hz/16000,c=std::cos(omega),s=std::sin(omega),rc=1,rs=0;
    for(int i=0;i<400;++i){double sample=pcm[at+i]*hann[i];re+=sample*rc;im+=sample*rs;double next=rc*c-rs*s;rs=rs*c+rc*s;rc=next;}
    out[k]+=std::log(1e-10+re*re+im*im);}
   ++frames;
  }
  if(!frames)return {};double mean=0;for(auto& x:out){x/=frames;mean+=x;}mean/=out.size();double norm=0;for(auto& x:out){x-=mean;norm+=x*x;}if(norm<1e-8)return {};for(auto& x:out)x/=std::sqrt(norm);return out;
 }
};
struct SpeakerCluster {
 std::vector<std::vector<float>> centers;std::vector<int> counts;
 int assign(const std::vector<float>& v){if(v.empty())return -1;float best=2;int id=-1;
  for(size_t c=0;c<centers.size();++c){if(centers[c].size()!=v.size())throw std::runtime_error("Embedding dimension mismatch");double dot=0;for(size_t i=0;i<v.size();++i)dot+=v[i]*centers[c][i];float dist=1-dot;if(dist<best){best=dist;id=int(c);}}
  if(id<0||best>.18){if(centers.size()>=8)return -1;centers.push_back(v);counts.push_back(1);return int(centers.size())-1;}
  double norm=0;for(size_t i=0;i<v.size();++i){centers[id][i]=(centers[id][i]*counts[id]+v[i])/(counts[id]+1);norm+=centers[id][i]*centers[id][i];}if(norm>1e-8)for(auto& x:centers[id])x/=std::sqrt(norm);++counts[id];return id;
 }
};
