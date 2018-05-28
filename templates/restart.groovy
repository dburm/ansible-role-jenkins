#!groovy

import jenkins.model.Jenkins
import hudson.model.RestartListener

class Actions {
    Actions(out) {
        this.out = out
        this.instance = Jenkins.instance
        this.pm = Jenkins.instance.getPluginManager()
        this.uc = Jenkins.instance.getUpdateCenter()
    }

    def out
    def instance
    def pm
    def uc

    def defaultManager = [restart: false]

    void configure(params) {
        def pmParams = defaultManager + params
        // Check if restart required and granted
        Boolean doRestart = pmParams.restart && (uc.requiresRestart || pm.getPlugins().find{ it.isDeleted() } != null)
        Boolean cancelled = false

        if (doRestart) {
            instance.doQuietDown(true, 0) //block, timeout
            while(!cancelled) {
                // Check if restart is not cancelled
                if(instance.isQuietingDown()) {
                    if(RestartListener.isAllReady()) {
                        out.println 'CHANGED'
                        cancelled = true
                    } else {
                        sleep(3000)
                    }
                } else {
                    cancelled = true
                }
            }
        }
    }
}

def actions = new Actions(out)
def params = new groovy.json.JsonSlurperClassic().parseText('''{{ jenkins.plugin_manager | to_json}}''')

actions.configure(params)

