#!/usr/bin/env python3
"""Host-only export of official IndicXlit Indic-to-Roman v1.0. Never invoked by Gradle.

Usage: PYTHONPATH=<torch/onnx environment> python scripts/export_indicxlit.py EXTRACTED_CHECKPOINT OUTPUT
Requirements: torch==2.5.1 (CPU), onnx==1.17.0; verification uses onnxruntime==1.20.1.
Only load the trusted official checkpoint: torch.load uses pickle. See licenses/IndicXlit-PROVENANCE.md.
"""
import torch, math, pathlib, json, sys, hashlib
from torch import nn
from torch.nn import functional as F
ROOT=pathlib.Path(sys.argv[1])
checkpoint=ROOT/'transformer/indicxlit.pt'
assert hashlib.sha256(checkpoint.read_bytes()).hexdigest() == '45ffecef88d990ae12f915939b91267a3fc8f69f26331baa59f7932005976f28', 'Untrusted checkpoint'
s=torch.load(checkpoint,map_location='cpu',weights_only=False)['model']
class Part(nn.Module):
 def __init__(self,side):
  super().__init__();self.side=side
  self.w=nn.ParameterDict({k.replace('.','_'):nn.Parameter(v.float(),requires_grad=False) for k,v in s.items() if k.startswith(side+'.') and not k.endswith('version') and not '_float_tensor' in k})
  half=128; freq=torch.exp(torch.arange(half)*(-math.log(10000)/(half-1)))
  phases=torch.arange(1026).unsqueeze(1)*freq.unsqueeze(0)
  pos=torch.cat([torch.sin(phases),torch.cos(phases)],1);pos[1]=0
  self.register_buffer('positions',pos)
 def get(self,k):return self.w[(self.side+'.'+k).replace('.','_')]
 def linear(self,x,k):return F.linear(x,self.get(k+'.weight'),self.get(k+'.bias'))
 def norm(self,x,k):return F.layer_norm(x,(256,),self.get(k+'.weight'),self.get(k+'.bias'),1e-5)
 def embed(self,ids):
  positions=(ids.ne(1).long().cumsum(1)*ids.ne(1).long())+1
  return self.norm(F.embedding(ids,self.get('embed_tokens.weight'))*16+F.embedding(positions,self.positions),'layernorm_embedding')
 def attn(self,q,k,v,name,causal=False):
  q=self.linear(q,name+'.q_proj');k=self.linear(k,name+'.k_proj');v=self.linear(v,name+'.v_proj')
  b,t,_=q.shape; sl=k.shape[1]
  q=q.reshape(b,t,4,64).transpose(1,2)*0.125
  k=k.reshape(b,sl,4,64).transpose(1,2);v=v.reshape(b,sl,4,64).transpose(1,2)
  scores=q@k.transpose(-1,-2)
  if causal:
   mask=torch.arange(t).unsqueeze(0)>torch.arange(t).unsqueeze(1)
   scores=scores.masked_fill(mask,float('-inf'))
  y=(scores.softmax(-1)@v).transpose(1,2).reshape(b,t,256)
  return self.linear(y,name+'.out_proj')
class Encoder(Part):
 def __init__(self):super().__init__('encoder')
 def forward(self,ids):
  x=self.embed(ids)
  for i in range(6):
   n=f'layers.{i}';z=self.norm(x,n+'.self_attn_layer_norm');x=x+self.attn(z,z,z,n+'.self_attn')
   x=x+self.linear(F.gelu(self.linear(self.norm(x,n+'.final_layer_norm'),n+'.fc1')),n+'.fc2')
  return self.norm(x,'layer_norm')
class Decoder(Part):
 def __init__(self):super().__init__('decoder')
 def forward(self,ids,memory):
  x=self.embed(ids)
  for i in range(6):
   n=f'layers.{i}';z=self.norm(x,n+'.self_attn_layer_norm');x=x+self.attn(z,z,z,n+'.self_attn',True)
   x=x+self.attn(self.norm(x,n+'.encoder_attn_layer_norm'),memory,memory,n+'.encoder_attn')
   x=x+self.linear(F.gelu(self.linear(self.norm(x,n+'.final_layer_norm'),n+'.fc1')),n+'.fc2')
  return F.linear(self.norm(x,'layer_norm')[:,-1,:],self.get('output_projection.weight'))
