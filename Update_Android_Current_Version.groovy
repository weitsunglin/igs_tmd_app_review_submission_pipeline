pipeline {
    agent any

    parameters {
        string(name: 'BUILDER_NAME', defaultValue: 'weitsunglin', description: '你的名字')
        string(name: 'CURRENT_VERSION', defaultValue: '1.1.xxx', description: '版本號:安卓單數1.1.175/IOS雙數1.1.184')
        choice(name: 'DB_ENVIORNMENT', choices: ['二測','外部'], description: '選擇DB環境')
    }

    environment {
        MSSQL_SERVER = 'db-gt-test24.gametower.com.tw'   //db-gt-test24.gametower.com.tw、10.100.20.25
        MSSQL_DATABASE = 'GameTower2_Config'
        TMD_DB = 'tmd_db'
        REMOTE_CREDENTIALS ='remote4463_login'
        CURRENTVERSION = ''
    }
    
    stages {
        stage('Parameter Check') {
            steps {
                script {
                    if (params.CURRENT_VERSION == '1.1.xxx') {
                        error("The CURRENT_VERSION parameter is set to '1.1.xxx', which is a placeholder and not a valid version name. Pipeline execution is terminated to prevent build configuration errors.")
                    }
                }
            }
        }

        stage('更新安卓商店最大版號') {
            steps {
                script {
                    try {
                        if (params.DB_ENVIORNMENT == '外部') {
                        MSSQL_SERVER = '10.100.20.25'
                        } 
                        else {
                        MSSQL_SERVER = 'db-gt-test24.gametower.com.tw'
                        }

                        CURRENTVERSION = params.CURRENT_VERSION

                        withCredentials([usernamePassword(credentialsId: REMOTE_CREDENTIALS, passwordVariable: 'REMOTE_PASSWORD', usernameVariable: 'REMOTE_USER')]) {
                            def remote = getRemoteConfig("192.168.44.63", REMOTE_USER, REMOTE_PASSWORD)
                            withCredentials([usernamePassword(credentialsId: TMD_DB, usernameVariable: 'DB_ACCOUNT', passwordVariable: 'DB_PASSWORD')]) {
                                //修改currentVersion 
                                def updateDbCommand = "sqlcmd -S ${MSSQL_SERVER} -d ${MSSQL_DATABASE} -U ${DB_ACCOUNT} -P ${DB_PASSWORD} -Q \"UPDATE MobileApp SET currentVersion = '${CURRENTVERSION}' WHERE gameId = '2112' AND userClientType = 'Android' AND variation = 0\""
                                sshCommand remote: remote, command: updateDbCommand

                                //確認currentVersion 
                                def confirm_dbCommand = "sqlcmd -S ${MSSQL_SERVER} -d ${MSSQL_DATABASE} -U ${DB_ACCOUNT} -P ${DB_PASSWORD} -Q \"SELECT TOP 999 * FROM MobileApp WHERE gameId = '2112' AND userClientType = 'Android' AND variation = 0\""
                                sshCommand remote: remote, command: confirm_dbCommand
                            }
                        }
                    } catch (Exception e) {
                        echo "更新安卓商店最大版號 with error: ${e.message}"
                        throw e
                    }
                }
            }
        }

        stage('更新安卓官方最大版號') {
            steps {
                script {
                    try {
                        //DB環境
                        if (params.DB_ENVIORNMENT == '外部') {
                        MSSQL_SERVER = '10.100.20.25'
                        } 
                        else {
                        MSSQL_SERVER = 'db-gt-test24.gametower.com.tw'
                        }

                        CURRENTVERSION = params.CURRENT_VERSION
                        
                        //REMOTE
                        withCredentials([usernamePassword(credentialsId: REMOTE_CREDENTIALS, passwordVariable: 'REMOTE_PASSWORD', usernameVariable: 'REMOTE_USER')]) {
                            def remote = getRemoteConfig("192.168.44.63", REMOTE_USER, REMOTE_PASSWORD)
                            withCredentials([usernamePassword(credentialsId: TMD_DB, usernameVariable: 'DB_ACCOUNT', passwordVariable: 'DB_PASSWORD')]) {
                                //修改currentVersion 
                                def updateDbCommand = "sqlcmd -S ${MSSQL_SERVER} -d ${MSSQL_DATABASE} -U ${DB_ACCOUNT} -P ${DB_PASSWORD} -Q \"UPDATE MobileApp SET currentVersion = '${CURRENTVERSION}' WHERE gameId = '2112' AND userClientType = 'Android_IGS' AND variation = 0\""
                                sshCommand remote: remote, command: updateDbCommand

                                //確認currentVersion 
                                def confirm_dbCommand = "sqlcmd -S ${MSSQL_SERVER} -d ${MSSQL_DATABASE} -U ${DB_ACCOUNT} -P ${DB_PASSWORD} -Q \"SELECT TOP 999 * FROM MobileApp WHERE gameId = '2112' AND userClientType = 'Android_IGS' AND variation = 0\""
                                sshCommand remote: remote, command: confirm_dbCommand
                            }
                        }
                    } catch (Exception e) {
                        echo "更新安卓官方最大版號 with error: ${e.message}"
                        throw e
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                if (currentBuild.result == 'SUCCESS') {
                    emailext(
                        subject: "SUCCESS Modify ${MSSQL_SERVER} IOS CurrentVersion",
                        body: "SUCCESS",
                        to: "${params.BUILDER_NAME}@igs.com.tw"
                    )
                } 
                else {
                    emailext(
                        subject: "Failure Modify ${MSSQL_SERVER} IOS CurrentVersion",
                        body: "Failure",
                        to: "${params.BUILDER_NAME}@igs.com.tw"
                    )
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