#!groovy

import jenkins.model.Jenkins

class Actions {
    Actions(out) {
        this.out = out
    }

    def out
    def instance = Jenkins.instance
    Boolean changed = false

    def defaultMailer = [
            host: null,
            port: null,
            suffix: null,
            reply_to: null,
            ssl: false,
            charset: 'UTF-8',
            username: null,
            password: null
        ]

    void configure(mailerParams) {
       def mailer = instance.getDescriptor('hudson.tasks.Mailer')
       if (mailer) {
           def params = defaultMailer + mailerParams
           def host = null
           if (params.host) {
               host = params.host.toString()
           }
           if (mailer.smtpHost != host) {
               mailer.smtpHost = host
               changed = true
           }
           def port = null
           if (params.port) {
               port = params.port.toString()
           }
           if (mailer.smtpPort != port) {
               mailer.smtpPort = port
               changed = true
           }
           if (mailer.defaultSuffix != params.suffix) {
               mailer.defaultSuffix = params.suffix
               changed = true
           }
           if (mailer.charset != params.charset) {
               mailer.charset = params.charset
               changed = true
           }
           if (mailer.useSsl != params.ssl) {
               mailer.useSsl = params.ssl
               changed = true
           }
           if (mailer.smtpAuthUsername != params.username) {
               mailer.smtpAuthUsername = params.username
               changed = true
           }
           if (mailer.smtpAuthPassword != params.password) {
               mailer.smtpAuthPassword = hudson.util.Secret.fromString(params.password)
               changed = true
           }
           if (mailer.replyToAddress != params.reply_to) {
               mailer.replyToAddress = params.reply_to
               changed = true
           }
       }
    }
}

def params = new groovy.json.JsonSlurperClassic()
    .parseText('''{{ jenkins.mailer | to_json}}''')
def actions = new Actions(out)

actions.configure(params)

if (actions.changed) {
    actions.instance.save()
    println 'CHANGED'
} else {
    println 'EXISTS'
}
