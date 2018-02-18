package com.example.demo;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.*;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.templ.HandlebarsTemplateEngine;

public class MainVerticle extends AbstractVerticle {


    @Override
    public void start() throws Exception {

        Router router = Router.router(vertx);


        //Index Route
        router.route("/").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            response
                .putHeader("content-type", "text/plain")
                .end("Running on Index");
        });



        HandlebarsTemplateEngine engine = HandlebarsTemplateEngine.create();
        router.get("/login").handler(ctx -> {
             engine.render(ctx, "templates/index.hbs", res -> {
                if (res.succeeded()) {
                    ctx.response().end(res.result());
                } else {
                    ctx.fail(res.cause());
                }
            });

        });

        router.route("/insert").handler(routingContext -> {

            String uri = "mongodb://localhost:27017";
            JsonObject mongoconfig = new JsonObject()
                .put("connection_string", uri)
                .put("db_name", "appproject");
            MongoClient mongoClient = MongoClient.createShared(vertx, mongoconfig);

            HttpServerResponse response = routingContext.response();
            HttpServerRequest request = routingContext.request();
            String phone = request.getParam("phone");
            String password = request.getParam("password");

            JsonObject document = new JsonObject()
                .put("phone", phone)
                .put("password", password);
                mongoClient.save("userlogin", document, res -> {
                    System.out.println(mongoconfig.getString("db_name"));
                if (res.succeeded())
                {

                    JsonObject query = new JsonObject();
                    mongoClient.find("userlogin", query, fetch ->
                    {
                        if (fetch.succeeded())
                        {
                            for (JsonObject json : fetch.result()) {
                                System.out.println(json);
                            }

                            response.putHeader("content-type","application/json").end(fetch.result());


                        }
                        else
                        {
                            fetch.cause().printStackTrace();
                        }

                    });
                }
                else
                {
                res.cause().printStackTrace();
                }

            });
        });

         vertx.createHttpServer().requestHandler(router::accept).listen(2018);
    }



}


