require recipes-external/gcc/gcc-external.inc
inherit external-toolchain-cross

PN .= "-${TARGET_ARCH}"
DEPENDS += "virtual/${TARGET_PREFIX}binutils virtual/${TARGET_PREFIX}libc-for-gcc ${EXTRADEPENDS}"
EXTRADEPENDS = ""
python () {
    if d.getVar("TARGET_OS").startswith("linux"):
        d.setVar("EXTRADEPENDS", "linux-libc-headers")
}

PROVIDES += "\
    virtual/${TARGET_PREFIX}gcc-initial \
    virtual/${TARGET_PREFIX}gcc-intermediate \
    virtual/${TARGET_PREFIX}gcc \
    virtual/${TARGET_PREFIX}g++ \
"

EXTERNAL_CROSS_BINARIES = "${gcc_binaries}"
