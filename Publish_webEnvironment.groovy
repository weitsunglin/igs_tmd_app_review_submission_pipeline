pipeline {
    agent any

    environment {
        REMOTE_CREDENTIALS = 'remote44102_login'
    }

    parameters {
        string(name: 'BUILDER_NAME', defaultValue: 'weitsunglin', description: '你的名字')
        choice(name: 'ENVIRONMENT_TYPE', choices: ['production', 'iostesting'], description: '環境類型:送審後production/送審前iostesting')
    }

    stages {
        stage('Fix WebEnvironment') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: REMOTE_CREDENTIALS, passwordVariable: 'PASSWORD', usernameVariable: 'USER')]) {
                        try {
                            def remote = getRemoteConfig('192.168.44.102', USER, PASSWORD)
                            def sourceFilePath = ''
                            def destinationFilePath = 'D:/webGame/Game/TMD_mobile/data/iostesting/Inanna/InannaLua/WebEnvironment.xml'

                            if (params.ENVIRONMENT_TYPE == 'production') {
                                sourceFilePath = 'C:/Users/Administrator/Desktop/TMD_Client/送審專用/jenkins_update_cdn_gcp/WebEnv_production/WebEnvironment.xml'
                            } else if (params.ENVIRONMENT_TYPE == 'iostesting') {
                                sourceFilePath = 'C:/Users/Administrator/Desktop/TMD_Client/送審專用/jenkins_update_cdn_gcp/WebEnv_iostesting/WebEnvironment.xml'
                            }

                            def command = "powershell -Command \"Copy-Item -Path '${sourceFilePath}' -Destination '${destinationFilePath}' -Force\""
                            sshCommand remote: remote, command: command
                        } catch (Exception e) {
                            echo "An error occurred: ${e.getMessage()}"
                            throw e
                        }
                    }
                }
            }
        }

        stage('Deploy to CDN') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: REMOTE_CREDENTIALS, passwordVariable: 'PASSWORD', usernameVariable: 'USER')]) {
                        try {
                            def remote = getRemoteConfig('192.168.44.102', USER, PASSWORD)
                            def command = "C:/Users/Administrator/Desktop/TMD_Client/送審專用/jenkins_update_cdn_gcp/jenkins_cdn_change_webEnvoroment.bat"
                            sshCommand remote: remote, command: command
                        } catch (Exception e) {
                            echo "An error occurred during deployment: ${e.getMessage()}"
                            throw e
                        }
                    }
                }
            }
        }

        stage('Deploy to GCP') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: REMOTE_CREDENTIALS, passwordVariable: 'PASSWORD', usernameVariable: 'USER')]) {
                        try {
                            def remote = getRemoteConfig('192.168.44.102', USER, PASSWORD)
                            def command = "C:/Users/Administrator/Desktop/TMD_Client/送審專用/jenkins_update_cdn_gcp/jenkins_gcp_change_webEnvoroment.bat"
                            sshCommand remote: remote, command: command
                        } catch (Exception e) {
                            echo "Deployment to GCP failed with error: ${e.getMessage()}"
                            throw e
                        }
                    }
                }
            }
        }
    }
    post {
        always {
            emailext(
                subject: "${currentBuild.currentResult} Deployment WebEnvironment.xml Notification",
                body: "${currentBuild.currentResult}, Environment Type: ${params.ENVIRONMENT_TYPE}",
                to: "${params.BUILDER_NAME}@igs.com.tw"
            )
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