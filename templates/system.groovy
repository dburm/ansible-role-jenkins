#!groovy

import jenkins.model.Jenkins
import jenkins.model.JenkinsLocationConfiguration
import hudson.slaves.EnvironmentVariablesNodeProperty
import hudson.tasks.Shell

class Actions {
    Actions(out) {
        this.out = out
    }

    def out
    def instance = Jenkins.instance
    def config = JenkinsLocationConfiguration.get()
    def shell = new Shell().getDescriptor()
    Boolean changed

    def defaultConfig = [
            message: '',
            usage_stats: true,
            quiet_period: 5,
            scm_checkout_retry_count: 0,
            shell: '',
            disabled_monitors: []
        ]
    def defaultNaming = [
            strategy: 'default',
            description: '',
            force_existing: false,
        ]

    Boolean compareObjects( Object a, b) {
        return Jenkins.XSTREAM.toXML(a) == Jenkins.XSTREAM.toXML(b)
    }

    void setEnvVars(params) {
        def globalNodeProperties = instance.getGlobalNodeProperties()
        def envVarsNodeProperty = globalNodeProperties.get(EnvironmentVariablesNodeProperty)

        def newEnvVarsNodeProperty = new EnvironmentVariablesNodeProperty()
        params.each { key, val ->
            newEnvVarsNodeProperty.getEnvVars().put(key, val)
        }
        if (!compareObjects(newEnvVarsNodeProperty, envVarsNodeProperty)) {
            if (envVarsNodeProperty) {
                globalNodeProperties.replace(envVarsNodeProperty, newEnvVarsNodeProperty)
            } else {
                globalNodeProperties.add(newEnvVarsNodeProperty)
            }
            changed = true
        }
    }

    void configure(systemParams) {
        def params = defaultConfig + (systemParams ?: [:])

        if (params.location && params.location != config.getUrl()) {
            config.setUrl(params.location)
            config.save()
            changed = true
        }
        if (params.email != null  && params.email != config.getAdminAddress()) {
            config.setAdminAddress(params.email)
            config.save()
            changed = true
        }
        if (params.message != (instance.systemMessage ?: '')) {
            instance.systemMessage = params.message
            changed = true
        }
        if (params.usage_stats != instance.isUsageStatisticsCollected()) {
            instance.setNoUsageStatistics(!params.usage_stats)
            changed = true
        }
        if (params.quiet_period != instance.quietPeriod) {
            instance.quietPeriod = params.quiet_period
            changed = true
        }
        if (params.scm_checkout_retry_count != instance.scmCheckoutRetryCount) {
            instance.scmCheckoutRetryCount = params.scm_checkout_retry_count
            changed = true
        }
        if (params.shell != (shell.shell ?: '')) {
            shell.setShell(params.shell)
            shell.save()
            changed = true
        }
        if (params.restrict_naming) {
            def namingParams = defaultNaming + params.restrict_naming ?: [:]
            def newStrategy
            if (namingParams.strategy == 'default') {
                newStrategy = ProjectNamingStrategy.DEFAULT_NAMING_STRATEGY
            } else {
                newStrategy = new ProjectNamingStrategy.PatternProjectNamingStrategy(
                    namingParams.strategy, namingParams.description,
                    namingParams.force_existing)
            }
            if (!compareObjects(instance.projectNamingStrategy, newStrategy)) {
                instance.projectNamingStrategy = newStrategy
                changed = true
            }
        }
        instance.administrativeMonitors.each {
            Boolean doDisable = params.disabled_monitors.contains(it.id) &&
                                it.isEnabled()
            Boolean doEnable = !(params.disabled_monitors.contains(it.id) ||
                               it.isEnabled())
            if (doDisable) {
                it.disable(true)
                changed = true
            } else if (doEnable) {
                it.disable(false)
                changed = true
            }
        }
        setEnvVars(params.env_vars)
    }
}

def params = new groovy.json.JsonSlurperClassic()
    .parseText('''{{ jenkins.system | to_json}}''')
def actions = new Actions(out)

actions.configure(params)

if (actions.changed) {
    actions.instance.save()
    println 'CHANGED'
} else {
    println 'EXISTS'
}
