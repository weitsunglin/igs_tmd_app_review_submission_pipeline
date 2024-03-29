pipeline {
    agent any

    parameters {
        string(name: 'BUILDER_NAME', defaultValue: 'weitsunglin', description: '你的名字')
        string(name: 'NEW_IOS_VERSION', defaultValue: '1.1.xxx', description: '版本號:安卓單數1.1.175/IOS雙數1.1.184')
        choice(name: 'DB_ENVIORNMENT', choices: [ '外部','二測'], description: '選擇DB環境')
    }

    environment {
        MSSQL_SERVER = 'db-gt-test24.gametower.com.tw'  //db-gt-test24.gametower.com.tw、10.100.20.17
        MSSQL_DATABASE = 'GameTower2_TMD'
        TMD_DB = 'tmd_db'
        REMOTE_CREDENTIALS ='remote4463_login'
        NEWIOSVERSION = ''
    }
    
    stages {
        stage('Parameter Check') {
            steps {
                script {
                    if (params.NEW_IOS_VERSION == '1.1.xxx') {
                        error("The NEW_IOS_VERSION parameter is set to '1.1.xxx', which is a placeholder and not a valid version name. Pipeline execution is terminated to prevent build configuration errors.")
                    }
                }
            }
        }

        stage('DB Modify') {
            steps {
                script {
                    try {
                        if (params.DB_ENVIORNMENT == '外部') {
                        MSSQL_SERVER = '10.100.20.17'
                        } 
                        else {
                        MSSQL_SERVER = 'db-gt-test24.gametower.com.tw'
                        }
                        
                        //版本號
                        NEWIOSVERSION = params.NEW_IOS_VERSION

                        withCredentials([usernamePassword(credentialsId: REMOTE_CREDENTIALS, passwordVariable: 'REMOTE_PASSWORD', usernameVariable: 'REMOTE_USER')]) {
                            def remote = getRemoteConfig("192.168.44.63", REMOTE_USER, REMOTE_PASSWORD)
                            //DB
                            withCredentials([usernamePassword(credentialsId: TMD_DB, usernameVariable: 'DB_ACCOUNT', passwordVariable: 'DB_PASSWORD')]) {
                                //revision + 1
                                def fix_dbCommand = "sqlcmd -S ${MSSQL_SERVER} -d ${MSSQL_DATABASE} -U ${DB_ACCOUNT} -P ${DB_PASSWORD} -Q \"UPDATE Client_LatestSvnRevision SET revision = revision - 1 WHERE deviceVersion = '${NEWIOSVERSION}' AND path = 'Inanna'\""
                                sshCommand remote: remote, command: fix_dbCommand

                                //確認revision號碼
                                def confirm_dbCommand = "sqlcmd -S ${MSSQL_SERVER} -d ${MSSQL_DATABASE} -U ${DB_ACCOUNT} -P ${DB_PASSWORD} -Q \"SELECT TOP 999 * FROM Client_LatestSvnRevision WHERE deviceVersion = '${NEWIOSVERSION}'\""
                                sshCommand remote: remote, command: confirm_dbCommand
                            }
                        }
                    } catch (Exception e) {
                        echo "DB Modify with error: ${e.message}"
                        throw e
                    }
                }
            }
        }
    }
    
    post {
        always {
            script {
                def emailSubject = (currentBuild.result == 'SUCCESS') ? "SUCCESS Modify 10.100.20.17 IOS Inanna Svn revision" : "Failure Modify 10.100.20.17 IOS Inanna Svn revision"
                def emailBody = (currentBuild.result == 'SUCCESS') ? "SUCCESS" : "Failure"

                emailext(
                    subject: emailSubject,
                    body: "${emailBody}\nBuild Number: ${env.BUILD_NUMBER}\nBuild URL: ${env.BUILD_URL}",
                    to: "${params.BUILDER_NAME}@igs.com.tw"
                )

                withCredentials([usernamePassword(credentialsId: "line_notify", usernameVariable: '_', passwordVariable: 'line_token')]) {
                    try {
                        sh """
                        curl -X POST -H 'Authorization: Bearer ${line_token}' -F "message=name: ${params.BUILDER_NAME} ${currentBuild.currentResult} modify 10.100.20.17 db ios Inanna Svn revision" https://notify-api.line.me/api/notify
                        """
                    } catch (Exception e) {
                        echo "Failed to send LINE notification: ${e.getMessage()}"
                    }
                }
            }
        }
    }
}

def getRemoteConfig(host, user, password) {
    def remote = [:]
    remote.name = user
    remote.host = host
    remote.user = user
    remote.password = password
    remote.allowAnyHosts = true
    return remote
}