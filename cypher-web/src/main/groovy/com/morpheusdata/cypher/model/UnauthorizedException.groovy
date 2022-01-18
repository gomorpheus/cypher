package com.morpheusdata.cypher.model

class UnauthorizedException extends Exception {
    UnauthorizedException(String msg) {
        super(msg);
    }
}
