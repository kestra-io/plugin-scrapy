#!/bin/sh
# Kestra's Docker task runner creates the working directory as root, so start as root,
# hand that directory to the unprivileged `kestra` user (UID/GID 1000), then drop privileges.
set -e

if [ "$(id -u)" = "0" ]; then
    case "$PWD" in
        /|/bin|/boot|/dev|/etc|/lib|/lib64|/opt|/proc|/root|/run|/sbin|/sys|/usr|/var|\
        /bin/*|/boot/*|/dev/*|/etc/*|/lib/*|/lib64/*|/opt/*|/proc/*|/root/*|/run/*|/sbin/*|/sys/*|/usr/*|/var/*)
            ;;
        *)
            chown -R 1000:1000 "$PWD" || echo "kestra-entrypoint: could not chown $PWD" >&2
            ;;
    esac
    export HOME=/home/kestra USER=kestra LOGNAME=kestra
    exec setpriv --reuid=1000 --regid=1000 --init-groups "$@"
fi

exec "$@"
