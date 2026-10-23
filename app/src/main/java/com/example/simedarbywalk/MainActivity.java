package com.example.simedarbywalk;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.InputType;
import android.util.Patterns;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import org.json.JSONObject;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private DemoStore store;
    private LinearLayout root, body;
    private String role = "", screen = "role_selection", selectedAlert = "", loginRole = "resident";
    private int red, navy, border, background, ink, green;
    private TownshipMapView liveMap;
    private TextView liveStatus, lightStatus, progressStatus, checkInText;
    private AlertDialog checkInDialog;
    private long checkInDeadline = 0, lastMovement = 0;
    private boolean paused = false, automaticAlertSent = false, foreground = false;
    private long sosDeadline = 0;
    private boolean sosArmed = false;
    private double destinationX = .82, destinationY = .20;
    private String destinationName = "Community centre";
    private int selectedRoute = 1;
    private int selectedRoleIndex = 0;
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            updateSimulation();
            handler.postDelayed(this, 1000);
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            getSplashScreen().setOnExitAnimationListener(android.window.SplashScreenView::remove);
        }
        EdgeToEdge.enable(this);
        store = new DemoStore(this);
        red = getColor(R.color.brand_crimson); navy = getColor(R.color.brand_navy);
        border = getColor(R.color.card_border); background = getColor(R.color.app_background);
        ink = getColor(R.color.body_text); green = getColor(R.color.resolved_green);
        // A fresh demo launch always shows authentication; recreation keeps the current session.
        role = savedInstanceState == null ? "" : savedInstanceState.getString("role", "");
        if (savedInstanceState != null) {
            loginRole = savedInstanceState.getString("loginRole", "resident");
            selectedRoleIndex = savedInstanceState.getInt("selectedRoleIndex", 0);
            destinationX = savedInstanceState.getDouble("dx", .82);
            destinationY = savedInstanceState.getDouble("dy", .20);
            selectedAlert = savedInstanceState.getString("alert", "");
        }
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (sosArmed) { cancelSos(); return; }
                if (screen.equals("role_selection")) finish();
                else if (screen.equals("login")) login();
                else if (screen.equals("register")) signInForm();
                else showHome();
            }
        });
        if (savedInstanceState == null) {
            shell("Township of Tomorrow", false); logo(body); title("SimeDarby Walk");
            text("Safer walks. Connected communities.");
            handler.postDelayed(this::login, 900);
        } else {
            String restoredScreen = savedInstanceState.getString("screen", "role_selection");
            switch (restoredScreen) {
                case "role_selection": login(); break;
                case "login": signInForm(); break;
                case "register": register(); break;
                case "permissions": permissions(); break;
                default: if (role.isEmpty()) login(); else showHome();
            }
        }
    }

    @Override protected void onStart() { super.onStart(); foreground = true; handler.postDelayed(tick, 1000); }
    @Override protected void onStop() {
        foreground = false;
        handler.removeCallbacks(tick);
        if (sosArmed) cancelSos();
        store.save(); super.onStop();
    }
    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (checkInDialog != null) checkInDialog.dismiss();
        super.onDestroy();
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putString("role", role);
        state.putString("screen", screen);
        state.putString("loginRole", loginRole);
        state.putInt("selectedRoleIndex", selectedRoleIndex);
        state.putDouble("dx", destinationX); state.putDouble("dy", destinationY);
        state.putString("alert", selectedAlert); store.save(); super.onSaveInstanceState(state);
    }

    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); return layout;
    }
    private TextView label(String value, int size, boolean heading) {
        TextView view = new TextView(this); view.setText(value); view.setTextSize(size);
        view.setTextColor(heading ? navy : ink);
        view.setTypeface(Typeface.create(heading ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
        view.setPadding(0, dp(4), 0, dp(8)); return view;
    }
    private TextView text(String value) { TextView t = label(value, 14, false); body.addView(t); return t; }
    private void title(String value) { body.addView(label(value, 26, true)); }
    private GradientDrawable cardBackground() {
        GradientDrawable drawable = new GradientDrawable(); drawable.setColor(Color.WHITE);
        drawable.setCornerRadius(dp(8)); drawable.setStroke(dp(1), border); return drawable;
    }
    private LinearLayout card(String heading, String detail) {
        LinearLayout card = column(); card.setPadding(dp(16), dp(12), dp(16), dp(12));
        card.setBackground(cardBackground());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2); params.bottomMargin = dp(12);
        body.addView(card, params);
        if (!heading.isEmpty()) card.addView(label(heading, 17, true));
        if (!detail.isEmpty()) card.addView(label(detail, 14, false)); return card;
    }
    private MaterialButton button(LinearLayout parent, String text, boolean primary, Runnable action) {
        MaterialButton button = new MaterialButton(this, null, primary ? com.google.android.material.R.attr.materialButtonStyle
                : com.google.android.material.R.attr.materialButtonOutlinedStyle);
        button.setText(text); button.setAllCaps(false); button.setCornerRadius(dp(8));
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(primary ? red : getColor(R.color.white)));
        button.setTextColor(primary ? getColor(R.color.white) : navy);
        button.setStrokeColor(android.content.res.ColorStateList.valueOf(primary ? red : border));
        button.setStrokeWidth(primary ? 0 : dp(1)); button.setElevation(0);
        button.setMinimumHeight(dp(48));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2); params.bottomMargin = dp(4);
        parent.addView(button, params); button.setOnClickListener(v -> action.run()); return button;
    }
    private TextInputEditText field(LinearLayout parent, String hint, String value, int inputType) {
        TextInputLayout box = (TextInputLayout) getLayoutInflater()
                .inflate(R.layout.component_text_field, parent, false);
        box.setHint(hint);
        box.setDefaultHintTextColor(android.content.res.ColorStateList.valueOf(navy));
        TextInputEditText input = (TextInputEditText) box.getEditText();
        input.setInputType(inputType); input.setText(value);
        parent.addView(box); return input;
    }
    private String value(EditText field) { return field.getText().toString().trim(); }
    private Spinner spinner(LinearLayout parent, String[] values) {
        Spinner spinner = new Spinner(this, Spinner.MODE_DROPDOWN);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, values) {
            @Override public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setTextColor(navy); view.setTextSize(16);
                android.graphics.drawable.Drawable arrow = getDrawable(android.R.drawable.arrow_down_float).mutate();
                arrow.setTint(navy);
                view.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, arrow, null);
                view.setCompoundDrawablePadding(dp(8));
                return view;
            }
            @Override public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                view.setTextColor(ink); view.setBackgroundColor(getColor(R.color.white));
                view.setMinHeight(dp(48)); return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter); spinner.setBackground(cardBackground()); spinner.setPadding(dp(12), 0, dp(12), 0);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(52)); params.bottomMargin = dp(12);
        parent.addView(spinner, params); return spinner;
    }
    private void logo(LinearLayout parent) {
        int id = getResources().getIdentifier("sime_darby_property_logo", "drawable", getPackageName());
        if (id != 0) {
            ImageView image = new ImageView(this); image.setImageResource(id); image.setAdjustViewBounds(true);
            image.setScaleType(ImageView.ScaleType.FIT_CENTER); image.setContentDescription("Sime Darby Property");
            parent.addView(image, new LinearLayout.LayoutParams(-1, dp(72)));
        }
    }
    private void shell(String heading, boolean navigation) {
        liveMap = null; liveStatus = null; lightStatus = null; progressStatus = null;
        root = column(); root.setBackgroundColor(background);
        setContentView(root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom); return insets;
        });
        LinearLayout header = column(); header.setPadding(dp(20), dp(12), dp(20), dp(4));
        header.addView(label("SimeDarby Walk", 20, true));
        TextView demo = label("OFFLINE DEMO  /  No real alerts are sent", 12, false); demo.setTextColor(red);
        header.addView(demo); root.addView(header);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        body = column(); body.setPadding(dp(20), dp(8), dp(20), dp(20)); scroll.addView(body);
        if (!heading.isEmpty()) title(heading);
        if (navigation) {
            LinearLayout nav = new LinearLayout(this); nav.setBackgroundColor(Color.WHITE);
            for (String tab : new String[]{"Home", "Report", "Contacts", "Profile"}) {
                MaterialButton b = new MaterialButton(this);
                b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
                b.setTextColor(navy); b.setElevation(0); b.setStrokeWidth(0);
                b.setText(tab); b.setTextSize(12); b.setAllCaps(false); b.setPadding(0, 0, 0, 0);
                nav.addView(b, new LinearLayout.LayoutParams(0, dp(56), 1));
                b.setOnClickListener(v -> {
                    switch (tab) {
                        case "Home": showHome(); break;
                        case "Report": report(false); break;
                        case "Contacts": contacts(); break;
                        default: profile();
                    }
                });
            }
            root.addView(nav);
        }
    }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_SHORT).show(); }
    private String date(long time) { return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(time); }
    private JSONObject walk() { return store.data.optJSONObject("walk"); }
    private boolean walking() { return walk() != null && walk().optString("status").equals("active"); }
    private double[][] route() { return DemoStore.route(walk().optInt("route"), walk().optDouble("dx"), walk().optDouble("dy")); }
    private void back() { button(body, "Back", false, this::showHome); }

    private void login() {
        screen = "role_selection"; shell(getString(R.string.choose_role), false); logo(body);
        text(getString(R.string.choose_role_description));
        LinearLayout selection = card(getString(R.string.role_dropdown_label), "");
        Spinner choices = spinner(selection, new String[]{getString(R.string.select_role),
                getString(R.string.role_resident), getString(R.string.role_guard), getString(R.string.role_management)});
        choices.setContentDescription(getString(R.string.select_role));
        MaterialButton next = button(selection, getString(R.string.continue_label), true, this::signInForm);
        next.setEnabled(selectedRoleIndex > 0);
        choices.setSelection(selectedRoleIndex);
        choices.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                selectedRoleIndex = position;
                if (position > 0) loginRole = new String[]{"resident", "guard", "management"}[position - 1];
                next.setEnabled(position > 0);
                next.setAlpha(position > 0 ? 1f : .5f);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { next.setEnabled(false); }
        });
    }

    private String loginRoleName() {
        return getString(loginRole.equals("guard") ? R.string.role_guard
                : loginRole.equals("management") ? R.string.role_management : R.string.role_resident);
    }

    private void signInForm() {
        screen = "login"; shell(getString(R.string.sign_in_title), false); logo(body);
        TextView roleLabel = text(getString(R.string.selected_role_label, loginRoleName()));
        roleLabel.setTextColor(navy); roleLabel.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        text(getString(R.string.demo_credentials_note));
        TextInputEditText email = field(body, getString(R.string.email_label), loginRole + "@demo.com",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        TextInputEditText password = field(body, getString(R.string.password_label), "Demo1234",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        button(body, getString(R.string.sign_in_title), true, () -> {
            if (!Patterns.EMAIL_ADDRESS.matcher(value(email)).matches()) { email.setError(getString(R.string.invalid_email_message)); return; }
            if (!value(password).equals("Demo1234")) { password.setError(getString(R.string.demo_password_message)); return; }
            String address = value(email).toLowerCase(Locale.ROOT);
            boolean matchingAccount = address.equals(loginRole + "@demo.com") ||
                    (loginRole.equals("resident") && address.equalsIgnoreCase(store.profile().optString("email"))
                            && !address.equals("guard@demo.com") && !address.equals("management@demo.com"));
            if (!matchingAccount) {
                email.setError(getString(R.string.selected_role_email_error, loginRoleName())); return;
            }
            role = loginRole;
            DemoStore.put(store.data, "sessionRole", role); store.save(); showHome();
        });
        button(body, getString(R.string.forgot_password_label), false, () -> new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.demo_sign_in_title).setMessage(R.string.demo_reset_message)
                .setPositiveButton(R.string.close_label, null).show());
        if (loginRole.equals("resident")) {
            String prompt = getString(R.string.registration_prompt);
            android.text.SpannableString styledPrompt = new android.text.SpannableString(prompt);
            int linkStart = prompt.indexOf(getString(R.string.resident_register));
            styledPrompt.setSpan(new android.text.style.ForegroundColorSpan(red), linkStart,
                    prompt.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            MaterialButton registerLink = button(body, prompt, false, this::register);
            registerLink.setStrokeWidth(0);
            registerLink.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.TRANSPARENT));
            registerLink.setText(styledPrompt);
        }
        button(body, getString(R.string.change_role), false, this::login);
    }

    private void register() {
        screen = "register"; shell("Create account", false); logo(body);
        text("This creates the resident profile on this device. Demo password: Demo1234.");
        TextInputEditText name = field(body, "Full name", "", InputType.TYPE_CLASS_TEXT);
        TextInputEditText email = field(body, "Email", "", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        TextInputEditText phone = field(body, "Phone number", "+60", InputType.TYPE_CLASS_PHONE);
        Spinner residence = spinner(body, new String[]{"Condominium", "Landed"});
        TextInputEditText township = field(body, "Township", "City of Elmina", InputType.TYPE_CLASS_TEXT);
        TextInputEditText block = field(body, "Block / house number", "A", InputType.TYPE_CLASS_TEXT);
        TextInputEditText level = field(body, "Level / street", "5", InputType.TYPE_CLASS_TEXT);
        TextInputEditText unit = field(body, "Unit (condominium)", "05-08", InputType.TYPE_CLASS_TEXT);
        button(body, "Create account", true, () -> {
            if (value(name).isEmpty()) { name.setError("Enter your name."); return; }
            if (!Patterns.EMAIL_ADDRESS.matcher(value(email)).matches()) { email.setError("Enter a valid email."); return; }
            if (!value(phone).matches("\\+[1-9][0-9]{7,14}")) { phone.setError("Include country code, for example +60123456789."); return; }
            JSONObject p = store.profile(); DemoStore.put(p, "name", value(name)); DemoStore.put(p, "email", value(email));
            DemoStore.put(p, "phone", value(phone)); DemoStore.put(p, "township", value(township));
            DemoStore.put(p, "residence", residence.getSelectedItem().toString());
            DemoStore.put(p, "block", value(block)); DemoStore.put(p, "house", value(block));
            DemoStore.put(p, "level", value(level)); DemoStore.put(p, "street", value(level)); DemoStore.put(p, "unit", value(unit));
            role = "resident"; DemoStore.put(store.data, "sessionRole", role); store.save(); permissions();
        });
        button(body, getString(R.string.back_to_sign_in), false, this::signInForm);
    }
    private void permissions() {
        screen = "permissions"; shell("Location and alerts", false);
        card("Location sharing", "The demo uses a simulated position. It does not access your real location.");
        card("Notifications", "Walk, hazard and SOS updates are shown inside this app. No notification permission is required for this offline demo.");
        button(body, "Continue", true, this::showHome);
    }
    private void showHome() {
        if (role.isEmpty()) { login(); return; }
        if (role.equals("guard")) { guard(); return; }
        if (role.equals("management")) { management(); return; }
        screen = "home"; shell(getString(R.string.resident_home_title), true);
        text(store.profile().optString("township") + "  /  " + store.hazards().size() + " active reports");
        TownshipMapView map = map(290); map.walker = walking() ? DemoStore.position(route(), walk().optDouble("progress")) : new double[]{.18, .82};
        map.listener = (x, y) -> {
            for (JSONObject h : store.hazards()) if (Math.hypot(x - h.optDouble("x"), y - h.optDouble("y")) < .08) { hazardDetails(h, false); return; }
        };
        text("Simulated township map. Tap a hazard marker for details.");
        button(body, walking() ? "Resume walk" : "Start walk", true, () -> { if (walking()) activeWalk(); else chooseRoute(); });
        button(body, "Send SOS", false, this::sos);
        button(body, "My reports", false, this::reports);
        if (walk() != null) button(body, "Preview trusted contact tracking", false, this::tracking);
        button(body, "Switch demo account", false, this::login);
    }
    private TownshipMapView map(int height) {
        TownshipMapView view = new TownshipMapView(this, store.hazards());
        body.addView(view, new LinearLayout.LayoutParams(-1, dp(height))); return view;
    }

    private void chooseRoute() {
        screen = "routes"; shell("Start walk", true);
        text("Choose a destination, then select a trusted contact.");
        TownshipMapView map = map(230); map.selected = new double[]{destinationX, destinationY};
        map.route = DemoStore.route(selectedRoute, destinationX, destinationY);
        map.listener = (x, y) -> { destinationX = x; destinationY = y; destinationName = "Selected map location"; chooseRoute(); };
        text("Simulated map. Tap to set a destination.");
        TextInputEditText search = field(body, "Search destination", destinationName, InputType.TYPE_CLASS_TEXT);
        button(body, "Search", false, () -> new MaterialAlertDialogBuilder(this).setTitle("Demo destinations")
                .setItems(new String[]{"Community centre", "North entrance", "Lake pavilion"}, (dialog, which) -> {
                    destinationName = new String[]{"Community centre", "North entrance", "Lake pavilion"}[which];
                    destinationX = new double[]{.82, .50, .82}[which]; destinationY = new double[]{.20, .08, .50}[which]; chooseRoute();
                }).show());
        List<JSONObject> contacts = store.list("contacts");
        if (contacts.isEmpty()) { text("Add a trusted contact before starting a walk."); button(body, "Add contact", true, this::contacts); return; }
        String[] names = new String[contacts.size()];
        for (int i = 0; i < names.length; i++) names[i] = contacts.get(i).optString("name");
        text("Trusted contact"); Spinner contact = spinner(body, names);
        int best = store.risk(0, destinationX, destinationY) < store.risk(1, destinationX, destinationY) ? 0 : 1;
        for (int i = 0; i < 2; i++) {
            final int index = i; int count = store.risk(i, destinationX, destinationY);
            LinearLayout option = card("Route " + (i + 1) + (i == best ? " · Recommended" : ""), count + " reported hazards on this route");
            button(option, selectedRoute == i ? "Selected" : "Select route", selectedRoute == i, () -> { selectedRoute = index; chooseRoute(); });
        }
        text("Routes are simulated. The recommendation counts active reports within 30 metres of each demo route.");
        button(body, "Start walk", true, () -> {
            JSONObject c = contacts.get(contact.getSelectedItemPosition());
            DemoStore.put(store.data, "walk", DemoStore.obj("status", "active", "route", selectedRoute,
                    "dx", destinationX, "dy", destinationY, "destination", destinationName,
                    "contact", c.optString("name"), "progress", 0.0));
            paused = false; automaticAlertSent = false; lastMovement = SystemClock.elapsedRealtime(); store.save(); activeWalk();
        });
    }

    private void activeWalk() {
        screen = "walk"; shell("Walk in progress", false);
        if (walk() == null) { back(); return; }
        text(walk().optString("destination") + "  /  Shared with " + walk().optString("contact"));
        liveMap = map(280); liveMap.route = route();
        liveMap.walker = DemoStore.position(route(), walk().optDouble("progress"));
        progressStatus = text(""); lightStatus = text(""); liveStatus = text("Simulated live tracking. No real location is collected.");
        button(body, "End walk", true, () -> { endWalk(); showHome(); toast("Walk ended. Demo location sharing stopped."); });
        button(body, "Send SOS", false, this::sos);
        button(body, "Preview trusted contact tracking", false, this::tracking);
        LinearLayout controls = card("Demo controls", "The walker moves while this app is open. These controls let you demonstrate check-ins without waiting two minutes.");
        button(controls, "Test check-in", false, this::checkIn);
        button(controls, paused ? "Resume walker" : "Pause walker", false, () -> { paused = !paused; lastMovement = SystemClock.elapsedRealtime(); activeWalk(); });
        button(controls, "Switch demo account", false, this::login);
        updateLiveViews();
    }
    private void endWalk() {
        if (walk() != null) DemoStore.put(walk(), "status", "ended");
        DemoStore.put(store.data, "light", false); paused = false; checkInDeadline = 0;
        if (checkInDialog != null) checkInDialog.dismiss(); store.save();
    }
    private void updateSimulation() {
        if (walking()) {
            if (!paused) {
                double progress = Math.min(1, walk().optDouble("progress") + .006);
                DemoStore.put(walk(), "progress", progress); lastMovement = SystemClock.elapsedRealtime();
                if (progress == 1) { paused = true; toast("Destination reached. End the walk to stop sharing."); }
            } else if (!automaticAlertSent && checkInDeadline == 0 && SystemClock.elapsedRealtime() - lastMovement > 120000) checkIn();
            double[] position = DemoStore.position(route(), walk().optDouble("progress"));
            DemoStore.put(store.data, "light", Math.hypot(position[0] - .18, position[1] - .35) <= .04);
            for (JSONObject alert : store.list("alerts")) if (!alert.optString("status").equals("resolved")) {
                DemoStore.put(alert, "x", position[0]); DemoStore.put(alert, "y", position[1]);
            }
            store.save();
        }
        if (checkInDeadline != 0) {
            int remaining = (int) Math.max(0, Math.ceil((checkInDeadline - SystemClock.elapsedRealtime()) / 1000.0));
            if (checkInText != null) checkInText.setText("An automatic demo SOS will be created in " + remaining + " seconds.");
            if (remaining == 0) {
                checkInDeadline = 0; automaticAlertSent = true;
                if (checkInDialog != null) checkInDialog.dismiss();
                JSONObject alert = createAlert(true, store.profile().optString("block"), store.profile().optString("level"), "P2");
                selectedAlert = alert.optString("id"); sosStatus();
            }
        }
        updateLiveViews();
    }
    private void updateLiveViews() {
        if (liveMap != null && walk() != null) {
            if (screen.equals("guardAlert")) {
                JSONObject alert = store.find("alerts", selectedAlert);
                liveMap.walker = alert != null && alert.has("x") ? new double[]{alert.optDouble("x"), alert.optDouble("y")} : null;
            } else if (walking()) liveMap.walker = DemoStore.position(route(), walk().optDouble("progress")); else liveMap.walker = null;
            liveMap.invalidate();
        }
        if (progressStatus != null && walk() != null) progressStatus.setText((int) (walk().optDouble("progress") * 100) + "% of demo route · " + walk().optString("status"));
        if (lightStatus != null) {
            boolean on = walking() && store.data.optBoolean("light");
            lightStatus.setText("Demo smart light: " + (on ? "On" : "Off") + " · 40 m trigger"); lightStatus.setTextColor(on ? getColor(R.color.brand_orange) : navy);
        }
        if (liveStatus != null && walking() && screen.equals("walk")) {
            double[] p = DemoStore.position(route(), walk().optDouble("progress"));
            String message = "Simulated live tracking. No real location is collected.";
            for (JSONObject h : store.hazards()) if (Math.hypot(p[0] - h.optDouble("x"), p[1] - h.optDouble("y")) < .05) message = "Nearby: " + h.optString("type");
            liveStatus.setText(message);
        }
    }
    private void checkIn() {
        if (!walking() || checkInDeadline != 0) return;
        checkInDeadline = SystemClock.elapsedRealtime() + 30000;
        checkInText = label("An automatic demo SOS will be created in 30 seconds.", 16, false);
        checkInText.setPadding(dp(24), dp(16), dp(24), dp(16));
        checkInDialog = new MaterialAlertDialogBuilder(this).setTitle("Are you okay?").setView(checkInText)
                .setCancelable(false).setPositiveButton("I am okay", (d, w) -> {
                    checkInDeadline = 0; lastMovement = SystemClock.elapsedRealtime(); toast("Check-in received.");
                }).show();
    }
    private void tracking() {
        screen = "tracking"; shell("Trusted contact tracking", false);
        if (walk() == null) { text("No demo walk has started."); back(); return; }
        text(store.profile().optString("name") + " → " + walk().optString("destination"));
        liveMap = map(300); if (walking()) liveMap.route = route();
        progressStatus = text(walk().optString("status")); lightStatus = text("");
        text("Contact preview on this device. This is not remote tracking.");
        button(body, "Call resident", false, () -> toast("Demo call: " + store.profile().optString("phone")));
        button(body, "Back", false, () -> { if (walking() && role.equals("resident")) activeWalk(); else showHome(); }); updateLiveViews();
    }

    private JSONObject createAlert(boolean automatic, String block, String level, String zone) {
        for (JSONObject existing : store.list("alerts")) if (!existing.optString("status").equals("resolved")) return existing;
        double[] p = walking() ? DemoStore.position(route(), walk().optDouble("progress")) : new double[]{.18, .82};
        JSONObject profile = store.profile();
        JSONObject alert = DemoStore.obj("id", UUID.randomUUID().toString(), "name", profile.optString("name"),
                "phone", profile.optString("phone"), "residence", profile.optString("residence"),
                "block", block, "level", level, "unit", profile.optString("unit"), "zone", zone,
                "house", profile.optString("house"), "street", profile.optString("street"),
                "status", "active", "created", System.currentTimeMillis(), "automatic", automatic,
                "x", p[0], "y", p[1]); store.add("alerts", alert); return alert;
    }
    private void sos() {
        screen = "sos"; shell("Confirm your location", false);
        text("Demo SOS stays on this device. No guards, contacts or emergency services are contacted.");
        JSONObject p = store.profile();
        TextInputEditText block = field(body, "Block", p.optString("block"), InputType.TYPE_CLASS_TEXT);
        TextInputEditText level = field(body, "Level", p.optString("level"), InputType.TYPE_CLASS_TEXT);
        TextInputEditText zone = field(body, "Car park zone", "P2", InputType.TYPE_CLASS_TEXT);
        text("Press and hold for two seconds, then a five-second cancel countdown begins.");
        TextView countdown = text("");
        MaterialButton send = button(body, "Hold to send SOS", true, () -> {});
        Runnable beginCountdown = () -> {
            if (!foreground || sosArmed || !screen.equals("sos")) return;
            sosArmed = true; sosDeadline = SystemClock.elapsedRealtime() + 5000;
            send.setEnabled(false);
            Runnable update = new Runnable() {
                @Override public void run() {
                    if (!sosArmed || !screen.equals("sos")) return;
                    int remaining = (int) Math.ceil((sosDeadline - SystemClock.elapsedRealtime()) / 1000.0);
                    countdown.setText("Sending demo SOS in " + Math.max(0, remaining) + " seconds.");
                    if (remaining <= 0) {
                        sosArmed = false; JSONObject alert = createAlert(false, value(block), value(level), value(zone));
                        selectedAlert = alert.optString("id"); sosStatus();
                    } else handler.postDelayed(this, 100);
                }
            };
            handler.post(update);
        };
        send.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) { handler.postDelayed(beginCountdown, 2000); view.setPressed(true); return true; }
            if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                handler.removeCallbacks(beginCountdown); view.setPressed(false); return true;
            }
            return true;
        });
        send.setOnClickListener(v -> handler.postDelayed(beginCountdown, 2000));
        button(body, "Cancel SOS", false, () -> { handler.removeCallbacks(beginCountdown); cancelSos(); if (walking()) activeWalk(); else showHome(); });
    }
    private void cancelSos() { sosArmed = false; sosDeadline = 0; }
    private void sosStatus() {
        screen = "sosStatus"; shell("SOS status", false);
        JSONObject alert = store.find("alerts", selectedAlert);
        if (alert == null) { text("No demo alert selected."); back(); return; }
        TextView status = text(alert.optString("status").toUpperCase(Locale.ROOT)); status.setTextColor(alert.optString("status").equals("resolved") ? green : red); status.setTextSize(24);
        text("Sent " + date(alert.optLong("created")));
        text("Location: Block " + alert.optString("block") + " · Level " + alert.optString("level") + " · Zone " + alert.optString("zone"));
        card("Demo handover", "Switch to the Guard demo account to acknowledge, mark on the way and resolve this alert. Data is shared locally on this emulator.");
        button(body, "Open guard demo", true, () -> { role = "guard"; DemoStore.put(store.data, "sessionRole", role); store.save(); guardAlert(); });
        back();
    }

    private void report(boolean management) {
        screen = "report"; shell(management ? "Add known works" : "Report a hazard", !management);
        Spinner type = spinner(body, DemoStore.TYPES); if (management) type.setSelection(3);
        text("Tap the simulated map to set the report pin.");
        TownshipMapView map = map(220); final double[] pin = {.42, .60}; map.selected = pin;
        map.listener = (x, y) -> { pin[0] = x; pin[1] = y; map.invalidate(); };
        TextInputEditText description = field(body, "Description, optional", "", InputType.TYPE_CLASS_TEXT);
        TextInputEditText fix = field(body, "Suggested fix, optional", "", InputType.TYPE_CLASS_TEXT);
        final long[] end = {0}; final TextView date = text("End date required for debris and road closures.");
        button(body, "Choose end date", false, () -> {
            Calendar calendar = Calendar.getInstance(); DatePickerDialog picker = new DatePickerDialog(this, (v, year, month, day) -> {
                Calendar chosen = Calendar.getInstance(); chosen.set(year, month, day, 23, 59, 59);
                chosen.set(Calendar.MILLISECOND, 999); end[0] = chosen.getTimeInMillis(); date.setText("Ends " + date(end[0]));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            picker.getDatePicker().setMinDate(System.currentTimeMillis() - 1000); picker.show();
        });
        button(body, "Submit report", true, () -> {
            int selection = type.getSelectedItemPosition();
            if ((selection == 2 || selection == 3) && end[0] <= System.currentTimeMillis()) { toast("Choose an end date for this hazard."); return; }
            store.hazard(type.getSelectedItem().toString(), value(description), value(fix), pin[0], pin[1], end[0], role);
            showHome(); toast("Hazard reported.");
        });
        if (management) back();
    }
    private void hazardDetails(JSONObject hazard, boolean management) {
        String detail = hazard.optString("description") + "\n\nSuggested fix: " + hazard.optString("fix")
                + "\nReported: " + date(hazard.optLong("created")) + "\nStatus: " + hazard.optString("status")
                + (hazard.optLong("end") > 0 ? "\nEnds: " + date(hazard.optLong("end")) : "");
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this).setTitle(hazard.optString("type")).setMessage(detail).setNegativeButton("Close", null);
        if (management) dialog.setPositiveButton("Change status", (d, w) -> new MaterialAlertDialogBuilder(this)
                .setTitle("Hazard status").setItems(new String[]{"open", "in progress", "resolved"}, (x, index) -> {
                    DemoStore.put(hazard, "status", new String[]{"open", "in progress", "resolved"}[index]); store.save(); management();
                }).show());
        dialog.show();
    }
    private void reports() {
        screen = "reports"; shell("My reports", true);
        for (JSONObject h : store.list("hazards")) if (h.optString("reporter").equals("resident")) {
            LinearLayout item = card(h.optString("type"), date(h.optLong("created")) + " · " + h.optString("status"));
            button(item, "Open on map", false, () -> {
                shell(h.optString("type"), true); TownshipMapView map = map(300);
                map.selected = new double[]{h.optDouble("x"), h.optDouble("y")}; text(h.optString("description"));
                button(body, "Report details", false, () -> hazardDetails(h, false)); button(body, "Back to my reports", false, this::reports);
            });
        }
    }

    private void contacts() {
        screen = "contacts"; shell("Trusted contacts", true);
        text(getString(R.string.registered_demo_numbers));
        for (JSONObject c : store.list("contacts")) {
            LinearLayout item = card(c.optString("name"), c.optString("phone"));
            button(item, "Edit", false, () -> editContact(c));
            button(item, "Remove", false, () -> new MaterialAlertDialogBuilder(this).setTitle("Remove contact?")
                    .setMessage(c.optString("name")).setNegativeButton("Cancel", null).setPositiveButton("Remove", (d, w) -> { store.remove("contacts", c.optString("id")); contacts(); }).show());
        }
        button(body, "Add contact", true, () -> editContact(null));
    }
    private void editContact(JSONObject contact) {
        LinearLayout form = column(); form.setPadding(dp(24), dp(8), dp(24), 0);
        EditText name = field(form, "Name", contact == null ? "" : contact.optString("name"), InputType.TYPE_CLASS_TEXT);
        EditText phone = field(form, "Phone", contact == null ? "+60" : contact.optString("phone"), InputType.TYPE_CLASS_PHONE);
        AlertDialog dialog = new MaterialAlertDialogBuilder(this).setTitle(contact == null ? "Add contact" : "Edit contact")
                .setView(form).setNegativeButton("Cancel", null).setPositiveButton("Save", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String number = value(phone).replace(" ", "").replace("-", "");
            if (value(name).isEmpty()) { name.setError("Enter a name."); return; }
            if (!number.equals("+60129876543") && !number.equals("+60121112222")) { phone.setError("Use a registered demo number shown on the contacts screen."); return; }
            for (JSONObject c : store.list("contacts")) if (c != contact && c.optString("phone").equals(number)) { phone.setError("This contact already exists."); return; }
            if (contact == null) store.add("contacts", DemoStore.obj("id", UUID.randomUUID().toString(), "name", value(name), "phone", number));
            else { DemoStore.put(contact, "name", value(name)); DemoStore.put(contact, "phone", number); store.save(); }
            dialog.dismiss(); contacts();
        })); dialog.show();
    }
    private void profile() {
        screen = "profile"; shell("Profile", role.equals("resident")); JSONObject p = store.profile();
        text("Current demo role: " + role);
        EditText name = field(body, "Resident name", p.optString("name"), InputType.TYPE_CLASS_TEXT);
        EditText phone = field(body, "Phone", p.optString("phone"), InputType.TYPE_CLASS_PHONE);
        EditText township = field(body, "Township", p.optString("township"), InputType.TYPE_CLASS_TEXT);
        Spinner residence = spinner(body, new String[]{"Condominium", "Landed"}); residence.setSelection(p.optString("residence").equals("Landed") ? 1 : 0);
        EditText block = field(body, "Block / house number", p.optString("block"), InputType.TYPE_CLASS_TEXT);
        EditText level = field(body, "Level / street", p.optString("level"), InputType.TYPE_CLASS_TEXT);
        EditText unit = field(body, "Unit", p.optString("unit"), InputType.TYPE_CLASS_TEXT);
        SwitchMaterial notifications = new SwitchMaterial(this); notifications.setText("Demo notification updates"); notifications.setChecked(store.data.optBoolean("notifications", true)); body.addView(notifications);
        text("The demo uses simulated positions. No real location or contact information is sent from this device.");
        button(body, "Save changes", true, () -> {
            if (value(name).isEmpty()) { name.setError("Enter a name."); return; }
            DemoStore.put(p, "name", value(name)); DemoStore.put(p, "phone", value(phone)); DemoStore.put(p, "township", value(township));
            DemoStore.put(p, "residence", residence.getSelectedItem().toString()); DemoStore.put(p, "block", value(block)); DemoStore.put(p, "house", value(block));
            DemoStore.put(p, "level", value(level)); DemoStore.put(p, "street", value(level)); DemoStore.put(p, "unit", value(unit));
            DemoStore.put(store.data, "notifications", notifications.isChecked()); store.save(); toast("Profile saved.");
        });
        button(body, "Switch demo account", false, this::login);
        button(body, "Sign out", false, () -> { endWalk(); role = ""; DemoStore.put(store.data, "sessionRole", ""); store.save(); login(); });
        button(body, "Reset demo data", false, () -> new MaterialAlertDialogBuilder(this).setTitle("Reset demo data?")
                .setMessage("This removes local demo reports, contacts and alerts and restores the sample data.")
                .setNegativeButton("Cancel", null).setPositiveButton("Reset", (d, w) -> { endWalk(); store.reset(); role = ""; login(); }).show());
        if (!role.equals("resident")) back();
    }

    private void guard() {
        screen = "guard"; shell("Guard desk", false);
        text("SOS alerts · active first");
        List<JSONObject> alerts = store.list("alerts");
        alerts.sort(Comparator.comparingInt((JSONObject a) -> a.optString("status").equals("active") ? 0 : a.optString("status").equals("resolved") ? 2 : 1)
                .thenComparing(Comparator.comparingLong((JSONObject a) -> a.optLong("created")).reversed()));
        if (alerts.isEmpty()) card("No SOS alerts", "Create an SOS from the Resident demo account, then return here.");
        for (JSONObject a : alerts) {
            LinearLayout item = card(a.optString("name"), a.optString("status").toUpperCase(Locale.ROOT) + " · " + date(a.optLong("created")));
            button(item, "Open alert", true, () -> { selectedAlert = a.optString("id"); guardAlert(); });
        }
        button(body, "Open hazards", false, () -> {
            shell("Open hazards", false);
            for (JSONObject h : store.hazards()) button(body, h.optString("type"), false, () -> hazardDetails(h, false)); back();
        });
        button(body, "Profile", false, this::profile); button(body, "Switch demo account", false, this::login);
    }
    private void guardAlert() {
        screen = "guardAlert"; shell("SOS details", false); JSONObject alert = store.find("alerts", selectedAlert);
        if (alert == null) { back(); return; }
        title(alert.optString("name")); text(alert.optString("phone")); text(alert.optString("status").toUpperCase(Locale.ROOT));
        text(alert.optString("residence") + " · Block " + alert.optString("block") + " · Level " + alert.optString("level")
                + " · Unit " + alert.optString("unit") + " · Zone " + alert.optString("zone"));
        if (!alert.optString("status").equals("resolved")) {
            liveMap = map(260); liveMap.walker = new double[]{alert.optDouble("x"), alert.optDouble("y")};
            String status = alert.optString("status");
            String next = status.equals("active") ? "acknowledged" : status.equals("acknowledged") ? "on the way" : "resolved";
            button(body, status.equals("active") ? "Acknowledge" : "Mark " + next, true, () -> {
                DemoStore.put(alert, "status", next); DemoStore.put(alert, "updated", System.currentTimeMillis());
                if (next.equals("acknowledged")) DemoStore.put(alert, "acknowledged", System.currentTimeMillis());
                if (next.equals("resolved")) { alert.remove("x"); alert.remove("y"); }
                store.save(); guardAlert();
            });
        } else text("Resolved. SOS location sharing stopped.");
        button(body, "Call resident", false, () -> toast("Demo call: " + alert.optString("phone")));
        button(body, "View resident status", false, this::sosStatus); back();
    }
    private void management() {
        screen = "management"; shell("Township overview", false);
        int active = 0; long total = 0; int acknowledged = 0;
        for (JSONObject alert : store.list("alerts")) {
            if (!alert.optString("status").equals("resolved")) active++;
            if (alert.has("acknowledged")) { total += alert.optLong("acknowledged") - alert.optLong("created"); acknowledged++; }
        }
        card("Open hazards", Integer.toString(store.hazards().size()));
        card("Active SOS alerts", Integer.toString(active));
        card("Average acknowledgement", acknowledged == 0 ? "No acknowledged alerts yet" : (total / acknowledged / 1000) + " seconds");
        TownshipMapView map = map(240); map.clusters = true;
        map.listener = (x, y) -> { for (JSONObject h : store.hazards()) if (Math.hypot(x - h.optDouble("x"), y - h.optDouble("y")) < .09) { hazardDetails(h, true); return; } };
        text("Simulated hazard map. Tap a numbered marker to change its status.");
        button(body, "Add known works", true, () -> report(true));
        body.addView(label("Reported hotspots", 20, true));
        int index = 1;
        for (JSONObject group : DemoStore.clusters(store.hazards())) {
            LinearLayout row = card("Area " + index++, group.optInt("count") + " reports");
            button(row, "Review report", false, () -> {
                List<JSONObject> nearby = new ArrayList<>();
                for (JSONObject h : store.hazards()) if ((int) (h.optDouble("x") * 8) == (int) (group.optDouble("x") * 8)
                        && (int) (h.optDouble("y") * 8) == (int) (group.optDouble("y") * 8)) nearby.add(h);
                String[] options = new String[nearby.size()];
                for (int i = 0; i < options.length; i++) options[i] = nearby.get(i).optString("type");
                new MaterialAlertDialogBuilder(this).setTitle("Area reports").setItems(options,
                        (dialog, which) -> hazardDetails(nearby.get(which), true)).setNegativeButton("Close", null).show();
            });
        }
        button(body, "Profile", false, this::profile); button(body, "Switch demo account", false, this::login);
    }
}
