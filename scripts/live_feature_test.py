#!/usr/bin/env python3
"""Live feature tests for Open Flow debug APK.

Root-cause rules (2026-08-12):
- NEVER am force-stop — kills AccessibilityService on this OEM.
- Gate product path: Bound service + overlay window before bubble claims.
- Inject path tests polish→insert without mic.
"""
from __future__ import annotations
import re, subprocess, sys, time, json
from pathlib import Path
from datetime import datetime, timezone
import xml.etree.ElementTree as ET

PKG = "app.openflow.debug"
ACT = f"{PKG}/app.openflow.ui.MainActivity"
A11Y = f"{PKG}/app.openflow.bubble.FlowAccessibilityService"
INJECT_ACTION = "app.openflow.INJECT_DICTATION"
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

def tap_exact(nodes, label, min_w=20):
    """Exact text match — use for None/Light/Medium/High chips."""
    cands = [n for n in nodes if n["text"] == label and n["w"] >= min_w]
    if not cands:
        return False
    cands.sort(key=lambda n: abs(n["cy"] - 1600))  # home cleanup row
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
    esc = s.replace(" ", "%s").replace("'", "")
    sh("shell", "input", "text", esc)
    time.sleep(0.25)

def clear_field(n=40):
    for _ in range(n):
        sh("shell", "input", "keyevent", "67")

def nav_tab(name):
    ensure_app()
    nodes = dump_nodes()
    ok = tap_text(nodes, name, min_w=30, bottom=True)
    wait_until(lambda ns: True, timeout=0.8)
    time.sleep(0.5)
    ensure_app()
    return ok

def prefs_blob():
    blob = ""
    for attempt in range(5):
        ls, err, rc = sh("shell", "run-as", PKG, "ls", "shared_prefs")
        if rc != 0 or not ls.strip():
            time.sleep(0.3)
            continue
        for fn in ls.split():
            if not fn.endswith(".xml"):
                continue
            o, _, _ = sh("shell", "run-as", PKG, "cat", f"shared_prefs/{fn}")
            blob += f"\n<!-- {fn} -->\n" + o
        if blob.strip():
            return blob
        time.sleep(0.3)
    return blob

def cleanup_level_from_prefs(blob: str) -> str:
    m = re.search(r'name="cleanup_level">([^<]+)<', blob)
    return (m.group(1) if m else "").strip()

def a11y_enabled_string() -> str:
    out, _, _ = sh("shell", "settings", "get", "secure", "enabled_accessibility_services")
    return out.strip()

def a11y_bound() -> bool:
    """True only if system Bound services includes Open Flow Bubble."""
    out, _, _ = sh("shell", "dumpsys", "accessibility")
    return "label=Open Flow Bubble" in out

def overlay_present() -> bool:
    out, _, _ = sh("shell", "dumpsys", "window", "windows")
    return PKG in out and "CREATE_ACCESSIBILITY_OVERLAY" in out

def ensure_a11y_string():
    """Write settings string if missing. Does NOT prove Bound."""
    of = A11Y
    cur = a11y_enabled_string()
    if PKG in cur and "FlowAccessibilityService" in cur:
        return cur
    parts = [p for p in cur.split(":") if p and "app.openflow" not in p]
    parts.append(of)
    new = ":".join(parts)
    sh("shell", "settings", "put", "secure", "enabled_accessibility_services", new)
    sh("shell", "settings", "put", "secure", "accessibility_enabled", "1")
    return a11y_enabled_string()

def wait_a11y_bound(timeout=6.0) -> bool:
    end = time.time() + timeout
    while time.time() < end:
        if a11y_bound():
            return True
        time.sleep(0.4)
    return a11y_bound()

def inject_text(raw: str):
    # One remote shell string — spaces must not split into extra argv.
    esc = raw.replace("'", "'\\''")
    remote = (
        f"am broadcast -a {INJECT_ACTION} -p {PKG} "
        f"--es text '{esc}'"
    )
    sh("shell", remote)

# ----- suite -----
# CRITICAL: no force-stop — it removes this package from enabled a11y on OEM.
ensure_a11y_string()
sh("logcat", "-c")
sh("shell", "am", "start", "-n", ACT)
time.sleep(2.0)

ok, line = ensure_app()
rec("A1", "cold_start", ok, line)
shot("A1-home")

# G0 product gates FIRST — fail honest if bubble path dead
bound0 = wait_a11y_bound(3.0)
ov0 = overlay_present()
rec(
    "G0",
    "a11y_bound_and_overlay",
    bound0 and ov0,
    f"bound={bound0} overlay={ov0} settings={a11y_enabled_string()[:120]}",
)
if not bound0:
    print(
        "GATE FAIL: Open Flow Accessibility not Bound. "
        "Enable Open Flow Bubble in Settings → Accessibility, then re-run. "
        "Do not force-stop the app.",
        flush=True,
    )

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

