#!groovy

import jenkins.model.Jenkins
import jenkins.security.s2m.AdminWhitelistRule
import jenkins.security.s2m.FilePathRuleConfig


class Actions {
    Actions(out) {
        this.out = out
    }

    def out
    def instance = Jenkins.instance
    def classLoader = instance.pluginManager.uberClassLoader
    Boolean changed = false

    def defaultConfig = [
            disable_remember_me: false,
            markup_formatter: [type: 'plain'],
            csp: "sandbox; default-src 'none'; img-src 'self'; style-src 'self';",
            agents: [port: 50000],
            csrf_protection: [
                enabled: true,
                proxy_compat: false
            ],
            remoting_cli: false,
            agent_master_security: [enabled: true],
            sshd_server: -1
        ]

    Boolean compareObjects( Object a, b) {
        return Jenkins.XSTREAM.toXML(a) == Jenkins.XSTREAM.toXML(b)
    }

    void setSSHD(value) {
        def sshDescriptor = instance.getDescriptor('org.jenkinsci.main.modules.sshd.SSHD')
        if (value != sshDescriptor.getPort()) {
            sshDescriptor.setPort(value)
            changed = true
        }
    }

    void setRealm(params) {
        def realm
        switch (params.realm) {
            case 'internal':
                realm = new hudson.security.HudsonPrivateSecurityRealm(params.allow_signup ?: false)
                break
            default:
                realm = hudson.security.SecurityRealm.NO_AUTHENTICATION
        }
        if (!compareObjects(realm, instance.getSecurityRealm())) {
            instance.setSecurityRealm(realm)
            changed = true
        }
    }

    void setAuthStrategy(params) {
        def authStrategy
        switch (params.type) {
           case 'allow-logged-in':
               authStrategy = new hudson.security.FullControlOnceLoggedInAuthorizationStrategy()
               authStrategy.setAllowAnonymousRead(params.allow_anon_read == null ? false : params.allow_anon_read)
               break
           default:
               authStrategy = hudson.security.AuthorizationStrategy.UNSECURED
        }
        if (!compareObjects(authStrategy, instance.getAuthorizationStrategy())) {
            instance.setAuthorizationStrategy(authStrategy)
            changed = true
        }
    }

    void setRememberMe(value) {
        if (value != instance.isDisableRememberMe()) {
             instance.setDisableRememberMe(value)
             changed = true
        }
    }

    void setMarkupFormatter(params) {
        def formatter = new hudson.markup.EscapedMarkupFormatter()
        if (params.type == 'safe_html') {
            try {
                Class<?> formatterClass = classLoader.loadClass('hudson.markup.RawHtmlMarkupFormatter')
                def formatterConstructor = formatterClass.getDeclaredConstructors()[0]
                formatter = formatterConstructor.newInstance(params.disable_syntax ?: false)
            } catch (ClassNotFoundException ex) { }
        }
        if (!compareObjects(formatter, instance.getMarkupFormatter())) {
            instance.setMarkupFormatter(formatter)
            changed = true
        }
    }

    void setCSFRProtection(params) {
        def newIssuer
        if (params.enabled) {
            newIssuer = new hudson.security.csrf.DefaultCrumbIssuer(params.proxy_compat ?: false)
        }
        if (!compareObjects(newIssuer, instance.getCrumbIssuer())) {
            instance.setCrumbIssuer(newIssuer)
            changed = true
        }
    }

    void setRemotingCli(value) {
        def cliDescriptor = instance.getDescriptor('jenkins.CLI')
        if (value != cliDescriptor.enabled) {
            cliDescriptor.enabled = value
            changed = true
        }
    }

    void setCSP(value) {
        //TODO: make CSP permanent
        def currentPolicy = System.getProperty('hudson.model.DirectoryBrowserSupport.CSP')
        if (value != currentPolicy) {
            System.setProperty('hudson.model.DirectoryBrowserSupport.CSP', value)
            changed = true
        }
    }

    void setAgents(params) {
        def port = params.port ?: -1
        if (port != instance.getSlaveAgentPort()) {
            instance.setSlaveAgentPort(port)
            changed = true
        }
        if (params.protocols) {
            Set protocols = new TreeSet(params.protocols)
            if (!instance.getAgentProtocols() == protocols) {
                instance.setAgentProtocols(protocols)
                changed = true
            }
        }
    }

    void setAgentToMasterSecuruty(params) {
        def whitelistRule = instance.injector.getInstance(AdminWhitelistRule)

        //reversed logic. `true` means disabled
        if (!whitelistRule.getMasterKillSwitch() != params.enabled) {
            whitelistRule.setMasterKillSwitch(!params.enabled)
            changed = true
        }

        if (params.whitelist) {
            File whitelistedFile = new File(whitelistRule.whitelisted.toString())
            if (!whitelistedFile.exists()) {
                whitelistRule.whitelisted.set('\n')
            }
            String newWhitelistedContent = params.whitelist
            String whitelistedContent = whitelistedFile.getText('UTF-8')
            if (!newWhitelistedContent.endsWith('\n')) {
                newWhitelistedContent += '\n'
            }
            if (newWhitelistedContent != whitelistedContent) {
                whitelistRule.whitelisted.set(newWhitelistedContent)
                changed = true
            }
        }

        if (params.file_acls) {
            File filePathRulesFile = new File(whitelistRule.filePathRules.toString())

            if (!filePathRulesFile.exists()) {
                filePathRulesFile.createNewFile()
            }

            String filePathRulesContent = filePathRulesFile.getText('UTF-8')
            if (params.file_acls != filePathRulesContent) {
                whitelistRule.filePathRules.parseTest(params.file_acls)
                whitelistRule.filePathRules.set(params.file_acls)
                changed = true
            }
        }
    }

    void configure(securityParams) {
        def params = defaultConfig + (securityParams ?: [:])
        if (params.access_control) {
            setRealm(params.access_control)
        }
        if (params.auth_strategy) {
            setAuthStrategy(params.auth_strategy)
        }
        setSSHD(params.sshd_server)
        setRememberMe(params.disable_remember_me)
        setMarkupFormatter(params.markup_formatter)
        setCSFRProtection(params.csrf_protection)
        setRemotingCli(params.remoting_cli)
        setCSP(params.csp)
        setAgents(params.agents)
        setAgentToMasterSecuruty(params.agent_master_security)
    }
}

def params = new groovy.json.JsonSlurperClassic()
        .parseText('''{{ jenkins.security | to_json}}''')

def actions = new Actions(out)

actions.configure(params)
if (actions.changed) {
    actions.instance.save()
    println 'CHANGED'
} else {
    println 'EXISTS'
}
