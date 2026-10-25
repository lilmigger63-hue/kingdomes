#!/usr/bin/env python3
"""Build a pinned external dependency; never copy its sources/assets into releases."""
import hashlib
import json
import os
from pathlib import Path
import subprocess
from urllib.parse import urlparse
import shutil
import zipfile

root = Path(__file__).resolve().parent.parent
spec = json.loads((root / 'dependencies/minecolonies.json').read_text())
cache_root = Path(os.environ.get('KINGDOMS_DEPENDENCY_CACHE', str(root / '.gradle')))
if not cache_root.is_absolute(): cache_root = root / cache_root
cache = cache_root / 'minecolonies' / spec['commit']
jar = cache / spec['jar']
if not jar.exists():
    source = cache_root / 'minecolonies-source' / spec['commit']
    if source.exists():
        dirty = subprocess.check_output(['git','-C',str(source),'status','--porcelain','--untracked-files=normal'],text=True)
        if dirty:
            raise SystemExit('Dependency source checkout has local changes; preserve them and use a clean pinned checkout')
    if not source.exists():
        source.parent.mkdir(parents=True, exist_ok=True)
        subprocess.run(['git', 'clone', '--no-checkout', '--depth=1', spec['repository'], str(source)], check=True)
    subprocess.run(['git', '-C', str(source), 'fetch', '--depth=1', 'origin', spec['commit']], check=True)
    subprocess.run(['git', '-C', str(source), 'checkout', '--detach', spec['commit']], check=True)
    actual = subprocess.check_output(['git','-C',str(source),'rev-parse','HEAD'], text=True).strip()
    dirty = subprocess.check_output(['git','-C',str(source),'status','--porcelain','--untracked-files=normal'],text=True)
    if dirty: raise SystemExit('Dependency source is not clean after checkout')
    if actual != spec['commit']:
        raise SystemExit('Dependency source revision mismatch')
    env = os.environ.copy()
    jdk = Path('/workspace/toolchains/jdk-25+36')
    if jdk.exists(): env['JAVA_HOME'] = str(jdk)
    env.setdefault('GRADLE_USER_HOME', '/workspace/.gradle-cache' if Path('/workspace').exists() else str(Path.home()/'.gradle'))
    args = []
    proxy = env.get('HTTPS_PROXY')
    if proxy:
        route = urlparse(proxy)
        if not route.hostname or route.username or route.password: raise SystemExit('Unsupported authenticated proxy URL')
        args += [f'-Dhttps.proxyHost={route.hostname}', f'-Dhttps.proxyPort={route.port or 80}']
    trust = Path('/etc/ssl/certs/java/cacerts')
    if trust.exists(): args += [f'-Djavax.net.ssl.trustStore={trust}']
    env['JAVA_OPTS'] = env.get('JAVA_OPTS','') + ' ' + ' '.join(args)
    wrapper = str(root / ('gradlew.bat' if os.name == 'nt' else 'gradlew'))
    subprocess.run([wrapper,'--no-daemon','--max-workers=2','-p',str(source),'compileJava','publishJar','-Pportassets.skipLangExtract=true'], cwd=source, env=env, check=True)
    cache.mkdir(parents=True, exist_ok=True)
    shutil.copy2(source / 'build/publish-libs' / spec['jar'], jar)
    (cache / (spec['jar']+'.sha256')).write_text(hashlib.sha256(jar.read_bytes()).hexdigest()+'\n')
checksum = cache / (spec['jar']+'.sha256')
expected = checksum.read_text().strip() if checksum.exists() else '6019926009e7924b6dca854f85e719efe985b88961b19863a57a583a62d7f327'
if hashlib.sha256(jar.read_bytes()).hexdigest() != expected:
    raise SystemExit('Cached dependency checksum mismatch; rebuild from pinned source')
with zipfile.ZipFile(jar) as z:
    metadata = z.read('META-INF/neoforge.mods.toml').decode()
    if f'version="{spec["version"]}"' not in metadata or 'com/minecolonies/api/colony/IColony.class' not in z.namelist():
        raise SystemExit('Unexpected MineColonies artifact')
print('MineColonies dependency ready:', jar)
