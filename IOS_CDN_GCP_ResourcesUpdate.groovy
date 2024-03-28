pipeline {
    agent any

    environment {
        REMOTE_CREDENTIALS = 'remote44102_login'
    }

    parameters {
        string(name: 'BUILDER_NAME', defaultValue: 'weitsunglin', description: '你的名字')
    }

    stages {

        stage('複製外部遊戲資源到送審遊戲資源') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: REMOTE_CREDENTIALS, passwordVariable: 'PASSWORD', usernameVariable: 'USER')]) {
                        try {
                            def remote = getRemoteConfig('192.168.44.102', USER, PASSWORD)
                            // Clear iostesting directory
                            def clearCommand = "powershell -Command \"Remove-Item -Path 'D:\\webGame\\Game\\TMD_mobile\\data\\iostesting\\*' -Recurse -Force\""
                            sshCommand remote: remote, command: clearCommand
                                    
                            // Copy iOS resources to iostesting directory
                            def copyCommand = "powershell -Command \"Copy-Item -Path 'D:\\webGame\\Game\\TMD_mobile\\data\\ios\\*' -Destination 'D:\\webGame\\Game\\TMD_mobile\\data\\iostesting' -Recurse -Force\""
                            sshCommand remote: remote, command: copyCommand
                        } catch (Exception e) {
                            echo "An error occurred: ${e.getMessage()}"
                        }
                    }
                }
            }
        }

        stage('GCP更新Resource') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: REMOTE_CREDENTIALS, passwordVariable: 'PASSWORD', usernameVariable: 'USER')]) {
                        try {
                            def remote = getRemoteConfig('192.168.44.102', USER, PASSWORD)
                            def command = "C:/Users/Administrator/Desktop/TMD_Client/送審專用/jenkins_update_cdn_gcp/ios/1_GCP更新Resource.bat"
                            sshCommand remote: remote, command: command
                        } catch (Exception e) {
                            echo "Error updating GCP resources: ${e.getMessage()}"
                        }
                    }
                }
            }
        }

        stage('GCP清除快取資源') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: REMOTE_CREDENTIALS, passwordVariable: 'PASSWORD', usernameVariable: 'USER')]) {
                        try {
                            def remote = getRemoteConfig('192.168.44.102', USER, PASSWORD)
                            def command = "C:/Users/Administrator/Desktop/TMD_Client/送審專用/jenkins_update_cdn_gcp/ios/2_GCP清除快取資源.bat"
                            sshCommand remote: remote, command: command
                        } catch (Exception e) {
                            echo "Error in GCP清除快取資源: ${e.getMessage()}"
                        }
                    }
                }
            }
        }

        stage('CDN更新Resource') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: REMOTE_CREDENTIALS, passwordVariable: 'PASSWORD', usernameVariable: 'USER')]) {
                        try {
                            def remote = getRemoteConfig('192.168.44.102', USER, PASSWORD)
                            def command = "C:/Users/Administrator/Desktop/TMD_Client/送審專用/jenkins_update_cdn_gcp/ios/3_CDN更新Resource.bat"
                            sshCommand remote: remote, command: command
                        } catch (Exception e) {
                            echo "Error in CDN更新Resource: ${e.getMessage()}"
                        }
                    }
                }
            }
        }


        stage('GCP更新Version') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: REMOTE_CREDENTIALS, passwordVariable: 'PASSWORD', usernameVariable: 'USER')]) {
                        try {
                            def remote = getRemoteConfig('192.168.44.102', USER, PASSWORD)
                            def command = "C:/Users/Administrator/Desktop/TMD_Client/送審專用/jenkins_update_cdn_gcp/ios/5_GCP更新Version.bat"
                            sshCommand remote: remote, command: command
                        } catch (Exception e) {
                            echo "Error in GCP更新Version: ${e.getMessage()}"
                        }
                    }
                }
            }
        }

        stage('GCP清除快取資源2') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: REMOTE_CREDENTIALS, passwordVariable: 'PASSWORD', usernameVariable: 'USER')]) {
                        try {
                            def remote = getRemoteConfig('192.168.44.102', USER, PASSWORD)
                            def command = "C:/Users/Administrator/Desktop/TMD_Client/送審專用/jenkins_update_cdn_gcp/ios/6_GCP清除快取資源.bat"
                            sshCommand remote: remote, command: command
                        } catch (Exception e) {
                            echo "Error in GCP清除快取資源2: ${e.getMessage()}"
                        }
                    }
                }
            }
        }

        stage('CDN更新Version2') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: REMOTE_CREDENTIALS, passwordVariable: 'PASSWORD', usernameVariable: 'USER')]) {
                        try {
                            def remote = getRemoteConfig('192.168.44.102', USER, PASSWORD)
                            def command = "C:/Users/Administrator/Desktop/TMD_Client/送審專用/jenkins_update_cdn_gcp/ios/7_CDN更新Version.bat"
                            sshCommand remote: remote, command: command
                        } catch (Exception e) {
                            echo "Error in CDN更新Version2: ${e.getMessage()}"
                        }
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