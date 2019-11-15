package com.revolut.controller;

import com.revolut.service.AccountService
import io.javalin.Javalin
import io.javalin.http.Context

fun addEndpoint(app: Javalin) {
    app.put("/api/1/account", AccountController::create)
    app.get("/api/1/account/:id", AccountController::get)
    //todo add pagination
    app.get("/api/1/account", AccountController::getAll)
    //Not orthodox rest, but more strict
    app.post("/api/1/account/deposit/:id/:amount", AccountController::deposit)
    app.post("/api/1/account/withdraw/:id/:amount", AccountController::withdraw)
    app.post("/api/1/account/transfer/:from/:to/:amount", AccountController::transfer)
    app.delete("/api/1/account/:id", AccountController::delete)
}

object AccountController {
    fun create(ctx: Context) {
        ctx.json(AccountService.create());
    }

    fun get(ctx: Context) {
        val id = ctx.pathParam<Long>("id").get()
        ctx.json(AccountService.get(id))
    }

    fun getAll(ctx: Context) {
        ctx.json(AccountService.getAll())
    }

    fun deposit(ctx: Context) {
        val id = ctx.pathParam<Long>("id").get()
        val amount = ctx.pathParam<Long>("amount").check({ it >= 0 }).get()
        ctx.json(AccountService.deposit(id, amount))
    }

    fun withdraw(ctx: Context) {
        val id = ctx.pathParam<Long>("id").get()
        val amount = ctx.pathParam<Long>("amount").check({ it >= 0 }).get()
        ctx.json(AccountService.withdraw(id, amount))
    }

    fun transfer(ctx: Context) {
        val from = ctx.pathParam<Long>("from").get()
        val to = ctx.pathParam<Long>("to").get()
        val amount = ctx.pathParam<Long>("amount").check({ it >= 0 }).get()
        AccountService.transfer(from, to, amount)
    }

    fun delete(ctx: Context) {
        AccountService.delete(ctx.pathParam(":id").toLong())
    }
}