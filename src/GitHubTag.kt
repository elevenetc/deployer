package com.elevenetc

class GitHubTag(
    val ref: String,
    val repository: Repository
) {
    class Repository {
        lateinit var clone_url: String
        lateinit var full_name: String
    }
}