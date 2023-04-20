# Copy debug sources into ${S} where package.bbclass can find them
EXTERNAL_DEBUGSOURCES ?= "0"
EXTERNAL_DEBUGSOURCES:external ?= "1"

# Sane default for external recipes
DEBUG_PREFIX_MAP:append:external = "\
    -fdebug-prefix-map=${S}=/usr/src/debug/${EXTERNAL_PN}/${EXTENDPE}${PV}-${PR} \
"

# Fallback
DEBUG_PREFIX_MAP:prepend:external = "\
    -fdebug-prefix-map=${S}=/usr/src/debug/${EXTERNAL_PN} \
"

inherit package copydebugsources_extra

python external_debugsrc () {
    enabled = d.getVar('EXTERNAL_DEBUGSOURCES')
    prefix_map = d.getVar('DEBUG_PREFIX_MAP')
    if prefix_map and str(enabled) != '0':
        # Handle any source paths owned by us which are not in the assumed path relative to WORKDIR, such as
        # sources generated and written in ${B}
        import shlex
        import subprocess

        prefix_maps = []
        for arg in shlex.split(prefix_map):
            if arg.startswith('-fdebug-prefix-map=') or arg.startswith('-ffile-prefix-map='):
                _, from_path, to_path = arg.split('=', 2)
                if to_path and 'recipe-sysroot' not in from_path:
                    # Entries like recipe-sysroot are excluded here
                    prefix_maps.append((from_path, to_path))

        if prefix_maps:
            seen = set()
            workdir = d.getVar('WORKDIR')
            pn, external_pn = d.getVar('PN'), d.getVar('EXTERNAL_PN')
            for from_path, to_path in reversed(prefix_maps):
                if to_path in seen or (from_path != workdir and not from_path.startswith(workdir + '/')):
                    continue

                to_path = os.path.normpath(to_path)
                sysroot_files = oe.external.find_sysroot_files([to_path], d)
                if sysroot_files and sysroot_files[0]:
                    seen.add(to_path)
                    for entry in sysroot_files:
                        if not entry:
                            continue

                        for sysroot_path in entry:
                            bb.utils.mkdirhier(from_path)
                            oe.path.copyhardlinktree(sysroot_path, from_path)
                            bb.debug(1, "external_debugsrc: '%s' -> '%s'" % (sysroot_path, from_path))
                else:
                    bb.debug(1, "external_debugsrc: '%s' not found" % to_path)

            if not seen:
                bb.note('external_debugsrc: no debug sources found')
}
do_unpack[postfuncs] += "external_debugsrc"
