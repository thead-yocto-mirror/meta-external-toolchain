# OpenEmbedded/Yocto layer for external toolchain support

## Dependencies

- [openembedded-core](https://github.com/openembedded/openembedded-core)
  layer, with a matching branch (i.e. master of oe-core and master of
  meta-external-toolchain).
- [bitbake](https://github.com/openembedded/bitbake), with a matching branch.
- An installed toolchain or OE/Yocto SDK
- An existing build directory configured for this bitbake and
  openembedded-core.

## Usage & Instructions

- Cd to an existing build directory.
- `bitbake-layers add-layer /path/to/meta-external-toolchain`.
- Set `EXTERNAL_TOOLCHAIN = "/path/to/your/external-toolchain-install"` in
  `conf/local.conf`.

### If using a Yocto/OpenEmbedded SDK as an external toolchain

- Set `TCMODE = "external-oe-sdk"` in `conf/local.conf`.

### If using a different external toolchain

A non-OE/Yocto external toolchain may well require additional tweaks, we would
advise creating a custom tcmode based upon the `external` one, much as we do for
`external-oe-sdk` in this layer.

- Set `TCMODE = "external"` in `conf/local.conf`

## Behavior

The tcmode performs a number of operations:

- Sets `TARGET_PREFIX` appropriately, after determining what prefix is in use by the toolchain
- Sanity checks `EXTERNAL_TOOLCHAIN`: does the path exist? does the expected sysroot exist?
- Sets preferences so that external recipes are used in preference to building
  them from source, including cross recipes which link/wrap the toolchain
  cross binaries

## Contributing

To contribute to this layer, please fork and submit pull requests to the
github [repository](https://github.com/MentorEmbedded/meta-external-toolchain), or open
issues for any bugs you find, or feature requests you have.

## Maintainer

This layer is maintained by [Siemens Digital Industries
Software](https://support.sw.siemens.com/). Please direct all support requests
for this layer to the GitHub repository issues interface.

## To Do List

See [TODO.md](TODO.md).
