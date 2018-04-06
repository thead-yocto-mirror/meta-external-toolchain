SUMMARY = "System call tracing tool"
HOMEPAGE = "http://strace.sourceforge.net"
SECTION = "console/utils"
LICENSE := "BSD"
COMMON_LIC_CHKSUM_BSD = "file://${COMMON_LICENSE_DIR}/BSD;md5=3775480a712fc46a69647678acb234cb"

inherit external-toolchain

EXTERNAL_INSTALL_SOURCE_PATHS =+ "${EXTERNAL_TOOLCHAIN}/${EXTERNAL_TARGET_SYS}/debug-root"

# Ensure that our rdeps are able to be set by shlibs processing
do_package[depends] += "\
    virtual/libc:do_packagedata \
    virtual/${TARGET_PREFIX}compilerlibs:do_packagedata \
"
