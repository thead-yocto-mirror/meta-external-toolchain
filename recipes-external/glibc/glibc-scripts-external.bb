SUMMARY = "utility scripts provided by glibc"
DESCRIPTION = "utility scripts provided by glibc"
RDEPENDS_${PN} = "glibc-mtrace libsotruss"
RPROVIDES_${PN}-scripts += "${TCLIBC}-scripts glibc-scripts"

inherit external-toolchain
require recipes-external/glibc/glibc-external-version.inc

PKG_${PN} = "glibc-scripts"

FILES_${PN} = "\
    ${bindir}/xtrace \
    ${bindir}/sotruss \
"

# Modify the installation files a bit
do_install_append() {
    # Avoid bash dependency
    sed -e '1s#bash#sh#; s#$"#"#g' -i "${D}${bindir}/xtrace"
    sed -e '1s#bash#sh#' -i "${D}${bindir}/sotruss"
}
