"""
Smoke test for the newly added modules:
  - trace query (ZS-01/02)
  - recall management (FC-05/06)
  - compliance guard (FC-02/03)
  - scan collect (CJ-01/02/05/06/07)
  - nonconforming disposal (FC-04)
  - package unit / box code (YM-01)
  - analytics (FX-01/03/04)

Usage:
  python scripts/smoke-test-new-modules.py [base_url]

Notes:
  - ASCII-only output so it works on a GBK console.
  - Creates real rows in the database (disposal / collect / package / recall).
  - Release step freezes PROD-SMOKE-0910212002 on purpose (it is a smoke-test batch).
"""

import json
import sys
import time
import urllib.error
import urllib.request

BASE = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8081/api/v1"
PASSWORD = "123456"
PRODUCT_MAIN = "PROD-SMOKE-0910211248"   # ON_SALE, has activated codes
PRODUCT_RECALL = "PROD-SMOKE-0910212002"  # used for recall release test
PLANTING = "RPB202609010001"              # PLANTED
GRAIN = "GRAIN-SMOKE-0910213209"
CODES = ["RC2026091000205617", "RC2026091000205618"]
RUN = time.strftime("%H%M%S")

results = []


def ascii_safe(text):
    if text is None:
        return ""
    return str(text).encode("ascii", "backslashreplace").decode()


def call(method, path, body=None, token=None):
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(BASE + path, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=25) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as err:
        raw = err.read().decode("utf-8", "replace")
        try:
            payload = json.loads(raw)
        except Exception:
            payload = {"code": err.code, "message": raw}
        payload["_http"] = err.code
        return payload


def check(name, ok, detail=""):
    results.append((name, ok))
    print(("PASS  " if ok else "FAIL  ") + name + (("  | " + ascii_safe(detail)) if detail else ""))


def ok_code(resp):
    return resp.get("code") == 0


print("==== smoke test: new modules ====")
print("base:", BASE)

# 1. login
login = call("POST", "/auth/login", {"username": "admin", "password": PASSWORD})
if not ok_code(login):
    print("login failed:", ascii_safe(login.get("message")))
    sys.exit(1)
token = login["data"]["token"]
check("01 login", True, login["data"]["user"]["role"])

# 2. trace search by product batch
r = call("GET", "/rice/trace/search?keyword=" + PRODUCT_MAIN, token=token)
chain = r.get("data", {}).get("chain", {})
nodes = chain.get("nodes", [])
types = [n.get("nodeType") for n in nodes]
check("02 trace/search product batch", ok_code(r) and len(nodes) >= 3,
      "nodes=%d types=%s" % (len(nodes), ",".join(types)))
check("03 chain has timeline/edges/proofs", len(chain.get("timeline", [])) > 0
      and len(chain.get("edges", [])) > 0 and len(chain.get("chainProofs", [])) > 0,
      "timeline=%d edges=%d proofs=%d" % (len(chain.get("timeline", [])), len(chain.get("edges", [])),
                                          len(chain.get("chainProofs", []))))

# 3. impact
r = call("GET", "/rice/trace/impact?businessType=PRODUCT_BATCH&businessId=" + PRODUCT_MAIN, token=token)
imp = r.get("data", {})
check("04 trace/impact", ok_code(r) and "traceCodeCount" in imp,
      "codes=%s scanned=%s regions=%s" % (imp.get("traceCodeCount"), imp.get("scannedCodeCount"),
                                          len(imp.get("regions", []))))

# 4. collect STORE on planting batch
r = call("POST", "/rice/collect", {"code": PLANTING, "action": "STORE", "terminalType": "SCANNER",
                                  "idempotencyKey": f"smoke-store-1-%s" % RUN, "source": "ONLINE"}, token=token)
rec = r.get("data", {})
check("05 collect STORE", ok_code(r) and rec.get("codeType") == "PLANTING_BATCH",
      "codeType=%s result=%s" % (rec.get("codeType"), rec.get("checkResult")))

