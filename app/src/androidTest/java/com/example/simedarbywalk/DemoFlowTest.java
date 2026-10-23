package com.example.simedarbywalk;

import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.material.button.MaterialButton;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class DemoFlowTest {
    @Test public void freshLaunchShowsLoginDespiteRememberedRoleAndCanOpenRegistration() {
        android.content.Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.getSharedPreferences("offline_demo_v1", 0).edit().clear().commit();
        DemoStore store = new DemoStore(context);
        DemoStore.put(store.data, "sessionRole", "management");
        store.save();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            await(scenario, "Choose your role");
            scenario.onActivity(activity -> assertNull(find(activity.getWindow().getDecorView(), "Township overview", false)));
            selectRole(scenario, "Resident");
            await(scenario, "Sign in");
            click(scenario, "No account? Register now");
            await(scenario, "Create account");
            scenario.recreate();
            await(scenario, "Create account");
            click(scenario, "Back to sign in");
            await(scenario, "Sign in");
        }
    }

    @Test public void residentWalkAndGuardHandoverWorkOffline() {
        InstrumentationRegistry.getInstrumentation().getTargetContext()
                .getSharedPreferences("offline_demo_v1", 0).edit().clear().commit();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            await(scenario, "Choose your role");
            selectRole(scenario, "Resident");
            click(scenario, "Sign in");
            await(scenario, "Start walk");
            click(scenario, "Start walk");
            click(scenario, "Start walk");
            await(scenario, "Walk in progress");
            click(scenario, "Test check-in");
            androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withText("Are you okay?"))
                    .inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog())
                    .check(androidx.test.espresso.assertion.ViewAssertions.matches(androidx.test.espresso.matcher.ViewMatchers.isDisplayed()));
            androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withText("I am okay"))
                    .inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog())
                    .perform(androidx.test.espresso.action.ViewActions.click());
            click(scenario, "Send SOS");
            click(scenario, "Hold to send SOS");
            await(scenario, "SOS status");
            click(scenario, "Open guard demo");
            click(scenario, "Acknowledge");
            click(scenario, "Mark on the way");
            click(scenario, "Mark resolved");
            click(scenario, "View resident status");
            await(scenario, "RESOLVED");
        }
    }

    @Test public void demoAccountsOpenTheirRoleScreens() {
        InstrumentationRegistry.getInstrumentation().getTargetContext()
                .getSharedPreferences("offline_demo_v1", 0).edit().clear().commit();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            await(scenario, "Choose your role"); selectRole(scenario, "Guard");
            scenario.onActivity(activity -> assertNull(find(activity.getWindow().getDecorView(), "No account? Register now", true)));
            scenario.recreate();
            await(scenario, "Guard access");
            click(scenario, "Sign in");
            await(scenario, "Guard desk"); click(scenario, "Switch demo account");
            selectRole(scenario, "Management");
            scenario.onActivity(activity -> assertNull(find(activity.getWindow().getDecorView(), "No account? Register now", true)));
            click(scenario, "Change role");
            await(scenario, "Choose your role");
            selectRole(scenario, "Management"); click(scenario, "Sign in");
            await(scenario, "Township overview");
        }
    }

    private void selectRole(ActivityScenario<MainActivity> scenario, String role) {
        scenario.onActivity(activity -> {
            android.widget.Spinner spinner = findSpinner(activity.getWindow().getDecorView());
            assertNotNull("Role dropdown missing", spinner);
            boolean selected = false;
            for (int i = 0; i < spinner.getCount(); i++) if (spinner.getItemAtPosition(i).equals(role)) {
                spinner.setSelection(i); selected = true; break;
            }
            assertTrue("Role missing: " + role, selected);
        });
        long deadline = SystemClock.elapsedRealtime() + 5000;
        AtomicBoolean enabled = new AtomicBoolean();
        while (SystemClock.elapsedRealtime() < deadline) {
            scenario.onActivity(activity -> {
                View next = find(activity.getWindow().getDecorView(), "Continue", true);
                enabled.set(next != null && next.isEnabled());
            });
            if (enabled.get()) break;
            SystemClock.sleep(50);
        }
        assertTrue("Continue must enable after choosing a role", enabled.get());
        click(scenario, "Continue");
    }

    private android.widget.Spinner findSpinner(View view) {
        if (view instanceof android.widget.Spinner) return (android.widget.Spinner) view;
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
            android.widget.Spinner spinner = findSpinner(((ViewGroup) view).getChildAt(i));
            if (spinner != null) return spinner;
        }
        return null;
    }

    private void click(ActivityScenario<MainActivity> scenario, String text) {
        scenario.onActivity(activity -> {
            View button = find(activity.getWindow().getDecorView(), text, true);
            assertNotNull("Button missing: " + text, button);
            assertTrue(button.performClick());
        });
    }

    private void await(ActivityScenario<MainActivity> scenario, String text) {
        long deadline = SystemClock.elapsedRealtime() + 12000;
        AtomicBoolean found = new AtomicBoolean();
        while (SystemClock.elapsedRealtime() < deadline) {
            scenario.onActivity(activity -> found.set(find(activity.getWindow().getDecorView(), text, false) != null));
            if (found.get()) return;
            SystemClock.sleep(50);
        }
        fail("Screen missing: " + text);
    }

    private View find(View view, String text, boolean buttonOnly) {
        if (view instanceof TextView && (!buttonOnly || view instanceof MaterialButton)
                && ((TextView) view).getText().toString().equals(text)) return view;
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
            View found = find(((ViewGroup) view).getChildAt(i), text, buttonOnly);
            if (found != null) return found;
        }
        return null;
    }
}
