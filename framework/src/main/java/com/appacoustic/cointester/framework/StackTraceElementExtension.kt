package com.appacoustic.cointester.framework

fun StackTraceElement.generateTag() = className.substringAfterLast(".")

fun StackTraceElement.generateMessage(msg: Any) = "$methodName() $msg"