# 5. idempotency (same key)
r2 = call("POST", "/rice/collect", {"code": PLANTING, "action": "STORE", "terminalType": "SCANNER",
                                   "idempotencyKey": f"smoke-store-1-%s" % RUN, "source": "ONLINE"}, token=token)
check("06 collect idempotency", ok_code(r2) and r2.get("data", {}).get("duplicated") is True,
      "duplicated=%s" % r2.get("data", {}).get("duplicated"))

# 6. mismatched code & action -> WARN
r = call("POST", "/rice/collect", {"code": PRODUCT_MAIN, "action": "STORE", "terminalType": "PDA",
                                  "idempotencyKey": f"smoke-mismatch-1-%s" % RUN, "source": "ONLINE"}, token=token)
check("07 collect mismatch warns", ok_code(r) and r.get("data", {}).get("checkResult") == "WARN",
      "result=%s msg=%s" % (r.get("data", {}).get("checkResult"), r.get("data", {}).get("checkMessage")))

# 7. batch upload (offline) - 2 new + 1 duplicated
batch = {"records": [
    {"code": PLANTING, "action": "STORE", "terminalType": "PDA", "source": "OFFLINE",
     "idempotencyKey": f"smoke-store-1-%s" % RUN},
    {"code": PRODUCT_MAIN, "action": "INSPECT", "terminalType": "PDA", "source": "OFFLINE",
     "idempotencyKey": f"smoke-offline-2-%s" % RUN},
    {"code": GRAIN, "action": "MILL", "terminalType": "PDA", "source": "OFFLINE",
     "idempotencyKey": f"smoke-offline-3-%s" % RUN},
]}
r = call("POST", "/rice/collect/batch", batch, token=token)
summary = r.get("data", {})
check("08 collect batch upload", ok_code(r) and summary.get("duplicated") == 1,
      "accepted=%s duplicated=%s failed=%s" % (summary.get("accepted"), summary.get("duplicated"),
                                               summary.get("failed")))

# 8. manual collect requires reason
r = call("POST", "/rice/collect", {"code": PLANTING, "action": "STORE", "source": "MANUAL",
                                  "idempotencyKey": f"smoke-manual-1-%s" % RUN}, token=token)
check("09 manual collect rejects empty reason", r.get("code") != 0, "code=%s" % r.get("code"))

# 9. disposal flow
r = call("POST", "/rice/quality/disposals", {"sourceType": "MANUAL", "businessType": "PRODUCT_BATCH",
                                            "businessId": PRODUCT_MAIN, "defectType": "MOISTURE",
                                            "defectDesc": "smoke test moisture", "riskLevel": "MEDIUM",
                                            "quantity": 10, "weightKg": 50, "disposition": "ISOLATE"},
         token=token)
disp = r.get("data", {})
disposal_id = disp.get("disposalId")
check("10 disposal create", ok_code(r) and disp.get("status") == "DRAFT",
      "no=%s status=%s" % (disp.get("disposalNo"), disp.get("status")))

check("11 disposal submit", ok_code(call("POST", "/rice/quality/disposals/%s/submit" % disposal_id, token=token)))

r = call("POST", "/rice/quality/disposals/%s/review" % disposal_id, {"approved": True, "comment": "smoke ok"},
         token=token)
check("12 disposal review approve", ok_code(r) and r["data"].get("isolated") is True,
      "status=%s isolated=%s" % (r.get("data", {}).get("status"), r.get("data", {}).get("isolated")))


def isolation_rule(business_id, action):
    resp = call("GET", "/rice/compliance/check?businessType=PRODUCT_BATCH&businessId=%s&action=%s"
                % (business_id, action), token=token)
    rules = resp.get("data", [])
    for rule in rules:
        if rule.get("ruleCode") == "NONCONFORMING_ISOLATED":
            return rule
    return {}


