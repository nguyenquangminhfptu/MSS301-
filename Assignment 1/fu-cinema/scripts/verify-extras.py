#!/usr/bin/env python3
"""Live regression checks. Run after the Postman collection and start.sh.

Uses only assignment-owned processes. Restores movie-service after the BR14 test.
"""
from pathlib import Path
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timedelta
from zoneinfo import ZoneInfo
import urllib.request,urllib.error,json,time,os,signal,subprocess,base64,hmac,hashlib,threading

root=Path(__file__).resolve().parent.parent
env=json.loads((root/'postman/run-environment.json').read_text())
values={x['key']:x['value'] for x in env['values']}
gateway=values['gateway'];results=[]
def request(method,path,token=None,body=None):
    headers={'Content-Type':'application/json'}
    if token:headers['Authorization']='Bearer '+token
    req=urllib.request.Request(gateway+path,data=json.dumps(body).encode() if body is not None else None,headers=headers,method=method)
    try:
        with urllib.request.urlopen(req,timeout=15) as response:return response.status,json.loads(response.read() or b'null')
    except urllib.error.HTTPError as error:return error.code,json.loads(error.read() or b'null')
def check(name,condition,detail):
    results.append({'name':name,'passed':bool(condition),'detail':detail})
    print(('PASS' if condition else 'FAIL')+' '+name+': '+json.dumps(detail,ensure_ascii=False),flush=True)
    assert condition,name
def jwt(claims,key):
    def enc(obj):return base64.urlsafe_b64encode(json.dumps(obj,separators=(',',':')).encode()).rstrip(b'=')
    message=enc({'alg':'HS256','typ':'JWT'})+b'.'+enc(claims)
    signature=base64.urlsafe_b64encode(hmac.new(key.encode(),message,hashlib.sha256).digest()).rstrip(b'=')
    return (message+b'.'+signature).decode()
secret='fu-cinema-booking-system-secret-key-2026-mss301'
admin=values['adminToken'];customer=values['customerToken'];other=values['customer2Token'];showtime=values['showtimeId']
now=int(time.time());claims={'iss':'fu-cinema','sub':'an@gmail.com','uid':1,'role':'CUSTOMER','iat':now-7200,'exp':now-3600}
status,_=request('GET','/api/customers/me',jwt(claims,secret));check('Expired JWT returns 401',status==401,{'status':status})
claims['iat']=now;claims['exp']=now+3600
status,_=request('GET','/api/customers/me',jwt(claims,'different-secret-with-at-least-32-bytes'));check('Wrong JWT signature returns 401',status==401,{'status':status})
status,_=request('POST','/api/bookings',customer,{'items':[{'showtimeId':showtime,'seatCode':'B2'}],'totalPrice':1});check('Client supplied price rejected',status==400,{'status':status})

barrier=threading.Barrier(2)
def buy(token):
    barrier.wait()
    return request('POST','/api/bookings',token,{'items':[{'showtimeId':showtime,'seatCode':'B3'}]})
with ThreadPoolExecutor(max_workers=2) as pool:
    futures=[pool.submit(buy,token) for token in [customer,other]]
    answers=[f.result() for f in futures]
statuses=sorted(x[0] for x in answers)
check('Concurrent seat purchase has one winner',statuses==[201,409],{'statuses':statuses})
winner=next(body['bookingId'] for status,body in answers if status==201)
status,_=request('PUT',f'/api/bookings/{winner}/cancel',admin);check('Admin cancels concurrent winning booking',status==200,{'status':status})
status,seatmap=request('GET',f'/api/bookings/showtimes/{showtime}/seats');check('Cancelled concurrent booking releases seat','B3' not in seatmap['bookedSeats'],{'bookedSeats':seatmap['bookedSeats']})

local=datetime.now(ZoneInfo('Asia/Ho_Chi_Minh')).replace(tzinfo=None,microsecond=0)
status,near=request('POST','/api/showtimes',admin,{'movieId':values['movieId'],'roomId':values['roomId'],'startTime':(local+timedelta(minutes=90)).isoformat(),'ticketPrice':95000})
assert status==201,(status,near)
status,b=request('POST','/api/bookings',customer,{'items':[{'showtimeId':near['showtimeId'],'seatCode':'B4'}]});assert status==201,(status,b)
status,_=request('PUT',f"/api/bookings/{b['bookingId']}/cancel",customer);check('Customer cancellation inside two hours denied',status==400,{'status':status})
status,_=request('PUT',f"/api/bookings/{b['bookingId']}/cancel",admin);check('Admin overrides cancellation deadline',status==200,{'status':status})
request('DELETE',f"/api/showtimes/{near['showtimeId']}",admin)

status,second=request('POST','/api/showtimes',admin,{'movieId':'66f200000000000000000001','roomId':'66f100000000000000000003','startTime':(local+timedelta(days=30)).isoformat(),'ticketPrice':150000});assert status==201,(status,second)
status,b=request('POST','/api/bookings',customer,{'items':[{'showtimeId':second['showtimeId'],'seatCode':'A8'}]});assert status==201,(status,b)
day=local.date().isoformat();status,report=request('GET',f'/api/bookings/report?startDate={day}&endDate={day}',admin)
revenues=[x['revenue'] for x in report['revenueByMovie']]
check('Report sorts multiple movie revenues descending',len(revenues)>=2 and revenues==sorted(revenues,reverse=True),{'revenues':revenues,'totalRevenue':report['totalRevenue']})
request('PUT',f"/api/bookings/{b['bookingId']}/cancel",admin);request('DELETE',f"/api/showtimes/{second['showtimeId']}",admin)

pid=int((root/'.run/movie-service.pid').read_text());os.kill(pid,signal.SIGTERM)
for _ in range(30):
    try:
        urllib.request.urlopen('http://localhost:8082/api/genres',timeout=1);time.sleep(.2)
    except urllib.error.URLError:break
try:
    status,error=request('POST','/api/bookings',customer,{'items':[{'showtimeId':showtime,'seatCode':'B2'}]})
    check('Movie Service unavailable returns 503',status==503,{'status':status,'message':error.get('message')})
finally:
    java_home=os.environ.get('JAVA_HOME')
    java=str(Path(java_home)/'bin/java') if java_home else 'java'
    log=(root/'.run/movie-service.log').open('a')
    proc=subprocess.Popen([java,'-Duser.timezone=Asia/Ho_Chi_Minh','-jar',str(root/'movie-service/target/movie-service-0.0.1-SNAPSHOT.jar'),'--logging.level.root=INFO'],cwd=root,stdout=log,stderr=subprocess.STDOUT)
    (root/'.run/movie-service.pid').write_text(str(proc.pid))
    for _ in range(120):
        try:
            with urllib.request.urlopen('http://localhost:8082/api/genres',timeout=1):break
        except urllib.error.URLError:time.sleep(.5)
    else:raise RuntimeError('Movie Service failed to restart')

status,_=request('GET','/api/genres');check('Movie Service restored after outage',status==200,{'status':status})
(root/'evidence/extra-checks.json').write_text(json.dumps(results,ensure_ascii=False,indent=2)+'\n')
# The automation process owns the restored service until Ctrl+C. All results are already saved.
if '--hold' in __import__('sys').argv:
    print('Holding restored Movie Service open',flush=True)
    try:proc.wait()
    except KeyboardInterrupt:proc.terminate()
