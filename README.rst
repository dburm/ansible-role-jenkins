===============================
Ansible Role: Jenkins CI client
===============================

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

