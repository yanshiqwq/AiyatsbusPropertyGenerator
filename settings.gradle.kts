rootProject.name = "AiyatsbusPropertyGenerator"


pluginManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/public/") }
        maven { url = uri("https://maven.aliyun.com/repositories/jcenter") }
        maven { url = uri("https://maven.aliyun.com/repositories/google") }
        maven { url = uri("https://maven.aliyun.com/repositories/central") }
        mavenCentral()
        gradlePluginPortal()
        google()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/public/") }
        maven { url = uri("https://maven.aliyun.com/repositories/jcenter") }
        maven { url = uri("https://maven.aliyun.com/repositories/google") }
        maven { url = uri("https://maven.aliyun.com/repositories/central") }
        mavenCentral()
        google()
    }
}