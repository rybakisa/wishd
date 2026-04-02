rootProject.name = "wishlist"

include(":clients:shared")
project(":clients:shared").projectDir = file("clients/shared")

include(":clients:android")
project(":clients:android").projectDir = file("clients/android")
