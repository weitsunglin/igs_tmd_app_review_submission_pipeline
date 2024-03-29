pipeline {
    agent any

    parameters {
        string(name: 'BUILDER_NAME', defaultValue: 'weitsunglin', description: '你的名字')
        string(name: 'VERSION_NAME', defaultValue: '1.1.xxx', description: '版本號:安卓單數1.1.175')
    }

    environment {
        VERSION_NAME_SHELL = ''
        REMOTE_CREDENTIALS = 'remote44102_login'
    }

    stages { 
        stage('Create Folder and Throw Android Apk') {
            steps {
                script {
                    try {
                        VERSION_NAME_SHELL = "${params.VERSION_NAME}"

                        sh """
                        SOURCE_FILE1="/Users/tmd/Desktop/testing_app_will_go_gcp/androidApk.apk"
                        SOURCE_FILE2="/Users/tmd/Desktop/testing_app_will_go_gcp/androidAllApk.apk"
                        
                        TARGET_DIR="/Volumes/webGame/tmdInstall/testing/${VERSION_NAME_SHELL}"

                        # Ensure the target directory exists
                        mkdir -p "\${TARGET_DIR}"

                        # Copy the file
                        cp "\${SOURCE_FILE1}" "\${TARGET_DIR}"
                        cp "\${SOURCE_FILE2}" "\${TARGET_DIR}"
                        """
                    } catch (Exception e) {
                        echo "Create Folder and Throw IOS IPA with error: ${e.message}"
                        throw e
                    }
                }
            }
        }

        stage('Execute Batch File') {
            steps {
                script {
                    try {
                        withCredentials([usernamePassword(credentialsId: REMOTE_CREDENTIALS, passwordVariable: 'PASSWORD', usernameVariable: 'USER')]) {
                            def remote = getRemoteConfig('192.168.44.102', USER, PASSWORD)
                            def command = "D:/webGame/tmdInstall/IPA_GCP_rsyncAndPurge_jenkins.bat"
                            sshCommand remote: remote, command: command
                        }
                    } catch (Exception e) {
                        echo "Execute Batch File with error: ${e.message}"
                        throw e
                    }
                }
            }
        }
    }

    post {
        always {
            emailext(
                subject: "上傳二測安卓遊戲到GCP結果: ${currentBuild.currentResult}",
                body: "上傳二測安卓遊戲到GCP結果: ${currentBuild.currentResult}",
                to: "${params.BUILDER_NAME}@igs.com.tw"
            )
        }
    }
}

def getRemoteConfig(host, user, password) {
    def remote = [:]
    remote.name = 'default'
    remote.host = host
    remote.user = user
    remote.password = password
    remote.allowAnyHosts = true
    return remote
}