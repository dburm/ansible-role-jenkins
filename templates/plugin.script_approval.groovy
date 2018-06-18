#!groovy

import jenkins.model.Jenkins
import org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval

class Actions {
    Actions(out) {
        this.out = out
    }

    def out
    def instance = Jenkins.instance
    def scriptApproval = ScriptApproval.get()
    Boolean changed = false

    Boolean compareObjects( Object a, b) {
        return Jenkins.XSTREAM.toXML(a) == Jenkins.XSTREAM.toXML(b)
    }

    void fillScriptApproval(sApp, params) {
        sApp.clearApprovedSignatures()
        //sApp.clearApprovedScripts()
        //sApp.clearApprovedClasspathEntries()
        if (params) {
            params.approved.each { sApp.approveSignature(it) }
            params.acl_approved.each { sApp.aclApproveSignature(it) }
            //params.script_hashes.each { sApp.approveScript(it) }
            //params.classpath_hashes.each { sApp.approveClasspathEntry(it) }
        }
    }

    void configure(approvalParams) {
        def newScriptApproval = new ScriptApproval()
        fillScriptApproval(newScriptApproval, approvalParams)
        if (!compareObjects(scriptApproval, newScriptApproval)) {
            fillScriptApproval(scriptApproval, approvalParams)
            changed = true
        }
        newScriptApproval = null
    }
}

def params = new groovy.json.JsonSlurperClassic()
    .parseText('''{{ jenkins.script_approval | to_json}}''')
def actions = new Actions(out)

actions.configure(params)

if (actions.changed) {
    actions.scriptApproval.save()
    println 'CHANGED'
} else {
    println 'EXISTS'
}
