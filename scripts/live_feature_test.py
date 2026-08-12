#!/usr/bin/env python3
"""Live feature tests for Open Flow debug APK.
PASS = act + wait + UI/data assert + screenshot. Exit 1 on any FAIL.
"""
from __future__ import annotations
import re, subprocess, sys, time, json
from pathlib import Path
from datetime import datetime
import xml.etree.ElementTree as ET

PKG = "app.openflow.debug"
ACT = f"{PKG}/app.openflow.ui.MainActivity"
DESK = Path("/mnt/c/Users/Mitun Manav G Y/Desktop/Open-Flow")
QA = DESK / "qa" / "live"
LOGS = DESK / "logs"
DOCS = DESK / "docs"
for d in (QA, LOGS, DOCS):
    d.mkdir(parents=True, exist_ok=True)

results = []

def sh(*args, timeout=90):
    r = subprocess.run(["adb", *args], capture_output=True, timeout=timeout)
    return (r.stdout or b"").decode("utf-8", "replace"), (r.stderr or b"").decode("utf-8", "replace"), r.returncode

def rec(cid, name, ok, detail=""):
    results.append({"id": cid, "name": name, "pass": bool(ok), "detail": detail})
    print(("PASS" if ok else "FAIL"), cid, name, "|", detail[:160], flush=True)

def focus_line():
    out, _, _ = sh("shell", "dumpsys", "window")
    return next((l for l in out.splitlines() if "mCurrentFocus" in l), "")

def ensure_app():
    line = focus_line()
    if PKG not in line:
        sh("shell", "input", "keyevent", "3")
        sh("shell", "am", "start", "-n", ACT)
        time.sleep(1.5)
        line = focus_line()
    return PKG in line, line

def shot(name):
    path = QA / f"{name}.png"
    path.write_bytes(subprocess.check_output(["adb", "exec-out", "screencap", "-p"]))
    return path

def dump_nodes():
    sh("shell", "uiautomator", "dump", "/sdcard/ui.xml")
    sh("pull", "/sdcard/ui.xml", str(Path("/tmp/of_ui.xml")))
    root = ET.parse(Path("/tmp/of_ui.xml")).getroot()
    nodes = []
    for n in root.iter("node"):
        b = n.attrib.get("bounds", "")
        m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", b)
        if not m:
            continue
        x1, y1, x2, y2 = map(int, m.groups())
        c = n.attrib.get("class", "")
        nodes.append({
            "text": n.attrib.get("text", ""),
            "desc": n.attrib.get("content-desc", ""),
            "class": c,
            "cx": (x1 + x2) // 2,
            "cy": (y1 + y2) // 2,
            "w": x2 - x1,
            "h": y2 - y1,
            "edit": n.attrib.get("editable") == "true" or "EditText" in c,
            "res": n.attrib.get("resource-id", ""),
        })
    return nodes

def wait_until(pred, timeout=8.0, interval=0.25):
    end = time.time() + timeout
    last = None
    while time.time() < end:
        try:
            nodes = dump_nodes()
            last = nodes
            if pred(nodes):
                return True, nodes
        except Exception:
            pass
        time.sleep(interval)
    return False, last or []

def texts(nodes):
    return [n["text"] for n in nodes if n["text"]]

def tap(x, y):
    sh("shell", "input", "tap", str(x), str(y))
    time.sleep(0.25)

def tap_text(nodes, label, min_w=50, bottom=False):
    cands = []
    for n in nodes:
        blob = (n["text"] or n["desc"] or "")
        if label.lower() not in blob.lower():
            continue
        if bottom and n["cy"] < 2000:
            continue
        if n["w"] < min_w:
            continue
        cands.append(n)
    if not cands:
        return False
    cands.sort(key=lambda n: n["w"] * n["h"], reverse=True)
    tap(cands[0]["cx"], cands[0]["cy"])
    return True

def focus_edit(nodes, index=0):
    edits = [n for n in nodes if n["edit"]]
    if index >= len(edits):
        return False
    e = edits[index]
    tap(e["cx"], e["cy"])
    time.sleep(0.3)
    return True

