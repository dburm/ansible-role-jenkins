#!groovy

import jenkins.model.Jenkins
import hudson.util.Secret
import jenkins.model.IdStrategy

class Actions {
    Actions(out) {
        this.out = out
    }

    def out
    def instance = Jenkins.instance
    Boolean changed = false

    def classLoader = instance.pluginManager.uberClassLoader

    def defaultLDAP = [
            login_case_sensitivity: 'insensitive',
            group_case_sensitivity: 'insensitive',
            disable_email_resolver: false,
            disable_role_backward_compatibility: true,
            enable_cache: false,
            cache_size: 20,
            cache_ttl: 300
        ]

    def defaultServer = [
            server: '',
            root_dn: '',
            allow_blank_root_dn: false,
            user_search_base: '',
            user_search_filter: 'uid={0}',
            group_search_base: '',
            group_search_filter: '',
            group_membership: 'group',
            group_membership_param: '',
            manager_dn: '',
            manager_password: '',
            displayname_attribute: 'displayname',
            email_attribute: 'mail',
            ignore_if_unavailable: false,
            environment_properties: ['':''],
        ]

    Boolean compareObjects( Object a, b) {
        String aXML = Jenkins.XSTREAM.toXML(a).replaceAll(/\{AQA[^\}]+\}/) {
                          Secret.decrypt(it) }
        String bXML = Jenkins.XSTREAM.toXML(b).replaceAll(/\{AQA[^\}]+\}/) {
                          Secret.decrypt(it) }
        return aXML == bXML
    }

    def getGroupMembership(params) {
        try {
            if (params.group_membership == 'group') {
                return classLoader.loadClass('jenkins.security.plugins.ldap.FromGroupSearchLDAPGroupMembershipStrategy')
                    .newInstance(params.group_membership_param)
            }
            return classLoader.loadClass('jenkins.security.plugins.ldap.FromUserRecordLDAPGroupMembershipStrategy')
                .newInstance(params.group_membership_param ?: 'memberOf')
        } catch (ClassNotFoundException ex) { }
    }

    def getConfiguration(params) {
        def serverParams = defaultServer + params
        def server
        try {
            server = classLoader.loadClass('jenkins.security.plugins.ldap.LDAPConfiguration')
                .newInstance(serverParams.server,
                              serverParams.root_dn,
                              serverParams.allow_blank_root_dn,
                              serverParams.manager_dn,
                              Secret.fromString(serverParams.manager_password))
        } catch (ClassNotFoundException ex) { }
        if (server) {
            server.ignoreIfUnavailable = serverParams.ignore_if_unavailable
            server.userSearchBase = serverParams.user_search_base
            server.userSearch = serverParams.user_search_filter
            server.groupSearchBase = serverParams.group_search_base
            server.groupSearchFilter = serverParams.group_search_filter
            server.displayNameAttributeName = serverParams.displayname_attribute
            server.mailAddressAttributeName = serverParams.email_attribute
            server.extraEnvVars = serverParams.environment_properties
            def groupMembership = getGroupMembership(serverParams)
            if (groupMembership) {
                server.groupMembershipStrategy = groupMembership
            }
        }
        return server
    }

    def getStrategyId(param) {
        switch (param) {
            case 'sensitive':
                return new IdStrategy.CaseSensitive()
            case 'sensitive_email':
                return new IdStrategy.CaseSensitiveEmailAddress()
            default:
                return new IdStrategy.CaseInsensitive()
        }
    }

    def getCacheConfig(params) {
        if (params.enable_cache) {
            try {
                return classLoader.loadClass('hudson.security.LDAPSecurityRealm$CacheConfiguration')
                    .newInstance(params.cache_size, params.cache_ttl)
            } catch (ClassNotFoundException ex) { }
        }
    }

    void configure(params) {
        def ldapParams = defaultLDAP + params
        def newServers = ldapParams.servers.collect {
            return getConfiguration(it)
        }
        def realm = instance.getSecurityRealm()
        def newRealm
        try {
            newRealm = classLoader.loadClass('hudson.security.LDAPSecurityRealm')
                .newInstance(newServers,
                ldapParams.disable_email_resolver,
                getCacheConfig(ldapParams),
                getStrategyId(ldapParams.login_case_sensitivity),
                getStrategyId(ldapParams.group_case_sensitivity))
        } catch (ClassNotFoundException ex) { }
        if (newRealm) {
            newRealm.disableRolePrefixing = ldapParams.disable_role_backward_compatibility
            if (!compareObjects(realm, newRealm)) {
                instance.setSecurityRealm(newRealm)
                changed = true
            }
        }
    }
}

def params = new groovy.json.JsonSlurperClassic()
    .parseText('''{{ jenkins.security.access_control.ldap | to_json}}''')
def actions = new Actions(out)

actions.configure(params)

if (actions.changed) {
    actions.instance.save()
    println 'CHANGED'
} else {
    println 'EXISTS'
}
