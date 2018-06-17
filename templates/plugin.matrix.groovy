#!groovy

import jenkins.model.Jenkins
import hudson.security.Permission

class Actions {
    Actions(out) {
        this.out = out
    }

    def out
    def instance = Jenkins.instance
    Boolean changed = false

    def classLoader = instance.pluginManager.uberClassLoader

    def strategyIdMap = [
            'Overall': 'hudson.model.Hudson',
            'Credentials': 'com.cloudbees.plugins.credentials.CredentialsProvider',
            'Gerrit': 'com.sonyericsson.hudson.plugins.gerrit.trigger.PluginImpl',
            'Agent': 'hudson.model.Computer',
            'Job': 'hudson.model.Item',
            'Run': 'hudson.model.Run',
            'View': 'hudson.model.View',
            'SCM': 'hudson.scm.SCM',
            'Metrics': 'jenkins.metrics.api.Metrics',
            'LockableResources': 'org.jenkins.plugins.lockableresources.LockableResourcesManager',
            'Artifactory': 'org.jfrog.hudson.ArtifactoryPlugin',
        ]

    Boolean compareObjects( Object a, b) {
        return Jenkins.XSTREAM.toXML(a) == Jenkins.XSTREAM.toXML(b)
    }

    Class<?> getStrategyClass(param) {
        String strategyName = 'hudson.security.GlobalMatrixAuthorizationStrategy'
        if (param == 'project_matrix') {
            strategyName = 'hudson.security.ProjectMatrixAuthorizationStrategy'
        }
        try {
            return classLoader.loadClass(strategyName)
        } catch (ClassNotFoundException ex) { }
    }

    def getPermission(param) {
        String permissionId
        String alias
        String field
        (alias, field) = param.tokenize('/')
        if (field) {
            permissionId = [strategyIdMap[alias], field].join('.')
        } else {
            permissionId = param
        }
        try {
             return Permission.fromId(permissionId)
        } catch (Exception ex) { }
    }

    void fillStrategy(strategy, user, params) {
        def permission
        params.each {
            permission = getPermission(it)
            if (permission) {
                strategy.add(permission, user)
            }
        }
    }

    void configure(matrixParams) {
        def strategy = instance.getAuthorizationStrategy()
        Class<?> strategyClass = getStrategyClass(matrixParams.type)
        def newStrategy
        if (strategyClass) {
            newStrategy = strategyClass.getDeclaredConstructor().newInstance()
            matrixParams.permissions.each { user, permissions ->
               fillStrategy(newStrategy, user, permissions)
            }
            if (!compareObjects(strategy, newStrategy)) {
                instance.setAuthorizationStrategy(newStrategy)
                changed = true
            }
        }
    }
}

def params = new groovy.json.JsonSlurperClassic()
    .parseText('''{{ jenkins.security.auth_strategy | to_json}}''')

def actions = new Actions(out)

actions.configure(params)

if (actions.changed) {
    actions.instance.save()
    println 'CHANGED'
} else {
    println 'EXISTS'
}
