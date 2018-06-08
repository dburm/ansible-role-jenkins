===============================
Ansible Role: Jenkins CI client
===============================

.. image:: https://travis-ci.org/dburm/ansible-role-jenkins.svg?branch=master
    :target: https://travis-ci.org/dburm/ansible-role-jenkins

Configures Jenkins CI instances

Connection config variables
---------------------------

.. code-block:: yaml

    jenkins:
      host: 'http://jenkins.host/'
      user: admin-user-name
      password: admin-password


Supported sections
==================

System configuration
--------------------

.. code-block:: yaml

    jenkins:
      system:
        location: 'http://jenkins.host/'
        message: ''
        email: ''
        usage_stats: false
        shell: ''
        quiet_period: 0
        scm_checkout_retry_count: 0
        restrict_naming:
          strategy: default
          description: ''
          force_existing: false
        disabled_monitors:
          - hudson.PluginManager$PluginCycleDependenciesMonitor
          - hudson.PluginManager$PluginUpdateMonitor
          - hudson.PluginWrapper$PluginWrapperAdministrativeMonitor
          - hudsonHomeIsFull
          - hudson.diagnosis.NullIdDescriptorMonitor
          - OldData
          - hudson.diagnosis.ReverseProxySetupMonitor
          - hudson.diagnosis.TooManyJobsButNoView
          - hudson.model.UpdateCenter$CoreUpdateMonitor
          - hudson.node_monitors.MonitorMarkedNodeOffline
          - hudson.triggers.SCMTrigger$AdministrativeMonitorImpl
          - jenkins.CLI
          - jenkins.diagnosis.HsErrPidList
          - jenkins.diagnostics.CompletedInitializationMonitor
          - jenkins.diagnostics.SecurityIsOffMonitor
          - jenkins.diagnostics.URICheckEncodingMonitor
          - jenkins.model.DownloadSettings$Warning
          - jenkins.model.Jenkins$EnforceSlaveAgentPortAdministrativeMonitor
          - jenkins.security.RekeySecretAdminMonitor
          - jenkins.security.UpdateSiteWarningsMonitor
          - jenkins.security.csrf.CSRFAdministrativeMonitor
          - slaveToMasterAccessControl
          - jenkins.security.s2m.MasterKillSwitchWarning
          - jenkins.slaves.DeprecatedAgentProtocolMonitor

Plugins
-------

.. code-block:: yaml

    jenkins:
      plugin_manager:
        restart: true
        update: false
        site: "url.to.update.site/updater.json"
        proxy:
          host: "my.proxy.host"
          port: 8080
          user_name: "proxy-user"
          password: "proxy-password"
          test_url: "url.to.check.if.proxy.works"
          no_proxy: "host list to bypass proxy"
        plugins:
          greenballs:
            update: false
            pin: false # TBD
          ldap:
            enabled: false
          chucknorris:
            present: false
          custom_plugin:
            from_file: 'URI.to.jpi' # TBD

Parameters:

  - plugin_manager.restart:

    restart Jenkins if plugin list is changed; default: false

  - plugin_manager.update:

    global update parameter for plugins list

  - plugin_manager.site:

    URI to json file of Update Center; default is
    'https://updates.jenkins-ci.org/update-center.json'

  - plugin_manager.proxy:

    parameters to configure Update Center proxy;
    default `proxy.present: false`

  - plugin_manager.plugins.plugin-name

    defaults: `present: true`; `enabled: true`

    default `update` parameter inherits from `plugin_manager.update`


Users
-----

.. code-block:: yaml

    jenkins:
      users:
        some-user-name:
          present: true
          full_name: "Full Name Of The User"
          description: "Some description"
          password: "some-secret-password"
          ssh_key: |
            user public ssh key
          email: the.user@e.mail

All vars are optional. Omitted var means do not change the field.

Use `present: false` to  remove user.

Credentials
-----------

Global domain is supported only. Default scope is `global`.

Use `present: false` to  remove credential.

Supported types:

- Username with password

.. code-block:: yaml

    jenkins:
      credentials:
        passwd_cred_id:
          type: password
          username: user
          password: passwd
          description: descr

- SSH Username with private key

if `key_file_content` is defined, then `key_file` will be ignored.
Both ommited means to use private key from the Jenkins master ~/.ssh

.. code-block:: yaml

    jenkins:
      credentials:
        ssh_cred_id:
          scope: system
          type: ssh_key
          username: user
          description: descr
          key_file: file_name
          key_file_content: |
            content
          passphrase: pass

Agents
------

Common settings:

.. code-block:: yaml

    jenkins:
      agents:
        myAgentName:
          remote_home: /var/jenkins_home                           # optional
          description: 'SSH Agent'                                 # optional
          executors: 5                                             # optional
          mode: exclusive                                          # optional
          retention_strategy: Demand                               # optional
          labels:                                                  # optional
            - my_label1
            - my_label2
          env_vars:                                                # optional
            varname: varval
          job_env:                                                 # optional
            unset_system_env: true
            props_file_path: 'some/file'

Supported launchers:

- SSH

.. code-block:: yaml

    jenkins:
      agents:
        sshAgentName:
          description: 'SSH Agent'
          mode: normal
          launcher:
            type: ssh
            host: 'agent.host.or.ip'
            credential_id: 'master-cred-id'
            host_verification: NonVerifyingKeyVerificationStrategy # optional
            port: 22444                                            # optional
            java_path: '/path/to/java'                             # optional
            jvm_opts: 'some java opts'                             # optional
            start_prefix: 'some prefix'                            # optional
            start_suffix: 'some suffix'                            # optional
            timeout: 60                                            # optional
            retry_count: 5                                         # optional
            retry_wait: 5                                          # optional

- JNLP

.. code-block:: yaml

    jenkins:
      agents:
        sshAgentName:
          description: 'JNLP Agent'
          retention_strategy: Always
          launcher:
            type: jnlp
            tunnel: 'mytunnel:50000'                               # optional
            jvm_opts: 'some java opts'                             # optional
            disable_workdir: true                                  # optional
            custom_workdir: '/home/jen'                            # optional
            internal_data_dir: 'temp'                              # optional
            fail_on_missing_workspace: true                        # optional

