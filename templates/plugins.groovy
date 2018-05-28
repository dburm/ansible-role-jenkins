#!groovy

import jenkins.model.Jenkins

class Actions {
    Actions(out) {
        this.out = out
        this.instance = Jenkins.instance
        this.pm = Jenkins.instance.getPluginManager()
        this.uc = Jenkins.instance.getUpdateCenter()
        if (this.needToUpdateSites()) {
            this.uc.updateAllSites()
        }
    }

    def out
    def instance
    def pm
    def uc

    def defaultProxy = [
        present: true,
        host: '',
        port: 1,
        user_name: '',
        no_proxy: '',
        password: '',
        test_url: ''
    ]

    def defaultPlugin = [
                present: true,
                enabled: true,
                update: false,
                from_file: '',
    ]

    Boolean changed = false

    Boolean compareObjects( Object a, b) {
        String aXML = Jenkins.XSTREAM.toXML(a).replaceAll(/\{AQA[^\}]+\}/) {
                          hudson.util.Secret.decrypt(it) }
        String bXML = Jenkins.XSTREAM.toXML(b).replaceAll(/\{AQA[^\}]+\}/) {
                          hudson.util.Secret.decrypt(it) }
        return aXML == bXML
    }

    Boolean needToUpdateSites(maxAgeInSec = 1800) {
        long oldestTimeStamp = 0
        for (def site : uc.siteList) {
            if (oldestTimeStamp == 0 || site.getDataTimestamp() < oldestTimeStamp) {
                oldestTimeStamp = site.getDataTimestamp()
            }
        }
        return (System.currentTimeMillis() - oldestTimeStamp) > maxAgeInSec * 1000
    }

    void configure(params) {
        //out.println "Restart required: ${uc.requiresRestart}"
        //def plugin = pm.getPlugin('chucknorris')
        //if (plugin) { out.println plugin.isDeleted() }
        if (params.proxy) {
            proxy(params.proxy)
        }
        if (params.plugins) {
            plugins(params.plugins)
        }
    }

    void proxy(params) {
        def proxyParams = defaultProxy + params
        def newProxy = proxyParams.present ? new hudson.ProxyConfiguration(
            proxyParams.host, proxyParams.port, proxyParams.user_name,
            proxyParams.no_proxy, proxyParams.password, proxyParams.test_url) : null

        if (!compareObjects(newProxy, instance.proxy)) {
            instance.proxy = newProxy
            changed = true
        }
    }

    void plugins(params) {
        params.each { name, value ->
            def pluginParams = defaultPlugin + (value ?: [:])
            if (pluginParams.present) {
                installPlugin(name, pluginParams)
                setPlugin(name, pluginParams.enabled)
            } else {
                removePlugin(name)
            }
        }
    }

    void setPlugin(name, enable) {
        def plugin = pm.getPlugin(name)
        if (plugin) {
            if (enable && !plugin.isEnabled()) {
                plugin.enable()
                changed = true
            } else if (!enable && plugin.isEnabled()) {
                plugin.disable()
                changed = true
            }
        }
    }

    void installPlugin(name, params) {
        def plugin = pm.getPlugin(name) ? null : uc.getPlugin(name)
        if (plugin) {
            def installFuture = plugin.deploy()
            while(!installFuture.isDone()) {
                sleep(3000)
            }
            changed = true
        }
    }

    void removePlugin(name) {
        def plugin = pm.getPlugin(name)
        if (plugin) {
            plugin.doDoUninstall()
            changed = true
        }
    }

    // TODO: Upload plugin with pm.doUploadPlugin(StaplerRequest)
}

def actions = new Actions(out)
def params = new groovy.json.JsonSlurperClassic().parseText('''{{ jenkins.plugin_manager | to_json}}''')

actions.configure(params)

// Get results
if (actions.changed) {
    actions.instance.save()
    println 'CHANGED'
} else {
    println 'EXISTS'
}

