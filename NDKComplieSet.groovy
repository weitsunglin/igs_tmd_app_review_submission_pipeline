pipeline {
    agent any
    
    parameters {
        string(name: 'BUILDER_NAME', defaultValue: 'weitsunglin', description: 'Your Name')
        choice(name: 'CPU_COUNT', choices: ['6', '8' , '10'], description: 'mac拉轉用,目前送審mac有12顆核心')
    }
    
    stages {
        stage('產商店底層') {
            steps {
                script {
                    build job: 'Complie Android NDK',
                    parameters: [
                        string(name: 'BUILDER_NAME', value: params.BUILDER_NAME),
                        string(name: 'ANDROID_VERSION', value: '商店版'),
                        string(name: 'CPU_COUNT', value: params.CPU_COUNT),
                    ]
                }
            }
        }
        
        stage('產官方底層') {
            steps {
                script {
                    build job: 'Complie Android NDK',
                    parameters: [
                        string(name: 'BUILDER_NAME', value: params.BUILDER_NAME),
                        string(name: 'ANDROID_VERSION', value: '官方版'),
                        string(name: 'CPU_COUNT', value: params.CPU_COUNT),
                    ]
                }
            }
        }
    }
    
    post {
        success {
            emailext(
                subject: "ndk編譯底層成功",
                body: "ndk編譯底層成功，/Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.android/ndk_temps",
                to: "${params.BUILDER_NAME}@igs.com.tw"
            )
        }
        failure {
            emailext(
                subject: "ndk編譯底層失敗，底層版本",
                body: "ndk編譯底層失敗",
                to:  "${params.BUILDER_NAME}@igs.com.tw"
            )
        }
    }
}