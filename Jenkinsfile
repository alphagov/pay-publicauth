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
  }

  stages {
    stage('Maven Build') {
      steps {
        sh 'docker pull govukpay/postgres:9.4.4'
        sh 'mvn clean package'
      }
    }
    stage('Docker Build') {
      steps {
        script {
          buildApp{
            app = "publicauth"
          }
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
          dockerTag {
            app = "publicauth"
          }
        }
      }
    }
    stage('Deploy') {
      when {
        branch 'master'
      }
      steps {
        deploy("publicauth", "test", null, true)
      }
    }
  }
}
