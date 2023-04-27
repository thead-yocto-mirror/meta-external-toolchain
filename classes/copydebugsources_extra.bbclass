# Handle any source paths owned by us which are not in the assumed path relative to WORKDIR, such as
# sources generated and written in ${B}
def copydebugsources(debugsrcdir, sources, d):
    # The debug src information written out to sourcefile is further processed
    # and copied to the destination here.

    import stat
    import subprocess

    if debugsrcdir and sources:
        sourcefile = d.expand("${WORKDIR}/debugsources.list")
        bb.utils.remove(sourcefile)

        # filenames are null-separated - this is an artefact of the previous use
        # of rpm's debugedit, which was writing them out that way, and the code elsewhere
        # is still assuming that.
        debuglistoutput = '\0'.join(sources) + '\0'
        with open(sourcefile, 'a') as sf:
           sf.write(debuglistoutput)

        dvar = d.getVar('PKGD')
        strip = d.getVar("STRIP")
        objcopy = d.getVar("OBJCOPY")
        workdir = d.getVar("WORKDIR")
        workparentdir = os.path.dirname(os.path.dirname(workdir))
        workbasedir = os.path.basename(os.path.dirname(workdir)) + "/" + os.path.basename(workdir)

        # If build path exists in sourcefile, it means toolchain did not use
        # -fdebug-prefix-map to compile
        if checkbuildpath(sourcefile, d):
            localsrc_prefix = workparentdir + "/"
        else:
            localsrc_prefix = "/usr/src/debug/"

        nosuchdir = []
        basepath = dvar
        for p in debugsrcdir.split("/"):
            basepath = basepath + "/" + p
            if not cpath.exists(basepath):
                nosuchdir.append(basepath)
        bb.utils.mkdirhier(basepath)
        cpath.updatecache(basepath)

        copy_extra = {}
        prefix_map = d.getVar('DEBUG_PREFIX_MAP')
        if prefix_map:
            # Handle any source paths owned by us which are not in the assumed path relative to WORKDIR, such as
            # sources generated and written in ${B}
            import shlex

            prefix_maps = []
            for arg in shlex.split(prefix_map):
                if arg.startswith('-fdebug-prefix-map=') or arg.startswith('-ffile-prefix-map='):
                    _, from_path, to_path = arg.split('=', 2)
                    if to_path and from_path != workdir:
                        # Entries like recipe-sysroot are excluded here
                        prefix_maps.append((from_path, to_path))

            if prefix_maps:
                copy_extra = {}
                srcroot = os.path.join(localsrc_prefix, workbasedir)
                for srcfile in list(sources):
                    if any(srcfile.endswith(i) for i in ['<internal>', '<built-in>']):
                        continue
                    elif 'recipe-sysroot' in srcfile:
                        continue

                    relative = srcfile.replace(localsrc_prefix, '')
                    existing = os.path.join(workparentdir, relative)
                    if not os.path.exists(existing):
                        if prefix_maps:
                            for from_path, to_path in reversed(prefix_maps):
                                if srcfile.startswith(to_path + os.sep):
                                    candidate = os.path.join(from_path, srcfile[len(to_path) + 1:])
                                    if os.path.exists(candidate):
                                        dest = oe.path.join(dvar, debugsrcdir, relative)
                                        copy_extra[candidate] = dest
                                        break
                            else:
                                bb.debug(1, "copydebugsources: '%s' not found" % srcfile)

        for src, dest in copy_extra.items():
            src = os.path.realpath(src)
            bb.utils.mkdirhier(os.path.dirname(dest))
            bb.debug(1, 'copydebugsources: %s -> %s' % (src, dest))
            oe.path.copyhardlink(src, dest)

        # Ignore files from the recipe sysroots (target and native)
        processdebugsrc =  "LC_ALL=C ; sort -z -u '%s' | egrep -v -z '((<internal>|<built-in>)$|/.*recipe-sysroot.*/)' | "
        # We need to ignore files that are not actually ours
        # we do this by only paying attention to items from this package
        processdebugsrc += "fgrep -zw '%s' | "
        # Remove prefix in the source paths
        processdebugsrc += "sed 's#%s##g' | "
        processdebugsrc += "(cd '%s' ; cpio -pd0mlL --no-preserve-owner '%s%s' 2>/dev/null)"

        cmd = processdebugsrc % (sourcefile, workbasedir, localsrc_prefix, workparentdir, dvar, debugsrcdir)
        try:
            subprocess.check_output(cmd, shell=True, stderr=subprocess.STDOUT)
        except subprocess.CalledProcessError:
            # Can "fail" if internal headers/transient sources are attempted
            pass

        # cpio seems to have a bug with -lL together and symbolic links are just copied, not dereferenced.
        # Work around this by manually finding and copying any symbolic links that made it through.
        cmd = "find %s%s -type l -print0 -delete | sed s#%s%s/##g | (cd '%s' ; cpio -pd0mL --no-preserve-owner '%s%s')" % \
                (dvar, debugsrcdir, dvar, debugsrcdir, workparentdir, dvar, debugsrcdir)
        subprocess.check_output(cmd, shell=True, stderr=subprocess.STDOUT)

        # The copy by cpio may have resulted in some empty directories!  Remove these
        cmd = "find %s%s -empty -type d -delete" % (dvar, debugsrcdir)
        subprocess.check_output(cmd, shell=True, stderr=subprocess.STDOUT)

        # Also remove debugsrcdir if its empty
        for p in nosuchdir[::-1]:
            if os.path.exists(p) and not os.listdir(p):
                os.rmdir(p)
