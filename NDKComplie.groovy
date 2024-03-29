pipeline {
    agent any

    parameters {
        string(name: 'BUILDER_NAME', defaultValue: 'weitsunglin', description: '你的名字')
        choice(name: 'ANDROID_VERSION', choices: ['商店版', '官方版'], description: '底層版本')
        choice(name: 'CPU_COUNT', choices: ['6', '8' , '10'], description: 'mac拉轉用,目前送審mac有12顆核心')
    }

    environment {
        PROJ_ANDROID_PATH = "/Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.android"
    }

    stages {
        stage('Delete Resources') {
            steps {
                script {
                    try {
                        sh "rm -rf /Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.android/libs_${params.ANDROID_VERSION}"
                        sh "rm -rf /Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.android/obj_${params.ANDROID_VERSION}"
                    } catch (Exception e) {
                        echo "Delete Resources with error: ${e.message}"
                        // You can choose to handle or throw the exception as per your requirement
                        // throw e
                    }
                }
            }
        }

        stage('Build Prepare') {
            steps {
                script {
                    try {
                        def filePath = '/Users/tmd/Documents/tmd/Inanna/Android.mk'
                        def fileContent = readFile(filePath).split("\n")
                        def newContent = []

                        fileContent.each { line ->
                            if (line.contains("LOCAL_CFLAGS += -DOFFICIAL_VERSION")) {
                                if (params.ANDROID_VERSION == '官方版') {
                                    newContent.add(line.replaceFirst("#", ""))
                                } else {
                                    if (!line.startsWith("#")) {
                                        newContent.add("#" + line)
                                    } else {
                                        newContent.add(line)
                                    }
                                }
                            } else {
                                newContent.add(line)
                            }
                        }
                        writeFile(file: filePath, text: newContent.join("\n"))
                    } catch (Exception e) {
                        echo "Build Prepare with error: ${e.message}"
                        throw e
                    }
                }
            }
        }

        stage('Build NDK') {
            steps {
                script {
                    try {
                        dir(PROJ_ANDROID_PATH) {
                            sh "bash build_release_jenkins.sh ${params.CPU_COUNT}"
                        }
                    } catch (Exception e) {
                        echo "Build NDK with error: ${e.message}"
                        throw e
                    }
                }
            }
        }

        stage('Fix Resources Name and Move to ndk_temps') {
            steps {
                script {
                    try {
                        sh "mkdir -p /Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.android/ndk_temps"
                        if (params.ANDROID_VERSION == '商店版') {

                            sh "rm -rf /Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.android/ndk_temps/obj_store"
                            sh "rm -rf /Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.android/ndk_temps/libs_store"

                            sh '''
                            mv /Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.android/obj /Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.android/ndk_temps/obj_store
                            mv /Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.android/libs /Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.android/ndk_temps/libs_store
                            '''
                        } else {

                            sh "rm -rf /Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.android/ndk_temps/obj_offical"
                            sh "rm -rf /Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.android/ndk_temps/libs_offical"

                            sh '''
                            mv /Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.android/obj /Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.android/ndk_temps/obj_offical
                            mv /Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.android/libs /Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.android/ndk_temps/libs_offical
                            '''
                        }
                    } catch (Exception e) {
                        echo "Fix Resources Name and Move to ndk_temps with error: ${e.message}"
                        throw e
                    }
                }
            }
        }
    }

    post {
        success {
            emailext(
                subject: "ndk編譯底層成功，底層版本${params.ANDROID_VERSION}，輸出路徑: /Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.android/lib_'${params.ANDROID_VERSION}'",
                body: "ndk編譯底層成功",
                to: "${params.BUILDER_NAME}@igs.com.tw"
            )
        }
        failure {
            emailext(
                subject: "ndk編譯底層失敗，底層版本${params.ANDROID_VERSION}",
                body: "ndk編譯底層失敗",
                to:  "${params.BUILDER_NAME}@igs.com.tw"
            )
        }
    }
}