SUMMARY = "System call tracing tool"
HOMEPAGE = "http://strace.sourceforge.net"
SECTION = "console/utils"
LICENSE := "BSD"
COMMON_LIC_CHKSUM_BSD = "file://${COMMON_LICENSE_DIR}/BSD;md5=3775480a712fc46a69647678acb234cb"

PV = "${@get_strace_version(d)}"

inherit external-toolchain

EXTERNAL_INSTALL_SOURCE_PATHS =+ "${EXTERNAL_TOOLCHAIN}/${EXTERNAL_TARGET_SYS}/debug-root"

def get_strace_version(d):
    import re

    output = external_run(d, 'ct-ng.config', '')
    for line in output.splitlines():
        result = re.match(r'CT_STRACE_VERSION="(\d{1,}\.\d{1,})"', line)
        if result:
            return result.group(1)

    return "4.10"

# Ensure that our rdeps are able to be set by shlibs processing
do_package[depends] += "\
    virtual/libc:do_packagedata \
    virtual/${TARGET_PREFIX}compilerlibs:do_packagedata \
"
