package gatling.config

object Config {
  val domain = System.getProperty("domain", "feature-aae-731.35.228.195.195.nip.io").toString()

  val realm = System.getProperty("realm", "activiti").toString()
}