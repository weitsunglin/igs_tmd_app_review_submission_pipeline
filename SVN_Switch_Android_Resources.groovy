def resources = [
    ['name': 'Boost', 'url': 'http://192.168.1.183/svn/Boost/version_1_55_0'],
    ['name': 'Casino', 'url': 'http://192.168.1.183/svn/Casino/tags/version_2_0_132'],
    ['name': 'Cocos2d-x', 'url': 'http://192.168.1.183/svn/Cocos2d-x/3.17/trunk'],
    ['name': 'GameTower2', 'url': 'http://192.168.1.183/svn/TMD_GameTower2/tags/version_3_0_5'],
    ['name': 'Ishtar', 'url': 'http://192.168.1.183/svn/Ishtar/trunk'],
    ['name': 'Libraries', 'url': 'http://192.168.1.183/svn/Libraries/trunk'],
    ['name': 'ManganDahen', 'url': 'http://192.168.1.183/svn/ManganDahen/branches/Release']
]

//['name': 'SouthPark', 'url': 'http://192.168.1.183/svn/SouthPark/trunk'] SouthPark不切 如果切每次都要解permission denined 問題
 
pipeline {
    agent any

    parameters {
        string(name: 'BUILDER_NAME', defaultValue: 'weitsunglin', description: '你的名字')
    }

    environment {
        SVN_INSTALL_PATH = '/opt/homebrew/bin/svn'
        TMD_RESOURCES_URL = "/Users/tmd/Documents/tmd/"
        SVM_CREDENTIALS_ID = '97731a4e-685d-4356-8c2b-d902c44ed6e9'
    }

    stages {
        stage('SVN Switching Resources') {
            steps {
                script {
                    try {
                        def InanaUrl = "http://192.168.1.183/svn/Inanna/branches/ReleaseAndroid"

                        dir("${TMD_RESOURCES_URL}Inanna/") {
                            withCredentials([usernamePassword(credentialsId: SVM_CREDENTIALS_ID, usernameVariable: 'SVN_USER', passwordVariable: 'SVN_PASSWORD')]) {
                                sh "${SVN_INSTALL_PATH} switch ${InanaUrl} --username \${SVN_USER} --password \$SVN_PASSWORD"
                                sh "${SVN_INSTALL_PATH} revert -R . --username \$SVN_USER --password \$SVN_PASSWORD"
                            }
                        }

                        resources.each { resource ->
                            dir("${TMD_RESOURCES_URL}${resource.name}") {
                                withCredentials([usernamePassword(credentialsId: SVM_CREDENTIALS_ID, usernameVariable: 'SVN_USER', passwordVariable: 'SVN_PASSWORD')]) {
                                    sh "${SVN_INSTALL_PATH} switch ${resource.url} --username \${SVN_USER} --password \$SVN_PASSWORD"
                                    sh "${SVN_INSTALL_PATH} revert -R . --username \${SVN_USER} --password \$SVN_PASSWORD"
                                }
                            }
                        }

                        dir("${TMD_RESOURCES_URL}SouthPark/") {
                            withCredentials([usernamePassword(credentialsId: SVM_CREDENTIALS_ID, usernameVariable: 'SVN_USER', passwordVariable: 'SVN_PASSWORD')]) {
                                sh "${SVN_INSTALL_PATH} update --username \${SVN_USER} --password \$SVN_PASSWORD"
                            }
                        }


                    } catch (Exception e) {
                        echo "SVN Switching Resources with error: ${e.message}"
                        throw e
                    }
                }
            }
        }
    }

    post {
        success {
            emailext(
                subject: "SVN 切換 MAC Android 送審資源成功",
                body: "切換成功",
                to: "${params.BUILDER_NAME}@igs.com.tw"
            )
        }
        failure {
            emailext(
                subject: "SVN 切換 MAC Android 送審資源失败",
                body: "切換失败",
                to: "${params.BUILDER_NAME}@igs.com.tw"
            )
        }
    }
}