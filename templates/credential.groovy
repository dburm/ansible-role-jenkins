#!groovy

import com.cloudbees.plugins.credentials.domains.Domain
import com.cloudbees.plugins.credentials.CredentialsScope
import com.cloudbees.plugins.credentials.CredentialsProvider
import jenkins.model.Jenkins

Boolean compareObjects( Object a, b) {
    String aXML = Jenkins.XSTREAM.toXML(a).replaceAll(/\{AQA[^\}]+\}/) {
                      hudson.util.Secret.decrypt(it) }
    String bXML = Jenkins.XSTREAM.toXML(b).replaceAll(/\{AQA[^\}]+\}/) {
                      hudson.util.Secret.decrypt(it) }
    return aXML == bXML
}

String credId = '{{ item.key }}'
def jsonSlurper = new groovy.json.JsonSlurperClassic()
def credParams = jsonSlurper.parseText('''{{ item.value | to_json}}''')

// Set defaults
if (credParams.scope == null) { credParams.scope = 'global' }
if (credParams.present == null) {
    credParams.present = true
} else {
    credParams.present = credParams.present.toBoolean()
}

Boolean changed = false

def classLoader = Jenkins.getInstance().pluginManager.uberClassLoader
def creds = CredentialsProvider.lookupCredentials(
        com.cloudbees.plugins.credentials.common.StandardCredentials,
        Jenkins.getInstance()
    )
def globalDomain = Domain.global()
def credentialsStore = Jenkins.instance.getExtensionList(
        'com.cloudbees.plugins.credentials.SystemCredentialsProvider'
    )[0].getStore()

def cred = creds.find { it.id == credId }

if (cred && !credParams.present) {
    credentialsStore.removeCredentials(globalDomain, cred)
    changed = true
}

String credClassName
switch (credParams.type) {
    case 'password':
        credClassName = 'com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl'
        break
    case 'ssh_key':
        credClassName = 'com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey'
        break
}

def credScope
switch (credParams.scope) {
    case 'global':
        credScope = CredentialsScope.GLOBAL
        break
    case 'system':
        credScope = CredentialsScope.SYSTEM
        break
    default:
        credScope = CredentialsScope.GLOBAL
}

Class<?> credClass
// Test if credential type is supported
if (credClassName) {
    try {
        credClass = classLoader.loadClass(credClassName)
    } catch (ClassNotFoundException ex) {
        credClass = null
    }
}

if (credClass && credParams.present) {
    // Create new credential
    def credConstructor
    def newCred
    if (credParams.type == 'password') {
       // Username with password
        credConstructor = credClass.getDeclaredConstructor(
            CredentialsScope,
            String,
            String,
            String,
            String)
        newCred = credConstructor.newInstance(
            credScope,
            credId,
            credParams.description,
            credParams.username,
            credParams.password)
    } else if (credParams.type == 'ssh_key') {
        // User name with SSH key
        def keySource
        Class<?> keyClass
        def keyConstructor

        if (credParams.key_file) {
            keyClass = classLoader.loadClass(credClassName
                + '$FileOnMasterPrivateKeySource')
            keyConstructor = keyClass.getDeclaredConstructor(String)
            keySource = keyConstructor.newInstance(
                credParams.key_file ?: '')
        } else if (credParams.key_file_content) {
            keyClass = classLoader.loadClass(credClassName
                + '$DirectEntryPrivateKeySource')
            keyConstructor = keyClass.getDeclaredConstructor(String)
            keySource = keyConstructor.newInstance(
                credParams.key_file_content ?: '')
        } else {
            keyClass = classLoader.loadClass(credClassName
                + '$UsersPrivateKeySource')
            keyConstructor = keyClass.getDeclaredConstructor()
            keySource = keyConstructor.newInstance()
        }

        credConstructor = credClass.getDeclaredConstructors()[0]
        newCred = credConstructor.newInstance(
            credScope,
            credId,
            credParams.username,
            keySource,
            credParams.passphrase,
            credParams.description)
    }
    if (cred) {
        // Check / Update existing credential
        if (!compareObjects(cred, newCred)) {
            credentialsStore.updateCredentials(
                globalDomain,
                cred,
                newCred)
            changed = true
        }
    } else if (newCred) {
        // Add new credential
        credentialsStore.addCredentials(globalDomain, newCred)
        changed = true
    }
}

if (changed) {
    println 'CHANGED'
} else {
    println 'EXISTS'
}
