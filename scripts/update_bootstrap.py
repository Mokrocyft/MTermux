#!/usr/bin/env python3
"""
Build-Time Bootstrap Downloader, Cache-Aware Updater, OpenSSH Pre-Installer,
APT Lists Pre-Populator & Binary Relocator for MTermux (com.mtermux).
Runs during APK build to:
1. Download JetBrainsMono.ttf and bootstrap-aarch64.zip directly if not already present.
2. If `.bootstrap_patched_mtermux_v5.stamp` is already present (restored from CI cache), skip redundant
   network downloads and .deb slipstreaming for instant cached builds.
3. Otherwise fetch the live Termux aarch64 package index & InRelease, pre-populate var/lib/apt/lists/,
   slipstream updated core .deb packages AND pre-install `openssh` (with all transitive dependencies)
   so `ssh`, `sftp`, `scp`, and `ssh-keygen` work 100% out of the box immediately on first launch!
4. Pre-select default mirror (`etc/termux/chosen_mirrors`) and pre-patch 16-byte tokens
   ("com.termux/files" -> "com.mtermux/file", "com.termux/cache" -> "com.mtermux/cach").
"""

import os
import shutil
import subprocess
import tempfile
import urllib.request
import zipfile

REPO_BASE = "https://packages-cf.termux.dev/apt/termux-main"
INRELEASE_URL = f"{REPO_BASE}/dists/stable/InRelease"
PACKAGES_URL = f"{REPO_BASE}/dists/stable/main/binary-aarch64/Packages"

BOOTSTRAP_ZIP = "app/src/main/cpp/bootstrap-aarch64.zip"
STAMP_FILE = "app/src/main/cpp/.bootstrap_patched_mtermux_v5.stamp"
FONT_FILE = "app/src/main/assets/fonts/JetBrainsMono.ttf"

BOOTSTRAP_URLS = [
    "https://github.com/termux/termux-packages/releases/latest/download/bootstrap-aarch64.zip",
    "https://github.com/termux/termux-packages/releases/download/bootstrap-2026.02.12-r1%2Bapt.android-7/bootstrap-aarch64.zip",
]
FONT_URL = "https://raw.githubusercontent.com/google/fonts/main/ofl/jetbrainsmono/JetBrainsMono%5Bwght%5D.ttf"

# Packages to pre-install (along with all their transitive Depends) into bootstrap-aarch64.zip
EXTRA_PREINSTALLED_PKGS = ["openssh"]

PREFIX_IN_DEB = "data/data/com.termux/files/usr/"
OLD_FILES_16 = b"com.termux/files"
NEW_FILES_16 = b"com.mtermux/file"
OLD_CACHE_16 = b"com.termux/cache"
NEW_CACHE_16 = b"com.mtermux/cach"


def ensure_font_downloaded():
    if os.path.exists(FONT_FILE) and os.path.getsize(FONT_FILE) > 50_000:
        print(f"[update_bootstrap] Cached font found: {FONT_FILE}")
        return
    os.makedirs(os.path.dirname(FONT_FILE), exist_ok=True)
    print(f"[update_bootstrap] Downloading JetBrainsMono.ttf from {FONT_URL} ...")
    req = urllib.request.Request(FONT_URL, headers={"User-Agent": "termux-pkg/2.0"})
    with urllib.request.urlopen(req, timeout=45) as resp, open(FONT_FILE, "wb") as out:
        shutil.copyfileobj(resp, out)
    print(f"[update_bootstrap] Downloaded {FONT_FILE} ({os.path.getsize(FONT_FILE):,} bytes).")


