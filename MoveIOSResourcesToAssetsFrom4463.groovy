pipeline {
    agent any

    environment {
        PROJ_ANDROID_URL = "/Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.android/"
        SVN_INSTALL_PATH = '/opt/homebrew/bin/svn'
        TMD_IOS_SOURCE = "/Volumes/TMD_PUBLIC/TMD/testing/ios"
        BASIC_GAME = 'TrainingFish TrainingMJ TrainingSlot OceanTale EgyptSlot ManganDahen'
        SVM_CREDENTIALS_ID = '97731a4e-685d-4356-8c2b-d902c44ed6e9'
    }

    parameters {
        string(name: 'BUILDER_NAME', defaultValue: 'weitsunglin', description: '你的名字')
        string(name: 'IOS_RESOURCES', defaultValue: 'DevilFire Caishen3 Volcano', description: 'IOS送審遊戲-避免載哩哩扣扣，問營運')      
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
                            // Write the JSON content to the specified file
                            writeFile file: PROJ_ANDROID_URL + "build-cfg-original.json", text: jsonContent

                            // Remove any existing temporary files to clean up before packing
                            sh "rm -rf /Users/tmd/Documents/tmd/SouthPark/ClientCocos/bin/release/tmp*"

                            // Prepare and execute the build shell script
                            BUILD_SHELL_FILE = "Build_Release_Encode.sh" 
                            sh "chmod +x /Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.android/${BUILD_SHELL_FILE}"
                            sh "./${BUILD_SHELL_FILE} release"
                        } catch (Exception e) {
                            echo "Error during 'Pack cocos': ${e.getMessage()}"
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
                            def svnRevertCmd = "${SVN_INSTALL_PATH} revert ${PROJ_ANDROID_URL + "build-cfg-original.json"}"
                            sh(svnRevertCmd)
                        } catch (Exception e) {
                            echo "Error in 'Good Guy cleaning build-cfg-original.json': ${e.getMessage()}"
                        }
                    }
                }
            }
        }

        stage('Move cocos Resource to IOS Assets') {
            steps {
                script {
                    try {
                        sh "rm -rf /Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.ios_mac/SouthPark_release/*"
                        sh "cp -r /Users/tmd/Documents/tmd/SouthPark/ClientCocos/bin/release/tmp/cocos /Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.ios_mac/SouthPark_release/"
                        sh "chmod +x /Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.ios_mac/SouthPark_release/cocos"
                    } catch (Exception e) {
                        echo "Error in 'Move cocos Resource to IOS Assets': ${e.getMessage()}"
                    }
                }
            }
        }

        stage('Move Inanna Resource to Ios Assets from 4463') {
            steps {
                script {
                    try {
                        def luaSource = '/Volumes/TMD_PUBLIC/TMD/testing/ios/Inanna/InannaLua'
                        def resourcesSource = '/Volumes/TMD_PUBLIC/TMD/testing/ios/Inanna/InannaResource'
                        def targetDirectory = '/Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.ios_mac/SouthPark_release/'

                        sh "cp -R ${luaSource} ${targetDirectory}"
                        sh "cp -R ${resourcesSource} ${targetDirectory}"

                        // Assuming decompressZipFiles is a custom function you've defined to handle ZIP file decompression
                        decompressZipFiles("/Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.ios_mac/SouthPark_release/InannaLua")
                        decompressZipFiles("/Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.ios_mac/SouthPark_release/InannaResource")
                    } catch (Exception e) {
                        echo "Error in 'Move Inanna Resource to Ios Assets from 4463': ${e.getMessage()}"
                    }
                }
            }
        }

        stage('Move Basic Game Resource to IOS Assets from 4463') {
            steps {
                script {
                    try {
                        def resources = BASIC_GAME.split(" ")
                        resources.each { resourceName ->
                            def sourceSubDir = resourceName
                            def sourcePath = "${env.TMD_IOS_SOURCE}/${sourceSubDir}"
                            def matchingFolders = sh(script: "find ${sourcePath} -type d \\( -name '*Lua' -o -name '*Resources' \\)", returnStdout: true).trim().split("\n")

                            matchingFolders.each { folderPath ->
                                def destinationPath = "/Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.ios_mac/SouthPark_release/"
                                sh "cp -R ${folderPath} ${destinationPath}"
                            }

                            def path = "/Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.ios_mac/SouthPark_release/${sourceSubDir}Lua"

                            if (sourceSubDir == 'ManganDahen') {
                                path = "/Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.ios_mac/SouthPark_release/MDLua"
                            }        

                            if (!path.isEmpty()) {
                                decompressZipFiles(path)
                            }
                        }
                    } catch (Exception e) {
                        echo "Error in 'Move Basic Game Resource to IOS Assets from 4463': ${e.getMessage()}"
                    }
                }
            }
        }

        stage('Move Review Game Resource to IOS Assets from 4463') {
            steps {
                script {
                    try {
                        def resources = params.IOS_RESOURCES.split(" ")
                        resources.each { resourceName ->
                            def sourceSubDir = resourceName
                            def sourcePath = "${env.TMD_IOS_SOURCE}/${sourceSubDir}"
                            def matchingFolders = sh(script: "find ${sourcePath} -type d \\( -name '*Lua' -o -name '*Resources' \\)", returnStdout: true).trim().split("\n")

                            matchingFolders.each { folderPath ->
                                def destinationPath = "/Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.ios_mac/SouthPark_release/"
                                sh "cp -R ${folderPath} ${destinationPath}"
                            }

                            def path = "/Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.ios_mac/SouthPark_release/${sourceSubDir}Lua"
                            if (sourceSubDir == 'XXX') {
                                path = "/Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.ios_mac/SouthPark_release/XXXLua"
                            } 
                            else if (sourceSubDir == 'TMD_Baccarat') {
                               path = "/Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.ios_mac/SouthPark_release/BaccaratLua"
                            }
                            else if (sourceSubDir == 'WuKong') {
                               path = "/Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.ios_mac/SouthPark_release/TripleWuKongSlotLua"
                            }
                            else if (sourceSubDir == 'Dinosaur') {
                               path = "/Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.ios_mac/SouthPark_release/DinosaurEggLua"
                            }
                            else if (sourceSubDir == 'TMD_StarPoseidon') {
                               path = "/Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.ios_mac/SouthPark_release/StarPoseidonLua"
                            }
                            else if (sourceSubDir == 'TMD_Aladdin') {
                               path = "/Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.ios_mac/SouthPark_release/AladdinLua"
                            }
                            else if (sourceSubDir == 'TMD_RomaX') {
                               path = "/Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.ios_mac/SouthPark_release/RomaSlotXLua"
                            }
                            else if (sourceSubDir == 'TMD_Luck2') {
                               path = "/Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.ios_mac/SouthPark_release/Luck2Lua"
                            }
                            else if (sourceSubDir == 'TMD_Luck') {
                               path = "/Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.ios_mac/SouthPark_release/LuckSlotLua"
                            }
                            else if (sourceSubDir == 'TMD_GemSlot') {
                               path = "/Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.ios_mac/SouthPark_release/GemSlotLua"
                            }
                            else if (sourceSubDir == 'TMD_FAFAFA') {
                               path = "/Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.ios_mac/SouthPark_release/FastFaFaFaLua"
                            }
                            else if (sourceSubDir == 'Super8') {
                               path = "/Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.ios_mac/SouthPark_release/Super8SlotLua"
                            }
                            else if (sourceSubDir == 'SicBo') {
                               path = "/Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.ios_mac/SouthPark_release/SICBOLua"
                            }
                            else if (sourceSubDir == 'Caishen') {
                               path = "/Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.ios_mac/SouthPark_release/CaishenSlotLua"
                            }

                            if (!path.isEmpty()) {
                                decompressZipFiles(path)
                            }
                        }
                    } catch (Exception e) {
                        echo "Error in 'Move Review Game Resource to IOS Assets from 4463': ${e.getMessage()}"
                    }
                }
            }
        }


        stage('Good Guy Clean Up') {
            steps {
                script {
                    try {
                        def targetDirectory = '/Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.ios_mac/SouthPark_release/'
                        sh "find ${targetDirectory} -type f -name '*.zip' -exec rm {} \\;"
                    } catch (Exception e) {
                        echo "Error during 'Good Guy Clean Up': ${e.getMessage()}"
                    }
                }
            }
        }


        stage('CheckOut WebEnviornment') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: SVM_CREDENTIALS_ID, usernameVariable: 'SVN_USER', passwordVariable: 'SVN_PASSWORD')]) {
                        try {
                            sh "chmod +x /Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.ios_mac/SouthPark_release/InannaLua"
                            sh "mkdir -p /Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.ios_mac/SouthPark_release/InannaLua"
                            sh "${SVN_INSTALL_PATH} export http://192.168.1.183/svn/Inanna/trunk/InannaLua/WebEnvironment.xml --depth files /Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.ios_mac/SouthPark_release/InannaLua --username ${SVN_USER} --password ${SVN_PASSWORD}"
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
                        sh "find /Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.ios_mac/SouthPark_release/ -type f -exec chmod 666 {} \\;"
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
                subject: "搬移IOS遊戲資源結果: ${currentBuild.currentResult}",
                body: "搬移IOS遊戲資源結果: ${currentBuild.currentResult}，位置在(192.168.123.123): SouthPark\\ClientCocos\\prject.ios_mac\\release\\tmp\\",
                to: "${params.BUILDER_NAME}@igs.com.tw"
            )
        }
    }
}

def decompressZipFiles(String directory) {
    sh """
        find ${directory} -type f -name "*.zip" | while read zipFile; do
            unzip -o "\$zipFile" -d "\$(dirname "\$zipFile")"
            echo "Decompressed: \$zipFile"
        done
    """
}

def removeZipFiles(String directory) {
    sh """
        find ${directory} -type f -name '*.zip' -exec rm -f {} \\;
    """
}