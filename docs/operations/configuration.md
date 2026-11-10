# Runtime configuration

Distant Lands reads command-line options first, environment variables second,
and defaults last. A command-line value therefore overrides the matching
environment variable. Packaged launchers provide `trusted-alpha` only when `DLQ_MODE` is absent;
explicit environment or command-line values still win.

Universal archives require Java 21 on the host. They do not require sbt or
Node. The OCI image bundles a Java 21 runtime and runs as the non-root
`distantlands` user (UID 10001).

Start with the [trusted-alpha quick start](quick-start.md). Before operating on
persistent files, read the [data and upgrade policy](data-policy.md). LAN,
firewall, HTTPS proxy, and provisional browser guidance is in
[network and browser guidance](network-and-browser.md).

## Options

| Command-line option | Environment variable | Default |
| --- | --- | --- |
| `--host HOST` | `DLQ_HOST` | `127.0.0.1` |
| `--port PORT` | `DLQ_PORT` | `8080` |
| `--public-base-url URL` | `DLQ_PUBLIC_BASE_URL` | unset |
| `--database-path PATH` | `DLQ_DATABASE_PATH` | `var/distantlands` |
| `--mode development\|trusted-alpha` | `DLQ_MODE` | `development`; packaged launcher uses `trusted-alpha` |

`development` accepts loopback bind hosts only. `trusted-alpha` accepts a
non-loopback host only when `--public-base-url` or `DLQ_PUBLIC_BASE_URL`
provides the browser-visible HTTP or HTTPS origin. Public base URLs cannot
contain credentials, a path, query, or fragment.

Requests that change state, such as creating a game or making a move, must come
from a page at the public base URL. In every mode, a page at `localhost`,
`127.0.0.1`, or `[::1]` on the listen port is also accepted, so the host can
play through a loopback address when it cannot reach its own public address.

## Universal archive

Extract either versioned archive, enter its top-level directory, and run:

```sh
DLQ_MODE=trusted-alpha \
DLQ_HOST=0.0.0.0 \
DLQ_PORT=8080 \
DLQ_PUBLIC_BASE_URL=http://192.168.1.20:8080 \
DLQ_DATABASE_PATH=/home/alex/distantlands-data/alpha-1/database \
bin/distantlands
```

The example data directory must exist and be writable by the process. For loopback
use, omit `DLQ_HOST` and `DLQ_PUBLIC_BASE_URL`. Windows users can set the
same environment variables and run `bin\distantlands.bat`; complete commands
for all three host operating systems are in the quick start.

## OCI image

The image sets `DLQ_HOST=0.0.0.0` and
`DLQ_DATABASE_PATH=/var/lib/distantlands/database` itself and declares
`/var/lib/distantlands` as a volume, so only the browser-visible origin has to
be supplied. Set `DLQ_IMAGE_REFERENCE` to the exact published, versioned image
reference supplied by the release operator. The placeholder must be replaced
before use:

```sh
export DLQ_IMAGE_REFERENCE='ghcr.io/<owner>/<repository>:v0.1.0-alpha.1'
docker pull "$DLQ_IMAGE_REFERENCE"
docker volume create distantlands-data
docker run --detach --name distantlands \
  --publish 8080:8080 \
  --env DLQ_PUBLIC_BASE_URL=http://192.168.1.20:8080 \
  --volume distantlands-data:/var/lib/distantlands \
  "$DLQ_IMAGE_REFERENCE"
docker logs --follow distantlands
```

Name a volume as shown to keep the database across container replacements; the
declared volume otherwise becomes an anonymous one.

Stop the container with `docker stop --time 15 distantlands`, wait for the
database-close log, and remove only the stopped container with
`docker rm distantlands`. The named volume remains. Follow the data policy for
stopped-volume backup and matching-release restore.

For host-local access only, replace `--publish 8080:8080` with
`--publish 127.0.0.1:8080:8080` and use a loopback public base URL such as
`http://127.0.0.1:8080`.

The container launcher supplies `trusted-alpha` mode. Add
`--env DLQ_MODE=...` to override it. Command-line options placed after the image name override environment
values.

End users run the published image and need neither sbt nor Node. For developers,
`Docker/publishLocal` produces an image for the build host's own architecture.
The manual [alpha release workflow](releases.md) builds, loads, and smokes both
`linux/amd64` and `linux/arm64` before its optional publication job can run.
Publication is disabled by default; workflow implementation does not establish
that any registry image has been published.

Internet exposure requires HTTPS at a trusted reverse proxy. Use the concrete
proxy and log-redaction requirements in the network guidance, then record
separate-machine LAN and TLS results in the
[per-build alpha acceptance record](alpha-acceptance.md). An unexecuted record
is a template, not acceptance evidence.

## Desktop launch profile

The Start files set `DLQ_LAUNCH=desktop`. Without that variable, nothing in
this section applies. Any other value of `DLQ_LAUNCH` is a configuration
error.

Under the desktop profile, values come from, in order of precedence:

1. Command-line options.
2. Environment variables.
3. The settings file.
4. Desktop defaults: listen on `0.0.0.0`, detect the local network address,
   store the database in the app-data folder, open the browser.
5. The defaults in the table above.

| Computer | App-data folder |
| --- | --- |
| macOS | `~/Library/Application Support/DistantLands` |
| Windows | `%LOCALAPPDATA%\DistantLands` |
| Linux and others | `$XDG_DATA_HOME/distantlands`, else `~/.local/share/distantlands` |

The database is `data/database` inside that folder. The settings file is
`distantlands.properties` inside that folder. The first start writes it with
every setting commented out. Remove the leading `#` from a line to use it, and
restart the server. It accepts only `DLQ_HOST`, `DLQ_PORT`,
`DLQ_PUBLIC_BASE_URL`, `DLQ_DATABASE_PATH`, and `DLQ_OPEN_BROWSER`
(`true` or `false`). On Windows, write paths with forward slashes, such as
`C:/DistantLandsData/database`.

The detected address is the private IPv4 address of the connection the
computer uses for its default route. If there is none, the server listens on
`127.0.0.1` only and says so. If the detected address is wrong, set
`DLQ_PUBLIC_BASE_URL` in the settings file. The port stays at 8080 unless
you change it; if it is in use, the server stops with a message naming the
settings file.
