/*
 * FB Claims — phone bridge.
 * Gives the claims page (web/claims.html, an exact copy of the Claude
 * artifact) the same `window.claude.use(...)` services it uses on claude.ai,
 * but backed by the phone: data is saved in the app's own storage, PDFs go to
 * the phone's Downloads folder and can be sent on WhatsApp.
 */
(function () {
  "use strict";
  var A = window.Android || null;
  var KEY = "fbclaims-data";

  function clone(o) { return o == null ? o : JSON.parse(JSON.stringify(o)); }

  function load() {
    try {
      var s = A ? A.loadData() : localStorage.getItem(KEY);
      if (s) { var o = JSON.parse(s); if (o && typeof o === "object") return o; }
    } catch (e) {}
    return null;
  }
  var store = load() || { claims: {}, config: null };
  if (!store.claims || typeof store.claims !== "object") store.claims = {};

  function persist() {
    var s = JSON.stringify(store);
    try {
      if (A) {
        var r = A.saveData(s);
        if (r !== "ok") return r || "save failed";
      } else {
        localStorage.setItem(KEY, s);
      }
      return null;
    } catch (e) { return String(e && e.message || e); }
  }

  var claimSubs = [], cfgSubs = [];
  function claimSnap() {
    return {
      docs: Object.keys(store.claims).sort().map(function (id) {
        var d = store.claims[id];
        return { id: id, exists: true, data: function () { return clone(d); }, metadata: { fromCache: false, hasPendingWrites: false } };
      }),
      metadata: { fromCache: false, hasPendingWrites: false }
    };
  }
  function cfgSnap() {
    var c = store.config;
    return { id: "main", exists: !!c, data: function () { return c ? clone(c) : undefined; }, metadata: { fromCache: false, hasPendingWrites: false } };
  }
  function emit() {
    setTimeout(function () {
      var s = claimSnap(); claimSubs.forEach(function (f) { try { f(s); } catch (e) { console.error(e); } });
      var c = cfgSnap(); cfgSubs.forEach(function (f) { try { f(c); } catch (e) { console.error(e); } });
    }, 0);
  }
  function write(fn) {
    var before = JSON.stringify(store);
    fn();
    var err = persist();
    if (err) {
      store = JSON.parse(before);
      return Promise.reject({ code: "unavailable", message: "Phone mein save nahi hua: " + err });
    }
    emit();
    return Promise.resolve();
  }

  var db = {
    doc: function (path) {
      if (path !== "config/main") throw new TypeError("Unsupported path: " + path);
      return {
        id: "main", path: path,
        get: function () { return Promise.resolve(cfgSnap()); },
        set: function (v) { return write(function () { store.config = clone(v); }); },
        onSnapshot: function (next) {
          cfgSubs.push(next); setTimeout(function () { next(cfgSnap()); }, 0);
          return function () { cfgSubs = cfgSubs.filter(function (f) { return f !== next; }); };
        }
      };
    },
    collection: function (path) {
      if (path !== "claims") throw new TypeError("Unsupported collection: " + path);
      return {
        path: path,
        doc: function (id) {
          return {
            id: id, path: path + "/" + id,
            get: function () { var d = store.claims[id]; return Promise.resolve({ id: id, exists: !!d, data: function () { return clone(d); } }); },
            set: function (v) { return write(function () { store.claims[id] = clone(v); }); },
            update: function (v) {
              if (!store.claims[id]) return Promise.reject({ code: "invalid_argument", message: "Claim nahi mila" });
              return write(function () { Object.assign(store.claims[id], clone(v)); });
            },
            "delete": function () { return write(function () { delete store.claims[id]; }); }
          };
        },
        get: function () { return Promise.resolve(claimSnap()); },
        onSnapshot: function (next) {
          claimSubs.push(next); setTimeout(function () { next(claimSnap()); }, 0);
          return function () { claimSubs = claimSubs.filter(function (f) { return f !== next; }); };
        }
      };
    }
  };

  var user = {
    can: function () { return Promise.resolve(true); },
    canEdit: function () { return true; },
    isOwner: function () { return true; }
  };

  var downloads = {
    save: function (req) {
      return new Promise(function (resolve, reject) {
        var blob = req.data instanceof Blob ? req.data : new Blob([req.data]);
        var fr = new FileReader();
        fr.onload = function () {
          var b64 = String(fr.result).split(",")[1] || "";
          if (!A) { reject({ code: "unavailable", message: "Phone app ke bahar download nahi" }); return; }
          var out = A.saveFile(String(req.filename || "report.pdf"), b64);
          if (out === "ok") resolve({ status: "saved" });
          else reject({ code: "unavailable", message: out || "save failed" });
        };
        fr.onerror = function () { reject({ code: "bad_request", message: "File tayar nahi hui" }); };
        fr.readAsDataURL(blob);
      });
    }
  };

  var services = { db: db, user: user, downloads: downloads };
  window.claude = {
    use: function (name) { return Promise.resolve(services[name] || null); }
  };

  /* ---------------- Backup / restore ---------------- */
  function today() {
    var d = new Date();
    return d.getFullYear() + "-" + String(d.getMonth() + 1).padStart(2, "0") + "-" + String(d.getDate()).padStart(2, "0");
  }
  window.FBBackup = {
    make: function () {
      var json = JSON.stringify({ app: "FB Claims", format: 1, exportedAt: new Date().toISOString(), claims: store.claims, config: store.config }, null, 1);
      if (!A) return;
      var b64 = btoa(unescape(encodeURIComponent(json)));
      A.saveFile("FB-Claims-backup-" + today() + ".json", b64);
    },
    // Called by the app after the person picks a backup file.
    restore: function (text) {
      var o;
      try { o = JSON.parse(text); } catch (e) { if (A) A.toast("Ye backup file sahi nahi hai"); return; }
      var claims = o && (o.claims || null);
      if (!claims || typeof claims !== "object") { if (A) A.toast("Is file mein claims nahi mile"); return; }
      var n = 0;
      var err = null;
      write(function () {
        Object.keys(claims).forEach(function (id) { store.claims[id] = clone(claims[id]); n++; });
        if (o.config && !store.config) store.config = clone(o.config);
      }).then(function () {
        if (A) A.toast(n + " claims backup se aa gaye");
      }, function (e) { err = e; if (A) A.toast(e.message); });
    }
  };

  function addBackupPanel() {
    var view = document.getElementById("view");
    if (!view || !document.getElementById("cfgBase") || document.getElementById("fbBackup")) return;
    var ver = A && A.versionName ? A.versionName() : "";
    var count = Object.keys(store.claims).length;
    var sec = document.createElement("section");
    sec.className = "panel"; sec.id = "fbBackup";
    sec.innerHTML =
      '<div class="ph"><h2>Data Backup</h2><small>' + count + ' claims is phone mein saved' + (ver ? ' · App ' + ver : '') + '</small></div>' +
      '<div class="pb" style="display:flex;gap:10px;flex-wrap:wrap;align-items:center">' +
      '<button class="btn primary" type="button" id="fbExport">⤓ Backup file banayein</button>' +
      '<button class="btn" type="button" id="fbImport">⤒ Backup restore karein</button>' +
      '<p class="note" style="margin:0;flex-basis:100%">Har hafte backup banayein aur WhatsApp ya Google Drive par rakhein. ' +
      'Naye phone par app install karke "Backup restore karein" se saare claims wapas aa jate hain. Restore purane claims nahi mitata.</p></div>';
    view.appendChild(sec);
    document.getElementById("fbExport").onclick = function () { window.FBBackup.make(); };
    document.getElementById("fbImport").onclick = function () { if (A) A.pickBackup(); };
  }
  document.addEventListener("DOMContentLoaded", function () {
    var view = document.getElementById("view");
    if (!view) return;
    new MutationObserver(addBackupPanel).observe(view, { childList: true });
    addBackupPanel();
  });
})();