def ensure_raw_bootstrap_downloaded():
    if os.path.exists(BOOTSTRAP_ZIP) and os.path.getsize(BOOTSTRAP_ZIP) > 38_000_000:
        return
    os.makedirs(os.path.dirname(BOOTSTRAP_ZIP), exist_ok=True)
    for url in BOOTSTRAP_URLS:
        try:
            print(f"[update_bootstrap] Downloading bootstrap-aarch64.zip from {url} ...")
            req = urllib.request.Request(url, headers={"User-Agent": "termux-pkg/2.0"})
            with urllib.request.urlopen(req, timeout=60) as resp, open(BOOTSTRAP_ZIP, "wb") as out:
                shutil.copyfileobj(resp, out)
            if os.path.getsize(BOOTSTRAP_ZIP) > 10_000_000:
                print(f"[update_bootstrap] Downloaded {BOOTSTRAP_ZIP} ({os.path.getsize(BOOTSTRAP_ZIP):,} bytes).")
                return
        except Exception as e:
            print(f"[update_bootstrap] Download attempt failed ({url}): {e}")
    raise RuntimeError("Failed to download bootstrap-aarch64.zip")


def parse_stanzas(text):
    stanzas = []
    for raw in text.strip().split("\n\n"):
        if not raw.strip():
            continue
        fields = {}
        last_key = None
        for line in raw.splitlines():
            if line.startswith((" ", "\t")) and last_key:
                fields[last_key] += "\n" + line
            elif ":" in line:
                k, v = line.split(":", 1)
                last_key = k.strip()
                fields[last_key] = v.strip()
        if fields:
            stanzas.append((raw, fields))
    return stanzas


def parse_dep_names(dep_str):
    if not dep_str:
        return []
    names = []
    for clause in dep_str.split(","):
        first_alt = clause.split("|")[0].strip()
        pkg = first_alt.split("(")[0].strip()
        if pkg:
            names.append(pkg)
    return names


def format_status_stanza(pkg_name, repo_info):
    lines = [
        f"Package: {pkg_name}",
        "Status: install ok installed",
    ]
    for k in (
        "Priority", "Section", "Installed-Size", "Maintainer", "Architecture",
        "Multi-Arch", "Version", "Depends", "Pre-Depends", "Recommends",
        "Suggests", "Breaks", "Conflicts", "Replaces", "Provides",
        "Homepage", "Description"
    ):
        if k in repo_info:
            lines.append(f"{k}: {repo_info[k]}")
    return "\n".join(lines)


