#!/usr/bin/env groovy

pipeline {
  agent any

  parameters {
    booleanParam(defaultValue: false, description: '', name: 'runEndToEndTestsOnPR')
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
    JAVA_HOME="/usr/lib/jvm/java-1.11.0-openjdk-amd64"
  }

  stages {
    stage('Maven Build') {
      steps {
        script {
          long stepBuildTime = System.currentTimeMillis()

          sh 'mvn -version'
          sh 'mvn clean verify'

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
      stages {
        stage('End-to-End Tests') {
            when {
                anyOf {
                  branch 'master'
                  environment name: 'RUN_END_TO_END_ON_PR', value: 'true'
                }
            }
            steps {
                runAppE2E("publicauth", "card,zap")
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
       deployEcs("publicauth")
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
