#!/usr/bin/env python3
"""Forward WSL 127.0.0.1:5037 to the Windows adb server (adb -a).

The `android` CLI talks to localhost:5037 in WSL and ignores ADB_SERVER_SOCKET.
WSL NAT cannot see Windows 127.0.0.1. Windows `adb -a` listens on 0.0.0.0:5037;
this bridge is the missing hop.
"""
from __future__ import annotations

import socket
import sys
import threading

LISTEN = ("127.0.0.1", 5037)


def pipe(src: socket.socket, dst: socket.socket) -> None:
    try:
        while True:
            data = src.recv(65536)
            if not data:
                break
            dst.sendall(data)
    except OSError:
        pass
    finally:
        try:
            src.shutdown(socket.SHUT_RD)
        except OSError:
            pass
        try:
            dst.shutdown(socket.SHUT_WR)
        except OSError:
            pass


def handle(client: socket.socket, remote_addr: tuple[str, int]) -> None:
    try:
        remote = socket.create_connection(remote_addr, timeout=5)
    except OSError:
        client.close()
        return
    t1 = threading.Thread(target=pipe, args=(client, remote), daemon=True)
    t2 = threading.Thread(target=pipe, args=(remote, client), daemon=True)
    t1.start()
    t2.start()
    t1.join()
    t2.join()
    client.close()
    remote.close()


def main() -> int:
    if len(sys.argv) != 2:
        print("usage: adb-bridge.py <windows-host-ip>", file=sys.stderr)
        return 2
    remote_addr = (sys.argv[1], 5037)
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    sock.bind(LISTEN)
    sock.listen(32)
    while True:
        client, _ = sock.accept()
        threading.Thread(target=handle, args=(client, remote_addr), daemon=True).start()


if __name__ == "__main__":
    raise SystemExit(main())
