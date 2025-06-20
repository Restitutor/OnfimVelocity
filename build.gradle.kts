allprojects {
    group = "me.arcator"
    version = "1.8.2"

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/") { name = "papermc-repo" }
        maven("https://oss.sonatype.org/content/groups/public/") { name = "sonatype" }
        maven("https://repo.minebench.de/") { name = "minebench-repo" }
    }
}
