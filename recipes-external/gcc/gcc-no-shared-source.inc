python () {
    lic_deps = d.getVarFlag('do_populate_lic', 'depends', False)
    d.setVarFlag('do_populate_lic', 'depends', lic_deps.replace('gcc-source-${PV}:do_unpack', ''))
    cfg_deps = d.getVarFlag('do_configure', 'depends', False)
    d.setVarFlag('do_configure', 'depends', cfg_deps.replace('gcc-source-${PV}:do_preconfigure', ''))
    epoch_deps = d.getVarFlag('do_deploy_source_date_epoch', 'depends', False)
    d.setVarFlag('do_deploy_source_date_epoch', 'depends', epoch_deps.replace('gcc-source-${PV}:do_deploy_source_date_epoch', ''))
}

addtask do_fetch
addtask do_unpack before do_install after do_fetch

do_deploy_source_date_epoch () {
    mkdir -p ${SDE_DEPLOYDIR}
    if [ -e ${SDE_FILE} ]; then
        echo "Deploying SDE from ${SDE_FILE} -> ${SDE_DEPLOYDIR}."
        cp -p ${SDE_FILE} ${SDE_DEPLOYDIR}/__source_date_epoch.txt
    else
        echo "${SDE_FILE} not found!"
    fi
}
