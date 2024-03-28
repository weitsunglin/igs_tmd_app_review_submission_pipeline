pipeline {
    agent any
    stages {
        stage('Job 1') {
            steps {
                // 触发Job 1
                build job: '更新遊戲資源/1 - Ios 更新 CDN GCP 遊戲資源'
            }
        }
        stage('Job 2') {
            steps {
                // 触发Job 2
                build job: '更新遊戲資源/2 - Mac SVN切換到IOS送審資源'
            }
        }
    }
}
