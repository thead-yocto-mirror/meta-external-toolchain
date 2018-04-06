SUMMARY = "Locale data from glibc"

inherit external-toolchain
require recipes-external/glibc/glibc-external-version.inc

FILESEXTRAPATHS_prepend := "${THISDIR}/glibc-external:"
SRC_URI = "\
    file://SUPPORTED \
"

PROVIDES += "virtual/libc-locale"

# Set dynamically generated locales prefix to be as of glibc
BPN = "glibc"
LOCALEBASEPN = "${MLPREFIX}glibc"

PACKAGES = "localedef ${PN}-dbg"
PACKAGES_DYNAMIC = "^locale-base-.* \
                    ^glibc-gconv-.* ^glibc-charmap-.* ^glibc-localedata-.* ^glibc-binary-localedata-.* \
                    ^${MLPREFIX}glibc-gconv$"

# Inherit the libc-package after the PACKAGES variable was ininitialized
inherit libc-package

# Even though some glibc locales are supplied with the toolchain  (${datadir}/locale) lets compile new ones
GLIBC_INTERNAL_USE_BINARY_LOCALE = "compile"

# Binary locales are generated at build time if ENABLE_BINARY_LOCALE_GENERATION
# is set. The idea is to avoid running localedef on the target (at first boot)
# to decrease initial boot time and avoid localedef being killed by the OOM
# killer which used to effectively break i18n on machines with < 128MB RAM.
# DISABLE THIS OPTION unless you really want it, since this option fetchs the
# whole Glibc to create the cross-localedef.
ENABLE_BINARY_LOCALE_GENERATION = "1"

#enable locale generation on these arches
# BINARY_LOCALE_ARCHES is a space separated list of regular expressions
BINARY_LOCALE_ARCHES ?= "arm.* aarch64 i[3-6]86 x86_64 powerpc mips mips64"

# set "1" to use cross-localedef for locale generation
# set "0" for qemu emulation of native localedef for locale generation
LOCALE_GENERATION_WITH_CROSS-LOCALEDEF = "1"

# Create a glibc-binaries package
ALLOW_EMPTY_${BPN}-binaries = "1"
PACKAGES += "${BPN}-binaries"
RRECOMMENDS_${BPN}-binaries =  "${@" ".join([p for p in d.getVar('PACKAGES').split() if p.find("glibc-binary") != -1])}"

# Create a glibc-charmaps package
ALLOW_EMPTY_${BPN}-charmaps = "1"
PACKAGES += "${BPN}-charmaps"
RRECOMMENDS_${BPN}-charmaps =  "${@" ".join([p for p in d.getVar('PACKAGES').split() if p.find("glibc-charmap") != -1])}"

# Create a glibc-gconvs package
ALLOW_EMPTY_${BPN}-gconvs = "1"
PACKAGES += "${BPN}-gconvs"
RRECOMMENDS_${BPN}-gconvs =  "${@" ".join([p for p in d.getVar('PACKAGES').split() if p.find("glibc-gconv") != -1])}"

# Create a glibc-localedatas package
ALLOW_EMPTY_${BPN}-localedatas = "1"
PACKAGES += "${BPN}-localedatas"
RRECOMMENDS_${BPN}-localedatas =  "${@" ".join([p for p in d.getVar('PACKAGES').split() if p.find("glibc-localedata") != -1])}"

# glibc-gconv is dynamically added into PACKAGES, thus
# FILES_glibc-gconv will not be automatically extended in multilib.
# Explicitly add ${MLPREFIX} for FILES_glibc-gconv.
FILES_${MLPREFIX}glibc-gconv = "\
    ${libdir}/gconv/* \
"

# localedef links with libc.so and glibc-collateral.inc inhibits all default deps
# cannot add virtual/libc to DEPENDS, because it would conflict with libc-initial in RSS
# We are going to have localedef as a separate package
DESCRIPTION_localedef = "glibc: compile locale definition files"
FILES_localedef = "\
    ${bindir}/localedef \
"
RDEPENDS_localedef += "glibc"

# Copy the locale-related files to the data directory so the libc-package would work with them
EXTERNAL_EXTRA_FILES += "\
    ${datadir}/i18n \
    ${libdir}/gconv \
    ${datadir}/locale \
    ${@'${localedir}' if d.getVar('GLIBC_INTERNAL_USE_BINARY_LOCALE', True) == 'precompiled' else ''} \
"

python do_install () {
    bb.build.exec_func('external_toolchain_do_install', d)
    bb.build.exec_func('glibc_locale_external_do_install_extra', d)
    bb.build.exec_func('adjust_locale_names', d)
}

python adjust_locale_names () {
    """Align locale charset names with glibc-locale expectations."""
    # Read in supported locales and associated encodings
    supported = {}
    with open(oe.path.join(d.getVar('WORKDIR', True), "SUPPORTED")) as f:
        for line in f.readlines():
            try:
                locale, charset = line.rstrip().split()
            except ValueError:
                continue
            supported[locale] = charset

    # GLIBC_GENERATE_LOCALES var specifies which locales to be generated. empty or "all" means all locales
    to_generate = d.getVar('GLIBC_GENERATE_LOCALES', True)
    if not to_generate or to_generate == 'all':
        to_generate = supported.keys()
    else:
        to_generate = to_generate.split()
        for locale in to_generate:
            if locale not in supported:
                if '.' in locale:
                    charset = locale.split('.')[1]
                else:
                    charset = 'UTF-8'
                    bb.warn("Unsupported locale '%s', assuming encoding '%s'" % (locale, charset))
                supported[locale] = charset

    localedir = oe.path.join(d.getVar('D', True), d.getVar('localedir', True))
    for locale in to_generate:
        if '.' not in locale:
            continue

        locale, charset = locale.split('.', 1)
        if '-' not in charset:
            continue

        oe_name = locale + '.' + charset.lower()
        existing_name = locale + '.' + charset.lower().replace('-', '')
        this_localedir = oe.path.join(localedir, existing_name)
        if os.path.exists(this_localedir):
            bb.debug(1, '%s -> %s' % (this_localedir, oe.path.join(localedir, oe_name)))
            os.rename(this_localedir, oe.path.join(localedir, oe_name))
}

glibc_locale_external_do_install_extra () {

    if [ "${GLIBC_INTERNAL_USE_BINARY_LOCALE}" != "precompiled" ]; then
        rm -rf ${D}${localedir}
    fi

    # Work around localedef failures for non-precompiled
    for locale in bo_CN bo_IN; do
        sed -i -e '/^name_fmt\s/s/""/"???"/' "${D}${datadir}/i18n/locales/$locale"
        if grep -q '^name_fmt.*""' "${D}${datadir}/i18n/locales/$locale"; then
            bbfatal "sed did not fix $locale"
        fi
    done

    # Create base_libdir directory so libc-package would be happy
    mkdir ${D}${base_libdir}

    # Also discard the gcov-modules file as unneeded
    #rm -f ${D}${libdir}/gconv/gconv-modules

    # Update bugus bem_ZM locale
    for locale in bem_ZM; do
        sed -i -e '/^int_curr_symbol\s/s/"<U005A><U004D><U004B><U0020>"/"<U005A><U004D><U0057><U0020>"/' "${D}${datadir}/i18n/locales/$locale"
    done
}
# Yeah, we need to silence the QA up, since ${base_libdir} left untouched
INSANE_SKIP_${PN} += "installed-vs-shipped"