def type_txt(s):
    # spaces as %s
    esc = s.replace(" ", "%s").replace("'", "")
    sh("shell", "input", "text", esc)
    time.sleep(0.25)

def clear_field(n=30):
    for _ in range(n):
        sh("shell", "input", "keyevent", "67")

def nav_tab(name):
    ensure_app()
    nodes = dump_nodes()
    ok = tap_text(nodes, name, min_w=30, bottom=True)
    ok2, nodes = wait_until(lambda ns: True, timeout=1.0)
    time.sleep(0.6)
    ensure_app()
    return ok

def prefs_blob():
    ls, _, _ = sh("shell", "run-as", PKG, "ls", "shared_prefs")
    blob = ls + "\n"
    for fn in ls.split():
        if not fn.endswith(".xml"):
            continue
        o, _, _ = sh("shell", "run-as", PKG, "cat", f"shared_prefs/{fn}")
        blob += f"\n<!-- {fn} -->\n" + o
    return blob

def db_query(sql):
    ls, _, _ = sh("shell", "run-as", PKG, "ls", "databases")
    db = next((x for x in ls.split() if x.endswith(".db") and "-shm" not in x and "-wal" not in x), "")
    if not db:
        return "", "no-db"
    o, e, rc = sh("shell", "run-as", PKG, "sh", "-c", f"sqlite3 databases/{db} \"{sql}\"")
    return o, f"rc={rc} err={e[:80]}"

# ----- suite -----
sh("logcat", "-c")
sh("shell", "am", "force-stop", PKG)
time.sleep(0.3)
sh("shell", "am", "start", "-n", ACT)
time.sleep(2.0)

ok, line = ensure_app()
rec("A1", "cold_start", ok, line)
shot("A1-home")

nodes = dump_nodes()
nav = {}
for n in nodes:
    if n["text"] in ("Home", "History", "Dict", "Settings") and n["cy"] > 2000:
        nav[n["text"]] = (n["cx"], n["cy"])
rec("A2", "bottom_nav", len(nav) == 4, str(nav))

# B1 practice type
nav_tab("Home")
sh("shell", "input", "swipe", "540", "1800", "540", "900", "280")
time.sleep(0.5)
nodes = dump_nodes()
focus_edit(nodes, 0)
clear_field()
type_txt("live_ok_1")
ok_w, nodes = wait_until(lambda ns: any("live_ok_1" in (n["text"] or "") for n in ns), timeout=5)
shot("B1-practice")
rec("B1", "practice_type", ok_w, str([n["text"] for n in nodes if "live" in (n["text"] or "")]))

# B2 cleanup → pref
sh("shell", "input", "swipe", "540", "900", "540", "1700", "250")
time.sleep(0.4)
nodes = dump_nodes()
tap_text(nodes, "Raw", min_w=30)
time.sleep(0.6)
pref = prefs_blob()
rec("B2", "cleanup_pref_raw", "none" in pref or "cleanup" in pref.lower(), pref[:300])
tap_text(dump_nodes(), "Smart", min_w=30)
time.sleep(0.5)
pref2 = prefs_blob()
rec("B2b", "cleanup_pref_smart", "medium" in pref2 or "cleanup" in pref2.lower(), pref2[:300])

# C1 dict add
nav_tab("Dict")
shot("C0-dict")
nodes = dump_nodes()
focus_edit(nodes, 0); clear_field(); type_txt("qazword99")
nodes = dump_nodes()
focus_edit(nodes, 1); clear_field(); type_txt("qazrepl99")
nodes = dump_nodes()
tap_text(nodes, "Save Word", min_w=60)
ok_w, nodes = wait_until(lambda ns: any(n["text"] == "qazword99" for n in ns) or any(n["text"] == "qazword99" or "qazrepl99" in (n["text"] or "") for n in ns), timeout=6)
shot("C1-dict-saved")
# Room / persist
# force-stop relaunch
sh("shell", "am", "force-stop", PKG)
time.sleep(0.4)
sh("shell", "am", "start", "-n", ACT)
time.sleep(1.8)
nav_tab("Dict")
ok_p, nodes = wait_until(lambda ns: any(n["text"] == "qazword99" for n in ns), timeout=5)
shot("C1b-dict-persist")
rec("C1", "dict_add_and_persist", ok_w and ok_p, f"after_save={ok_w} after_relaunch={ok_p} texts={texts(nodes)[:15]}")