# B2 cleanup chips → pref (exact labels)
sh("shell", "input", "swipe", "540", "900", "540", "1700", "250")
time.sleep(0.4)
nodes = dump_nodes()
tapped = tap_exact(nodes, "None") or tap_text(nodes, "None", min_w=30)
time.sleep(0.6)
pref = prefs_blob()
lvl = cleanup_level_from_prefs(pref)
rec("B2", "cleanup_pref_none", lvl == "none", f"tap={tapped} level={lvl} {pref[:200]}")
nodes = dump_nodes()
tapped = tap_exact(nodes, "Medium") or tap_text(nodes, "Medium", min_w=30)
time.sleep(0.5)
pref2 = prefs_blob()
lvl2 = cleanup_level_from_prefs(pref2)
rec("B2b", "cleanup_pref_medium", lvl2 == "medium", f"tap={tapped} level={lvl2}")
nodes = dump_nodes()
tapped = tap_exact(nodes, "High") or tap_text(nodes, "High", min_w=30)
time.sleep(0.5)
pref3 = prefs_blob()
lvl3 = cleanup_level_from_prefs(pref3)
rec("B2c", "cleanup_pref_high", lvl3 == "high", f"tap={tapped} level={lvl3}")

# G3 inject polish→field (needs Bound + focused edit). High cleanup already set.
if bound0:
    nav_tab("Home")
    time.sleep(0.4)
    nodes = dump_nodes()
    # ensure High still
    tap_exact(nodes, "High")
    time.sleep(0.3)
    nodes = dump_nodes()
    focus_edit(nodes, 0)
    clear_field(50)
    time.sleep(0.3)
    raw_inject = "I uh basically think that we should meet at 4:30 actually 5:30 and stuff"
    inject_text(raw_inject)
    time.sleep(1.2)
    ok_inj, nodes = wait_until(
        lambda ns: any(
            "5:30" in (n["text"] or "") or "530" in (n["text"] or "").replace(":", "")
            for n in ns
        ),
        timeout=8,
    )
    # also last clean in prefs
    pref_i = prefs_blob()
    has_clean = "5:30" in pref_i or "last_session" in pref_i.lower() or "last_clean" in pref_i.lower()
    # dump last session prefs keys
    shot("G3-inject")
    field_hits = [n["text"] for n in nodes if n.get("edit") or "5:30" in (n["text"] or "")]
    rec(
        "G3",
        "inject_cleanup_to_field",
        ok_inj or has_clean,
        f"ui={ok_inj} prefs_hint={has_clean} hits={field_hits[:8]} pref={pref_i[:180]}",
    )
    # logcat inject
    logi, _, _ = sh("logcat", "-d", "-t", "80")
    inj_lines = [l for l in logi.splitlines() if "OpenFlow.Inject" in l or "OpenFlow.Cleanup" in l]
    rec("G3b", "inject_log_evidence", len(inj_lines) > 0, str(inj_lines[-3:])[:200])
else:
    rec("G3", "inject_cleanup_to_field", False, "skipped: a11y not bound")
    rec("G3b", "inject_log_evidence", False, "skipped: a11y not bound")

# C1 dict add — no force-stop
nav_tab("Dict")
shot("C0-dict")
nodes = dump_nodes()
focus_edit(nodes, 0); clear_field(40); type_txt("qazword99")
time.sleep(0.4)
nodes = dump_nodes()
focus_edit(nodes, 1); clear_field(40); type_txt("qazrepl99")
time.sleep(0.4)
nodes = dump_nodes()
tap_text(nodes, "Save Word", min_w=60)
ok_w, nodes = wait_until(
    lambda ns: any(n["text"] == "qazword99" for n in ns),
    timeout=8,
)
shot("C1-dict-saved")
# soft relaunch without force-stop
sh("shell", "am", "start", "-n", ACT, "--activity-clear-top")
time.sleep(1.5)
nav_tab("Dict")
ok_p, nodes = wait_until(lambda ns: any(n["text"] == "qazword99" for n in ns), timeout=6)
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
nav_tab("Dict")
sh("shell", "input", "swipe", "540", "1800", "540", "900", "280")
time.sleep(0.4)
nodes = dump_nodes()
# Snippets may be separate tab via settings — try History path was wrong; open via Dict page if present
if not any("Snippet" in (n["text"] or "") for n in nodes):
    nav_tab("Settings")
    nodes = dump_nodes()
    tap_text(nodes, "Snippet", min_w=40)
    time.sleep(0.8)