enc=Encoder().eval();dec=Decoder().eval();torch.set_num_threads(2)
def vocab(path):return ['<s>','<pad>','</s>','<unk>']+[x.rsplit(' ',1)[0] for x in path.read_text().splitlines()]
src=vocab(ROOT/'corpus-bin/dict.hi.txt');tgt=vocab(ROOT/'corpus-bin/dict.en.txt')
# Fairseq multilingual task appends language symbols to each dictionary.
langs=['en','as','bn','brx','gom','gu','hi','kn','ks','mai','ml','mni','mr','ne','or','pa','sa','sd','si','ta','te','ur']
src+=['__'+x+'__' for x in langs];tgt+=['__'+x+'__' for x in langs]
print('vocab',len(src),len(tgt),s['encoder.embed_tokens.weight'].shape,s['decoder.embed_tokens.weight'].shape)
assert len(src)==806 and len(tgt)==54
out=pathlib.Path(sys.argv[2]);out.mkdir(parents=True,exist_ok=True)
for name, words in [('source',src),('target',tgt)]:
 (out/(name+'.txt')).write_text('\n'.join(words)+'\n')
(out/'vocab.json').write_text(json.dumps({'source':src,'target':tgt,'language':'__hi__','pad':1,'eos':2,'unk':3},ensure_ascii=False))
with torch.no_grad():
 for word in ['मैं','घर','जा','रहा','हूँ','में','बस','बेबी','नोटबुक','चाहिए','देन','आइल','कॉल','यू']:
  ids=torch.tensor([[src.index('__hi__')]+[src.index(c) if c in src else 3 for c in word]+[2]])
  mem=enc(ids);seq=[2]
  for _ in range(40):
   logits=dec(torch.tensor([seq]),mem);logits[:,[0,1,3]]=float('-inf');idx=logits.argmax(-1).item()
   if idx==2:break
   seq.append(idx)
  print(word,''.join(tgt[x] for x in seq[1:]))
 ids=torch.tensor([[src.index('__hi__'),20,25,2]]);mem=enc(ids)
 torch.onnx.export(enc,(ids,),str(out/'encoder.onnx'),input_names=['tokens'],output_names=['memory'],dynamic_axes={'tokens':{0:'batch',1:'source'},'memory':{0:'batch',1:'source'}},opset_version=17)
 torch.onnx.export(dec,(torch.tensor([[2,4]]),mem),str(out/'decoder.onnx'),input_names=['tokens','memory'],output_names=['logits'],dynamic_axes={'tokens':{0:'batch',1:'target'},'memory':{0:'batch',1:'source'},'logits':{0:'batch'}},opset_version=17)

# Verify exported FP32 tensors against eager inference for several dynamic word/prefix lengths.
import numpy as np
import onnxruntime as ort
options=ort.SessionOptions();options.intra_op_num_threads=2
encoder=ort.InferenceSession(str(out/'encoder.onnx'),options)
decoder=ort.InferenceSession(str(out/'decoder.onnx'),options)
with torch.no_grad():
 for word in ['मैं','घर','नोटबुक','चाहिए']:
  ids=torch.tensor([[src.index('__hi__')]+[src.index(c) for c in word]+[2]])
  reference=enc(ids);actual=encoder.run(None,{'tokens':ids.numpy()})[0]
  np.testing.assert_allclose(actual,reference.numpy(),atol=2e-4,rtol=2e-4)
  for prefix in [[2],[2,4],[2,4,5,6,7]]:
   tokens=torch.tensor([prefix]);reference_logits=dec(tokens,reference).numpy()
   actual_logits=decoder.run(None,{'tokens':tokens.numpy(),'memory':actual})[0]
   np.testing.assert_allclose(actual_logits,reference_logits,atol=2e-4,rtol=2e-4)
print('ONNX/eager FP32 parity: four words, three decoder prefix lengths PASS')
