#!/usr/bin/env python3
"""Exercise the three running services through the gateway using fresh test records."""
import json
import time
from pathlib import Path
from urllib.error import HTTPError
from urllib.request import Request, urlopen

BASE = 'http://localhost:8080'
checks = []

def call(method, path, expected_http=200, expected_status=1, body=None):
    data = None if body is None else json.dumps(body).encode()
    request = Request(BASE + path, data=data, method=method, headers={'Content-Type': 'application/json'})
    try:
        response = urlopen(request, timeout=15)
    except HTTPError as error:
        response = error
    payload = json.loads(response.read())
    assert response.code == expected_http, (method, path, response.code, payload)
    assert payload['status'] == expected_status, (method, path, payload)
    assert payload['timestamp'].endswith('Z'), payload
    checks.append(f'{method} {path}: HTTP {response.code}, status {payload["status"]}')
    return payload.get('data')

username = f'slot16_test_{int(time.time())}'
user_body = {'username': username, 'email': 'test@example.com', 'role': 'CREATOR',
             'ratePerPost': 500000, 'maxDailyPosts': 3, 'level': 1}
user = call('POST', '/api/users', 201, body=user_body)
assert user['status'] == 'ACTIVE'
user_id = user['userId']
call('POST', '/api/users', 226, 3, user_body)
call('POST', '/api/users', 406, 2, {**user_body, 'username': username + 'x', 'maxDailyPosts': 11})
call('PUT', f'/api/users/{user_id}', body={'level': 2})
call('GET', f'/api/users/{user_id}')
users = call('GET', '/api/users?role=CREATOR&status=ACTIVE&size=100')
assert any(row['userId'] == user_id for row in users['content'])
call('GET', '/api/users?size=101', 406, 2)
call('GET', '/api/users/2147483647', 404, 4)

post_body = {'advertiserName': 'Slot 16 kiểm thử', 'advertiserEmail': 'ads@example.com',
             'advertiserPhone': '0900000000', 'startDate': '25/07/2026', 'endDate': '28/07/2026',
             'numberOfPosts': 2, 'userId': user_id}
campaign = call('POST', '/api/posts', 201, body=post_body)
assert campaign['totalCost'] == 1500000 and campaign['status'] == 'SCHEDULED'
post_id = campaign['postId']
detail = call('GET', f'/api/posts/{post_id}')
assert detail['startDate'] == '25/07/2026' and detail['endDate'] == '28/07/2026'
assert detail['user']['userId'] == user_id and 'userId' not in detail
assert detail['advertiserName'] == post_body['advertiserName']
call('POST', '/api/posts', 406, 2, {**post_body, 'numberOfPosts': 4})
call('POST', '/api/posts', 406, 2, {**post_body, 'endDate': '31/02/2026'})
call('POST', '/api/posts', 404, 4, {**post_body, 'userId': 2147483647})
call('PUT', f'/api/posts/{post_id}', 406, 2, {'startDate': '29/07/2026', 'advertiserName': 'Changed'})
assert call('GET', f'/api/posts/{post_id}')['advertiserName'] == post_body['advertiserName']
call('PUT', f'/api/users/{user_id}', body={'ratePerPost': 600000})
campaign = call('PUT', f'/api/posts/{post_id}', body={'endDate': '29/07/2026', 'totalCost': 1})
assert campaign['totalCost'] == 2400000
page = call('GET', '/api/posts?advertiserName=Slot%2016&status=SCHEDULED&size=100')
assert any(row['postId'] == post_id for row in page['content'])
call('GET', '/api/posts?size=101', 406, 2)
call('GET', '/api/posts/2147483647', 404, 4)
call('DELETE', f'/api/posts/{post_id}')
assert call('GET', f'/api/posts/{post_id}')['status'] == 'CANCELLED'
call('DELETE', f'/api/users/{user_id}')
assert call('GET', f'/api/users/{user_id}')['status'] == 'DEACTIVATED'
call('POST', '/api/posts', 400, 5, post_body)

for port in (8080, 8081, 8082):
    request = Request(f'http://localhost:{port}/v3/api-docs', headers={'Origin': 'https://example.com'})
    with urlopen(request, timeout=15) as response:
        assert response.code == 200
        assert 'openapi' in json.loads(response.read())
        assert response.headers['Access-Control-Allow-Origin'] == 'https://example.com'
    with urlopen(f'http://localhost:{port}/swagger-ui.html', timeout=15) as response:
        assert response.code == 200
    checks.append(f'Port {port}: OpenAPI, Swagger UI, CORS passed')

result = {'passed': len(checks), 'failed': 0, 'userId': user_id, 'postId': post_id, 'checks': checks}
output = Path(__file__).resolve().parent.parent / 'evidence' / 'smoke-results.json'
output.parent.mkdir(exist_ok=True)
output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
print(json.dumps(result, ensure_ascii=False, indent=2))
