pipeline {
    agent any
    
    parameters {
        string(name: 'BUILDER_NAME', defaultValue: 'weitsunglin', description: 'Your Name')
        string(name: 'VERSION_NAME', defaultValue: '1.1.xxx', description: 'Version Name: Android version 1.1.175')
        string(name: 'VERSION_CODE', defaultValue: '1000xxx', description: 'Version Code: Increment by one when uploading to PLAYSTORE')
    }
    
    stages {
        stage('產商店apk') {
            steps {
                script {
                    build job: 'Build android 二測 tmd app',
                    parameters: [
                        string(name: 'BUILDER_NAME', value: params.BUILDER_NAME),
                        string(name: 'BUILD_TYPE', value: 'apk'),
                        string(name: 'NDK_TYPE', value: 'store'),
                        string(name: 'BUILD_ENV', value: 'testing'),
                        string(name: 'VERSION_NAME', value: params.VERSION_NAME),
                        string(name: 'VERSION_CODE', value: params.VERSION_CODE)
                    ]
                }
            }
        }
        
        stage('產官方apk') {
            steps {
                script {
                    build job: 'Build android 二測 tmd app',
                    parameters: [
                        string(name: 'BUILDER_NAME', value: params.BUILDER_NAME),
                        string(name: 'BUILD_TYPE', value: 'apk'),
                        string(name: 'NDK_TYPE', value: 'offical'),
                        string(name: 'BUILD_ENV', value: 'testing'),
                        string(name: 'VERSION_NAME', value: params.VERSION_NAME),
                        string(name: 'VERSION_CODE', value: params.VERSION_CODE)
                    ]
                }
            }
        }
    }
    
    post {
        success {
            emailext(
                subject: "android編譯app成功",
                body: "android編譯底層成功，/Users/tmd/Desktop/production_android_app/",
                to: "${params.BUILDER_NAME}@igs.com.tw"
            )
        }
        failure {
            emailext(
                subject: "android編譯app失敗",
                body: "android編譯底層失敗",
                to:  "${params.BUILDER_NAME}@igs.com.tw"
            )
        }
    }
}