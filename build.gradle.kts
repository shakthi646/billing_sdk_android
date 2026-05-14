// Root build for the standalone billing-sdk project.
// Plugin versions are declared once here; subprojects (the billing-sdk module)
// apply them by id without a version.
//
// When this module is consumed by a host app via path-based include
// (see README), the host's plugin classpath is used instead and these versions
// are ignored.
plugins {
    id("com.android.library") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "2.2.10" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
    id("com.google.dagger.hilt.android") version "2.57.1" apply false
    id("com.google.devtools.ksp") version "2.2.10-2.0.2" apply false
}
