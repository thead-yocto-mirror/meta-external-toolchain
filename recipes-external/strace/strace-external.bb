
require strace.inc

# Basic strace utility
FILES_${PN} = "${bindir}/strace"
RDEPENDS_${PN} = "glibc"
