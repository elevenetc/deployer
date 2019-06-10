package com.elevenetc

class GitHubTag(
    val ref: String,
    val repository: Repository,
    val full_name: String
) {
    class Repository {
        lateinit var clone_url: String
    }
}