rule = isolation_rule(PRODUCT_MAIN, "ACTIVATE")
check("13 guard blocks isolated batch", rule.get("passed") is False and rule.get("level") == "BLOCK",
      "msg=%s" % rule.get("message"))

# 10. package unit
pkg = call("POST", "/rice/packages", {"unitLevel": "BOX", "productBatchId": PRODUCT_MAIN,
                                      "childCodes": CODES, "packageSpec": "5kg"}, token=token)
unit = pkg.get("data", {})
unit_code = unit.get("unitCode")
check("14 package box create", ok_code(pkg) and unit.get("childCount") == 2,
      "code=%s childCount=%s totalQty=%s" % (unit_code, unit.get("childCount"), unit.get("totalQuantity")))

r = call("GET", "/rice/packages/%s" % unit_code, token=token)
detail = r.get("data", {})
check("15 package expand to trace codes", ok_code(r) and len(detail.get("traceCodes", [])) == 2,
      "expanded=%s" % len(detail.get("traceCodes", [])))

r = call("GET", "/rice/trace/search?keyword=" + unit_code, token=token)
check("16 trace by box code", ok_code(r) and r.get("data", {}).get("resolved", {}).get("matchedType") == "BOX_CODE",
      "matchedType=%s" % r.get("data", {}).get("resolved", {}).get("matchedType"))

# 11. duplicate binding must fail
pkg2 = call("POST", "/rice/packages", {"unitLevel": "BOX", "productBatchId": PRODUCT_MAIN,
                                       "childCodes": CODES}, token=token)
check("17 package rejects duplicate binding", pkg2.get("code") != 0, "code=%s" % pkg2.get("code"))

# 12. dispose close -> isolation released
r = call("POST", "/rice/quality/disposals/%s/close" % disposal_id, {"remark": "smoke close"}, token=token)
check("18 disposal close", ok_code(r) and r["data"].get("isolated") is False,
      "status=%s isolated=%s" % (r.get("data", {}).get("status"), r.get("data", {}).get("isolated")))
rule = isolation_rule(PRODUCT_MAIN, "ACTIVATE")
check("19 guard passes after close", rule.get("passed") is True, "msg=%s" % rule.get("message"))

# 13. recall flow on the throwaway batch
r = call("GET", "/rice/recalls/scope-preview?businessType=PRODUCT_BATCH&businessId=" + PRODUCT_RECALL, token=token)
check("20 recall scope preview", ok_code(r), "codes=%s" % (r.get("data") or {}).get("traceCodeCount"))

r = call("POST", "/rice/recalls", {"triggerType": "QUALITY_FAIL", "triggerBusinessType": "PRODUCT_BATCH",
                                   "triggerBusinessId": PRODUCT_RECALL, "scopeBusinessType": "PRODUCT_BATCH",
                                   "scopeBusinessId": PRODUCT_RECALL, "riskLevel": "HIGH",
                                   "reason": "smoke test recall", "includeTraceCodes": True,
                                   "maxTraceCodeItems": 20}, token=token)
recall = r.get("data", {})
recall_id = recall.get("recallTaskId")
check("21 recall create", ok_code(r) and recall.get("status") == "DRAFT",
      "no=%s items=%s" % (recall.get("recallNo"), len(recall.get("items", []) or [])))

check("22 recall submit", ok_code(call("POST", "/rice/recalls/%s/submit" % recall_id, token=token)))
check("23 recall approve", ok_code(call("POST", "/rice/recalls/%s/approve" % recall_id,
                                        {"comment": "smoke approve"}, token=token)))

r = call("POST", "/rice/recalls/%s/release" % recall_id, {"notifyChannels": ["SMS", "WECHAT"]}, token=token)
check("24 recall release", ok_code(r) and r["data"].get("status") == "RELEASED",
      "status=%s" % r.get("data", {}).get("status"))

