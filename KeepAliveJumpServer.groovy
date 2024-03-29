pipeline {
    agent any

    environment {
        REMOTE_CREDENTIALS ='remote4463_login'
        REMOTE_CREDENTIALS2 = 'remote44102_login'
    }

    stages {
        stage('KeepAliveJumpServer') {
                steps {
                    script {
                        try {
                            withCredentials([usernamePassword(credentialsId: REMOTE_CREDENTIALS, passwordVariable: 'REMOTE_PASSWORD', usernameVariable: 'REMOTE_USER')]) {
                                def remote = getRemoteConfig("192.168.44.63", REMOTE_USER, REMOTE_PASSWORD)
                                def keepalive4463Command = "echo 'Keep alive'"
                                sshCommand remote: remote, command: keepalive4463Command
                            }

                            withCredentials([usernamePassword(credentialsId: REMOTE_CREDENTIALS2, passwordVariable: 'REMOTE_PASSWORD', usernameVariable: 'REMOTE_USER')]) {
                                def remote = getRemoteConfig("192.168.44.102", REMOTE_USER, REMOTE_PASSWORD)
                                def keepalive44102Command = "echo 'Keep alive'"
                                sshCommand remote: remote, command: keepalive44102Command
                            }
                        } 
                        catch (Exception e) {
                            echo "Failed to create new version resources in 4463: ${e.getMessage()}"
                        }
                    }
                }
            }
        }
    }

def getRemoteConfig(host, user, password) {
    def remote = [:]
    remote.name = user
    remote.host = host
    remote.user = user
    remote.password = password
    remote.allowAnyHosts = true
    return remote
}