SUMMARY = "System-Wide Profiler"
DESCRIPTION = "OProfile is a system-wide profiler for Linux systems, capable \
of profiling all running code at low overhead."
HOMEPAGE = "http://oprofile.sourceforge.net/news/"
BUGTRACKER = "http://sourceforge.net/tracker/?group_id=16191&atid=116191"
SECTION = "devel"

inherit external-toolchain

FILES:${PN} = "\
    ${bindir}/ocount \
    ${bindir}/op-* \
    ${bindir}/operf \
    ${bindir}/opannotate \
    ${bindir}/opimport \
    ${bindir}/opgprof \
    ${bindir}/ophelp \
    ${bindir}/opjitconv \
    ${bindir}/oparchive \
    ${bindir}/opreport \
    ${datadir}/oprofile \
"
FILES:${PN}-doc = " \
    ${docdir}/oprofile \
    ${mandir}/man1/ocount \
    ${mandir}/man1/op-* \
    ${mandir}/man1/operf \
    ${mandir}/man1/opannotate \
    ${mandir}/man1/opimport \
    ${mandir}/man1/opgprof \
    ${mandir}/man1/ophelp \
    ${mandir}/man1/opjitconv \
    ${mandir}/man1/opparchive \
    ${mandir}/man1/opreport \
"
FILES:${PN}-staticdev = "${libdir}/oprofile/*.a"

