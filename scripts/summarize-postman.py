#!/usr/bin/env python3
"""Create shareable API evidence without exporting live access tokens."""
from pathlib import Path
import json,sys
root=Path(__file__).resolve().parent.parent
source=Path(sys.argv[1]) if len(sys.argv)>1 else root/'.run/newman-raw.json'
data=json.loads(source.read_text())
def sanitize(obj):
    if isinstance(obj,dict):return {k:('[redacted]' if k.lower() in ['password','accesstoken'] else sanitize(v)) for k,v in obj.items()}
    if isinstance(obj,list):return [sanitize(v) for v in obj]
    return obj
executions=[]
for x in data['run']['executions']:
    response=x.get('response') or {}
    stream=response.get('stream',{}).get('data',[])
    try:body=json.loads(bytes(stream).decode())
    except Exception:body=None
    executions.append({'name':x['item']['name'],'method':x['request']['method'],'status':response.get('code'),
        'responseTimeMs':response.get('responseTime'),'body':sanitize(body),
        'assertions':[{'name':a['assertion'],'passed':'error' not in a} for a in x.get('assertions',[])]})
summary={'stats':data['run']['stats'],'timings':data['run']['timings'],'failureCount':len(data['run']['failures']),'executions':executions}
target=root/'evidence/newman-summary.json';target.write_text(json.dumps(summary,ensure_ascii=False,indent=2)+'\n')
print('Requests:',summary['stats']['requests']['total'],'Assertions:',summary['stats']['assertions']['total'],'Failures:',summary['failureCount'])
