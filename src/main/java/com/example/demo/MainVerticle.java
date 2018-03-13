package com.example.demo;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServerFileUpload;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.*;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.templ.HandlebarsTemplateEngine;
import org.bson.internal.Base64;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Set;
import static jdk.nashorn.internal.runtime.regexp.joni.constants.AsmConstants.S;

public class MainVerticle extends AbstractVerticle {
    @Override
    public void start() throws Exception {
    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create().setUploadsDirectory("uploads"));
        String uri = "mongodb://localhost:27017";
        JsonObject mongoconfig = new JsonObject()
            .put("connection_string", uri)
            .put("db_name", "appproject");
        MongoClient mongoClient = MongoClient.createShared(vertx, mongoconfig);

        //Index Route
        router.route("/").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            response
                .putHeader("content-type", "text/plain")
                .end("Running on Index");
        });

        //Template Handler
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
        //Database Connection and Insert Data
        Route handler = router.post("/user_data").handler((RoutingContext routingContext) -> {
            HttpServerResponse response = routingContext.response();
            HttpServerRequest request = routingContext.request();
            String phone = request.getParam("phone");
            String email = request.getParam("email");
            String question = request.getParam("question");

            response.putHeader("Content-Type", "text/plain");
            response.setChunked(true);
            for (FileUpload fu : routingContext.fileUploads()) {
                try {
                    File f = new File(fu.uploadedFileName());
                    BufferedImage image = ImageIO.read(f);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(image, "png", baos);
                    byte[] res = baos.toByteArray();
                    String encodedImage = Base64.encode(baos.toByteArray());

                    if (!encodedImage.equalsIgnoreCase("")) {

                        JsonObject document = new JsonObject()
                        .put("phone", phone)
                        .put("email", email)
                        .put("question", question)
                        .put("file", encodedImage);
                        mongoClient.save("userlogin", document, resDb -> {
                            if (resDb.succeeded())
                            {
                                JsonObject query = new JsonObject();
                                mongoClient.find("userlogin", query, fetch ->
                                {
                                    if (fetch.succeeded())
                                    {
                                        routingContext.put("dataList", fetch.result());
                                        engine.render(routingContext, "templates/user_data.hbs", hnd -> {
                                            if (hnd.succeeded()) {
                                                routingContext.response().putHeader("content-type","text/html").end(hnd.result());
                                            } else {
                                                routingContext.fail(hnd.cause());
                                            }
                                        });
                                    }
                                    else
                                    {
                                    fetch.cause().printStackTrace();
                                    }
                                });
                            }
                            else
                            {
                            resDb.cause().printStackTrace();
                            }

                        });
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


        });

        //Update User Data

        Route delete = router.get("/delete").handler((RoutingContext routingContext) -> {
            HttpServerResponse response = routingContext.response();
            HttpServerRequest request = routingContext.request();
            String userObject = request.getParam("userObject");
            System.out.println(userObject);

        });





        vertx.createHttpServer().requestHandler(router::accept).listen(2018);
    }



}


