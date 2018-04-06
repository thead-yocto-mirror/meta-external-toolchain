SUMMARY = "mtrace utility provided by glibc"
DESCRIPTION = "mtrace utility provided by glibc"
RDEPENDS_${PN} = "perl"
RPROVIDES_${PN} += "${TCLIBC}-mtrace glibc-mtrace libc-mtrace"

inherit external-toolchain
require recipes-external/glibc/glibc-external-version.inc

PKG_${PN} = "glibc-mtrace"
FILES_${PN} = "\
    ${bindir}/mtrace \
"
