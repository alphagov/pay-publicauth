#!/usr/bin/env groovy

pipeline {
  agent any

  options {
    ansiColor('xterm')
    timestamps()
  }

  libraries {
    lib("pay-jenkins-library@master")
  }

  environment {
    DOCKER_HOST = "unix:///var/run/docker.sock"
    HOSTED_GRAPHITE_ACCOUNT_ID = credentials('graphite_account_id')
    HOSTED_GRAPHITE_API_KEY = credentials('graphite_api_key')
  }

  stages {
    stage('Maven Build') {
      steps {
        sh 'docker pull govukpay/postgres:9.4.4'
        sh 'mvn clean package'
      }
      post {
        failure {
          postMetric("publicauth.maven-build.failure", 1, "new")
        }
        success {
          postSuccessfulMetrics("publicauth.maven-build")
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
          postMetric("publicauth.docker-build.failure", 1, "new")
        }
      }
    }
    stage('Test') {
      steps {
        runEndToEnd("publicauth")
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
          postMetric("publicauth.docker-tag.failure", 1, "new")
        }
      }
    }
    stage('Deploy') {
      when {
        branch 'master'
      }
      steps {
        deployEcs("publicauth", "test", null, true, true)
      }
    }
  }
  post {
    failure {
      postMetric("publicauth.failure", 1, "new")
    }
    success {
      postSuccessfulMetrics("publicauth")
    }
  }
}
