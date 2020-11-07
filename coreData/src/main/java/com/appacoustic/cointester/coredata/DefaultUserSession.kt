package com.appacoustic.cointester.coredata

import com.appacoustic.cointester.coredomain.session.UserSession

class DefaultUserSession : UserSession {

    private var foo = false

    override fun isFoo(): Boolean = foo

    override fun setFoo(b: Boolean) {
        this.foo = b
    }
}
