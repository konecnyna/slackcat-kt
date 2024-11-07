package com.slackcat.app

fun main(args: Array<String>) {
    val slackcatApp = SlackcatApp()
    slackcatApp.onCreate(args.joinToString(" "))
}
