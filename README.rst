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
Both ommited means to use private key from the Jenkins master ~/.ssh.
Use `present: false` to remove a credential.

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

