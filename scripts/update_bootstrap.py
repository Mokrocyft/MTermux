#!/usr/bin/env python3
"""
Build-Time Bootstrap Updater, APT Lists Pre-Populator & Binary Relocator for Termuxx (com.termuxx).
Runs during APK build to:
1. Fetch the live Termux aarch64 package index & InRelease from packages-cf.termux.dev.
2. Pre-populate var/lib/apt/lists/ inside bootstrap-aarch64.zip so `apt install <pkg>` works
   immediately out of the box without needing `apt update` first.
3. Compare every package in bootstrap-aarch64.zip's var/lib/dpkg/status with the live repository
   and slipstream any newer .deb packages directly into the bootstrap archive.
4. Pre-patch 16-byte tokens ("com.termux/files" -> "com.termuxx/file", "com.termux/cache" -> "com.termuxx/cach")
   so the APK ships 100% up-to-date out of the box.
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

PREFIX_IN_DEB = "data/data/com.termux/files/usr/"
OLD_FILES_16 = b"com.termux/files"
NEW_FILES_16 = b"com.termuxx/file"
OLD_CACHE_16 = b"com.termux/cache"
NEW_CACHE_16 = b"com.termuxx/cach"


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


def main():
    if not os.path.exists(BOOTSTRAP_ZIP):
        print(f"[update_bootstrap] {BOOTSTRAP_ZIP} not found, skipping.")
        return

    print(f"[update_bootstrap] Inspecting {BOOTSTRAP_ZIP} ...")
    work_dir = tempfile.mkdtemp(prefix="termuxx_bs_")
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

                # Pre-populate var/lib/apt/lists/ so `apt install <pkg>` works immediately out of the box!
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
                                new_block_lines = [
                                    f"Package: {pkg_name}",
                                    "Status: install ok installed",
                                ]
                                for k in ("Priority", "Section", "Installed-Size", "Maintainer", "Architecture",
                                          "Multi-Arch", "Version", "Depends", "Pre-Depends", "Recommends",
                                          "Suggests", "Breaks", "Conflicts", "Replaces", "Provides",
                                          "Homepage", "Description"):
                                    if k in repo_info:
                                        new_block_lines.append(f"{k}: {repo_info[k]}")
                                updated_status_blocks.append("\n".join(new_block_lines))
                                continue
                    updated_status_blocks.append(raw)

                if upgraded_count > 0:
                    with open(status_path, "w", encoding="utf-8") as f:
                        f.write("\n\n".join(updated_status_blocks) + "\n\n")
                print(f"[update_bootstrap] Slipstreamed {upgraded_count} updated package(s) into bootstrap.")
            except Exception as e:
                print(f"[update_bootstrap] Live repo sync warning (continuing with base bootstrap): {e}")

        # Configure apt.conf.d/00termuxx inside the bootstrap so apt uses /data/data/com.termuxx/cach/apt
        apt_conf_dir = os.path.join(extract_dir, "etc/apt/apt.conf.d")
        os.makedirs(apt_conf_dir, exist_ok=True)
        with open(os.path.join(apt_conf_dir, "00termuxx"), "w", encoding="utf-8") as f:
            f.write(
                'Dir::Cache "/data/data/com.termuxx/cach/apt";\n'
                'Dir::Cache::archives "/data/data/com.termuxx/cach/apt/archives";\n'
            )

        # Write updated SYMLINKS.txt with com.termuxx/file paths
        if symlinks_map:
            with open(symlinks_path, "w", encoding="utf-8") as f:
                for rel_link, target in sorted(symlinks_map.items()):
                    patched_target = target.replace("com.termux/files", "com.termuxx/file").replace("com.termux/cache", "com.termuxx/cach")
                    patched_link = rel_link.replace("com.termux/files", "com.termuxx/file").replace("com.termux/cache", "com.termuxx/cach")
                    f.write(f"{patched_target}←{patched_link}\n")

        # Pre-patch all regular files in the bootstrap for com.termuxx
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
        print(f"[update_bootstrap] Pre-patched {patched_files} files for com.termuxx.")

        # Repack bootstrap-aarch64.zip
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

    list_entries = []
    for root, dirs, files in os.walk(usr_src):
        rel_root = os.path.relpath(root, usr_src).replace("\\", "/")
        if rel_root == ".":
            rel_root = ""
        for d in list(dirs):
            src_d = os.path.join(root, d)
            rel_d = f"{rel_root}/{d}" if rel_root else d
            list_entries.append(f"/data/data/com.termuxx/file/usr/{rel_d}")
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
            list_entries.append(f"/data/data/com.termuxx/file/usr/{rel_f}")
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
        f.write("/.\n/data\n/data/data\n/data/data/com.termuxx\n/data/data/com.termuxx/file\n/data/data/com.termuxx/file/usr\n")
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
