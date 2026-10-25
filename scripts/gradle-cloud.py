#!/usr/bin/env python3
"""Run the pinned wrapper using the cloud proxy and system certificate trust."""
import os
from pathlib import Path
import subprocess
import sys
from urllib.parse import urlparse

env = os.environ.copy()
jdk = Path('/workspace/toolchains/jdk-25+36')
if jdk.exists():
    env['JAVA_HOME'] = str(jdk)
    env['PATH'] = str(jdk / 'bin') + os.pathsep + env['PATH']
env.setdefault('GRADLE_USER_HOME', '/workspace/.gradle-cache')
options = []
proxy = env.get('HTTPS_PROXY') or env.get('https_proxy')
if proxy:
    route = urlparse(proxy)
    if not route.hostname or route.username or route.password:
        raise SystemExit('An unauthenticated platform proxy hostname is required')
    options += [f'-Dhttps.proxyHost={route.hostname}', f'-Dhttps.proxyPort={route.port or 80}',
                f'-Dhttp.proxyHost={route.hostname}', f'-Dhttp.proxyPort={route.port or 80}']
trust = Path('/etc/ssl/certs/java/cacerts')
if trust.exists():
    options += [f'-Djavax.net.ssl.trustStore={trust}']
env['JAVA_OPTS'] = env.get('JAVA_OPTS', '') + ' ' + ' '.join(options)
root = Path(__file__).resolve().parent.parent
raise SystemExit(subprocess.call([str(root / 'gradlew'), '--no-daemon', '--max-workers=2', *sys.argv[1:]], cwd=root, env=env))
