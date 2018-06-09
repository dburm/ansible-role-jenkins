#!groovy

import jenkins.model.Jenkins
import jenkins.install.InstallState
import hudson.model.User
import hudson.security.HudsonPrivateSecurityRealm
import hudson.security.HudsonPrivateSecurityRealm.Details

class Actions {
    Actions(out) {
        this.out = out
    }

    def out
    def instance = Jenkins.instance
    def userList = User.getAll()
    Boolean changed = false

    void configure(userName, password) {
        def user = userList.find { it.id == userName }
        def details = user ? user.getProperty(Details) : null
        if (!(user && details)) {
            def hudsonRealm = new HudsonPrivateSecurityRealm(false)
            user = hudsonRealm.createAccount(userName, password)
            instance.setSecurityRealm(hudsonRealm)
            details = user.getProperty(Details)
            changed = true
        }
        if (user) {
            if (!details.isPasswordCorrect(password)) {
                user.addProperty(Details.fromPlainPassword(password))
                changed = true
            }
        }
    }
}


def actions = new Actions(out)

actions.configure('{{ jenkins.user }}', '{{ jenkins.password }}')

if (actions.changed) {
    actions.instance.setInstallState(InstallState.RUNNING)
    actions.instance.save()
    println 'CHANGED'
} else {
    println 'EXISTS'
}