def main():
    ensure_font_downloaded()

    if (
        os.path.exists(STAMP_FILE)
        and os.path.exists(BOOTSTRAP_ZIP)
        and os.path.getsize(BOOTSTRAP_ZIP) > 10_000_000
    ):
        print(f"[update_bootstrap] Using cached pre-patched {BOOTSTRAP_ZIP} ({os.path.getsize(BOOTSTRAP_ZIP):,} bytes).")
        return

    ensure_raw_bootstrap_downloaded()

    print(f"[update_bootstrap] Inspecting {BOOTSTRAP_ZIP} ...")
    work_dir = tempfile.mkdtemp(prefix="mtermux_bs_")
    extract_dir = os.path.join(work_dir, "root")
    os.makedirs(extract_dir, exist_ok=True)

    try:
        with zipfile.ZipFile(BOOTSTRAP_ZIP, "r") as zf:
            zf.extractall(extract_dir)

        symlinks_path = os.path.join(extract_dir, "SYMLINKS.txt")
        symlinks_map = {}
        if os.path.exists(symlinks_path):
            with open(symlinks_path, "r", encoding="utf-8") as f:
                for line in f:
                    line = line.rstrip("\r\n")
                    if "←" in line:
                        target, rel_link = line.split("←", 1)
                        symlinks_map[rel_link] = target

        status_path = os.path.join(extract_dir, "var/lib/dpkg/status")
        if os.path.exists(status_path):
            try:
                print(f"[update_bootstrap] Fetching live package index from {PACKAGES_URL} ...")
                req = urllib.request.Request(PACKAGES_URL, headers={"User-Agent": "termux-pkg/2.0"})
                with urllib.request.urlopen(req, timeout=30) as resp:
                    repo_bytes = resp.read()
                    repo_text = repo_bytes.decode("utf-8", errors="replace")

                apt_lists_dir = os.path.join(extract_dir, "var/lib/apt/lists")
                os.makedirs(os.path.join(apt_lists_dir, "partial"), exist_ok=True)
                pkg_list_file = os.path.join(
                    apt_lists_dir,
                    "packages-cf.termux.dev_apt_termux-main_dists_stable_main_binary-aarch64_Packages"
                )
                with open(pkg_list_file, "wb") as f:
                    f.write(repo_bytes)

                try:
                    inrel_req = urllib.request.Request(INRELEASE_URL, headers={"User-Agent": "termux-pkg/2.0"})
                    with urllib.request.urlopen(inrel_req, timeout=30) as inrel_resp:
                        inrel_bytes = inrel_resp.read()
                    inrel_file = os.path.join(
                        apt_lists_dir,
                        "packages-cf.termux.dev_apt_termux-main_dists_stable_InRelease"
                    )
                    with open(inrel_file, "wb") as f:
                        f.write(inrel_bytes)
                    print("[update_bootstrap] Pre-populated var/lib/apt/lists/ with live InRelease & Packages.")
                except Exception as e:
                    print(f"[update_bootstrap] InRelease fetch note: {e}")

                repo_pkgs = {}
                for raw, fields in parse_stanzas(repo_text):
                    pkg_name = fields.get("Package")
                    if pkg_name:
                        repo_pkgs[pkg_name] = fields

                with open(status_path, "r", encoding="utf-8") as f:
                    status_text = f.read()

                status_stanzas = parse_stanzas(status_text)
                installed_pkgs = {fields.get("Package") for _, fields in status_stanzas if fields.get("Package")}
                updated_status_blocks = []
                upgraded_count = 0

                for raw, fields in status_stanzas:
                    pkg_name = fields.get("Package")
                    inst_ver = fields.get("Version")
                    repo_info = repo_pkgs.get(pkg_name)
                    if repo_info and inst_ver and repo_info.get("Version") != inst_ver:
                        new_ver = repo_info["Version"]
                        deb_rel = repo_info.get("Filename")
                        if deb_rel:
                            print(f"[update_bootstrap] Upgrading {pkg_name}: {inst_ver} -> {new_ver}")
                            if slipstream_deb(work_dir, extract_dir, symlinks_map, pkg_name, f"{REPO_BASE}/{deb_rel}"):
                                upgraded_count += 1
                                updated_status_blocks.append(format_status_stanza(pkg_name, repo_info))
                                continue
                    updated_status_blocks.append(raw)

                # Resolve and pre-install EXTRA_PREINSTALLED_PKGS (openssh + transitive dependencies)
                install_order = []
                visited = set(installed_pkgs)

                def resolve_deps_dfs(pkg):
                    if pkg in visited or pkg not in repo_pkgs:
                        return
                    visited.add(pkg)
                    info = repo_pkgs[pkg]
                    deps = parse_dep_names(info.get("Pre-Depends", "")) + parse_dep_names(info.get("Depends", ""))
                    for d in deps:
                        resolve_deps_dfs(d)
                    install_order.append(pkg)

                for extra_pkg in EXTRA_PREINSTALLED_PKGS:
                    resolve_deps_dfs(extra_pkg)

                installed_extra_count = 0
                for pkg_name in install_order:
                    repo_info = repo_pkgs.get(pkg_name)
                    deb_rel = repo_info.get("Filename") if repo_info else None
                    if deb_rel:
                        print(f"[update_bootstrap] Pre-installing {pkg_name} ({repo_info.get('Version')}) ...")
                        if slipstream_deb(work_dir, extract_dir, symlinks_map, pkg_name, f"{REPO_BASE}/{deb_rel}"):
                            installed_extra_count += 1
                            updated_status_blocks.append(format_status_stanza(pkg_name, repo_info))

                if upgraded_count > 0 or installed_extra_count > 0:
                    with open(status_path, "w", encoding="utf-8") as f:
                        f.write("\n\n".join(updated_status_blocks) + "\n\n")
                print(
                    f"[update_bootstrap] Slipstreamed {upgraded_count} updated package(s) and "
                    f"pre-installed {installed_extra_count} extra package(s) (openssh stack)."
                )
            except Exception as e:
                print(f"[update_bootstrap] Live repo sync warning (continuing with base bootstrap): {e}")

        apt_conf_dir = os.path.join(extract_dir, "etc/apt/apt.conf.d")
        os.makedirs(apt_conf_dir, exist_ok=True)
        with open(os.path.join(apt_conf_dir, "00mtermux"), "w", encoding="utf-8") as f:
            f.write(
                'Dir::Cache "/data/data/com.mtermux/cach/apt";\n'
                'Dir::Cache::archives "/data/data/com.mtermux/cach/apt/archives";\n'
            )

        if "etc/termux/chosen_mirrors" not in symlinks_map:
            symlinks_map["etc/termux/chosen_mirrors"] = "/data/data/com.mtermux/file/usr/etc/termux/mirrors/default"

        if symlinks_map:
            with open(symlinks_path, "w", encoding="utf-8") as f:
                for rel_link, target in sorted(symlinks_map.items()):
                    patched_target = target.replace("com.termux/files", "com.mtermux/file").replace("com.termux/cache", "com.mtermux/cach")
                    patched_link = rel_link.replace("com.termux/files", "com.mtermux/file").replace("com.termux/cache", "com.mtermux/cach")
                    f.write(f"{patched_target}←{patched_link}\n")

        patched_files = 0
        for root, dirs, files in os.walk(extract_dir):
            for fname in files:
                if fname == "SYMLINKS.txt":
                    continue
                fpath = os.path.join(root, fname)
                if os.path.islink(fpath):
                    continue
                with open(fpath, "rb") as f:
                    data = f.read()
                if OLD_FILES_16 in data or OLD_CACHE_16 in data:
                    data = data.replace(OLD_FILES_16, NEW_FILES_16).replace(OLD_CACHE_16, NEW_CACHE_16)
                    with open(fpath, "wb") as f:
                        f.write(data)
                    patched_files += 1
        print(f"[update_bootstrap] Pre-patched {patched_files} files for com.mtermux.")

        tmp_zip = BOOTSTRAP_ZIP + ".new"
        with zipfile.ZipFile(tmp_zip, "w", zipfile.ZIP_DEFLATED, compresslevel=6) as zf:
            for root, dirs, files in os.walk(extract_dir):
                dirs.sort()
                for d in dirs:
                    full_d = os.path.join(root, d)
                    if os.path.islink(full_d):
                        continue
                    rel_d = os.path.relpath(full_d, extract_dir).replace("\\", "/") + "/"
                    zf.writestr(rel_d, b"")
                for fname in sorted(files):
                    full_f = os.path.join(root, fname)
                    if os.path.islink(full_f):
                        continue
                    rel_f = os.path.relpath(full_f, extract_dir).replace("\\", "/")
                    zf.write(full_f, rel_f)

        os.replace(tmp_zip, BOOTSTRAP_ZIP)
        with open(STAMP_FILE, "w", encoding="utf-8") as sf:
            sf.write("patched_mtermux_v5_with_openssh\n")
        print(f"[update_bootstrap] Repacked {BOOTSTRAP_ZIP} ({os.path.getsize(BOOTSTRAP_ZIP):,} bytes).")
    finally:
        shutil.rmtree(work_dir, ignore_errors=True)


