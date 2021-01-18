package de.blinkt.openvpn.core.connection;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

// Adapter for Gson used to serialize and deserialize abstract Connection class, adds a property about the implemented class
public class ConnectionAdapter implements JsonSerializer<Connection>, JsonDeserializer<Connection> {

    public final static String META_TYPE = ConnectionAdapter.class.getSimpleName() + ".META_TYPE";
    @Override
    public Connection deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        String className = jsonObject.get(META_TYPE).getAsString();
        try {
            Class<?> clz = Class.forName(className);
            return context.deserialize(json, clz);
        } catch (ClassNotFoundException e) {
            throw new JsonParseException(e);
        }
    }

    @Override
    public JsonElement serialize(Connection src, Type typeOfSrc, JsonSerializationContext context) {
         JsonElement json = context.serialize(src, src.getClass());
         json.getAsJsonObject().addProperty(META_TYPE, src.getClass().getCanonicalName());
         return json;
    }
}
