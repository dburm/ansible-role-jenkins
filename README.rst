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