def slipstream_deb(work_dir, extract_dir, symlinks_map, pkg_name, deb_url):
    deb_dir = tempfile.mkdtemp(dir=work_dir, prefix=f"deb_{pkg_name}_")
    deb_file = os.path.join(deb_dir, f"{pkg_name}.deb")
    req = urllib.request.Request(deb_url, headers={"User-Agent": "termux-pkg/2.0"})
    with urllib.request.urlopen(req, timeout=30) as resp, open(deb_file, "wb") as out:
        shutil.copyfileobj(resp, out)

    data_dir = os.path.join(deb_dir, "data")
    ctrl_dir = os.path.join(deb_dir, "ctrl")
    os.makedirs(data_dir, exist_ok=True)
    os.makedirs(ctrl_dir, exist_ok=True)

    subprocess.run(["dpkg-deb", "-x", deb_file, data_dir], check=True)
    subprocess.run(["dpkg-deb", "-e", deb_file, ctrl_dir], check=True)

    usr_src = os.path.join(data_dir, PREFIX_IN_DEB)
    if not os.path.isdir(usr_src):
        return False

    old_list_file = os.path.join(extract_dir, "var/lib/dpkg/info", f"{pkg_name}.list")
    old_dirs = []
    if os.path.exists(old_list_file):
        with open(old_list_file, "r", encoding="utf-8", errors="ignore") as olf:
            for line in olf:
                p = line.strip()
                for pref in ("/data/data/com.termux/files/usr/", "/data/data/com.mtermux/file/usr/"):
                    if p.startswith(pref):
                        rel_old = p[len(pref):]
                        if not rel_old:
                            continue
                        if rel_old in symlinks_map:
                            del symlinks_map[rel_old]
                        full_old = os.path.join(extract_dir, rel_old)
                        if os.path.isfile(full_old) or os.path.islink(full_old):
                            try:
                                os.remove(full_old)
                            except OSError:
                                pass
                        elif os.path.isdir(full_old):
                            old_dirs.append(full_old)
        for d in sorted(old_dirs, key=len, reverse=True):
            try:
                os.rmdir(d)
            except OSError:
                pass

    list_entries = []
    for root, dirs, files in os.walk(usr_src):
        rel_root = os.path.relpath(root, usr_src).replace("\\", "/")
        if rel_root == ".":
            rel_root = ""
        for d in list(dirs):
            src_d = os.path.join(root, d)
            rel_d = f"{rel_root}/{d}" if rel_root else d
            list_entries.append(f"/data/data/com.mtermux/file/usr/{rel_d}")
            dst_d = os.path.join(extract_dir, rel_d)
            if os.path.islink(src_d):
                target = os.readlink(src_d)
                symlinks_map[rel_d] = target
                dirs.remove(d)
                if os.path.lexists(dst_d):
                    if os.path.isdir(dst_d) and not os.path.islink(dst_d):
                        shutil.rmtree(dst_d, ignore_errors=True)
                    else:
                        os.remove(dst_d)
            else:
                os.makedirs(dst_d, exist_ok=True)
        for fname in files:
            src_f = os.path.join(root, fname)
            rel_f = f"{rel_root}/{fname}" if rel_root else fname
            list_entries.append(f"/data/data/com.mtermux/file/usr/{rel_f}")
            dst_f = os.path.join(extract_dir, rel_f)
            os.makedirs(os.path.dirname(dst_f), exist_ok=True)
            if os.path.islink(src_f):
                target = os.readlink(src_f)
                symlinks_map[rel_f] = target
                if os.path.lexists(dst_f):
                    os.remove(dst_f)
            else:
                if rel_f in symlinks_map:
                    del symlinks_map[rel_f]
                if os.path.lexists(dst_f) and os.path.islink(dst_f):
                    os.remove(dst_f)
                shutil.copy2(src_f, dst_f)

    info_dir = os.path.join(extract_dir, "var/lib/dpkg/info")
    os.makedirs(info_dir, exist_ok=True)
    with open(os.path.join(info_dir, f"{pkg_name}.list"), "w", encoding="utf-8") as f:
        f.write("/.\n/data\n/data/data\n/data/data/com.mtermux\n/data/data/com.mtermux/file\n/data/data/com.mtermux/file/usr\n")
        for item in sorted(list_entries):
            f.write(item + "\n")

    for ctrl_name in os.listdir(ctrl_dir):
        if ctrl_name == "control":
            continue
        src_c = os.path.join(ctrl_dir, ctrl_name)
        if os.path.isfile(src_c):
            dst_c = os.path.join(info_dir, f"{pkg_name}.{ctrl_name}")
            shutil.copy2(src_c, dst_c)
    return True


if __name__ == "__main__":
    main()
