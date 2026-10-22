#!/usr/bin/env python3
"""Exercise KB6/KB8 on this Compose project only; always restore services."""
import json
import os
from pathlib import Path
import subprocess
import time
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
os.chdir(ROOT)
OUT = ROOT / 'evidence'
OUT.mkdir(exist_ok=True)

def compose(*args, delay='0'):
    subprocess.run(['docker', 'compose', *args], check=True,
                   env={**os.environ, 'PAYMENT_DELAY_MS': delay})

def get(path, port=8081):
    with urllib.request.urlopen(f'http://localhost:{port}{path}', timeout=5) as response:
        return json.load(response)

def post(path, port):
    with urllib.request.urlopen(urllib.request.Request(
            f'http://localhost:{port}{path}', data=b'', method='POST'), timeout=10) as response:
        return response.read().decode()

def runner(folder, filename):
    return [str(ROOT / 'node_modules/.bin/newman'), 'run',
            'postman/Saga-outages.postman_collection.json',
            '-e', 'postman/Saga-local.postman_environment.json', '--folder', folder,
            '--timeout-request', '40000', '--reporters', 'cli,json',
            '--reporter-json-export', str(OUT / filename)]

wallets_before = get('/api/payments/wallets', 8083)
try:
    compose('stop', 'payment-service')
    subprocess.run(runner('KB6 - payment offline', 'kb6-newman.json'), check=True)
finally:
    compose('start', 'payment-service')
    compose('up', '-d', '--wait')
wallets_after = get('/api/payments/wallets', 8083)
assert sorted(wallets_before, key=lambda w: w['customerId']) == sorted(wallets_after, key=lambda w: w['customerId'])
(OUT / 'kb6-wallets.json').write_text(json.dumps({'before': wallets_before, 'after': wallets_after}, indent=2) + '\n')

process = None
stuck_id = None
try:
    compose('up', '-d', '--wait', 'order-service', delay='15000')
    known = {order['id'] for order in get('/api/orders')}
    before = {p['id']: p['stock'] for p in get('/api/inventory/products', 8082)}
    process = subprocess.Popen(runner('KB8 - compensation offline', 'kb8-newman.json'))
    deadline = time.monotonic() + 35
    while time.monotonic() < deadline:
        for order in get('/api/orders'):
            if order['id'] not in known:
                logs = get(f"/api/orders/{order['id']}/saga-logs")
                if any(log['step'] == 'RESERVE_INVENTORY' for log in logs):
                    stuck_id = order['id']
                    break
        if stuck_id is not None:
            break
        time.sleep(0.2)
    if stuck_id is None:
        raise RuntimeError('KB8 did not reach RESERVE_INVENTORY before deadline')
    compose('stop', '-t', '1', 'inventory-service', 'payment-service')
    code = process.wait(timeout=45)
    if code:
        raise RuntimeError(f'KB8 Newman failed: {code}')
    logs = get(f'/api/orders/{stuck_id}/saga-logs')
    assert len([l for l in logs if l['step'] == 'COMPENSATION' and l['result'] == 'FAILED']) == 3
    (OUT / 'kb8-saga-logs.json').write_text(json.dumps(logs, ensure_ascii=False, indent=2) + '\n')
finally:
    if process is not None and process.poll() is None:
        process.terminate()
        process.wait(timeout=10)
    compose('start', 'inventory-service', 'payment-service')
    compose('up', '-d', '--wait', delay='0')
    if stuck_id is not None:
        after = {p['id']: p['stock'] for p in get('/api/inventory/products', 8082)}
        assert after['P2'] == before['P2'] - 1, (before, after)
        # Explicit test cleanup. The Saga remains COMPENSATING: no recovery job is implemented.
        post(f'/api/inventory/release/{stuck_id}', 8082)
        cleaned = {p['id']: p['stock'] for p in get('/api/inventory/products', 8082)}
        assert cleaned == before, (before, cleaned)
        (OUT / 'kb8-stock.json').write_text(json.dumps({
            'orderId': stuck_id, 'before': before, 'stuck': after,
            'afterManualRelease': cleaned,
            'note': 'Inventory restored by manual idempotent release; order still COMPENSATING.'
        }, ensure_ascii=False, indent=2) + '\n')
print('KB6 and KB8 passed; all services restored, delay=0.')
