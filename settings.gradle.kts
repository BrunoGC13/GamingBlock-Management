rootProject.name = "GamingBlock_Management"
include("paper", "velocity")

println(rootProject.name)
println(project(":paper").name)
println(project(":velocity").name)
