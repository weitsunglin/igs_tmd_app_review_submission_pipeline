pipeline {
    agent any

    environment {
        PROJECT_WORKSPACE_PATH = "/Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.ios_mac/"
        EXPORT_OPTION = "exportOptions.plist"
        SOUTH_PARK_INFO = ""
        BUILD_CONFIG = 'Release'
        BUILD_TARGET = ''
        RELEASE_WEB_ENVIORNMET = '/Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.ios_mac/SouthPark_release/InannaLua/WebEnvironment.xml'
    }

    parameters {
        string(name: 'BUILDER_NAME', defaultValue: 'weitsunglin', description: '你的名字')
        choice(name: 'BUILD_TYPE', choices: ['外部'], description: '選擇編譯環境')
        string(name: 'VERSION', defaultValue: '1.1.xxx', description: '版本號:IOS雙數1.1.184')
        string(name: 'BUILD', defaultValue: '1', description: 'BUILD number appleStore上架過要+1')
    }
    
    stages {
        stage('Parameter Check') {
            steps {
                script {
                    if (params.VERSION == '1.1.xxx') {
                        error("The VERSION parameter is set to '1.1.xxx', which is a placeholder and not a valid version name. Pipeline execution is terminated to prevent build configuration errors.")
                    }
                }
            }
        }

        stage('ExportOption Setting') {
            steps {
                script {
                    try {
                        if (params.BUILD_TYPE == '外部') {
                            BUILD_TARGET = 'SouthPark release'
                            EXPORT_OPTION = "ExportOption_Release.plist"
                            SOUTH_PARK_INFO = "/Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.ios_mac/ios/SouthPark release-Info.plist"
                        } else if(params.BUILD_TYPE == '二測') {
                            BUILD_TARGET = 'SouthPark debug package'
                            EXPORT_OPTION = "ExportOption_Test.plist"
                            SOUTH_PARK_INFO = "/Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.ios_mac/ios/SouthPark debug package-Info.plist"
                        }
                        echo "BUILD_TARGET: ${BUILD_TARGET}"
                    } catch (Exception e) {
                        echo "ExportOption Setting failed with error: ${e.message}"
                        throw e
                    }
                }
            }
        }

        stage('Xcode Clean') {
            steps {
                script {
                    dir(PROJECT_WORKSPACE_PATH){
                        try {
                            sh '''
                            if [ -d "/Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.ios_mac/build" ]; then
                                chmod +x /Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.ios_mac/build
                                echo "Directory permissions changed."
                            else
                                echo "Directory does not exist."
                            fi
                            '''
                            sh "rm -rf /Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.ios_mac/build*"
                            sh "xcodebuild clean -workspace SouthPark.xcworkspace -scheme '${BUILD_TARGET}' -configuration ${BUILD_CONFIG}"
                            sh "xcodebuild clean -workspace SouthPark.xcworkspace -scheme 'PushService' -configuration ${BUILD_CONFIG}"
                        } catch (Exception e) {
                            echo "Xcode Clean failed with error: ${e.message}"
                            throw e
                        }
                    }
                }
            }
        }


        stage('Version and Build Number Setting') {
            steps {
                script {
                    dir(PROJECT_WORKSPACE_PATH){
                        try {
                            echo "Version and Build Number Setting"
                            //設定滿貫
                            setVersion(params.VERSION, SOUTH_PARK_INFO)
                            setBuildNumber(params.BUILD, SOUTH_PARK_INFO)

                            //設定pushService 
                            setVersion(params.VERSION, "/Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.ios_mac/PushService/Info.plist")
                            setBuildNumber(params.BUILD, "/Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.ios_mac/PushService/Info.plist")
                            
                        } catch (Exception e) {
                            echo "Xcode Version and Build Number Setting failed with error: ${e.message}"
                            throw e
                        }
                    }
                }
            }
        }

        stage('Setting WebEnvironment') {
            steps {
                script {
                    try {
                        def filePath = ''
                        def Env = ''
                        filePath = RELEASE_WEB_ENVIORNMET
                        if (params.BUILD_TYPE == '外部') {
                            Env = 'production'
                        } else if(params.BUILD_TYPE == '二測') {
                            Env = 'testing'
                        }
                        def fileContent = readFile(filePath)
                        def newContent = fileContent.replaceAll(/(<type>).*?(<\/type>)/, "\$1${Env}\$2")
                        writeFile(file: filePath, text: newContent)
                    } catch (Exception e) {
                        println("Setting WebEnvironment error occurred: ${e.message}")
                    }
                }
            }
        }

        stage('Xcode Setting Mobile Provisioning') {
            steps {
                script {
                    try {
                        if (params.BUILD_TYPE == '外部') {
                            sh "cp /Users/tmd/Desktop/homebrew_jenkins_backup_data/PushService_AppStore_2023.mobileprovision ~/Library/MobileDevice/Provisioning\\ Profiles/"
                            sh "xcodebuild -workspace SouthPark.xcworkspace -scheme PushService -configuration Release PROVISIONING_PROFILE_SPECIFIER='PushService_AppStore_2023' CODE_SIGN_IDENTITY='iPhone Developer'"
                        } else if (params.BUILD_TYPE == '二測') {
                            sh "cp /Users/tmd/Desktop/homebrew_jenkins_backup_data/TMD_IN_HOUSE.mobileprovision ~/Library/MobileDevice/Provisioning\\ Profiles/"
                            sh "xcodebuild -workspace SouthPark.xcworkspace -scheme PushService -configuration Release PROVISIONING_PROFILE_SPECIFIER='TMD_IN_HOUSE' CODE_SIGN_IDENTITY='iPhone Developer'"
                        }
                    } catch (Exception e) {
                        println("Xcode Setting Mobile Provisioning error occurred: ${e.message}")
                    }
                }
            }
        }



        stage('Xcode Build') {
            steps {
                script {
                    dir(PROJECT_WORKSPACE_PATH){
                        try {
                            sh "xcodebuild -workspace SouthPark.xcworkspace -scheme '${BUILD_TARGET}' -configuration ${BUILD_CONFIG} -destination 'generic/platform=iOS' build"
                            sh "xcodebuild -workspace SouthPark.xcworkspace -scheme PushService -configuration ${BUILD_CONFIG} -destination 'generic/platform=iOS' build"
                        } catch (Exception e) {
                            echo "Xcode Build failed with error: ${e.message}"
                            throw e
                        }
                    }
                }
            }
        }

        stage('Xcode Archive') {
            steps {
                script {
                    dir(PROJECT_WORKSPACE_PATH) {
                        try {
                            sh "xcodebuild -workspace SouthPark.xcworkspace -scheme '${BUILD_TARGET}' -sdk iphoneos -configuration ${BUILD_CONFIG} archive -archivePath ./build/SouthPark.xcarchive && xcodebuild -exportArchive -archivePath ./build/SouthPark.xcarchive -exportOptionsPlist ${EXPORT_OPTION} -exportPath ./build"
                        } catch (Exception e) {
                            echo "Xcode Archive failed with error: ${e.message}"
                            throw e 
                        }
                    }
                }
            }
        }

        stage('Mode production app to desktop') {
            steps {
                script {
                    dir(PROJECT_WORKSPACE_PATH) {
                        try {
                           sh 'rm -rf /Users/tmd/Desktop/production_ios_app/*'
                           sh 'mv /Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.ios_mac/build/* /Users/tmd/Desktop/production_ios_app/'
                        } catch (Exception e) {
                            echo "Mode production app to desktop error: ${e.message}"
                            throw e 
                        }
                    }
                }
            }
        }

        stage('Xcode Distrubution') {
            steps {
                script {
                    dir(PROJECT_WORKSPACE_PATH) {
                        try {
                            sh 'open "/Users/tmd/Desktop/production_ios_app/SouthPark.xcarchive"'
                        } catch (Exception e) {
                            echo "Xcode Distrubution failed with error: ${e.message}"
                            throw e 
                        }
                    }
                }
            }
        }
    }
    
    post {
        success {
            emailext(
                subject: "Xcode 編譯成功，編譯TARGET為 '${BUILD_TARGET}'",
                body: "編譯版本為 ${BUILD_TARGET}，輸出路徑: /Users/tmd/Desktop/production_ios_app/",
                to: "${params.BUILDER_NAME}@igs.com.tw"
            )
        }
        failure {
            emailext(
                subject: "Xcode 編譯失敗了，怎辦，編譯TARGET為 '${BUILD_TARGET}'",
                body: "編譯失敗",
                to: "${params.BUILDER_NAME}@igs.com.tw"
            )
        }
    }
}

