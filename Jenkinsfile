#!/usr/bin/env groovy

pipeline {
  agent any

  parameters {
    booleanParam(defaultValue: true, description: '', name: 'runEndToEndTestsOnPR')
    booleanParam(defaultValue: true, description: '', name: 'runAcceptTestsOnPR')
    booleanParam(defaultValue: false, description: '', name: 'runZapTestsOnPR')
  }

  options {
    ansiColor('xterm')
    timestamps()
  }

  libraries {
    lib("pay-jenkins-library@master")
  }

  environment {
    DOCKER_HOST = "unix:///var/run/docker.sock"
    RUN_END_TO_END_ON_PR = "${params.runEndToEndTestsOnPR}"
    RUN_ACCEPT_ON_PR = "${params.runAcceptTestsOnPR}"
    RUN_ZAP_ON_PR = "${params.runZapTestsOnPR}"
  }

  stages {
    stage('Maven Build') {
      steps {
        script {
          def long stepBuildTime = System.currentTimeMillis()

          sh 'docker pull govukpay/postgres:9.4.4'
          sh 'mvn clean package'

          postSuccessfulMetrics("publicauth.maven-build", stepBuildTime)
        }
      }
      post {
        failure {
          postMetric("publicauth.maven-build.failure", 1)
        }
      }
    }
    stage('Docker Build') {
      steps {
        script {
          buildAppWithMetrics {
            app = "publicauth"
          }
        }
      }
      post {
        failure {
          postMetric("publicauth.docker-build.failure", 1)
        }
      }
    }
    stage('Tests') {
      failFast true
      parallel {
        stage('Card Payment End-to-End Tests') {
            when {
                anyOf {
                  branch 'master'
                  environment name: 'RUN_END_TO_END_ON_PR', value: 'true'
                }
            }
            steps {
                runCardPaymentsE2E("publicauth")
            }
        }
        stage('Accept Tests') {
            when {
                anyOf {
                  branch 'master'
                  environment name: 'RUN_ACCEPT_ON_PR', value: 'true'
                }
            }
            steps {
                runAccept("publicauth")
            }
        }
         stage('ZAP Tests') {
            when {
                anyOf {
                  branch 'master'
                  environment name: 'RUN_ZAP_ON_PR', value: 'true'
                }
            }
            steps {
                runZap("publicauth")
            }
         }
      }
    }
    stage('Docker Tag') {
      steps {
        script {
          dockerTagWithMetrics {
            app = "publicauth"
          }
        }
      }
      post {
        failure {
          postMetric("publicauth.docker-tag.failure", 1)
        }
      }
    }
    stage('Deploy') {
     when {
       branch 'master'
     }
     steps {
       deployEcs("publicauth", "test", null, false, false)
     }
   }
   stage('Smoke Tests') {
     failFast true
     parallel {
       stage('Card Smoke Test') {
         when { branch 'master' }
         steps { runCardSmokeTest() }
       }
       stage('Direct Debit Smoke Test') {
         when { branch 'master' }
         steps { runDirectDebitSmokeTest() }
       }
     }
   }
   stage('Complete') {
     failFast true
     parallel {
       stage('Tag Build') {
         when {
           branch 'master'
         }
         steps {
           tagDeployment("publicauth")
         }
       }
       stage('Trigger Deploy Notification') {
         when {
           branch 'master'
         }
         steps {
           triggerGraphiteDeployEvent("publicauth")
         }
       }
     }
   }
  }
  post {
    failure {
      postMetric(appendBranchSuffix("publicauth") + ".failure", 1)
    }
    success {
      postSuccessfulMetrics(appendBranchSuffix("publicauth"))
    }
  }
}