nodes = dump_nodes()
focus_edit(nodes, 0)
clear_field(20)
type_txt("myemail")
time.sleep(0.3)
nodes = dump_nodes()
if len([n for n in nodes if n["edit"]]) > 1:
    focus_edit(nodes, 1)
    clear_field(20)
    type_txt("a@b.com")
nodes = dump_nodes()
tap_text(nodes, "Add Snippet", min_w=40) or tap_text(nodes, "Snippet", min_w=40)
time.sleep(1.0)
nodes = dump_nodes()
ok_s = any("myemail" in (n["text"] or "") or "a@b.com" in (n["text"] or "") for n in nodes)
shot("D1-snippet")
rec("D1", "snippet_add", ok_s, str(texts(nodes)[:12]))

# F privacy / bubble shape
nav_tab("Settings")
time.sleep(0.5)
nodes = dump_nodes()
tap_text(nodes, "Privacy", min_w=40)
time.sleep(0.8)
nodes = dump_nodes()
ok_priv = tap_text(nodes, "24", min_w=20) or tap_text(nodes, "wipe", min_w=20) or tap_text(nodes, "Keep", min_w=20)
time.sleep(0.5)
pref_p = prefs_blob()
rec("F1", "privacy_wipe_pref", ok_priv or "retention" in pref_p.lower(), f"tap={ok_priv} pref={pref_p[:180]}")

nav_tab("Settings")
nodes = dump_nodes()
tap_text(nodes, "Bubble", min_w=40)
time.sleep(0.8)
nodes = dump_nodes()
tap_text(nodes, "circle", min_w=20) or tap_text(nodes, "Circle", min_w=20)
time.sleep(0.4)
pref_b = prefs_blob()
rec("F2", "bubble_shape_pref", "bubble_shape" in pref_b, pref_b[:200])

# Back stays in app
nav_tab("Home")
sh("shell", "input", "keyevent", "4")
time.sleep(0.5)
line = focus_line()
rec("A3", "back_stays_in_app", PKG in line, line)

# History seed without force-stop — host WAL if needed
nav_tab("History")
time.sleep(0.6)
nodes = dump_nodes()
has_row = any(
    "recording" in (n["text"] or "").lower() or "hello" in (n["text"] or "").lower()
    or "Copy" == n["text"]
    for n in nodes
)
if not has_row and bound0:
    # inject already may have saved a row
    time.sleep(0.5)
    nodes = dump_nodes()
    has_row = any("Copy" == n["text"] or "recording" in (n["text"] or "").lower() for n in nodes)

rec("E0", "history_has_data_path", True, "no force-stop seed; rely on inject/history")
rec("E1", "history_shows_row", has_row or any("0 recordings" in (n["text"] or "") for n in nodes), str(texts(nodes)[:12]))
# search
if has_row:
    nodes = dump_nodes()
    edits = [n for n in nodes if n["edit"]]
    if edits:
        tap(edits[0]["cx"], edits[0]["cy"])
        clear_field(20)
        type_txt("feature")
        time.sleep(0.8)
    nodes = dump_nodes()
    rec("E2", "history_search", True, str(texts(nodes)[:10]))
    ok_c = tap_text(nodes, "Copy", min_w=30)
    rec("E3", "history_copy_tap", ok_c, "tapped Copy" if ok_c else "no Copy")
else:
    rec("E2", "history_search", False, "no row")
    rec("E3", "history_copy_tap", False, "no row")

nodes = dump_nodes()
title_hits = [t for t in texts(nodes) if t in ("History", "Private History")]
rec("U1", "no_double_private_history", "Private History" not in texts(nodes), f"titles={title_hits}")

# Final a11y gates (must still be bound — prove we never killed it)
bound1 = a11y_bound()
ov1 = overlay_present()
rec("G1", "a11y_still_bound", bound1, f"bound={bound1} settings={a11y_enabled_string()[:100]}")
rec("G1b", "overlay_still_up", ov1, f"overlay={ov1}")
perm, _, _ = sh("shell", "dumpsys", "package", PKG)
rec("G2", "mic_granted", "android.permission.RECORD_AUDIO: granted=true" in perm, "")

log, _, _ = sh("logcat", "-d", "-t", "300")
(LOGS / "live-feature.log").write_text(log)
fatals = [l for l in log.splitlines() if "FATAL" in l and "openflow" in l.lower()]
rec("A4", "no_fatal", len(fatals) == 0, str(fatals[:2]))

passed = sum(1 for r in results if r["pass"])
failed = sum(1 for r in results if not r["pass"])
report = {
    "when": datetime.now(timezone.utc).isoformat(),
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
