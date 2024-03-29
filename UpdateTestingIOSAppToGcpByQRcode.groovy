pipeline {
    agent any

    parameters {
        string(name: 'BUILDER_NAME', defaultValue: 'weitsunglin', description: '你的名字')
        string(name: 'VERSION_NAME', defaultValue: '1.1.xxx', description: '版本號:IOS雙數1.1.184')
    }

    environment {
        VERSION_NAME_SHELL = ''
        REMOTE_CREDENTIALS = 'remote44102_login'
    }

    stages {

        stage('Create Folder and Throw IOS IPA') {
            steps {
                script {
                    try {
                        VERSION_NAME_SHELL = "${params.VERSION_NAME}"

                        sh """
                        SOURCE_FILE="/Users/tmd/Desktop/testing_app_will_go_gcp/iosipa.ipa"
                        TARGET_DIR="/Volumes/webGame/tmdInstall/testing/${VERSION_NAME_SHELL}"

                        # Ensure the target directory exists
                        mkdir -p "\${TARGET_DIR}"

                        # Copy the file
                        cp "\${SOURCE_FILE}" "\${TARGET_DIR}"
                        """
                    } catch (Exception e) {
                        echo "Create Folder and Throw IOS IPA with error: ${e.message}"
                        throw e
                    }
                }
            }
        }


        
        stage('Create Manifest') {
            steps {
                script {
                    try {
                        def VERSION_NAME_SHELL = "${params.VERSION_NAME}"

                        def manifestDir = "/Volumes/webGame/tmdInstall/testing/${VERSION_NAME_SHELL}"
                        def manifestFile = "${manifestDir}/manifest.plist"

                        def manifestContent = """<?xml version="1.0" encoding="UTF-8"?>
                        <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
                        <plist version="1.0">
                        <dict>
                            <key>items</key>
                            <array>
                                <dict>
                                    <key>assets</key>
                                    <array>
                                        <dict>
                                            <key>kind</key>
                                            <string>software-package</string>
                                            <key>url</key>
                                            <string>https://cdn-g.gametower.com.tw/rd5/tmdInstall/testing/${VERSION_NAME_SHELL}/iosipa.ipa</string>
                                        </dict>
                                    </array>
                                    <key>metadata</key>
                                    <dict>
                                        <key>bundle-identifier</key>
                                        <string>com.igs.TMD</string>
                                        <key>bundle-version</key>
                                        <string>${VERSION_NAME_SHELL}</string>
                                        <key>kind</key>
                                        <string>software</string>
                                        <key>title</key>
                                        <string>${VERSION_NAME_SHELL}</string>
                                    </dict>
                                </dict>
                            </array>
                        </dict>
                        </plist>"""

                        // Ensure the directory exists
                        sh "mkdir -p ${manifestDir}"
                        
                        // Write the content to the manifest file
                        writeFile file: manifestFile, text: manifestContent
                    } catch (Exception e) {
                        echo "Create Manifest with error: ${e.message}"
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