package io.arsha.api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import io.arsha.api.cache.CacheManager;
import io.arsha.api.common.AppConfig;
import io.arsha.api.market.Marketplace;
import io.arsha.api.routes.utility.Scraper;
import io.arsha.api.routes.utility.Utility;
import io.arsha.api.routes.v1.V1;
import io.arsha.api.routes.v2.V2;
import io.arsha.api.util.Util;
import io.arsha.api.util.mongodb.Mongo;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;

public class API extends AbstractVerticle {
	private static Logger logger = LoggerFactory.getLogger(API.class);
	public static AppConfig config = new AppConfig();

	public static void main(String[] args) {
		try {
			String file = new String(Files.readAllBytes(Paths.get("conf/config.json")));
			config = new AppConfig(file);

			new String(Files.readAllBytes(Paths.get("api/key.txt")));
			new String(Files.readAllBytes(Paths.get("conf/mongo.json"))); 
		} catch (IOException io) {
			logger.error("Exiting: error reading a config: " + io.getMessage());
			return;
		}



		JsonObject metrics = config.getMetrics();
		Boolean useMetrics = config.getDebug() ?  false : metrics.getBoolean("use");
		if (!useMetrics) logger.warn("Starting in development mode - metrics will be disabled");

		VertxOptions options = new VertxOptions().setMetricsOptions(
			new MicrometerMetricsOptions()
				.setPrometheusOptions(
				new VertxPrometheusOptions()
					.setEnabled(useMetrics)
					.setStartEmbeddedServer(useMetrics)
					.setEmbeddedServerOptions(
					new HttpServerOptions().setHost(metrics.getString("host"))
						.setPort(metrics.getInteger("port")))
						.setEmbeddedServerEndpoint(metrics.getString("endpoint"))
						.setEnabled(useMetrics))
			.setEnabled(useMetrics));

		Vertx vertx = Vertx.vertx(options);
		vertx.deployVerticle(new API())
			.onSuccess(deploy -> logger.info("Deployed verticle"))
			.onFailure(fail -> logger.error("Failed to deploy: " + fail.getMessage()));
	}

	public static Future<Void> init(Vertx vertx) {
		Promise<Void> init = Promise.promise();
		Marketplace.init(vertx, config).onSuccess(mp -> {
			CompositeFuture.all(
				CacheManager.init(config), 
				Mongo.init(vertx), 
				Scraper.init(vertx))
			.onSuccess(cf -> init.complete())
			.onFailure(init::fail);
		}).onFailure(init::fail);

		return init.future();
	}

	@Override
	public void start() {
		Util.init(vertx, config).onSuccess(util -> {
			JsonObject appConfig = config.getApp();
			JsonObject metricsConfig = config.getMetrics();

			HttpServerOptions options = new HttpServerOptions()
				.setHost(appConfig.getString("host"))
				.setPort(appConfig.getInteger("port"));

			HttpServer server = vertx.createHttpServer(options);
			RouterBuilder.create(vertx, "api/OpenAPI.yaml").onComplete(builder -> {
				RouterBuilder rb = builder.result();
				V1.registerOperations(rb);
				V2.registerOperations(rb);
				Utility.registerOperations(rb);

				Router mainRouter = rb.createRouter();
				mainRouter.route("/").handler(ctx -> ctx.redirect(config.getDocs()));
				mainRouter.mountSubRouter("/scraper", Scraper.getScraperRouter());
				mainRouter.getRoutes().forEach(Route::disable);

				server.requestHandler(mainRouter).listen(srv -> {
					if (srv.succeeded()) {
						String running = String.format("API listening on %s:%s", 
							appConfig.getString("host"),
							srv.result().actualPort());
						
						logger.info(running);

						if (srv.result().isMetricsEnabled()) {
							String metrics = String.format("Embedded metrics available on %s:%s%s",
								metricsConfig.getString("host"), metricsConfig.getInteger("port"),
								metricsConfig.getString("endpoint"));
							logger.info(metrics);
						} else {
							logger.warn("Metrics disabled");
						}

						init(vertx).onSuccess(init -> {
							mainRouter.getRoutes().forEach(Route::enable);
							logger.info("Init done");
						}).onFailure(fail -> {
							logger.error("Failed to start: " + fail.getMessage());
							server.close(done -> vertx.close());
						});
					} else {
						logger.error("Failed to start:" + srv.cause().getMessage());
						vertx.close();
					}
				});
			}).onFailure(fail -> {
				logger.error("Failed building API specification: " + fail.getMessage());
				vertx.close();
			});
		}).onFailure(fail -> {
			logger.error(fail.getMessage());
			vertx.close();
		});
	}
}