package platform

import org.yaml.snakeyaml.Yaml
import static org.edx.jenkins.dsl.JenkinsPublicConstants.GENERAL_PRIVATE_JOB_SECURITY
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR
import static org.edx.jenkins.dsl.JenkinsPublicConstants.GHPRB_CANCEL_BUILDS_ON_UPDATE

/* stdout logger */
Map config = [:]
Binding bindings = getBinding()
config.putAll(bindings.getVariables())
PrintStream out = config['out']

/* Map to hold the k:v pairs parsed from the secret file */
Map ghprbMap = [:]
try {
    out.println('Parsing secret YAML file')
    String ghprbConfigContents = new File("${GHPRB_SECRET}").text
    Yaml yaml = new Yaml()
    ghprbMap = yaml.load(ghprbConfigContents)
    out.println('Successfully parsed secret YAML file')
}
catch (any) {
    out.println('Jenkins DSL: Error parsing secret YAML file')
    out.println('Exiting with error code 1')
    return 1
}

Map publicBokchoyJobConfig = [
    open: true,
    jobName: 'edx-platform-bokchoy-pipeline-pr',
    repoName: 'edx-platform',
    context: 'jenkins/bokchoy',
    onlyTriggerPhrase: false,
    triggerPhrase: /.*jenkins\W+run\W+bokchoy.*/,
    jenkinsFileDir: 'scripts/Jenkinsfiles',
    jenkinsFileName: 'bokchoy'
]

Map privateBokchoyJobConfig = [
    open: false,
    jobName: 'edx-platform-bokchoy-pipeline-pr_private',
    repoName: 'edx-platform-private',
    context: 'jenkins/bokchoy',
    onlyTriggerPhrase: false,
    triggerPhrase: /.*jenkins\W+run\W+bokchoy.*/,
    jenkinsFileDir: 'scripts/Jenkinsfiles',
    jenkinsFileName: 'bokchoy'
]

Map publicLettuceJobConfig = [
    open: true,
    jobName: 'edx-platform-lettuce-pipeline-pr',
    repoName: 'edx-platform',
    context: 'jenkins/lettuce',
    onlyTriggerPhrase: false,
    triggerPhrase: /.*jenkins\W+run\W+lettuce.*/,
    jenkinsFileDir: 'scripts/Jenkinsfiles',
    jenkinsFileName: 'lettuce'
]

Map privateLettuceJobConfig = [
    open: false,
    jobName: 'edx-platform-lettuce-pipeline-pr_private',
    repoName: 'edx-platform-private',
    context: 'jenkins/lettuce',
    onlyTriggerPhrase: false,
    triggerPhrase: /.*jenkins\W+run\W+lettuce.*/,
    jenkinsFileDir: 'scripts/Jenkinsfiles',
    jenkinsFileName: 'lettuce'
]

Map publicPythonJobConfig = [
    open: true,
    jobName: 'edx-platform-python-pipeline-pr',
    repoName: 'edx-platform',
    context: 'jenkins/python',
    onlyTriggerPhrase: true,
    triggerPhrase: /.*jenkins\W+run\W+python.*/,
    jenkinsFileDir: 'scripts/Jenkinsfiles',
    jenkinsFileName: 'python'
]

Map privatePythonJobConfig = [
    open: false,
    jobName: 'edx-platform-python-pipeline-pr_private',
    repoName: 'edx-platform-private',
    context: 'jenkins/python',
    onlyTriggerPhrase: true,
    triggerPhrase: /.*jenkins\W+run\W+python.*/,
    jenkinsFileDir: 'scripts/Jenkinsfiles',
    jenkinsFileName: 'python'
]

Map publicQualityJobConfig = [
    open: true,
    jobName: 'edx-platform-quality-pipeline-pr',
    repoName: 'edx-platform',
    context: 'jenkins/quality',
    onlyTriggerPhrase: false,
    triggerPhrase: /.*jenkins\W+run\W+quality.*/,
    jenkinsFileDir: 'scripts/Jenkinsfiles',
    jenkinsFileName: 'quality'
]

Map privateQualityJobConfig = [
    open: false,
    jobName: 'edx-platform-quality-pipeline-pr_private',
    repoName: 'edx-platform-private',
    context: 'jenkins/quality',
    onlyTriggerPhrase: false,
    triggerPhrase: /.*jenkins\W+run\W+quality.*/,
    jenkinsFileDir: 'scripts/Jenkinsfiles',
    jenkinsFileName: 'quality'
]

List jobConfigs = [
    publicBokchoyJobConfig,
    privateBokchoyJobConfig,
    publicLettuceJobConfig,
    privateLettuceJobConfig,
    publicPythonJobConfig,
    privatePythonJobConfig,
    publicQualityJobConfig,
    privateQualityJobConfig
]

/* Iterate over the job configurations */
jobConfigs.each { jobConfig ->

    // This is the job DSL responsible for creating the main pipeline job.
    pipelineJob(jobConfig.jobName) {

        definition {

            if (!jobConfig.open.toBoolean()) {
                authorization GENERAL_PRIVATE_JOB_SECURITY()
            }
            properties {
                githubProjectUrl("https://github.com/edx/${jobConfig.repoName}/")
            }
            logRotator JENKINS_PUBLIC_LOG_ROTATOR(7)
            environmentVariables(
                REPO_NAME: "${jobConfig.repoName}"
            )

            triggers {
                githubPullRequest {
                    admins(ghprbMap['admin'])
                    useGitHubHooks()
                    triggerPhrase(jobConfig.triggerPhrase)
                    onlyTriggerPhrase(jobConfig.onlyTriggerPhrase)
                    userWhitelist(ghprbMap['userWhiteList'])
                    orgWhitelist(ghprbMap['orgWhiteList'])
                    extensions {
                        commitStatus {
                            context(jobConfig.context)
                        }
                    }
                }
            }

            configure GHPRB_CANCEL_BUILDS_ON_UPDATE(false)

            cpsScm {
                scm {
                    git {
                        extensions {
                            cloneOptions {
                                honorRefspec(true)
                                noTags(true)
                                shallow(true)
                            }
                            sparseCheckoutPaths {
                                sparseCheckoutPaths {
                                    sparseCheckoutPath {
                                        path(jobConfig.jenkinsFileDir)
                                    }
                                }
                            }
                        }
                        remote {
                            credentials('jenkins-worker')
                            github("edx/${jobConfig.repoName}", 'ssh', 'github.com')
                            refspec('+refs/pull/${ghprbPullId}/*:refs/remotes/origin/pr/${ghprbPullId}/*')
                            branch('\${sha1}')
                        }
                    }
                }
                scriptPath(jobConfig.jenkinsFileDir + '/' + jobConfig.jenkinsFileName)
            }
        }
    }
}
