#!/usr/bin/env python3
"""Exercise the pinned rclone RC protocol using temporary files and fake credentials.
Never contacts a user's Drive; a browser redirect is NOT proof of working Google OAuth.
Usage: python3 scripts/test-drive-protocol.py /path/to/rclone
"""
import concurrent.futures
import json
import pathlib
import socket
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid

binary = str(pathlib.Path(sys.argv[1]).resolve())
with tempfile.TemporaryDirectory() as directory:
    config = pathlib.Path(directory) / 'rclone.conf'
    password = uuid.uuid4().hex
    with socket.socket() as sock:
        sock.bind(('127.0.0.1', 0))
        port = sock.getsockname()[1]
    url = f'http://127.0.0.1:{port}'
    process = subprocess.Popen([binary, '--config', str(config), 'rcd', '--rc-addr', f'127.0.0.1:{port}', '--rc-user', 'setup', '--rc-pass', password], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    def rc(endpoint, payload=None, timeout=15):
        result = subprocess.run([binary, 'rc', '--url', url, '--user', 'setup', '--pass', password, endpoint, '--json', json.dumps(payload or {})], capture_output=True, text=True, timeout=timeout)
        if result.returncode:
            raise RuntimeError('RC request failed: ' + endpoint)
        return json.loads(result.stdout)
    try:
        for attempt in range(60):
            try:
                rc('rc/noop', timeout=2)
                break
            except (RuntimeError, subprocess.TimeoutExpired):
                time.sleep(.1)
        else:
            raise AssertionError('RC server never started')
        # A second local app cannot use this RC server without the per-session password.
        unauthorized = subprocess.run([binary, 'rc', '--url', url, 'rc/noop'], capture_output=True, timeout=5)
        assert unauthorized.returncode != 0
        payload = {'name': 'protocol_test', 'type': 'drive', 'parameters': {}, 'opt': {'nonInteractive': True, 'all': True, 'obscure': True}}
        step = rc('config/create', payload)
        names = []
        answers = {'client_id': 'test-client.apps.googleusercontent.com', 'client_secret': 'test-secret', 'scope': 'drive.readonly', 'service_account_file': '', 'config_fs_advanced': 'false'}
        for _ in range(20):
            option = step.get('Option') or {}
            name = option.get('Name')
            assert step.get('State') and name, 'Setup ended before OAuth'
            names.append(name)
            if name == 'config_is_local':
                break
            assert name in answers, 'Unexpected question: ' + str(name)
            payload['opt'].update({'continue': True, 'state': step['State'], 'result': answers[name]})
            step = rc('config/update', payload)
        assert names[:3] == ['client_id', 'client_secret', 'scope'], names
        assert names[-1] == 'config_is_local', names
        payload['opt'].update({'continue': True, 'state': step['State'], 'result': 'true'})
        with concurrent.futures.ThreadPoolExecutor() as executor:
            pending = executor.submit(rc, 'config/update', payload, 30)
            for _ in range(100):
                status = rc('config/oauthstatus')
                if status.get('authUrl'):
                    break
                time.sleep(.1)
            auth_url = status.get('authUrl', '')
            assert urllib.parse.urlparse(auth_url).hostname in ('127.0.0.1', 'localhost')
            class NoRedirect(urllib.request.HTTPRedirectHandler):
                def redirect_request(self, *args):
                    return None
            opener = urllib.request.build_opener(urllib.request.ProxyHandler({}), NoRedirect())
            try:
                opener.open(auth_url, timeout=5)
                raise AssertionError('Expected redirect to Google')
            except urllib.error.HTTPError as redirect:
                assert redirect.code in (302, 307)
                location = redirect.headers['Location']
                query = urllib.parse.parse_qs(urllib.parse.urlparse(location).query)
                assert query['client_id'] == [answers['client_id']]
                assert 'drive.readonly' in query['scope'][0]
                assert urllib.parse.urlparse(query['redirect_uri'][0]).hostname in ('127.0.0.1', 'localhost')
            rc('config/oauthstop')
            try:
                denied = pending.result(timeout=10)
            except RuntimeError:
                pass
            else:
                assert denied.get('Error') or denied.get('State'), 'Cancellation incorrectly reported success'
        print('PASS: authenticated RC, Client ID, Client Secret, scope, optional empty answer, OAuth loopback redirect and cancellation.')
        print('NOT TESTED: real Google consent, token exchange, Android browser return or live Drive listing.')
    finally:
        process.terminate()
        try:
            process.wait(timeout=5)
        except subprocess.TimeoutExpired:
            process.kill()
