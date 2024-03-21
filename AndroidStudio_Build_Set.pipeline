pipeline {
    agent any
    
    parameters {
        string(name: 'BUILDER_NAME', defaultValue: 'weitsunglin', description: 'Your Name')
        string(name: 'VERSION_NAME', defaultValue: '1.1.xxx', description: 'Version Name: Android version 1.1.175')
        string(name: 'VERSION_CODE', defaultValue: '1000xxx', description: 'Version Code: Increment by one when uploading to PLAYSTORE')
    }
    
    stages {
        stage('產商店aab') {
            steps {
                script {
                    build job: 'Build android tmd app',
                    parameters: [
                        string(name: 'BUILDER_NAME', value: params.BUILDER_NAME),
                        string(name: 'BUILD_TYPE', value: 'aab'),
                        string(name: 'NDK_TYPE', value: 'store'),
                        string(name: 'BUILD_ENV', value: 'production'),
                        string(name: 'VERSION_NAME', value: params.VERSION_NAME),
                        string(name: 'VERSION_CODE', value: params.VERSION_CODE)
                    ]
                }
            }
        }
        
        stage('產商店apk') {
            steps {
                script {
                    build job: 'Build android tmd app',
                    parameters: [
                        string(name: 'BUILDER_NAME', value: params.BUILDER_NAME),
                        string(name: 'BUILD_TYPE', value: 'apk'),
                        string(name: 'NDK_TYPE', value: 'store'),
                        string(name: 'BUILD_ENV', value: 'production'),
                        string(name: 'VERSION_NAME', value: params.VERSION_NAME),
                        string(name: 'VERSION_CODE', value: params.VERSION_CODE)
                    ]
                }
            }
        }
        
        stage('產官方apk') {
            steps {
                script {
                    build job: 'Build android tmd app',
                    parameters: [
                        string(name: 'BUILDER_NAME', value: params.BUILDER_NAME),
                        string(name: 'BUILD_TYPE', value: 'apk'),
                        string(name: 'NDK_TYPE', value: 'offical'),
                        string(name: 'BUILD_ENV', value: 'production'),
                        string(name: 'VERSION_NAME', value: params.VERSION_NAME),
                        string(name: 'VERSION_CODE', value: params.VERSION_CODE)
                    ]
                }
            }
        }
    }
    
    post {
        always {
            emailext (
                subject: "${currentBuild.currentResult}",
                body: "${currentBuild.currentResult}, Build android tmd app",
                to: "${params.BUILDER_NAME}@igs.com.tw",
            )
        }
    }
}