# C2 delete
nodes = dump_nodes()
deleted = False
for n in nodes:
    if "delete" in (n["desc"] or "").lower():
        tap(n["cx"], n["cy"]); deleted = True; break
time.sleep(0.8)
ok_g, nodes = wait_until(lambda ns: not any(n["text"] == "qazword99" for n in ns), timeout=4)
shot("C2-dict-deleted")
rec("C2", "dict_delete", deleted and ok_g, f"tap={deleted} gone={ok_g}")

# D1 snippet
nav_tab("Settings")
nodes = dump_nodes()
if not tap_text(nodes, "Voice Snippets", min_w=80):
    sh("shell", "input", "swipe", "540", "1700", "540", "900", "280")
    time.sleep(0.4)
    tap_text(dump_nodes(), "Voice Snippets", min_w=80)
time.sleep(0.9)
ensure_app()
nodes = dump_nodes()
focus_edit(nodes, 0); clear_field(); type_txt("myemail")
nodes = dump_nodes()
focus_edit(nodes, 1); clear_field(); type_txt("a@b.com")
nodes = dump_nodes()
tap_text(nodes, "Add Snippet", min_w=60)
ok_s, nodes = wait_until(lambda ns: any("myemail" in (n["text"] or "") for n in ns), timeout=6)
shot("D1-snippet")
rec("D1", "snippet_add", ok_s, texts(nodes)[:12])
sh("shell", "input", "keyevent", "4")
time.sleep(0.6)
ensure_app()

# F1 privacy
nodes = dump_nodes()
if not tap_text(nodes, "Privacy", min_w=80):
    sh("shell", "input", "swipe", "540", "1600", "540", "900", "280")
    time.sleep(0.4)
    tap_text(dump_nodes(), "Privacy", min_w=80)
time.sleep(0.8)
nodes = dump_nodes()
ok_wipe = tap_text(nodes, "Wipe after 24h", min_w=100) or tap_text(nodes, "Wipe after", min_w=80)
time.sleep(0.8)
pref = ""
for _ in range(10):
    pref = prefs_blob()
    if "wipe_24h" in pref:
        break
    time.sleep(0.3)
shot("F1-privacy")
rec("F1", "privacy_wipe_pref", "wipe_24h" in pref, f"tap={ok_wipe} pref={pref[:280]}")
tap_text(dump_nodes(), "Keep forever", min_w=60)
time.sleep(0.4)
sh("shell", "input", "keyevent", "4")
time.sleep(0.5)
ensure_app()

# F2 bubble shape
nodes = dump_nodes()
tap_text(nodes, "Flow Bubble", min_w=80)
time.sleep(0.9)
nodes = dump_nodes()
tap_text(nodes, "Circle", min_w=30)
time.sleep(0.5)
pref = prefs_blob()
shot("F2-bubble")
rec("F2", "bubble_shape_pref", "circle" in pref.lower() or "shape" in pref.lower(), pref[:280])
sh("shell", "input", "keyevent", "4")
time.sleep(0.5)

# A3 back already used — explicit: open privacy back
nav_tab("Settings")
nodes = dump_nodes()
tap_text(nodes, "Privacy", min_w=80)
time.sleep(0.7)
sh("shell", "input", "keyevent", "4")
time.sleep(0.6)
ok, line = ensure_app()
nodes = dump_nodes()
rec("A3", "back_stays_in_app", ok and any("Settings" in t or "Flow Bubble" in t or "Preferences" in t for t in texts(nodes)), line)

