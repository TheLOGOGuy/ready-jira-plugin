package com.smartbear.ready.plugin.jira.factories;

import com.eviware.soapui.actions.Prefs;
import com.eviware.soapui.model.settings.Settings;
import com.eviware.soapui.plugins.auto.PluginPrefs;
import com.eviware.soapui.support.components.SimpleForm;
import com.eviware.soapui.support.types.StringToStringMap;
import com.smartbear.ready.plugin.jira.impl.JiraProvider;
import com.smartbear.ready.plugin.jira.settings.BugTrackerPrefs;

import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

@PluginPrefs
public class JiraPrefsFactory implements Prefs {
    public static final String BUG_TRACKER_LOGIN = "Username:";
    public static final String BUG_TRACKER_PASSWORD = "Password:";
    public static final String BUG_TRACKER_LOGIN_DESCRIPTION = "Your JIRA user account (not an email)";
    public static final String BUG_TRACKER_LOGIN_IN_FIELD_DESCRIPTION = "Your JIRA user account (not an email)";
    public static final String BUG_TRACKER_PASSWORD_DESCRIPTION = "The password for logging in";
    public static final String BUG_TRACKER_URL = "JIRA server URL:";
    public static final String BUG_TRACKER_URL_DESCRIPTION = "The URL of your JIRA instance, for instance, https://mycompany.atlassian.net";
    public static final String BUG_TRACKER_URL_IN_FIELD_DESCRIPTION = "The URL of your JIRA instance (https://...)";
    public static final String JIRA_PREFS_TITLE = "JIRA";

    private SimpleForm form;

    private class BugTrackerSettingsChangeListener implements DocumentListener{

        @Override
        public void insertUpdate(DocumentEvent e) {
            JiraProvider.freeProvider();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            JiraProvider.freeProvider();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            JiraProvider.freeProvider();
        }
    }

    private class BugTrackerUrlFieldFocusListener implements FocusListener {

        @Override
        public void focusGained(FocusEvent e) {
            JTextField bugTrackerUrl = (JTextField)e.getSource();
            if (bugTrackerUrl.getText() != null &&
                    bugTrackerUrl.getText().equals(BUG_TRACKER_URL_IN_FIELD_DESCRIPTION)) {
                bugTrackerUrl.setText("");
            }
        }

        @Override
        public void focusLost(FocusEvent e) {
            JTextField bugTrackerUrl = (JTextField)e.getSource();
            if (bugTrackerUrl.getText() != null &&
                    bugTrackerUrl.getText().equals("")) {
                bugTrackerUrl.setText(BUG_TRACKER_URL_IN_FIELD_DESCRIPTION);
            }
        }
    }

    private class BugTrackerLoginFieldFocusListener implements FocusListener {

        @Override
        public void focusGained(FocusEvent e) {
            JTextField loginField = (JTextField)e.getSource();
            if (loginField.getText() != null &&
                    loginField.getText().equals(BUG_TRACKER_LOGIN_IN_FIELD_DESCRIPTION)) {
                loginField.setText("");
            }
        }

        @Override
        public void focusLost(FocusEvent e) {
            JTextField loginField = (JTextField)e.getSource();
            if (loginField.getText() != null &&
                    loginField.getText().equals("")) {
                loginField.setText(BUG_TRACKER_LOGIN_IN_FIELD_DESCRIPTION);
            }
        }
    }

    @Override
    public SimpleForm getForm() {
        if (form == null) {
            form = new SimpleForm();
            form.addSpace();
            JTextField loginField = form.appendTextField(BUG_TRACKER_LOGIN, BUG_TRACKER_LOGIN_DESCRIPTION);
            loginField.getDocument().addDocumentListener(new BugTrackerSettingsChangeListener());
            loginField.addFocusListener(new BugTrackerLoginFieldFocusListener());
            JPasswordField passwordField = form.appendPasswordField(BUG_TRACKER_PASSWORD, BUG_TRACKER_PASSWORD_DESCRIPTION);
            passwordField.getDocument().addDocumentListener(new BugTrackerSettingsChangeListener());
            JTextField bugTrackerUrl = form.appendTextField(BUG_TRACKER_URL, BUG_TRACKER_URL_DESCRIPTION);
            bugTrackerUrl.getDocument().addDocumentListener(new BugTrackerSettingsChangeListener());
            bugTrackerUrl.addFocusListener(new BugTrackerUrlFieldFocusListener());
        }

        return form;
    }

    @Override
    public void setFormValues(Settings settings) {
        getForm().setValues(getValues(settings));
    }

    @Override
    public void getFormValues(Settings settings) {
        StringToStringMap values = new StringToStringMap();
        form.getValues(values);
        storeValues(values, settings);
    }

    @Override
    public void storeValues(StringToStringMap values, Settings settings) {
        if (values.get(BUG_TRACKER_LOGIN) != null && values.get(BUG_TRACKER_LOGIN).equals(BUG_TRACKER_LOGIN_IN_FIELD_DESCRIPTION)) {
            settings.setString(BugTrackerPrefs.LOGIN, values.get(BUG_TRACKER_LOGIN));
        }
        settings.setString(BugTrackerPrefs.PASSWORD, values.get(BUG_TRACKER_PASSWORD));
        if (values.get(BUG_TRACKER_URL) != null && values.get(BUG_TRACKER_URL).equals(BUG_TRACKER_URL_IN_FIELD_DESCRIPTION)) {
            settings.setString(BugTrackerPrefs.DEFAULT_URL, values.get(BUG_TRACKER_URL));
        }
    }

    @Override
    public StringToStringMap getValues(Settings settings) {
        StringToStringMap values = new StringToStringMap();
        values.put(BUG_TRACKER_LOGIN, settings.getString(BugTrackerPrefs.LOGIN, BUG_TRACKER_LOGIN_IN_FIELD_DESCRIPTION));
        values.put(BUG_TRACKER_PASSWORD, settings.getString(BugTrackerPrefs.PASSWORD, ""));
        values.put(BUG_TRACKER_URL, settings.getString(BugTrackerPrefs.DEFAULT_URL, BUG_TRACKER_URL_IN_FIELD_DESCRIPTION));
        return values;
    }

    @Override
    public String getTitle() {
        return JIRA_PREFS_TITLE;
    }
}
