#!groovy

import hudson.model.Slave
import hudson.model.Node
import hudson.slaves.EnvironmentVariablesNodeProperty
import hudson.slaves.DumbSlave
import jenkins.model.Jenkins
import jenkins.slaves.RemotingWorkDirSettings

class Actions {
    Actions(out) {
        this.out = out
        this.slaves = Jenkins.instance.getNodesObject()
    }

    def slaves
    def out

    Class<?> loadClass(String clazz) {
        def classLoader = Jenkins.instance.pluginManager.uberClassLoader
        try {
            return classLoader.loadClass(clazz)
        } catch (ClassNotFoundException e) {
            out.println "ERROR: Can't load class $clazz"
            out.println '       Perhaps because of missed plugin'
            throw new ClassNotFoundException(e)
        }
    }

    Boolean compareObjects( Object a, b) {
        return Jenkins.XSTREAM.toXML(a) == Jenkins.XSTREAM.toXML(b)
    }

    List nodeProperty (params) {
        List result = []
        if (params.env_vars) {
            List envVars = params.env_vars.vars.collect { it, val ->
                    return new EnvironmentVariablesNodeProperty.Entry(it, val) }
            result.add(new EnvironmentVariablesNodeProperty(envVars))
        }
        if (params.job_env) {
            def clazz = loadClass('org.jenkinsci.plugins.envinject.EnvInjectNodeProperty')
            result.add(clazz.getDeclaredConstructor(Boolean, String).newInstance(
                params.job_env.unset_system_env ?: false, params.job_env.props_file_path ?: ''))
        }
        //TODO: Implement hudson.tools.ToolLocationNodeProperty
        return result
    }

    def launcher(params) {
        def result
        if (params.type == 'jnlp') {
            result = new hudson.slaves.JNLPLauncher(
                params.tunnel ?: '',
                params.jvm_opts ?: '')
            result.workDirSettings = new RemotingWorkDirSettings(
                params.disable_workdir, params.custom_workdir,
                params.internal_data_dir, params.fail_on_missing_workspace)
        } else if (params.type == 'ssh') {
            def strategy = loadClass(['hudson.plugins.sshslaves.verifiers',
                params.host_verification].join('.'))
                .getDeclaredConstructor().newInstance()

            result = new hudson.plugins.sshslaves.SSHLauncher(
                params.host, params.port, params.credential_id, params.jvm_opts,
                params.java_path, params.start_prefix, params.start_suffix, params.timeout,
                params.retry_count, params.retry_wait, strategy)
        }
        return result
    }

    def retStrategy(params) {
        return hudson.slaves.RetentionStrategy
            .forName(['hudson.slaves.RetentionStrategy', params.retention_strategy].join('$'))
            .getDeclaredConstructor().newInstance()
    }

    def defaultAgent = [ present: true,
        remote_home: '/var/lib/jenkins',
        description: '',
        executors: 1,
        mode: 'normal',
        retention_strategy: 'Always',
        labels: [],
        launcher: [type: 'jnlp']]

    def defaultLauncher = [
        'ssh': [
            type: 'ssh',
            host: '',
            credential_id: '',
            host_verification: 'KnownHostsFileKeyVerificationStrategy',
            port: 22,
            java_path: '',
            jvm_opts: '',
            start_prefix: '',
            start_suffix: '',
            timeout: 0,
            retry_count: 0,
            retry_wait: 0
        ],
        'jnlp': [
            type: 'jnlp',
            tunnel: '',
            jvm_opts: '',
            disable_workdir: false,
            custom_workdir: '',
            internal_data_dir: 'remoting',
            fail_on_missing_workspace: false
        ]
    ]


    void setAgent(name, params) {
        String agentName = name
        def agentParams = defaultAgent + params
        agentParams.launcher = defaultLauncher[agentParams.launcher.type] + agentParams.launcher

        Slave agent = slaves.getNode(agentName)
        if (agentParams.present) {
            Slave newAgent = new DumbSlave(
                              agentName,
                              agentParams.description,
                              agentParams.remote_home,
                              agentParams.executors.toString(),
                              Node.Mode.valueOf(agentParams.mode.toUpperCase()),
                              agentParams.labels.join(' '),
                              launcher(agentParams.launcher),
                              retStrategy(agentParams),
                              nodeProperty(agentParams))

            if (compareObjects(newAgent, agent)) {
                out.println 'EXISTS'
            } else {
                if (agent) {
                    slaves.replaceNode(agent, newAgent)
                    out.println 'CHANGED'
                } else {
                    slaves.addNode(newAgent)
                    out.println 'CHANGED'
                }
                Jenkins.instance.save()
            }
        } else if (agent) {
            slaves.removeNode(agent)
            Jenkins.instance.save()
            out.println 'CHANGED'
        } else {
            out.println 'EXISTS'
        }
    }
}

new groovy.json.JsonSlurperClassic().parseText('''{{ jenkins.agents | to_json}}''')
    .each { name, value ->
        new Actions(out).setAgent(name, value)
}
