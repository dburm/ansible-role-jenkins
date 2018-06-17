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

    def defaultMailer = [
            host: null,
            port: null,
            suffix: null,
            reply_to: null,
            ssl: false,
            charset: 'UTF-8',
            username: null,
            password: null
        ]

    Boolean compareObjects( Object a, b) {
        return Jenkins.XSTREAM.toXML(a) == Jenkins.XSTREAM.toXML(b)
    }

    void setMailer(mailerParams) {
       def mailer = instance.getDescriptor('hudson.tasks.Mailer')
       if (mailer) {
           def params = defaultMailer + mailerParams
           if (mailer.smtpHost != params.host) {
               mailer.smtpHost = params.host
               changed = true
           }
           def port = null
           if (params.port) {
               port = params.port.toString()
           }
           if (mailer.smtpPort != port) {
               mailer.smtpPort = port
               changed = true
           }
           if (mailer.defaultSuffix != params.suffix) {
               mailer.defaultSuffix = params.suffix
               changed = true
           }
           if (mailer.charset != params.charset) {
               mailer.charset = params.charset
               changed = true
           }
           if (mailer.useSsl != params.ssl) {
               mailer.useSsl = params.ssl
               changed = true
           }
           if (mailer.smtpAuthUsername != params.username) {
               mailer.smtpAuthUsername = params.username
               changed = true
           }
           if (mailer.smtpAuthPassword != params.password) {
               mailer.smtpAuthPassword = hudson.util.Secret.fromString(params.password)
               changed = true
           }
           if (mailer.replyToAddress != params.reply_to) {
               mailer.replyToAddress = params.reply_to
               changed = true
           }
       }
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
        if (params.mailer) {
            setMailer(params.mailer)
        }
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
