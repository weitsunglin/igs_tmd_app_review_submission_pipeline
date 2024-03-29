pipeline {
    agent any

    parameters {
        string(name: 'BUILDER_NAME', defaultValue: 'weitsunglin', description: '你的名字')
        choice(name: 'BUILD_TYPE', choices: [ 'apk'], description: '編譯類型:上傳GCP')
        choice(name: 'NDK_TYPE', choices: ['store', 'offical'], description: '底層類型:商店版/官網版')
        choice(name: 'BUILD_ENV', choices: ['testing'], description: '編譯環境:二測')
        string(name: 'VERSION_NAME', defaultValue: '1.1.xxx', description: '版本號:安卓單數1.1.175')
        string(name: 'VERSION_CODE', defaultValue: '1000249', description: 'versionCode上傳過PLAYSTORE商店重編要再加一')
    }

    environment {
        ANDROID_HOME = "/Users/tmd/Library/Android/sdk"
        ANDROID_STUDIO_PATH = "/Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.android-studio"
        PROJ_ANDROID_PATH = "/Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.android"
        INANNALUA_ENV_PATH = "/Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.android/assets/InannaLua/WebEnvironment.xml"
        BUILD_MODE = 'release'
    }

    stages {
        stage('Parameter Check') {
            steps {
                script {
                    if (params.VERSION_NAME == '1.1.xxx') {
                        error("The VERSION_NAME parameter is set to '1.1.xxx', which is a placeholder and not a valid version name. Pipeline execution is terminated to prevent build configuration errors.")
                    }
                }
            }
        }

        stage('Setting NDK_TYPE') {
            steps {
                script {
                    try {
                        dir(PROJ_ANDROID_PATH) {
                            def objDir = params.NDK_TYPE == 'store' ? 'obj_store' : 'obj_offical'
                            def libsDir = params.NDK_TYPE == 'store' ? 'libs_store' : 'libs_offical'

                            if (fileExists('obj')) {
                                sh "rm -rf obj"
                            }
                            if (fileExists('libs')) {
                                sh "rm -rf libs"
                            }
                            
                            if (fileExists("${PROJ_ANDROID_PATH}/ndk_temps/${objDir}")) {
                                sh "cp -r ${PROJ_ANDROID_PATH}/ndk_temps/${objDir} ${PROJ_ANDROID_PATH}/obj"
                            } else {
                                echo "${objDir} does not exist, skipping copy."
                            }

                            if (fileExists("${PROJ_ANDROID_PATH}/ndk_temps/${libsDir}")) {
                                sh "cp -r ${PROJ_ANDROID_PATH}/ndk_temps/${libsDir} ${PROJ_ANDROID_PATH}/libs"
                            } else {
                                echo "${libsDir} does not exist, skipping copy."
                            }
                        }
                    } catch (Exception e) {
                        echo "Error in Setting NDK_TYPE stage: ${e.getMessage()}"
                    }
                }
            }
        }



        stage('Setting project') {
            steps {
                script {
                    try {
                        dir(PROJ_ANDROID_PATH) {
                            def gradleFilePath = "build.gradle"
                            def gradleContent = readFile(gradleFilePath)

                            gradleContent = gradleContent.replaceAll(/versionName\s+".*"/, 'versionName "' + VERSION_NAME + '"')
                                                        .replaceAll(/versionCode\s+\d+/, 'versionCode ' + VERSION_CODE)

                            writeFile file: gradleFilePath, text: gradleContent
                        }
                    } catch (Exception e) {
                        echo "Error in Setting project stage: ${e.getMessage()}"
                    }
                }
            }
        }


        stage('Setting environment') {
            steps {
                script {
                    try {
                        def filePath = INANNALUA_ENV_PATH
                        def fileContent = readFile(filePath)
                        def newContent = fileContent.replaceAll(/(<type>).*?(<\/type>)/, "\$1${params.BUILD_ENV}\$2")
                        writeFile(file: filePath, text: newContent)
                    } catch (Exception e) {
                        echo "Error in Setting environment stage: ${e.getMessage()}"
                    }
                }
            }
        }


        stage('android studio build') {
            steps {
                script {
                    dir(ANDROID_STUDIO_PATH) {
                        try {
                            def buildCommand = ""
                            if (params.BUILD_TYPE == 'aab') {
                                buildCommand = "bundle${BUILD_MODE.capitalize()}"
                            } else {
                                buildCommand = "assemble${BUILD_MODE.capitalize()}"
                            }

                            sh "chmod +x /Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.android/assets/"

                            sh """
                            export PATH=\$PATH:\$ANDROID_HOME/tools:\$ANDROID_HOME/platform-tools
                            chmod +x ./gradlew
                            ./gradlew clean ${buildCommand} -Dorg.gradle.jvmargs=-Xmx2048m
                            """
                        } 
                        catch (Exception e) {
                            echo "android studio build  error: ${e}"
                            throw e
                        }
                    }
                }
            }
        }
        
        stage('Change app Name and Throw to desktop') {
            steps {
                script {
                    try {
                        def apkSource = "/Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.android/build/outputs/apk/release/proj.android-release.apk"
                        def destDir = "/Users/tmd/Desktop/testing_app_will_go_gcp"
                        def apkName = "android-release-${params.NDK_TYPE}.apk"
                        def finalName = "${params.BUILD_ENV}.${apkName}"

                        if (params.NDK_TYPE == 'store') {
                            finalName = 'androidApk.apk'
                        } else {
                            finalName = 'androidAllApk.apk'
                        }

                        sh "mkdir -p '${destDir}'"
                        sh "cp '${apkSource}' '${destDir}/${apkName}'"
                        sh "mv '${destDir}/${apkName}' '${destDir}/${finalName}'"
                        
                    } catch (Exception e) {
                        echo "Error in Change app Name and Throw to desktop stage: ${e.getMessage()}"
                    }
                }
            }
        }
    }

    post {
        success {
            emailext(
                subject: "androdi studio 編譯成功，環境為 ${params.BUILD_ENV}，類型為 ${params.BUILD_TYPE}",
                body: "編譯成功，路徑在:/Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.android/build/outputs",
                to: "${params.BUILDER_NAME}@igs.com.tw"
            )
        }
        failure {
            emailext(
                subject: "androdi studio 編譯失敗，怎辦，環境為 ${params.BUILD_ENV}，類型為 ${params.BUILD_TYPE}",
                body: "編譯失敗",
                to:  "${params.BUILDER_NAME}@igs.com.tw"
            )
        }
    }
}