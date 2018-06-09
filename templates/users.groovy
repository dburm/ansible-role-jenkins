#!groovy

import jenkins.model.Jenkins
import hudson.model.User
import hudson.security.HudsonPrivateSecurityRealm.Details

class Actions {
    Actions(out) {
        this.out = out
        this.instance = Jenkins.instance
        this.userList = User.getAll()
    }

    def out
    def instance
    def userList
    Boolean changed = false
    def defaultUser = [present: true]

    def classLoader = Jenkins.getInstance().pluginManager.uberClassLoader

    Boolean compareObjects( Object a, b) {
        return Jenkins.XSTREAM.toXML(a) == Jenkins.XSTREAM.toXML(b)
    }

    void configure(params) {
        def userParams
        params.each { name, value ->
            userParams = defaultUser + (value ?: [:])

            if (userParams.present) {
                setUser(name, defaultUser + (value ?: [:]))
            } else {
                removeUser(name)
            }
        }
    }

    void setUser(name, params) {
        def user = userList.find { it.id == name }
        def details = user ? user.getProperty(Details) : null
        if (!(user && details)) {
            user = Jenkins.getInstance().securityRealm
                .createAccount(name, params.password)
            details = user.getProperty(Details)
            changed = true
        }
        if (user) {
            if (params.full_name && user.fullName != params.full_name) {
                user.setFullName(params.full_name)
                changed = true
            }
            if (params.description && user.description != params.description) {
                user.setDescription(params.description)
                changed = true
            }
            if (params.password && !details.isPasswordCorrect(params.password)) {
                user.addProperty(Details.fromPlainPassword(params.password))
                changed = true
            }
            if (params.ssh_key) {
               Class<?> sshClass = classLoader.loadClass('org.jenkinsci.main.modules.cli.auth.ssh.UserPropertyImpl')
               def sshProperty = user.getProperty(sshClass)
               def authorizedKeys = sshProperty ? sshProperty.authorizedKeys : ''
               if (authorizedKeys != params.ssh_key) {
                   def sshConstructor = sshClass.getDeclaredConstructor(String)
                   user.addProperty(sshConstructor.newInstance(params.ssh_key))
                   changed = true
               }
            }
            if (params.email) {
                Class<?> emailPropertyClass
                try {
                    emailPropertyClass = classLoader.loadClass('hudson.tasks.Mailer$UserProperty')
                } catch (ClassNotFoundException ex) {}

                if (emailPropertyClass) {
                    def emailConstructor = emailPropertyClass.getDeclaredConstructor(String)
                    def newEmailProperty = emailConstructor.newInstance(params.email)
                    def emailProperty = user.getProperty(emailPropertyClass)
                    if (!compareObjects(emailProperty, newEmailProperty)) {
                        user.addProperty(newEmailProperty)
                        changed = true
                    }
                }
            }
        }
    }

    void removeUser(name) {
        def user = userList.find { it.id == name }
        def details = user ? user.getProperty(Details) : null
        if (details) {
            user.delete()
            changed = true
        }
    }
}

def users = new groovy.json.JsonSlurperClassic()
    .parseText('''{{ jenkins.users | to_json}}''')
def actions = new Actions(out)

actions.configure(users)

if (actions.changed) {
    actions.instance.save()
    println 'CHANGED'
} else {
    println 'EXISTS'
}
