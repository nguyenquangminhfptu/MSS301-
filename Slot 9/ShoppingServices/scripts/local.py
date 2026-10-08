#!/usr/bin/env python3
"""Start/stop only this workspace's Java services; Docker data is preserved."""
import json
import os
from pathlib import Path
import signal
import socket
import subprocess
import sys
import time
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
RUN = ROOT / '.run'
SERVICES = {'product-service': 8080, 'inventory-service': 8082,
            'order-service': 8081, 'api-gateway': 9000}
STATE = RUN / 'processes.json'


def stop():
    if STATE.exists():
        for service, pid in json.loads(STATE.read_text()).items():
            command = subprocess.run(['ps', '-p', str(pid), '-o', 'command='],
                                     capture_output=True, text=True).stdout
            if str(ROOT / service / 'target') in command:
                os.kill(pid, signal.SIGTERM)
                print(f'Stopped {service} ({pid})')
        STATE.unlink()


def wait_url(url, seconds=120):
    for _ in range(seconds):
        try:
            with urllib.request.urlopen(url, timeout=2) as response:
                if response.status == 200:
                    return
        except (OSError, TimeoutError):
            time.sleep(1)
    raise RuntimeError(f'Timed out waiting for {url}; inspect .run/*.log')


def start():
    if STATE.exists():
        raise RuntimeError('Run scripts/local.py stop before starting again')
    for port in [*SERVICES.values(), 8181, 3339, 27029]:
        with socket.socket() as sock:
            if sock.connect_ex(('127.0.0.1', port)) == 0:
                # Infrastructure may already be running from this compose.
                if port in SERVICES.values():
                    raise RuntimeError(f'Port {port} is already occupied')
    env = os.environ.copy()
    if not env.get('JAVA_HOME') and Path('/usr/libexec/java_home').exists():
        env['JAVA_HOME'] = subprocess.check_output(
            ['/usr/libexec/java_home', '-v', '21'], text=True).strip()
    java = str(Path(env['JAVA_HOME']) / 'bin/java') if env.get('JAVA_HOME') else 'java'
    RUN.mkdir(exist_ok=True)
    for service in SERVICES:
        with (RUN / f'{service}-build.log').open('w') as log:
            subprocess.run(['sh', './mvnw', '-B', '-ntp', '-DskipTests', 'package'],
                           cwd=ROOT / service, env=env, stdout=log,
                           stderr=subprocess.STDOUT, check=True)
        print(f'Built {service}', flush=True)
    subprocess.run(['docker', 'compose', 'up', '-d', '--wait'], cwd=ROOT, check=True)
    wait_url('http://localhost:8181/realms/spring-microservices-realm/.well-known/openid-configuration')
    processes = {}
    try:
        for service, port in SERVICES.items():
            jar = ROOT / service / 'target' / f'{service}-0.0.1-SNAPSHOT.jar'
            with (RUN / f'{service}.log').open('w') as log:
                process = subprocess.Popen([java, '-jar', str(jar),
                                            '--spring.profiles.active=local'],
                                           cwd=ROOT, env=env, stdout=log,
                                           stderr=subprocess.STDOUT, start_new_session=True)
            processes[service] = process.pid
            STATE.write_text(json.dumps(processes))
            path = '/v3/api-docs/swagger-config' if service == 'api-gateway' else '/api-docs'
            wait_url(f'http://localhost:{port}{path}')
            print(f'Ready: {service} on {port}', flush=True)
    except BaseException:
        stop()
        raise
    print('Swagger UI: http://localhost:9000/swagger-ui.html')


if __name__ == '__main__':
    action = sys.argv[1] if len(sys.argv) == 2 else ''
    if action == 'start':
        start()
    elif action == 'stop':
        stop()
    else:
        sys.exit('Usage: python3 scripts/local.py start|stop')
