package com.slackcat

class Foo(vararg ls: String) {
    var lines: List<String> = emptyList()

    init {
        lines = ls.toList()
    }
}