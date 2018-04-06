SUMMARY = "Sanitized set of kernel headers for the C library's use"
HOMEPAGE = "https://www.kernel.org/"
SECTION = "devel"
LICENSE = "GPLv2"

inherit external-toolchain

PV := "${EXTERNAL_LIBC_KERNEL_VERSION}"
PKG_${PN} = "linux-libc-headers"
PKG_${PN}-dev = "linux-libc-headers-dev"

linux_headers_file = "${@bb.utils.which('${FILESPATH}', 'linux.headers')}"
FILES_${PN}-dev = "\
    ${@' '.join('${includedir}/' + f.rstrip() for f in oe.utils.read_file('${linux_headers_file}').splitlines())} \
"
FILES_${PN}-dev[file-checksums] += "${linux_headers_file}"