def setVersion(String newMarketingVersion, String plistPath) {
    echo "setVersion"
    sh """
        #!/bin/sh

        # 打印原始 CFBundleShortVersionString 值
        echo "原始值:"
        grep -A 1 "CFBundleShortVersionString" "${plistPath}"

        # 使用 awk 来更新 CFBundleShortVersionString 的值
        awk -v newVersion="${newMarketingVersion}" '/<key>CFBundleShortVersionString<\\/key>/{print; getline; sub(/>.*</, ">" newVersion "<"); print; next}1' "${plistPath}" > temp.plist && mv temp.plist "${plistPath}"

        # 打印更新后的 CFBundleShortVersionString 值
        echo "更新後的值:"
        grep -A 1 "CFBundleShortVersionString" "${plistPath}"
    """
}

def setBuildNumber(String currentProjectVersion, String plistPath) {
    echo "setBuildNumber"
    sh """
        #!/bin/sh

        # 打印原始 CFBundleVersion 值
        echo "原始值:"
        grep -A 1 "CFBundleVersion" "${plistPath}"

        # 使用 awk 来更新 CFBundleVersion 的值
        awk -v newVersion="${currentProjectVersion}" '/<key>CFBundleVersion<\\/key>/{print; getline; sub(/>.*</, ">" newVersion "<"); print; next}1' "${plistPath}" > temp.plist && mv temp.plist "${plistPath}"

        # 打印更新后的 CFBundleVersion 值
        echo "更新後的值:"
        grep -A 1 "CFBundleVersion" "${plistPath}"
    """
}