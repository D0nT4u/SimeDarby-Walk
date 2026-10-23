package com.example.simedarbywalk;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

final class DemoStore {
    static final String[] TYPES = {"Stray dog", "Broken light", "Debris", "Road closed or resurfacing", "Blind corner", "Dark car park", "Other"};
    private final SharedPreferences preferences;
    JSONObject data;

    DemoStore(Context context) {
        preferences = context.getSharedPreferences("offline_demo_v1", Context.MODE_PRIVATE);
        try { data = new JSONObject(preferences.getString("data", "{}")); }
        catch (JSONException ignored) { data = new JSONObject(); }
        if (!data.has("hazards")) reset();
        if (profile() != null && profile().optString("name").equals("Aina Rahman"))
            put(profile(), "name", context.getString(R.string.sample_resident_name));
        for (JSONObject contact : list("contacts")) if (contact.optString("name").equals("Daniel Tan"))
            put(contact, "name", context.getString(R.string.sample_contact_name));
        for (JSONObject alert : list("alerts")) if (alert.optString("name").equals("Aina Rahman"))
            put(alert, "name", context.getString(R.string.sample_resident_name));
        if (data.optJSONObject("walk") != null && data.optJSONObject("walk").optString("contact").equals("Daniel Tan"))
            put(data.optJSONObject("walk"), "contact", context.getString(R.string.sample_contact_name));
        save();
    }

    static JSONObject obj(Object... fields) {
        JSONObject result = new JSONObject();
        for (int i = 0; i < fields.length; i += 2) put(result, (String) fields[i], fields[i + 1]);
        return result;
    }

    static void put(JSONObject target, String key, Object value) {
        try { target.put(key, value); }
        catch (JSONException e) { throw new IllegalArgumentException(e); }
    }

    void save() { preferences.edit().putString("data", data.toString()).apply(); }
    List<JSONObject> list(String key) {
        List<JSONObject> result = new ArrayList<>();
        JSONArray array = data.optJSONArray(key);
        if (array != null) for (int i = 0; i < array.length(); i++) {
            JSONObject value = array.optJSONObject(i);
            if (value != null) result.add(value);
        }
        return result;
    }
    void add(String key, JSONObject value) { data.optJSONArray(key).put(value); save(); }
    JSONObject find(String key, String id) {
        for (JSONObject item : list(key)) if (item.optString("id").equals(id)) return item;
        return null;
    }
    void remove(String key, String id) {
        JSONArray retained = new JSONArray();
        for (JSONObject item : list(key)) if (!item.optString("id").equals(id)) retained.put(item);
        put(data, key, retained); save();
    }
    JSONObject profile() { return data.optJSONObject("profile"); }
    boolean active(JSONObject hazard) {
        return !hazard.optString("status").equals("resolved") &&
                (hazard.optLong("end") == 0 || hazard.optLong("end") > System.currentTimeMillis());
    }
    List<JSONObject> hazards() {
        List<JSONObject> result = new ArrayList<>();
        for (JSONObject h : list("hazards")) if (active(h)) result.add(h);
        return result;
    }
    static List<JSONObject> clusters(List<JSONObject> hazards) {
        Map<String, JSONObject> groups = new LinkedHashMap<>();
        for (JSONObject hazard : hazards) {
            double x = hazard.optDouble("x"), y = hazard.optDouble("y");
            String key = (int) (x * 8) + ":" + (int) (y * 8);
            JSONObject group = groups.get(key);
            if (group == null) {
                group = obj("x", x, "y", y, "count", 0, "id", hazard.optString("id")); groups.put(key, group);
            }
            int count = group.optInt("count");
            put(group, "x", (group.optDouble("x") * count + x) / (count + 1));
            put(group, "y", (group.optDouble("y") * count + y) / (count + 1));
            put(group, "count", count + 1);
        }
        List<JSONObject> result = new ArrayList<>(groups.values());
        result.sort((a, b) -> Integer.compare(b.optInt("count"), a.optInt("count"))); return result;
    }
    void hazard(String type, String description, String fix, double x, double y, long end, String reporter) {
        add("hazards", obj("id", UUID.randomUUID().toString(), "type", type, "description", description,
                "fix", fix, "x", x, "y", y, "end", end, "reporter", reporter,
                "status", "open", "created", System.currentTimeMillis()));
    }
    void reset() {
        data = obj("hazards", new JSONArray(), "alerts", new JSONArray(), "contacts", new JSONArray(),
                "profile", obj("name", "Resident", "email", "resident@demo.com", "phone", "+60123456789",
                        "township", "City of Elmina", "residence", "Condominium", "block", "A", "level", "5", "unit", "05-08", "street", "Jalan Elmina 1", "house", "12"));
        hazard("Broken light", "Street light is not working near the crossing.", "Replace the lamp", .5, .5, 0, "resident");
        hazard("Debris", "Construction material on the footpath.", "Clear the footpath", .5, .35, System.currentTimeMillis() + 86400000, "resident");
        hazard("Blind corner", "Limited visibility at the car park exit.", "Install a convex mirror", .76, .65, 0, "resident");
        hazard("Road closed or resurfacing", "Previous maintenance works.", "Remove the barrier", .3, .2, System.currentTimeMillis() - 86400000, "management");
        add("contacts", obj("id", "sample-contact", "name", "Trusted contact", "phone", "+60129876543")); save();
    }
    static double[][] route(int index, double x, double y) {
        return index == 0 ? new double[][]{{.18, .82}, {.5, .82}, {.5, y}, {x, y}}
                : new double[][]{{.18, .82}, {.18, y}, {x, y}};
    }
    static double distance(double x, double y, double[][] route) {
        double best = Double.POSITIVE_INFINITY;
        for (int i = 1; i < route.length; i++) {
            double ax = route[i - 1][0], ay = route[i - 1][1];
            double dx = route[i][0] - ax, dy = route[i][1] - ay, length = dx * dx + dy * dy;
            double t = length == 0 ? 0 : Math.max(0, Math.min(1, ((x - ax) * dx + (y - ay) * dy) / length));
            best = Math.min(best, Math.hypot(x - ax - t * dx, y - ay - t * dy));
        }
        return best * 1000;
    }
    int risk(int route, double x, double y) {
        int count = 0;
        for (JSONObject h : hazards()) if (distance(h.optDouble("x"), h.optDouble("y"), route(route, x, y)) <= 30) count++;
        return count;
    }
    static double[] position(double[][] route, double fraction) {
        double total = 0;
        for (int i = 1; i < route.length; i++) total += Math.hypot(route[i][0] - route[i - 1][0], route[i][1] - route[i - 1][1]);
        double remaining = total * Math.max(0, Math.min(1, fraction));
        for (int i = 1; i < route.length; i++) {
            double length = Math.hypot(route[i][0] - route[i - 1][0], route[i][1] - route[i - 1][1]);
            if (remaining <= length && length > 0) {
                double t = remaining / length;
                return new double[]{route[i - 1][0] + t * (route[i][0] - route[i - 1][0]), route[i - 1][1] + t * (route[i][1] - route[i - 1][1])};
            }
            remaining -= length;
        }
        return route[route.length - 1].clone();
    }
}
