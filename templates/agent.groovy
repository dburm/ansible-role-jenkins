#!groovy

import hudson.model.Slave
import hudson.model.Node
import hudson.slaves.EnvironmentVariablesNodeProperty
import hudson.slaves.DumbSlave
import hudson.slaves.RetentionStrategy
import jenkins.model.Jenkins
import jenkins.slaves.RemotingWorkDirSettings

Boolean compareObjects( Object a, b) {
    return Jenkins.XSTREAM.toXML(a) == Jenkins.XSTREAM.toXML(b)
}

def jsonSlurper = new groovy.json.JsonSlurperClassic()

def agentParams = jsonSlurper.parseText('''{{ item.value | to_json}}''')
String agentName = '{{ item.key }}'

def classLoader = Jenkins.getInstance().pluginManager.uberClassLoader

// Set defaults
if (agentParams.env_vars) {
    if (agentParams.env_vars.vars && agentParams.env_vars.enabled == null) {
        agentParams.env_vars.enabled = true
    }
} else {
    agentParams.env_vars.enabled = false
}
if (agentParams.job_env) {
    if (agentParams.job_env.enabled == null &&
        !(agentParams.job_env.props_file_path == null &&
          agentParams.job_env.unset_system_env == null)) {
          agentParams.job_env.enabled = true
    }
} else {
    agentParams.job_env.enabled = false
}
if (!agentParams.remote_home) { agentParams.remote_home = '/var/lib/jenkins' }
if (!agentParams.labels) { agentParams.labels = [] }
if (!agentParams.mode) { agentParams.mode = 'normal' }
if (!agentParams.retention_strategy) { agentParams.retention_strategy = 'Always' }
if (agentParams.executors) {
    agentParams.executors = agentParams.executors.toString()
} else { agentParams.executors = '1' }

agent = Jenkins.instance.slaves.find { it.name = agentName }

//Create new NodeProperty
List nodeProperty = []
if (agentParams.env_vars.enabled) {
    List envVars = agentParams.env_vars.vars.collect { it, val ->
            return new EnvironmentVariablesNodeProperty.Entry(it, val) }
    envProperty = new EnvironmentVariablesNodeProperty(envVars)
    nodeProperty.add(envProperty)
}
if (agentParams.job_env.enabled) {
    def jobEnvProperty = org.jenkinsci.plugins.envinject.EnvInjectNodeProperty(
        agentParams.job_env.unset_system_env ?: false,
        agentParams.job_env.props_file_path ?: '')
    nodeProperty.add(jobEnvProperty)
}

//TODO: Implement hudson.tools.ToolLocationNodeProperty

//Create new launcher
def newLauncher
if (agentParams.launcher.type == 'jnlp') {
    newLauncher = new hudson.slaves.JNLPLauncher(
        agentParams.launcher.tunnel ?: '',
        agentParams.jvm_opts ?: '')
    newLauncher.workDirSettings = new RemotingWorkDirSettings(
        agentParams.launcher.disable_workdir ?: false,
        agentParams.launcher.custom_workdir ?: '',
        agentParams.launcher.internal_data_dir ?: 'remoting',
        agentParams.launcher.fail_on_missing_workspace ?: false)
} else if (agentParams.launcher.type == 'ssh') {
    if (!agentParams.host_verification) {
        agentParams.host_verification = 'KnownHostsFileKeyVerificationStrategy'
    }

    strategy = classLoader.loadClass(['hudson.plugins.sshslaves.verifiers', agentParams.host_verification].join('.'))
        .getDeclaredConstructor().newInstance()

    newLauncher = new hudson.plugins.sshslaves.SSHLauncher(
        agentParams.host ?: '',
        agentParams.port ?: 22,
        agentParams.credential_id ?: '',
        agentParams.jvm_opts ?: '',
        agentParams.java_path ?: '',
        agentParams.start_prefix ?: '',
        agentParams.start_suffix ?: '',
        agentParams.timeout ?: 0,
        agentParams.retry_count ?: 0,
        agentParams.retry_wait ?: 0,
        strategy)
}

def newRetStrategy = RetentionStrategy
    .forName(['hudson.slaves.RetentionStrategy', agentParams.retention_strategy].join('$'))
    .getDeclaredConstructor().newInstance()

//Create new agent
Slave newAgent = new DumbSlave(
                  agentName,
                  agentParams.description ?: '',
                  agentParams.remote_home,
                  agentParams.executors,
                  Node.Mode.valueOf(agentParams.mode.toUpperCase()),
                  agentParams.labels.join(' '),
                  newLauncher,
                  newRetStrategy,
                  nodeProperty)
// Check / Update / Install agent
if (compareObjects(newAgent, agent)) {
    println 'EXISTS'
} else {
    Jenkins.instance.addNode(newAgent)
    println 'CHANGED'
}

