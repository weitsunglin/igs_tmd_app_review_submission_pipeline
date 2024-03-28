pipeline {
    agent any
    stages {
        stage('1 - android更新 CDN GCP 遊戲資源') {
            steps {
                // 触发Job 1
                build job: '資源切換及更新/1 - android更新 CDN GCP 遊戲資源'
            }
        }
        stage('2 - Mac SVN切換到安卓送審資源') {
            steps {
                // 触发Job 2
                build job: '資源切換及更新/2 - Mac SVN切換到安卓送審資源'
            }
        }
        stage('3 - android遊戲資源搬移到android studio asset') {
            steps {
                // 触发Job 3
                build job: '資源切換及更新/3 - android遊戲資源搬移到android studio asset'
            }
        }
    }
}
