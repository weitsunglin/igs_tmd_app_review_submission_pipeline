pipeline {
    agent any

    environment {
        PROJ_ANDROID_URL = "/Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.android/"
        SVN_INSTALL_PATH = '/opt/homebrew/bin/svn'
        SVM_CREDENTIALS_ID = '97731a4e-685d-4356-8c2b-d902c44ed6e9'
    }

    parameters {
        string(name: 'BUILDER_NAME', defaultValue: 'weitsunglin', description: '你的名字')
    }

    stages {

        stage('Pack cocos') {
            steps {
                script {
                    dir(PROJ_ANDROID_URL) {
                        try {
                            // Define the JSON content for resource copying
                            def jsonContent = """{
                                "copy_resources": [
                                    {
                                        "from": "../../../Cocos2d-x/cocos/scripting/lua-bindings/script",
                                        "to": "cocos",
                                        "zip": false
                                    },
                                    {
                                        "from": "../../../Inanna/InannaLua",
                                        "to": "InannaLua",
                                        "zip": true
                                    },
                                    {
                                        "from": "../../../Inanna/InannaResource",
                                        "to": "InannaResource",
                                        "zip": true
                                    }
                                ]
                            }"""
                            // Write the JSON content to the build-cfg-original.json file
                            writeFile file: PROJ_ANDROID_URL + "build-cfg-original.json", text: jsonContent

                            // Remove any existing temporary files
                            sh "rm -rf /Users/tmd/Documents/tmd/SouthPark/ClientCocos/bin/release/tmp*"

                            // Set the build shell file executable and execute it
                            BUILD_SHELL_FILE = "Build_Release_Encode.sh" 
                            sh 'chmod +x /Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.android/${BUILD_SHELL_FILE}'
                            sh "./${BUILD_SHELL_FILE} release"
                        } catch (Exception e) {
                            echo "Error in 'Pack cocos': ${e.getMessage()}"
                        }
                    }
                }
            }
        }

        stage('Good Guy cleaning build-cfg-original.json') {
            steps {
                script {
                    dir(PROJ_ANDROID_URL) {
                        try {
                            // Prepare the SVN revert command
                            def svnRevertCmd = "${SVN_INSTALL_PATH} revert ${PROJ_ANDROID_URL + "build-cfg-original.json"}"
                            // Execute the SVN revert command
                            sh(svnRevertCmd)
                        } catch (Exception e) {
                            // Log any errors that occur during the revert process
                            echo "Error during 'Good Guy cleaning build-cfg-original.json': ${e.getMessage()}"
                        }
                    }
                }
            }
        }

        stage('Move cocos to Android Assets') {
            steps {
                script {
                    try {
                        sh "chmod +x /Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.android/assets"
                        sh "cp -r /Users/tmd/Documents/tmd/SouthPark/ClientCocos/bin/release/tmp/cocos /Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.android/assets/"
                    } catch (Exception e) {
                        echo "Error moving 'cocos' to Android Assets: ${e.getMessage()}"
                    }
                }
            }
        }

        stage('Move PackingData to Android Assets from 4463') {
            steps {
                script {
                    try {
                        sh "rm -rf /Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.android/assets/InannaLua*"
                        sh "rm -rf /Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.android/assets/InannaResource*"

                        def luaSource = '/Volumes/TMD_PUBLIC/TMD/testing/android/Inanna/InannaLua'
                        def resourcesSource = '/Volumes/TMD_PUBLIC/TMD/testing/android/Inanna/InannaResource'
                        def targetDirectory = '/Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.android/assets/'

                        sh "cp -R ${luaSource} ${targetDirectory}"
                        sh "cp -R ${resourcesSource} ${targetDirectory}"
                    } catch (Exception e) {
                        echo "Error moving PackingData to Android Assets from 4463: ${e.getMessage()}"
                    }
                }
            }
        }

        stage('Decompress and Clean Up') {
            steps {
                script {
                    try {
                        def InannaResourceDirectory = '/Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.android/assets/InannaResource'
                        sh """
                            cd ${InannaResourceDirectory}
                            find . -name "*.zip" -exec unzip -o {} -d \$(dirname {}) \\;
                        """

                        def InannaLuaDirectory = '/Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.android/assets/InannaLua'
                        sh """
                            cd ${InannaLuaDirectory}
                            find . -name "*.zip" -exec unzip -o {} -d \$(dirname {}) \\;
                        """
                    } catch (Exception e) {
                        echo "Error during 'Decompress and Clean Up': ${e.getMessage()}"
                    }
                }
            }
        }

        stage('Clean Up') {
            steps {
                script {
                    try {
                        def targetDirectory = '/Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.android/assets/'
                        sh "find ${targetDirectory} -type f -name '*.zip' -exec rm {} \\;"
                    } catch (Exception e) {
                        echo "Error during 'Clean Up': ${e.getMessage()}"
                    }
                }
            }
        }


        stage('CheckOut WebEnviornment') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: SVM_CREDENTIALS_ID, usernameVariable: 'SVN_USER', passwordVariable: 'SVN_PASSWORD')]) {
                        try {
                            sh "chmod +x /Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.android/assets/InannaLua"
                            sh "mkdir -p /Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.android/assets/InannaLua"
                            sh "${SVN_INSTALL_PATH} export http://192.168.1.183/svn/Inanna/trunk/InannaLua/WebEnvironment.xml --depth files /Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.android/assets/InannaLua --username ${SVN_USER} --password ${SVN_PASSWORD}"
                        } catch (Exception e) {
                            echo "Error during 'CheckOut WebEnvironment': ${e.getMessage()}"
                        }
                    }
                }
            }
        }

        stage('Give permission') {
            steps {
                script {
                    try {
                        sh "find /Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.android/assets/ -type f -exec chmod 666 {} \\;"
                    } catch (Exception e) {
                        echo "Error during 'Give permission': ${e.getMessage()}"
                    }
                }
            }
        }
    }

    post {
        always {
            emailext(
                subject: "搬移Android遊戲資源結果: ${currentBuild.currentResult}",
                body: "搬移Android遊戲資源結果: ${currentBuild.currentResult}，位置在(192.168.123.123): SouthPark\\ClientCocos\\prject.android\\assets\\",
                to: "${params.BUILDER_NAME}@igs.com.tw"
            )
        }
    }
}