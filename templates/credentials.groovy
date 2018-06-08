#!groovy

import com.cloudbees.plugins.credentials.domains.Domain
import com.cloudbees.plugins.credentials.CredentialsScope
import com.cloudbees.plugins.credentials.CredentialsProvider
import jenkins.model.Jenkins

class Actions {
    Actions(out) {
        this.out = out
    }

    def out
    def instance = Jenkins.instance
    Boolean changed

    def classLoader = instance.pluginManager.uberClassLoader
    def jsonSlurper = new groovy.json.JsonSlurperClassic()
    def globalDomain = Domain.global()
    def credentialsStore = instance.getExtensionList(
            'com.cloudbees.plugins.credentials.SystemCredentialsProvider'
        )[0].getStore()
    def creds = CredentialsProvider.lookupCredentials(
            com.cloudbees.plugins.credentials.common.StandardCredentials,
            instance
        )

    def defaultCredential = [
            present:true,
            scope: 'global',
            type: 'password',
            description: ''
    ]

    Boolean compareObjects( Object a, b) {
        String aXML = Jenkins.XSTREAM.toXML(a).replaceAll(/\{AQA[^\}]+\}/) {
                          hudson.util.Secret.decrypt(it) }
        String bXML = Jenkins.XSTREAM.toXML(b).replaceAll(/\{AQA[^\}]+\}/) {
                          hudson.util.Secret.decrypt(it) }
        return aXML == bXML
    }

    Class<?> getCredClass(credClassName) {
        try {
            return classLoader.loadClass(credClassName)
        } catch (ClassNotFoundException ex) {
            return null
        }
    }

    def getCredScope(scope) {
        switch (scope) {
            case 'global':
                return CredentialsScope.GLOBAL
            case 'system':
                return CredentialsScope.SYSTEM
            default:
                return CredentialsScope.GLOBAL
        }
    }

    def getNewCred(credId, credParams) {
        CredentialsScope credScope = getCredScope(credParams.scope)
        switch (credParams.type) {
            case 'password':
                return getNewPasswordCred(credScope, credId, credParams)
            case 'ssh_key':
                return getNewSSHCred(credScope, credId, credParams)
        }
    }

    def getNewPasswordCred(credScope, credId, credParams) {
        Class<?> credClass = getCredClass(
            'com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl')
        def credConstructor
        try {
            credConstructor = credClass.getDeclaredConstructor(
                CredentialsScope, String, String, String, String)
        } catch (ClassNotFoundException ex) {
            return null
        }
        return credConstructor.newInstance(credScope, credId,
            credParams.description, credParams.username, credParams.password)
    }

    def getNewSSHCred(credScope, credId, credParams) {
        Class<?> credClass = getCredClass(
            'com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey')
        def keySource
        def keyConstructor
        Class<?> keyClass
        if (credClass) {
            if (credParams.key_file) {
                keyClass = classLoader.loadClass(credClass.getName()
                    + '$FileOnMasterPrivateKeySource')
                keyConstructor = keyClass.getDeclaredConstructor(String)
                keySource = keyConstructor.newInstance(
                    credParams.key_file ?: '')
            } else if (credParams.key_file_content) {
                keyClass = classLoader.loadClass(credClass.getName()
                    + '$DirectEntryPrivateKeySource')
                keyConstructor = keyClass.getDeclaredConstructor(String)
                keySource = keyConstructor.newInstance(
                    credParams.key_file_content ?: '')
            } else {
                keyClass = classLoader.loadClass(credClass.getName()
                    + '$UsersPrivateKeySource')
                keyConstructor = keyClass.getDeclaredConstructor()
                keySource = keyConstructor.newInstance()
            }
            def credConstructor = credClass.getDeclaredConstructors()[0]
            return credConstructor.newInstance(
                credScope,
                credId,
                credParams.username ?: '',
                keySource,
                credParams.passphrase ?: '',
                credParams.description)
        }
    }

    void configure(params) {
        params.each { credId, value ->
            def credParams = defaultCredential + (value ?: [:])
            if (credParams.present) {
                setCredential(credId, credParams)
            } else {
                removeCredential(credId)
            }
        }
    }

    void removeCredential(credId) {
        def cred = creds.find { it.id == credId }
        if (cred) {
            credentialsStore.removeCredentials(globalDomain, cred)
            changed = true
        }
    }

    void setCredential(credId, credParams) {
        def cred = creds.find { it.id == credId }
        def newCred = getNewCred(credId, credParams)
        if (cred && !compareObjects(cred, newCred)) {
            credentialsStore.updateCredentials(
                globalDomain, cred, newCred)
            changed = true
        } else if (!cred && newCred) {
            credentialsStore.addCredentials(globalDomain, newCred)
            changed = true
        }
    }
}

def credentials = new groovy.json.JsonSlurperClassic()
    .parseText('''{{ jenkins.credentials | to_json}}''')
def actions = new Actions(out)

actions.configure(credentials)

if (actions.changed) {
    actions.instance.save()
    println 'CHANGED'
} else {
    println 'EXISTS'
}
