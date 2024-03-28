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
                                // 清空 androidtesting 
                                def clearCommand = "powershell -Command \"Remove-Item -Path 'D:\\webGame\\Game\\TMD_mobile\\data\\androidtesting\\*' -Recurse -Force\""
                                sshCommand remote: remote, command: clearCommand
                                
                                // 複製 android 到 androidtesting
                                def copyCommand = "powershell -Command \"Copy-Item -Path 'D:\\webGame\\Game\\TMD_mobile\\data\\android\\*' -Destination 'D:\\webGame\\Game\\TMD_mobile\\data\\androidtesting' -Recurse -Force\""
                                sshCommand remote: remote, command: copyCommand
                        } catch (Exception e) {
                            echo "An error occurred: ${e.getMessage()}"
                            throw e
                        }
                    }
                }
            }
        }

        stage('GCP更新Resource') {
            steps {
                script {
                    // 使用認證執行遠程命令
                    withCredentials([usernamePassword(credentialsId: REMOTE_CREDENTIALS, passwordVariable: 'PASSWORD', usernameVariable: 'USER')]) {
                        try {
                            def remote = getRemoteConfig('192.168.44.102', USER, PASSWORD)
                            def command = "C:/Users/Administrator/Desktop/TMD_Client/送審專用/jenkins_update_cdn_gcp/android/1_GCP更新Resource.bat"
                            sshCommand remote: remote, command: command
                        } catch (Exception e) {
                            // 處理錯誤
                            echo "更新GCP資源時發生錯誤: ${e.getMessage()}"
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
                            def command = "C:/Users/Administrator/Desktop/TMD_Client/送審專用/jenkins_update_cdn_gcp/android/2_GCP清除快取資源.bat"
                            sshCommand remote: remote, command: command
                        } catch (Exception e) {
                            echo "清除GCP快取資源時發生錯誤: ${e.getMessage()}"
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
                            def command = "C:/Users/Administrator/Desktop/TMD_Client/送審專用/jenkins_update_cdn_gcp/android/3_CDN更新Resource.bat"
                            sshCommand remote: remote, command: command
                        } catch (Exception e) {
                            echo "更新CDN資源時發生錯誤: ${e.getMessage()}"
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
                            def command = "C:/Users/Administrator/Desktop/TMD_Client/送審專用/jenkins_update_cdn_gcp/android/5_GCP更新Version.bat"
                            sshCommand remote: remote, command: command
                        } catch (Exception e) {
                            echo "更新GCP版本時發生錯誤: ${e.getMessage()}"
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
                            def command = "C:/Users/Administrator/Desktop/TMD_Client/送審專用/jenkins_update_cdn_gcp/android/6_GCP清除快取資源.bat"
                            sshCommand remote: remote, command: command
                        } catch (Exception e) {
                            echo "清除GCP快取資源2時發生錯誤: ${e.getMessage()}"
                        }
                    }
                }
            }
        }

        stage('CDN更新Version') {
            steps {
                script {
                    // 使用認證進行遠端SSH命令執行
                    withCredentials([usernamePassword(credentialsId: REMOTE_CREDENTIALS, passwordVariable: 'PASSWORD', usernameVariable: 'USER')]) {
                        try {
                            // 獲取遠端配置
                            def remote = getRemoteConfig('192.168.44.102', USER, PASSWORD)
                            // 指定需要執行的批處理檔案路徑和名稱
                            def command = "C:/Users/Administrator/Desktop/TMD_Client/送審專用/jenkins_update_cdn_gcp/android/7_CDN更新Version.bat"
                            // 執行遠端SSH命令
                            sshCommand remote: remote, command: command
                        } catch (Exception e) {
                            // 捕捉到異常時，在Jenkins日誌中輸出錯誤訊息
                            echo "更新CDN版本時發生錯誤: ${e.getMessage()}"
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