rule = isolation_rule(PRODUCT_RECALL, "ACTIVATE")
recall_rule = [x for x in call("GET", "/rice/compliance/check?businessType=PRODUCT_BATCH&businessId=%s&action=ACTIVATE"
                              % PRODUCT_RECALL, token=token).get("data", [])
               if x.get("ruleCode") == "RECALL_FROZEN"]
check("25 guard blocks recalled batch", recall_rule and recall_rule[0].get("passed") is False,
      "msg=%s" % (recall_rule[0].get("message") if recall_rule else "no rule"))

# 14. analytics
r = call("GET", "/rice/analytics/overview", token=token)
data = r.get("data", {})
check("26 analytics overview", ok_code(r) and len(data.get("qualityTrend", [])) == 6
      and "riskProfile" in data,
      "trend=%d warnings=%d score=%s" % (len(data.get("qualityTrend", [])),
                                         len(data.get("warningDistribution", [])),
                                         data.get("riskProfile", {}).get("score")))

# 15. listing endpoints
for name, path in [("27 collect list", "/rice/collect?pageNo=1&pageSize=5"),
                   ("28 disposal list", "/rice/quality/disposals?pageNo=1&pageSize=5"),
                   ("29 package list", "/rice/packages?pageNo=1&pageSize=5"),
                   ("30 recall list", "/rice/recalls?pageNo=1&pageSize=5")]:
    resp = call("GET", path, token=token)
    check(name, ok_code(resp) and resp.get("data", {}).get("total") is not None,
          "total=%s" % resp.get("data", {}).get("total"))


# 16. manual collect + review
r = call("POST", "/rice/collect", {"code": PRODUCT_MAIN, "action": "SHELF", "terminalType": "WEB",
                                  "source": "MANUAL", "reason": "scanner broken",
                                  "idempotencyKey": f"smoke-manual-ok-{RUN}"}, token=token)
manual_rec = r.get("data") or {}
check("31 manual collect pending review", ok_code(r) and manual_rec.get("status") == "PENDING_REVIEW",
      "status=%s" % manual_rec.get("status"))
r = call("POST", "/rice/collect/%s/review" % manual_rec.get("collectId"),
         {"approved": True, "comment": "smoke approve"}, token=token)
check("32 manual collect review", ok_code(r) and (r.get("data") or {}).get("status") == "APPROVED",
      "status=%s" % (r.get("data") or {}).get("status"))

# 17. package upward lookup
r = call("GET", "/rice/packages/trace-code/%s/upward" % CODES[0], token=token)
up = r.get("data") or {}
check("33 package upward", ok_code(r) and up.get("box") is not None,
      "box=%s" % (up.get("box") or {}).get("unitCode"))

# 18. recall item handle + close
items = (recall.get("items") or [])
target = next((i for i in items if i.get("itemType") == "PRODUCT_BATCH"), items[0] if items else None)
if target:
    r = call("POST", "/rice/recalls/items/%s/handle" % target["itemId"],
             {"handleStatus": "OFF_SHELF", "remark": "smoke off shelf"}, token=token)
    check("34 recall item handle", ok_code(r) and (r.get("data") or {}).get("handleStatus") == "OFF_SHELF",
          "status=%s" % (r.get("data") or {}).get("handleStatus"))
else:
    check("34 recall item handle", False, "no items")

r = call("POST", "/rice/recalls/%s/close" % recall_id, {"closeResult": "smoke closed"}, token=token)
closed = r.get("data") or {}
check("35 recall close", ok_code(r) and closed.get("status") == "CLOSED",
      "status=%s recovery=%s" % (closed.get("status"), (closed.get("progress") or {}).get("recoveryRate")))

failed = [n for n, good in results if not good]
print("----")
print("total=%d passed=%d failed=%d" % (len(results), len(results) - len(failed), len(failed)))
if failed:
    print("failed:", ", ".join(failed))
    sys.exit(2)
print("ALL PASS")
