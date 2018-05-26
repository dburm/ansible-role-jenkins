#!groovy

import jenkins.model.Jenkins
import hudson.model.User
import hudson.security.HudsonPrivateSecurityRealm.Details

Boolean changed = false

String userName = '{{ item.key }}'
String password = '{{ item.value["password"] | default("null") }}'
String fullName = '{{ item.value["full_name"] | default("null") }}'
String description = '''{{ item.value["description"] | default("null") }}'''
String email = '{{ item.value["email"] | default("null") }}'
String sshKey = '''{{ item.value["ssh_key"] | default("null") }}'''
Boolean present = '{{ item.value["present"] | default("true") }}'.toBoolean()

def classLoader = Jenkins.getInstance().pluginManager.uberClassLoader

def user = User.getAll().find { it.id == userName }
def detailsProperty

if (user) {
    detailsProperty = user.getProperty(Details)
}

if (user && detailsProperty && !present) {
    user.delete()
    changed = true
}

if (present) {
    if (!user || !detailsProperty) {
        user = Jenkins.getInstance().securityRealm.createAccount(userName, password)
        detailsProperty = user.getProperty(Details)
        changed = true
    }
}

if (user) {
    if ( fullName != 'null' ) {
        if ( user.fullName != fullName ) {
            user.setFullName(fullName)
            changed = true
        }
    }
    if ( description != 'null' ) {
        if ( user.description != description ) {
            user.setDescription(description)
            changed = true
        }
    }
    if ( password != 'null' ) {
        if (! detailsProperty.isPasswordCorrect(password)) {
            user.addProperty(Details.fromPlainPassword(password))
            changed = true
        }
    }

    if ( email != 'null' ) {
        def newEmailProperty
        // Test Mailer plugin installed
        try {
            newEmailProperty = new hudson.tasks.Mailer.UserProperty(email)
        } catch (Exception e) {
            newEmailProperty = null
        }
        if (newEmailProperty) {
            def emailProperty = user.getProperty(hudson.tasks.Mailer.UserProperty)
            if ( (emailProperty.getAddress() ?: '') != email ) {
                user.addProperty(newEmailProperty)
                changed = true
            }
        }
    }
    if ( sshKey != 'null' ) {
       Class<?> sshClass = classLoader.loadClass('org.jenkinsci.main.modules.cli.auth.ssh.UserPropertyImpl')
       def sshProperty = user.getProperty(sshClass)
       def authorizedKeys = sshProperty ? sshProperty.authorizedKeys : ''
       if (authorizedKeys != sshKey) {
           def sshConstructor = sshClass.getDeclaredConstructor(String)
           user.addProperty(sshConstructor.newInstance(sshKey))
           changed = true
       }
    }
}

if (changed) {
    println 'CHANGED'
} else {
    println 'EXISTS'
}
