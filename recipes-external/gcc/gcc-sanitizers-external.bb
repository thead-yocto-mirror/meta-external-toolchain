PV = "${GCC_VERSION}"
BINV = "${GCC_VERSION}"

require recipes-devtools/gcc/gcc-sanitizers.inc
require recipes-external/gcc/gcc-no-shared-source.inc
inherit external-toolchain

# Undo various bits we don't want from the upstream include
EXTRA_OECONF = ""
BBCLASSEXTEND = ""
COMPILERDEP = ""

do_package[depends] += "virtual/${MLPREFIX}libc:do_packagedata"