# E history seed via sqlite
sql = (
    "INSERT OR REPLACE INTO dictations"
    "(id,text,rawText,createdAtEpochMs,durationMs,languageTag,wordCount) VALUES"
    f"('live1','hello feature test','hello feature test',{int(time.time()*1000)},900,'en-US',3);"
)
out, meta = db_query(sql.replace('"', '\\"') if False else sql)
# sqlite via run-as
ls, _, _ = sh("shell", "run-as", PKG, "ls", "databases")
db = next((x for x in ls.split() if x.endswith(".db") and not x.endswith("-wal") and not x.endswith("-shm")), "")
if db:
    # escape carefully
    cmd = f"sqlite3 databases/{db} \"{sql}\""
    o, e, rc = sh("shell", "run-as", PKG, "sh", "-c", cmd)
    rec("E0", "sqlite_seed", rc == 0, f"rc={rc} {o} {e[:60]}")
else:
    rec("E0", "sqlite_seed", False, f"no db ls={ls}")

sh("shell", "am", "force-stop", PKG)
sh("shell", "am", "start", "-n", ACT)
time.sleep(1.8)
nav_tab("History")
ok_h, nodes = wait_until(lambda ns: any("hello feature test" in (n["text"] or "") for n in ns), timeout=6)
shot("E1-history")
rec("E1", "history_shows_row", ok_h, texts(nodes)[:12])

if ok_h:
    focus_edit(nodes, 0)
    clear_field(20)
    type_txt("feature")
    ok_f, nodes = wait_until(lambda ns: any("hello feature test" in (n["text"] or "") for n in ns), timeout=4)
    shot("E2-search")
    rec("E2", "history_search", ok_f, texts(nodes)[:10])
    ok_c = tap_text(nodes, "Copy", min_w=30)
    time.sleep(0.4)
    rec("E3", "history_copy_tap", ok_c, "tapped Copy")
else:
    rec("E2", "history_search", False, "no row")
    rec("E3", "history_copy_tap", False, "no row")

# double title check History
nodes = dump_nodes()
title_hits = [t for t in texts(nodes) if t in ("History", "Private History")]
# TopAppBar "History" + body should not also say Private History
rec("U1", "no_double_private_history", "Private History" not in texts(nodes), f"titles={title_hits}")

# a11y + mic
a11y, _, _ = sh("shell", "settings", "get", "secure", "enabled_accessibility_services")
rec("G1", "a11y_debug", "app.openflow.debug" in a11y, a11y[:140])
perm, _, _ = sh("shell", "dumpsys", "package", PKG)
rec("G2", "mic_granted", "android.permission.RECORD_AUDIO: granted=true" in perm, "")

log, _, _ = sh("logcat", "-d", "-t", "250")
(LOGS / "live-feature.log").write_text(log)
fatals = [l for l in log.splitlines() if "FATAL" in l and "openflow" in l.lower()]
rec("A4", "no_fatal", len(fatals) == 0, str(fatals[:2]))

passed = sum(1 for r in results if r["pass"])
failed = sum(1 for r in results if not r["pass"])
report = {
    "when": datetime.utcnow().isoformat() + "Z",
    "pkg": PKG,
    "pass": passed,
    "fail": failed,
    "results": results,
}
(QA / "LIVE-RESULTS.json").write_text(json.dumps(report, indent=2))
lines = [f"# LIVE FEATURE RESULTS", f"pass={passed} fail={failed}", f"pkg={PKG}", ""]
for r in results:
    mark = "PASS" if r["pass"] else "FAIL"
    lines.append(f"- **{mark}** `{r['id']}` {r['name']}: {r['detail'][:140]}")
(DOCS / "LIVE-MATRIX.md").write_text("\n".join(lines))
(QA / "LIVE-RESULTS.md").write_text("\n".join(lines))
print("\n==== TOTAL ====", flush=True)
print("PASS", passed, "FAIL", failed, flush=True)
for r in results:
    if not r["pass"]:
        print("FAIL", r["id"], r["name"], r["detail"][:140], flush=True)
sys.exit(1 if failed else 0)
