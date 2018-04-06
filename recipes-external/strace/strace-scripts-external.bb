
require strace.inc

# Additional perl-scripts
FILES_${PN} = "${bindir}/strace-graph ${bindir}/strace-log-merge"
RDEPENDS_${PN} = "perl"
