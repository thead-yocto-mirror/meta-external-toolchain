SRC_URI = "\
    file://nscd.init;subdir=${REL_S}/nscd \
    file://nscd.conf;subdir=${REL_S}/nscd \
    file://nscd.service;subdir=${REL_S}/nscd \
"
REL_S = "${@os.path.relpath('${S}', '${WORKDIR}')}"

require recipes-core/glibc/glibc-common.inc
inherit external-toolchain
require recipes-external/glibc/glibc-external-version.inc

DEPENDS += "virtual/${TARGET_PREFIX}binutils"
PROVIDES += "glibc \
             virtual/${TARGET_PREFIX}libc-for-gcc \
             virtual/${TARGET_PREFIX}libc-initial \
             virtual/libc \
             virtual/libintl \
             virtual/libiconv \
             virtual/crypt"
PACKAGES_DYNAMIC = ""

def get_external_libc_license(d):
    errnosearch = os.path.join(d.getVar('includedir', True), 'errno.h')
    found = oe.external.find_sysroot_files([errnosearch], d)
    if found:
        errno_paths = found[0]
        if errno_paths:
            with open(errno_paths[0], 'rU') as f:
                text = f.read()

            lictext = """   The GNU C Library is free software; you can redistribute it and/or
   modify it under the terms of the GNU Lesser General Public
   License as published by the Free Software Foundation; either
   version 2.1 of the License, or (at your option) any later version."""

            if lictext in text:
                return 'LGPL-2.1+'

    return 'UNKNOWN'

LICENSE := "${@get_external_libc_license(d)}"

require recipes-external/glibc/glibc-sysroot-setup.inc
require recipes-external/glibc/glibc-package-adjusted.inc

FILES_MIRRORS .= "\
    ${base_sbindir}/|/usr/bin/ \n\
    ${base_sbindir}/|/usr/${baselib}/bin/ \n\
    ${sbindir}/|/usr/bin/ \n\
    ${sbindir}/|/usr/${baselib}/bin/ \n\
"

python do_install () {
    bb.build.exec_func('external_toolchain_do_install', d)
    bb.build.exec_func('glibc_external_do_install_extra', d)
}

# Remove stash locale task as one unneeded anymore, since glibc-locale is in separate package
deltask do_stash_locale
deltask do_poststash_install_cleanup

glibc_external_do_install_extra () {
    mkdir -p ${D}${sysconfdir}
    touch ${D}${sysconfdir}/ld.so.conf

    if [ ! -e ${D}${libdir}/libc.so ]; then
        bbfatal "Unable to locate installed libc.so file (${libdir}/libc.so)." \
                "This may mean that your external toolchain uses a different" \
                "multi-lib setup than your machine configuration"
    fi

    # Avoid bash dependency
    sed -e '1s#bash#sh#; s#$"#"#g' -i "${D}${bindir}/ldd"
    sed -e '1s#bash#sh#' -i "${D}${bindir}/tzselect"
}

bberror_task-install () {
    # Silence any errors from oe_multilib_header, as we don't care about
    # missing multilib headers, as the oe-core glibc version isn't necessarily
    # the same as our own.
    :
}

# These files are picked up out of the sysroot by glibc-locale, so we don't
# need to keep them around ourselves.
do_install_locale_append() {
	rm -rf ${D}${localedir}
}

python () {
    # Undo the do_install_append which joined shell to python
    install = d.getVar('do_install', False)
    python, shell = install.split('rm -f ', 1)
    d.setVar('do_install_glibc', 'rm -f ' + shell)
    d.setVarFlag('do_install_glibc', 'func', '1')
    new_install = python + '\n    bb.build.exec_func("do_install_glibc", d)\n'
    d.setVar('do_install', new_install.replace('\t', '    '))

    # Ensure that we pick up just libm, not all libs that start with m
    baselibs = d.getVar('libc_baselibs', False)
    baselibs = baselibs.replace('${base_libdir}/libm*.so.*', '${base_libdir}/libm.so.* ${base_libdir}/libmvec.so.*')
    d.setVar('libc_baselibs', baselibs)
}

# Default pattern is too greedy
FILES_${PN}-utils = "\
    ${bindir}/gencat \
    ${bindir}/getconf \
    ${bindir}/getent \
    ${bindir}/iconv \
    ${sbindir}/iconvconfig \
    ${bindir}/lddlibc4 \
    ${bindir}/ldd \
    ${bindir}/locale \
    ${bindir}/makedb \
    ${bindir}/pcprofiledump \
    ${bindir}/pldd \
    ${bindir}/sprof \
"
FILES_${PN}-doc += "${infodir}/libc.info*"

FILES_${PN}-dev_remove := "${datadir}/aclocal"

# Add multilib headers created in the glibc-package.inc by calling oe_multilib_header
FILES_${PN}-dev_remove = "/lib/*.o"
FILES_${PN}-dev += " \
    ${libdir}/*crt*.o \
    ${includedir}/bits/syscall* \
    ${includedir}/bits/long-double* \
"

libc_baselibs_dev += "${@' '.join('${libdir}/' + os.path.basename(l.replace('${SOLIBS}', '${SOLIBSDEV}')) for l in '${libc_baselibs}'.replace('${base_libdir}/ld*${SOLIBS}', '').split() if l.endswith('${SOLIBS}'))}"
FILES_${PN}-staticdev = "\
    ${@'${libc_baselibs_dev}'.replace('${SOLIBSDEV}', '.a')} \
    ${libdir}/libg.a \
    ${libdir}/libieee.a \
    ${libdir}/libmcheck.a \
"

FILES_${PN}-dev += "\
    ${libc_baselibs_dev} \
    ${libdir}/libcidn${SOLIBSDEV} \
    ${libdir}/libthread_db${SOLIBSDEV} \
    ${libdir}/libpthread${SOLIBSDEV} \
"
libc_headers_file = "${@bb.utils.which('${FILESPATH}', 'libc.headers')}"
FILES_${PN}-dev += "\
    ${@' '.join('${includedir}/' + f.rstrip() for f in oe.utils.read_file('${libc_headers_file}').splitlines())} \
"
FILES_${PN}-dev[file-checksums] += "${libc_headers_file}"

# glibc's utils need libgcc
do_package[depends] += "${MLPREFIX}libgcc:do_packagedata"
do_package_write_ipk[depends] += "${MLPREFIX}libgcc:do_packagedata"
do_package_write_deb[depends] += "${MLPREFIX}libgcc:do_packagedata"
do_package_write_rpm[depends] += "${MLPREFIX}libgcc:do_packagedata"

FILES_${PN}-dev_remove = "${base_libdir}/*_nonshared.a ${libdir}/*_nonshared.a"
FILES_${PN}-dev += "${libdir}/libc_nonshared.a ${libdir}/libpthread_nonshared.a ${libdir}/libmvec_nonshared.a"
