package io.github.joht.showcase.quarkuseventsourcing.messaging.infrastructure.axon.serializer.jsonb.axon.adapter;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.adapter.JsonbAdapter;

import org.axonframework.eventhandling.ReplayToken;
import org.axonframework.eventhandling.TrackingToken;

/**
 * This adapter provides serialization support for the {@link ReplayToken}.
 * <p>
 * Since it contains exchangeable types, this adapter needs to be specified explicitly.
 * 
 * @author JohT
 */
class JsonbReplayTokenAdapter implements JsonbAdapter<ReplayToken, JsonObject> {

    private static final Logger LOGGER = Logger.getLogger(JsonbReplayTokenAdapter.class.getName());

    private static final String TYPE_FIELD = "@class";
    private static final String TOKEN_AT_RESET = "tokenAtReset";
    private static final String CURRENT_TOKEN = "currentToken";

    private static final String MISSING_TYPE_FIELD = "No type field \"%s\" found in %s";

    private JsonbConfig config; // deferred immutable -> gets cloned before first use
    private transient Jsonb jsonb = null;

    public static final JsonbAdapter<ReplayToken, JsonObject> using(JsonbConfig config) {
        return new JsonbReplayTokenAdapter(config);
    }

    JsonbReplayTokenAdapter(JsonbConfig config) {
        this.config = config;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject adaptToJson(ReplayToken obj) {
        return Json.createObjectBuilder()
                .add(CURRENT_TOKEN, buildJsonFrom(obj.getCurrentToken()))
                .add(TOKEN_AT_RESET, buildJsonFrom(obj.getTokenAtReset()))
                .build();
    }

    private JsonObjectBuilder buildJsonFrom(Object obj) {
        JsonObject jsonObject = getJsonb().fromJson(getJsonb().toJson(obj), JsonObject.class);
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add(TYPE_FIELD, obj.getClass().getName());
        builder.addAll(Json.createObjectBuilder(jsonObject));
        return builder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReplayToken adaptFromJson(JsonObject obj) {
        TrackingToken currentToken = tokenFrom(obj.getJsonObject(CURRENT_TOKEN));
        TrackingToken tokenAtReset = tokenFrom(obj.getJsonObject(TOKEN_AT_RESET));
        return new ReplayToken(tokenAtReset, currentToken);
    }

    private TrackingToken tokenFrom(JsonObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }
        return (TrackingToken) getJsonb().fromJson(jsonObject.toString(), forName(getTypeNameOf(jsonObject)));
    }

    private String getTypeNameOf(JsonObject obj) {
        if (!obj.containsKey(TYPE_FIELD)) {
            throw new IllegalArgumentException(String.format(MISSING_TYPE_FIELD, TYPE_FIELD, obj + ""));
        }
        return obj.getString(TYPE_FIELD);
    }

    private static Class<?> forName(String typename) {
        try {
            return Class.forName(typename);
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.WARNING, e, () -> "Class " + typename + " not found");
            throw new IllegalArgumentException(e);
        }
    }

    private Jsonb getJsonb() {
        if (jsonb == null) {
            jsonb = JsonbBuilder.create(config = cloneConfig(config));
        }
        return jsonb;
    }

    private static JsonbConfig cloneConfig(JsonbConfig config) {
        JsonbConfig clonedConfig = new JsonbConfig();
        config.getAsMap().forEach(clonedConfig::setProperty);
        return clonedConfig;
    }

    @Override
    public String toString() {
        return "JsonbReplayTokenAdapter [config=" + config + "]";
    }
}